package org.bpel4chor.mergechoreography.pattern.communication.async;

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
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.Variable;

/**
 * Pattern for Merging Simple Invoke
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class AsyncPattern21 extends MergePattern {
	
	/** {@link MessageLink} containing the send- and receiveActivity */
	private MessageLink mLink;
	
	
	public AsyncPattern21(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Pick r = (Pick) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		// We need the <onMessage> from the pick containing vR
		OnMessage rOM = (OnMessage) ChoreoMergeUtil.resolveActivity(this.mLink.getReceiveActivity());
		
		// First we uplift the vR used by r into the process<scope> of the
		// merged process
		Variable vR = ChoreoMergeUtil.resolveVariable(rOM.getVariable().getName(), r);
		this.log.info("vR => " + vR);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vR, this.pkg.getMergedProcess());
		
		// Now we create a new guard variable vR_activated in process<scope> of
		// the merged process and initialize it inline with false()
		Variable guard = ChoreoMergeUtil.createGuardVariableInMergedProcess(r.getName() + WSUIDGenerator.getId() + "_activated");
		
		// Now we create <if>s for every sending MessageLink with an <assign> A
		// with <from>vS</from><to>vR</> as long vR_activated=false(). A is
		// connected with a link to a <throw> for ns:pickFault1 inside the
		// <flow> of scpPick.
		If newIf = ChoreoMergeUtil.createIfFromInvoke(s, guard, vR);
		
		// Delete first copy from <assign>
		Assign newAssign = (Assign) newIf.getActivity();
		newAssign.getCopy().remove(0);
		
		// create <scope> replacing r
		Scope newScope = ChoreoMergeUtil.createScopeFromPickWithoutOM(r, Arrays.asList(rOM), guard);
		
		// create new <throw> for receiving <onMessage> rOM
		// create <throw>
		QName qName = new QName(this.pkg.getMergedProcess().getTargetNamespace(), this.mLink.getReceiveActivity() + "Fault");
		Throw newThrow = ChoreoMergeUtil.createThrowForUserDefinedFault(qName);
		
		// Add newThrow to <flow> inside newScope
		((Flow) newScope.getActivity()).getActivities().add(newThrow);
		
		// Create new <link> in MergedFlow connecting <assign> in newIf and
		// newThrow
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(newIf.getName() + "TO" + newScope.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
		ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssign);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newThrow);
		
		// replace old activities
		ChoreoMergeUtil.replaceActivity(s, newIf);
		ChoreoMergeUtil.replaceActivity(r, newScope);
		
	}
	
	public void setmLink(MessageLink mLink) {
		this.mLink = mLink;
	}
	
}
