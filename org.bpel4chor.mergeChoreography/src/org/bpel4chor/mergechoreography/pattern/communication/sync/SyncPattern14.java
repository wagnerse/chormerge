package org.bpel4chor.mergechoreography.pattern.communication.sync;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.Variable;

public class SyncPattern14 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected List<MLEnvironment> envReplys = new ArrayList<>();
	
	/** List of {@link MessageLink}s */
	private List<MessageLink> replyMLinks = new ArrayList<>();
	
	
	public SyncPattern14(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
		super(envSend, pkg);
		this.envReplys.add(envReply);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive rec = (Receive) this.env.getR();
		Reply repl = (Reply) this.envReplys.get(0).getS();
		
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
		Assign newAssignS = null;
		Scope newScopeS = null;
		// Create new <assign> with surrounding <scope> for the multiple
		// <reply>s
		newScopeS = ChoreoMergeUtil.createScopeFromInvoke(s, vRec);
		newAssignS = (Assign) newScopeS.getActivity();
		
		// Create new <sequence> surrounding newAssignS
		Sequence newSeq = BPELFactory.eINSTANCE.createSequence();
		// Add newAssignS to the newSeq
		newSeq.getActivities().add(newScopeS.getActivity());
		// Add newSeq to newScopeS
		newScopeS.setActivity(newSeq);
		
		// create new QName for Exit-<catch>-FaultHandler
		QName qName = new QName(this.pkg.getMergedProcess().getTargetNamespace(), "exitFault");
		
		// Create new <catch>-Fault Handler in newScopes for exiting
		Catch newCatchExit = ChoreoMergeUtil.createCatchForUserDefinedFault(qName);
		
		// Add <empty> to newCatchExit
		newCatchExit.setActivity(BPELFactory.eINSTANCE.createEmpty());
		
		// Add newCatchExit to newScopeS
		ChoreoMergeUtil.addCatchToBPELExtensibleElement(newCatchExit, newScopeS);
		
		// Create new <empty> replacing <receive> rec
		Empty newEmptyRec = ChoreoMergeUtil.createEmptyFromActivity(rec);
		
		// Create new <link> in MergedFlow connection newAssignS and
		// newEmptyRec
		Link newLinkSend = BPELFactory.eINSTANCE.createLink();
		newLinkSend.setName(newAssignS.getName() + "TO" + newEmptyRec.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkSend);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkSend, newAssignS);
		
		// Combine joinCondition in newEmpty if needed
		ChoreoMergeUtil.combineJCWithLink(newEmptyRec, newLinkSend);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkSend, newEmptyRec);
		
		// Create new <flow> inside newSeq for the following <throw>s
		Flow newFlow = BPELFactory.eINSTANCE.createFlow();
		newSeq.getActivities().add(newFlow);
		
		// Create thew corresponding <assign>s for all the <reply>s and link
		// them with the <throw>s in newFlow
		for (MLEnvironment env : this.envReplys) {
			Reply reply = (Reply) env.getS();
			// Create new <assign> replacing the <reply> reply
			Assign newAssignRepl = ChoreoMergeUtil.createAssignFromSendAct(reply, vReply);
			
			Throw newThrow = null;
			if (reply.getFaultName() != null) {
				newThrow = ChoreoMergeUtil.createThrowForUserDefinedFault(reply.getFaultName());
			} else {
				newThrow = ChoreoMergeUtil.createThrowForUserDefinedFault(qName);
			}
			
			// Add newThrow to newFlow
			newFlow.getActivities().add(newThrow);
			
			// Create new <link> in MergedFlow connection newAssignRepl and
			// newThrow
			Link newLinkReply = BPELFactory.eINSTANCE.createLink();
			newLinkReply.setName(newAssignRepl.getName() + "TO" + (newThrow.getName() != null ? newThrow.getName() : "Throw"));
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkReply);
			ChoreoMergeUtil.createSource4LinkInActivity(newLinkReply, newAssignRepl);
			ChoreoMergeUtil.createTarget4LinkInActivity(newLinkReply, newThrow);
			
			// Replace old activity reply
			ChoreoMergeUtil.replaceActivity(reply, newAssignRepl);
		}
		
		// Replace old activities (s, rec)
		ChoreoMergeUtil.replaceActivity(s, newScopeS);
		ChoreoMergeUtil.replaceActivity(rec, newEmptyRec);
		
	}
	
	/**
	 * Add new {@link MLEnvironment} to linkEnvs
	 * 
	 * @param mlEnv
	 */
	public void addReplyMLinkEnv(MLEnvironment mlReplyEnv) {
		this.envReplys.add(mlReplyEnv);
	}
	
	/**
	 * Add new {@link MessageLink} to mLinks
	 * 
	 * @param link new {@link MessageLink}
	 */
	public void addReplyMessageLink(MessageLink link) {
		this.replyMLinks.add(link);
	}
	
	public List<MLEnvironment> getLinkEnvs() {
		return this.envReplys;
	}
	
	public List<MessageLink> getReplyMLinks() {
		return this.replyMLinks;
	}
}
