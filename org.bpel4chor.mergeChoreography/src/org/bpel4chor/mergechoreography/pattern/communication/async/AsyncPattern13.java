package org.bpel4chor.mergechoreography.pattern.communication.async;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
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
public class AsyncPattern13 extends MergePattern {
	
	public AsyncPattern13(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive r = (Receive) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		// First we uplift the vS used by s into the process<scope> of the
		// merged process
		Variable vS = ChoreoMergeUtil.resolveVariable(s.getInputVariable().getName(), s);
		this.log.info("vS => " + vS);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vS, this.pkg.getMergedProcess());
		
		// Create new <empty> replacing the <invoke> s
		Empty newEmptyS = ChoreoMergeUtil.createEmptyFromActivity(s);
		
		// Create new <empty> replacing the <receive> r
		Empty newEmptyR = ChoreoMergeUtil.createEmptyFromActivity(r);
		
		// Create new <link> in MergedFlow connection newAssign and newEmpty
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName(newEmptyS.getName() + "TO" + newEmptyR.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
		ChoreoMergeUtil.createSource4LinkInActivity(newLink, newEmptyS);
		
		// Combine joinCondition in newEmpty if needed
		ChoreoMergeUtil.combineJCWithLink(newEmptyR, newLink);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newEmptyR);
		
		ChoreoMergeUtil.replaceActivity(s, newEmptyS);
		ChoreoMergeUtil.replaceActivity(r, newEmptyR);
	}
}
