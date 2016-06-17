package org.bpel4chor.mergechoreography.pattern.communication.sync;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.utils.WSUIDGenerator;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Variable;

public class SyncPattern11 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected MLEnvironment envReply;
	
	
	public SyncPattern11(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
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
		
		// First we uplift vRec used by rec and vReply used by s into
		// the process<scope> of the merged process
		Variable vRec = ChoreoMergeUtil.resolveVariable(rec.getVariable().getName(), rec);
		Variable vReply = ChoreoMergeUtil.resolveVariable(s.getOutputVariable().getName(), s);
		this.log.info("vRec => " + vRec);
		this.log.info("vReply => " + vReply);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vRec, this.pkg.getMergedProcess());
		ChoreoMergeUtil.upliftVariableToProcessScope(vReply, this.pkg.getMergedProcess());
		
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
		
		// Create new <link> in MergedFlow connection newAssignS and
		// newEmptyRec
		Link newLinkSend = BPELFactory.eINSTANCE.createLink();
		newLinkSend.setName(newAssignS.getName() + "TO" + newEmptyRec.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkSend);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkSend, newAssignS);
		
		// Combine joinCondition in newEmpty if needed
		ChoreoMergeUtil.combineJCWithLink(newEmptyRec, newLinkSend);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkSend, newEmptyRec);
		
		// Create new <link> in MergedFlow connection newAssignRepl and
		// newEmptyReply
		Link newLinkReply = BPELFactory.eINSTANCE.createLink();
		newLinkReply.setName(newAssignRepl.getName() + "TO" + (newEmptyReply.getName() != null ? newEmptyReply.getName() : "Empty" + WSUIDGenerator.getId()));
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkReply);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkReply, newAssignRepl);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkReply, newEmptyReply);
		
		// Replace old activities (s, rec, repl)
		ChoreoMergeUtil.replaceActivity(s, newSeq);
		ChoreoMergeUtil.replaceActivity(rec, newEmptyRec);
		ChoreoMergeUtil.replaceActivity(repl, newAssignRepl);
		
	}
}
