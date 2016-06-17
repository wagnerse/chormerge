package org.bpel4chor.mergechoreography.pattern.communication.sync;

import java.util.Arrays;

import javax.xml.namespace.QName;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.utils.WSUIDGenerator;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.Variable;

public class SyncPattern21 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected MLEnvironment envReply;
	
	/** {@link MessageLink} containing the send- and receiveActivity */
	private MessageLink mLinkSend;
	
	
	public SyncPattern21(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
		super(envSend, pkg);
		this.envReply = envReply;
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Pick rec = (Pick) this.env.getR();
		Reply repl = (Reply) this.envReply.getS();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(rec);
		ChoreoMergeUtil.propagateCorrelInit(repl);
		
		// We need the <onMessage> from the pick containing vRec
		OnMessage rOM = (OnMessage) ChoreoMergeUtil.resolveActivity(this.mLinkSend.getReceiveActivity());
		
		// First we uplift vRec used by rec and vReply used by s into
		// the process<scope> of the merged process
		Variable vRec = ChoreoMergeUtil.resolveVariable(rOM.getVariable().getName(), rec);
		Variable vReply = ChoreoMergeUtil.resolveVariable(s.getOutputVariable().getName(), s);
		this.log.info("vRec => " + vRec);
		this.log.info("vReply => " + vReply);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vRec, this.pkg.getMergedProcess());
		ChoreoMergeUtil.upliftVariableToProcessScope(vReply, this.pkg.getMergedProcess());
		
		// Now we create a new guard variable vR_activated in process<scope> of
		// the merged process and initialize it inline with false()
		Variable guard = ChoreoMergeUtil.createGuardVariableInMergedProcess(rec.getName() + WSUIDGenerator.getId() + "_activated");
		
		// Now we create <if>s for every sending MessageLink with an <assign> A
		// with <from>vS</from><to>vR</> as long vR_activated=false(). A is
		// connected with a link to a <throw> for ns:pickFault1 inside the
		// <flow> of scpPick.
		If newIf = ChoreoMergeUtil.createIfFromInvoke(s, guard, vRec);
		
		// Delete first copy from <assign>
		Assign newAssignS = (Assign) newIf.getActivity();
		newAssignS.getCopy().remove(0);
		
		// Create new <sequence> surrounding newAssignS
		Sequence newSeq = BPELFactory.eINSTANCE.createSequence();
		// Set <targets> and <sources> newAssignS to newSeq
		newSeq.setSources(newIf.getSources());
		newSeq.setTargets(newIf.getTargets());
		// Create new <empty> after newAssignS for synchronization of the
		// responding link
		Empty newEmptyReply = BPELFactory.eINSTANCE.createEmpty();
		newSeq.getActivities().add(newIf);
		newSeq.getActivities().add(newEmptyReply);
		
		// create <scope> replacing r
		Scope newScope = ChoreoMergeUtil.createScopeFromPickWithoutOM(rec, Arrays.asList(rOM), guard);
		
		// create new <throw> for receiving <onMessage> rOM
		// create <throw>
		QName qName = new QName(this.pkg.getMergedProcess().getTargetNamespace(), this.mLinkSend.getReceiveActivity() + "Fault");
		Throw newThrow = ChoreoMergeUtil.createThrowForUserDefinedFault(qName);
		
		// Add newThrow to <flow> inside newScope
		((Flow) newScope.getActivity()).getActivities().add(newThrow);
		
		// Create new <assign> replacing the <reply> repl
		Assign newAssignRepl = ChoreoMergeUtil.createAssignFromSendAct(repl, vReply);
		
		// Create new <link> in MergedFlow connecting <assign> in newIf and
		// newThrow
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(newIf.getName() + "TO" + newScope.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
		ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssignS);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newThrow);
		
		// Create new <link> in MergedFlow connecting newAssignRepl and
		// newEmptyReply
		Link newLinkReply = BPELFactory.eINSTANCE.createLink();
		newLinkReply.setName(newAssignRepl.getName() + "TO" + (newEmptyReply.getName() != null ? newEmptyReply.getName() : "Empty" + WSUIDGenerator.getId()));
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkReply);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkReply, newAssignRepl);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkReply, newEmptyReply);
		
		// Replace old activities (s, rec, repl)
		ChoreoMergeUtil.replaceActivity(s, newSeq);
		ChoreoMergeUtil.replaceActivity(rec, newScope);
		ChoreoMergeUtil.replaceActivity(repl, newAssignRepl);
		
	}
	
	public MessageLink getmLinkSend() {
		return this.mLinkSend;
	}
	
	public void setmLinkSend(MessageLink mLinkSend) {
		this.mLinkSend = mLinkSend;
	}
}
