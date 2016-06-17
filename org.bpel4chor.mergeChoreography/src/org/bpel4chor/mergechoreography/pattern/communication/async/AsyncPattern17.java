package org.bpel4chor.mergechoreography.pattern.communication.async;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;

/**
 * Pattern for Merging Simple Invoke
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class AsyncPattern17 extends MergePattern {
	
	/** {@link MessageLink} from <invoke> in {@link FaultHandler} (Khalaf Split) */
	private MessageLink fhMlink = null;
	
	/**
	 * Informations about the analyzed environment of the {@link MessageLink}
	 * inside the {@link FaultHandler}
	 */
	private MLEnvironment fhMlinkEnv = null;
	
	
	public AsyncPattern17(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive r = (Receive) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		Sequence sSeq = (Sequence) s.eContainer();
		String linkName = sSeq.getTargets().getChildren().get(0).getLink().getName();
		Scope sScpe = (Scope) sSeq.eContainer();
		
		// Get predecessor of s and surrounding <scope>, there's just one as we
		// know
		Activity preS = ChoreoMergeUtil.getPreceedingActivities(sSeq).get(0);
		
		// Get successor of r, there's also just one as we know
		Activity succR = ChoreoMergeUtil.getSucceedingActivities(r).get(0);
		
		// As the link connecting sSeq and s has the same name as the link
		// connecting r and succR we just have to delete him in the <flow> of
		// <scope> of s and the <flow> of <scope> of r and create a new one in
		// the MergedFlow
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(linkName);
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
		ChoreoMergeUtil.removeLinkFromFlow(ChoreoMergeUtil.findLinkOwnerFlow(preS, linkName), newLink);
		ChoreoMergeUtil.removeLinkFromFlow(ChoreoMergeUtil.findLinkOwnerFlow(succR, linkName), newLink);
		
		// Now we delete sScpe and r
		ChoreoMergeUtil.removeActivityFromContainer(sScpe);
		ChoreoMergeUtil.removeActivityFromContainer(r);
		
	}
	
	public MessageLink getFhMlink() {
		return this.fhMlink;
	}
	
	public void setFhMlink(MessageLink fhMlink) {
		this.fhMlink = fhMlink;
	}
	
	public MLEnvironment getFhMlinkEnv() {
		return this.fhMlinkEnv;
	}
	
	public void setFhMlinkEnv(MLEnvironment fhMlinkEnv) {
		this.fhMlinkEnv = fhMlinkEnv;
	}
}
