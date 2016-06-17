package org.bpel4chor.mergechoreography.pattern.communication.async;

import java.util.List;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
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
public class AsyncPattern15 extends MergePattern {
	
	public AsyncPattern15(MLEnvironment env, ChoreographyPackage pkg) {
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
		
		// TODO: examine <joinCondition>s in succ(r) !!
		// TODO: examine <transitionCondition>s of r for variables other than vR
		// Get all <sources> of r
		// CHECK: links if necessary
		if (r.getSources() != null) {
			List<Source> sourcesOfR = r.getSources().getChildren();
			
			// Remove every link from sourcesOfR from owning <flow>
			for (Source source : sourcesOfR) {
				ChoreoMergeUtil.removeLinkFromFlow(ChoreoMergeUtil.findLinkOwnerFlow(r, source.getLink().getName()), source.getLink());
			}
			
			// Remove every <target> from every activity in succ(r)
			for (Activity activity : this.env.getSuccR()) {
				for (Source source : sourcesOfR) {
					Target target = ChoreoMergeUtil.findTargetInActivity(activity, source.getLink().getName());
					if (target != null) {
						ChoreoMergeUtil.removeTargetFromActivity(activity, target);
					}
				}
			}
		}
		
		// Create new <source> for every activity in succ(r)
		for (Activity activity : this.env.getSuccR()) {
			Link newLink = BPELFactory.eINSTANCE.createLink();
			newLink.setName(newAssign.getName() + "TO" + activity.getName());
			
			// Add newLink to MergedFlow
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
			
			// Add new <source> for newLink in newAssign
			ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssign);
			
			// Add new <target> for newLink in activity
			ChoreoMergeUtil.createTarget4LinkInActivity(newLink, activity);
		}
		
		ChoreoMergeUtil.replaceActivity(s, (newScope != null ? newScope : newAssign));
		ChoreoMergeUtil.removeActivityFromContainer(r);
	}
}
