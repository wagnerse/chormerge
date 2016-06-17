package org.bpel4chor.mergechoreography.pattern.communication.async;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
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
public class AsyncPattern11 extends MergePattern {
	
	public AsyncPattern11(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive r = (Receive) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		// First we uplift the vR used by r into the process<scope> of the
		// merged process
		Variable vR = ChoreoMergeUtil.resolveVariable(r.getVariable().getName(), r);
		this.log.info("vR => " + vR);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vR, this.pkg.getMergedProcess());
		
		// Create new <assign> replacing the <invoke> s
		Assign newAssign = null;
		Scope newScope = null;
		if (ChoreoMergeUtil.hasFHsOrCH(s)) {
			newScope = ChoreoMergeUtil.createScopeFromInvoke(s, vR);
			newAssign = (Assign) newScope.getActivity();
		} else {
			newAssign = ChoreoMergeUtil.createAssignFromInvoke(s, vR);
		}
		
		// Create new <empty> replacing the <receive> r
		Empty newEmpty = ChoreoMergeUtil.createEmptyFromActivity(r);
		
		// Create new <link> in MergedFlow connection newAssign and newEmpty
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(newAssign.getName() + "TO" + newEmpty.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
		ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssign);
		
		// Combine joinCondition in newEmpty if needed
		ChoreoMergeUtil.combineJCWithLink(newEmpty, newLink);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newEmpty);
		
		ChoreoMergeUtil.replaceActivity(s, (newScope != null ? newScope : newAssign));
		ChoreoMergeUtil.replaceActivity(r, newEmpty);
		
		ChoreoMergeUtil.optimizeEmpty(newEmpty);
	}
}
