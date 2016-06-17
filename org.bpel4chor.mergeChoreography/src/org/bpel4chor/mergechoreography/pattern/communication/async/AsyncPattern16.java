package org.bpel4chor.mergechoreography.pattern.communication.async;

import java.util.ArrayList;
import java.util.List;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Variable;

import de.uni_stuttgart.iaas.bpel.model.utilities.FragmentDuplicator;

/**
 * Pattern for Merging Simple Invoke
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class AsyncPattern16 extends MergePattern {
	
	/** List of succeeding {@link MessageLink}s */
	private List<MessageLink> succLinks = new ArrayList<>();
	
	/**
	 * List of Information about the analyzed environment of the succeeding
	 * {@link MessageLink}s
	 */
	private List<MLEnvironment> succEnvs = new ArrayList<>();
	
	
	public AsyncPattern16(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		// Create new <assign> replacing the <invoke>s of the MessageLinks in
		// succLinks
		Assign newAssign = BPELFactory.eINSTANCE.createAssign();
		
		// It gets the name of the first <invoke>
		newAssign.setName(this.succEnvs.get(0).getS().getName());
		
		// For every <invoke> in succEnvs we create new <copy>s in newAssign and
		// replace the <receive> and link it with the newAssign
		for (int i = 0; i < this.succEnvs.size(); i++) {
			Invoke s = (Invoke) this.succEnvs.get(i).getS();
			Receive r = (Receive) this.succEnvs.get(i).getR();
			
			// Propagate possible correlationSet initializations
			ChoreoMergeUtil.propagateCorrelInit(s);
			ChoreoMergeUtil.propagateCorrelInit(r);
			
			// First we uplift the vR used by r into the process<scope> of the
			// merged process
			Variable vR = ChoreoMergeUtil.resolveVariable(r.getVariable().getName(), r);
			this.log.info("vR => " + vR);
			
			ChoreoMergeUtil.upliftVariableToProcessScope(vR, this.pkg.getMergedProcess());
			
			// Copy standardAttributes and -Elements
			FragmentDuplicator.copyStandardAttributes(s, newAssign);
			FragmentDuplicator.copyStandardElements(s, newAssign);
			
			// Remove link connecting two <invoke>s, if any exists
			if (i > 0) {
				Activity pre = this.succEnvs.get(i - 1).getS();
				Source preS = ChoreoMergeUtil.getMatchingSource(pre, s);
				if (preS != null) {
					Target preToS = ChoreoMergeUtil.getMatchingTarget(pre, s);
					ChoreoMergeUtil.removeSourceFromActivity(newAssign, preS.getLink().getName());
					ChoreoMergeUtil.removeTargetFromActivity(newAssign, preToS.getLink().getName());
					ChoreoMergeUtil.removeLinkFromFlow(ChoreoMergeUtil.findLinkOwnerFlow(s, preToS.getLink().getName()), preToS.getLink());
				}
			}
			
			// Create new <copy> for s.inputVariable and vR in newAssign
			newAssign.getCopy().add(ChoreoMergeUtil.createCopy(s.getInputVariable(), vR));
			
			// Create new <empty> replacing the <receive> r
			Empty newEmpty = ChoreoMergeUtil.createEmptyFromActivity(r);
			
			// Create new <link> in MergedFlow connection newAssign and newEmpty
			Link newLink = BPELFactory.eINSTANCE.createLink();
			newLink.setName(newAssign.getName() + "TO" + newEmpty.getName());
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
			this.log.info("Creating <source> for link : " + newLink.getName() + " in : " + newAssign);
			ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssign);
			
			// Combine joinCondition in newEmpty if needed
			ChoreoMergeUtil.combineJCWithLink(newEmpty, newLink);
			ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newEmpty);
			
			if (i == 0) {
				ChoreoMergeUtil.replaceActivity(s, newAssign);
			} else {
				ChoreoMergeUtil.removeActivityFromContainer(s);
			}
			ChoreoMergeUtil.replaceActivity(r, newEmpty);
		}
		
	}
	
	/**
	 * Add new {@link MessageLink} to succLinks
	 * 
	 * @param link new {@link MessageLink}
	 */
	public void addMessageLink(MessageLink link) {
		this.succLinks.add(link);
	}
	
	/**
	 * Add new {@link MLEnvironment} to succEnvs
	 * 
	 * @param mlEnv
	 */
	public void addMLinkEnv(MLEnvironment mlEnv) {
		this.succEnvs.add(mlEnv);
	}
	
	public List<MessageLink> getSuccLinks() {
		return this.succLinks;
	}
	
	public List<MLEnvironment> getSuccEnvs() {
		return this.succEnvs;
	}
}
