package org.bpel4chor.mergechoreography.pattern.communication.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.utils.WSUIDGenerator;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
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

public class SyncPattern24 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected List<MLEnvironment> envReplys = new ArrayList<>();
	
	/** List of {@link MessageLink}s */
	private List<MessageLink> replyMLinks = new ArrayList<>();
	
	/** Wsu:id of <onMessage>-branch */
	private String wsuIDrec;
	
	
	public SyncPattern24(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
		super(envSend, pkg);
		this.envReplys.add(envReply);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Pick rec = (Pick) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(rec);
		
		// We need the <onMessage> from the pick containing vRec
		OnMessage rOM = (OnMessage) ChoreoMergeUtil.resolveActivity(this.wsuIDrec);
		
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
		
		// Create new <assign> replacing the <invoke> s
		Assign newAssignS = null;
		Scope newScopeS = null;
		// Create new <assign> with surrounding <scope> for the multiple
		// <reply>s
		newScopeS = ChoreoMergeUtil.createScopeFromInvoke(s, vRec);
		newAssignS = (Assign) newScopeS.getActivity();
		
		// Now we create <if>s for every sending MessageLink with an <assign> A
		// with <from>vS</from><to>vR</> as long vR_activated=false(). A is
		// connected with a link to a <throw> for ns:pickFault1 inside the
		// <flow> of scpPick.
		If newIf = ChoreoMergeUtil.createIfFromInvoke(s, guard, vRec);
		
		// Remove unused <sources> and <targets> from newIf
		newIf.setSources(null);
		newIf.setTargets(null);
		
		// Set newAssigns as activity in newIf
		newIf.setActivity(newAssignS);
		
		// Create new <sequence> surrounding newAssignS
		Sequence newSeq = BPELFactory.eINSTANCE.createSequence();
		// Add newAssignS to the newSeq
		newSeq.getActivities().add(newIf);
		// Add newSeq to newScopeS
		newScopeS.setActivity(newSeq);
		
		// create <scope> replacing r
		Scope newScope = ChoreoMergeUtil.createScopeFromPickWithoutOM(rec, Arrays.asList(rOM), guard);
		
		// create new <throw> for receiving <onMessage> rOM
		// create <throw>
		QName qName = new QName(this.pkg.getMergedProcess().getTargetNamespace(), this.wsuIDrec + "Fault");
		Throw newThrow = ChoreoMergeUtil.createThrowForUserDefinedFault(qName);
		
		// Add newThrow to <flow> inside newScope
		((Flow) newScope.getActivity()).getActivities().add(newThrow);
		
		// // Create new <assign> replacing the <reply> repl
		// Assign newAssignRepl =
		// ChoreoMergeUtil.createAssignFromSendAct(repl, vReply);
		//
		// Create new <link> in MergedFlow connecting <assign> in newIf and
		// newThrow
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(newIf.getName() + "TO" + newScope.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
		ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssignS);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newThrow);
		
		// create new QName for Exit-<catch>-FaultHandler
		QName qName2 = new QName(this.pkg.getMergedProcess().getTargetNamespace(), "exitFault");
		
		// Create new <catch>-Fault Handler in newScopes for exiting
		Catch newCatchExit = ChoreoMergeUtil.createCatchForUserDefinedFault(qName2);
		
		// Add <empty> to newCatchExit
		newCatchExit.setActivity(BPELFactory.eINSTANCE.createEmpty());
		
		// Add newCatchExit to newScopeS
		ChoreoMergeUtil.addCatchToBPELExtensibleElement(newCatchExit, newScopeS);
		
		// Create new <flow> inside newSeq for the following <throw>s
		Flow newFlow = BPELFactory.eINSTANCE.createFlow();
		newSeq.getActivities().add(newFlow);
		
		// Create the corresponding <assign>s for all the <reply>s and link
		// them with the <throw>s in newFlow
		for (MLEnvironment env : this.envReplys) {
			Reply reply = (Reply) env.getS();
			
			// Propagate possible correlationSet initializations
			ChoreoMergeUtil.propagateCorrelInit(reply);
			
			// Create new <assign> replacing the <reply> reply
			Assign newAssignRepl = ChoreoMergeUtil.createAssignFromSendAct(reply, vReply);
			
			Throw newThrow2 = null;
			if (reply.getFaultName() != null) {
				newThrow2 = ChoreoMergeUtil.createThrowForUserDefinedFault(reply.getFaultName());
			} else {
				newThrow2 = ChoreoMergeUtil.createThrowForUserDefinedFault(qName2);
			}
			
			// Add newThrow to newFlow
			newFlow.getActivities().add(newThrow2);
			
			// Create new <link> in MergedFlow connection newAssignRepl and
			// newThrow
			Link newLinkReply = BPELFactory.eINSTANCE.createLink();
			newLinkReply.setName(newAssignRepl.getName() + "TO" + (newThrow2.getName() != null ? newThrow2.getName() : "Throw"));
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkReply);
			ChoreoMergeUtil.createSource4LinkInActivity(newLinkReply, newAssignRepl);
			ChoreoMergeUtil.createTarget4LinkInActivity(newLinkReply, newThrow2);
			
			// Replace old activity reply
			ChoreoMergeUtil.replaceActivity(reply, newAssignRepl);
		}
		
		// Replace old activities (s, rec, repl)
		ChoreoMergeUtil.replaceActivity(s, newScopeS);
		ChoreoMergeUtil.replaceActivity(rec, newScope);
		
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
	
	public void setWsuIDrec(String wsuIDrec) {
		this.wsuIDrec = wsuIDrec;
	}
	
}
