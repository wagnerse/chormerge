package org.bpel4chor.mergechoreography.pattern.communication.async;

import java.util.ArrayList;
import java.util.List;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
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
public class AsyncPattern18 extends MergePattern {
	
	/** List of {@link MessageLink}s */
	private List<MessageLink> mLinks = new ArrayList<>();
	
	/**
	 * List of Information about the analyzed environment of the
	 * {@link MessageLink}s
	 */
	private List<MLEnvironment> linkEnvs = new ArrayList<>();
	
	
	public AsyncPattern18(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		Receive r = (Receive) this.linkEnvs.get(0).getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		// First we uplift the vR used by r into the process<scope> of the
		// merged process
		Variable vR = ChoreoMergeUtil.resolveVariable(r.getVariable().getName(), r);
		this.log.info("vR => " + vR);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vR, this.pkg.getMergedProcess());
		
		// Now we create a new guard variable vR_written in process<scope> of
		// the merged process and initialize it inline with false()
		Variable guard = ChoreoMergeUtil.createGuardVariableInMergedProcess(vR.getName() + "_written");
		
		// Now we create a new <scope> with a <flow> inside a <catchAll>-FH with
		// an <empty> inside
		Scope rScope = ChoreoMergeUtil.createRCScopeFromActivity(r);
		
		// Now we create <if>s for every sending MessageLink with an <assign> A
		// with <from>true()</from><to>vR_written</to><from>vS</from><to>vR</>
		// as long vR_written=false(). A is connected with a link to a <throw>
		// for bpel:joinFailure inside the <flow> of rScope.
		for (MLEnvironment mlEnv : this.linkEnvs) {
			Invoke inv = (Invoke) mlEnv.getS();
			
			// Propagate possible correlationSet initializations
			ChoreoMergeUtil.propagateCorrelInit(inv);
			
			If newIf = ChoreoMergeUtil.createIfFromInvoke(inv, guard, vR);
			
			// Create new <throw> with bpel:joinFailure
			Throw newThrow = ChoreoMergeUtil.createThrowBPELJoinFailure();
			
			// Add newThrow to <flow> inside rScope
			((Flow) rScope.getActivity()).getActivities().add(newThrow);
			
			// Get <assign> inside newIf
			Assign newAssign = (Assign) newIf.getActivity();
			
			// Create new <link> in MergedFlow connection <assign> in newIf and
			// newEmpty
			Link newLink = BPELFactory.eINSTANCE.createLink();
			newLink.setName(newIf.getName() + "TO" + rScope.getName());
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
			ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssign);
			ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newThrow);
			
			ChoreoMergeUtil.replaceActivity(inv, newIf);
		}
		
		ChoreoMergeUtil.replaceActivity(r, rScope);
	}
	
	/**
	 * Add new {@link MessageLink} to mLinks
	 * 
	 * @param link new {@link MessageLink}
	 */
	public void addMessageLink(MessageLink link) {
		this.mLinks.add(link);
	}
	
	/**
	 * Add new {@link MLEnvironment} to linkEnvs
	 * 
	 * @param mlEnv
	 */
	public void addMLinkEnv(MLEnvironment mlEnv) {
		this.linkEnvs.add(mlEnv);
	}
	
	public List<MessageLink> getmLinks() {
		return this.mLinks;
	}
	
	public List<MLEnvironment> getLinkEnvs() {
		return this.linkEnvs;
	}
}
