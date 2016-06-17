package org.bpel4chor.mergechoreography.pattern.communication.sync;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.utils.WSUIDGenerator;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.CompensationHandler;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Variable;

public class SyncPattern15 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected MLEnvironment envReply;
	
	
	public SyncPattern15(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
		super(envSend, pkg);
		this.envReply = envReply;
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive rec = (Receive) this.env.getR();
		Reply repl = (Reply) this.envReply.getS();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(rec);
		ChoreoMergeUtil.propagateCorrelInit(repl);
		
		// Get the FCTE-Handler which contain <invoke> s
		BPELExtensibleElement fcteHandler = ChoreoMergeUtil.getFCTEHandlerOfActivity(s);
		this.log.info("fcteHandler of <invoke> s is : " + fcteHandler);
		
		// First we uplift vRec used by rec and vReply used by s into
		// the process<scope> of the merged process
		Variable vRec = ChoreoMergeUtil.resolveVariable(rec.getVariable().getName(), rec);
		Variable vReply = ChoreoMergeUtil.resolveVariable(s.getOutputVariable().getName(), s);
		this.log.info("vRec => " + vRec);
		this.log.info("vReply => " + vReply);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vRec, this.pkg.getMergedProcess());
		ChoreoMergeUtil.upliftVariableToProcessScope(vReply, this.pkg.getMergedProcess());
		
		// Create new <flow> and move activity from fcte and <scope> containing
		// rec and repl inside this <flow>
		Flow newFlow = BPELFactory.eINSTANCE.createFlow();
		Activity fcteAct = ChoreoMergeUtil.getActivityFromFCTEHandler(fcteHandler);
		newFlow.getActivities().add(fcteAct);
		ChoreoMergeUtil.setActivityForFCTEHandler(fcteHandler, newFlow);
		Scope scopeRecRepl = ChoreoMergeUtil.getHighestScopeOfActivity(rec);
		newFlow.getActivities().add(scopeRecRepl);
		// Add new compensationHandler to scopeRecRepl with <empty> inside
		CompensationHandler ch = BPELFactory.eINSTANCE.createCompensationHandler();
		ch.setActivity(BPELFactory.eINSTANCE.createEmpty());
		scopeRecRepl.setCompensationHandler(ch);
		
		// Create new <assign> replacing the <invoke> s
		Assign newAssignS = ChoreoMergeUtil.createAssignFromSendAct(s, vRec);
		// Create new <sequence> surrounding newAssignS
		Sequence newSeq = BPELFactory.eINSTANCE.createSequence();
		// Set <targets> and <sources> newAssignS to newSeq
		newSeq.setSources(newAssignS.getSources());
		newSeq.setTargets(newAssignS.getTargets());
		// Create new <empty> after newAssignS for synchronization of the
		// responding link
		Empty newEmptyReply = BPELFactory.eINSTANCE.createEmpty();
		newSeq.getActivities().add(newAssignS);
		newSeq.getActivities().add(newEmptyReply);
		// Create new <empty> replacing <receive> rec
		Empty newEmptyRec = ChoreoMergeUtil.createEmptyFromActivity(rec);
		// Create new <assign> replacing the <reply> repl
		Assign newAssignRepl = ChoreoMergeUtil.createAssignFromSendAct(repl, vReply);
		
		// Create new <link> in newFlow connecting newAssignS and
		// newEmptyRec
		Link newLinkSend = BPELFactory.eINSTANCE.createLink();
		newLinkSend.setName(newAssignS.getName() + "TO" + newEmptyRec.getName());
		ChoreoMergeUtil.addLinkToFlow(newFlow, newLinkSend);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkSend, newAssignS);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkSend, newEmptyRec);
		
		// Create new <link> in new connecting newAssignRepl and
		// newEmptyReply
		Link newLinkReply = BPELFactory.eINSTANCE.createLink();
		newLinkReply.setName(newAssignRepl.getName() + "TO" + (newEmptyReply.getName() != null ? newEmptyReply.getName() : "Empty" + WSUIDGenerator.getId()));
		ChoreoMergeUtil.addLinkToFlow(newFlow, newLinkReply);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkReply, newAssignRepl);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkReply, newEmptyReply);
		
		// Replace old activities (s, rec, repl)
		ChoreoMergeUtil.replaceActivity(s, newSeq);
		ChoreoMergeUtil.replaceActivity(rec, newEmptyRec);
		ChoreoMergeUtil.replaceActivity(repl, newAssignRepl);
		
	}
}
