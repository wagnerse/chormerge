package org.bpel4chor.mergechoreography.pattern.communication.async;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Receive;

/**
 * Pattern for Merging Simple Invoke
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class AsyncPattern12 extends MergePattern {
	
	public AsyncPattern12(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive r = (Receive) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		// Create new <empty> replacing the <invoke> s
		Empty newEmptyS = ChoreoMergeUtil.createEmptyFromActivity(s);
		
		// Create new <empty> replacing the <receive> r
		Empty newEmptyR = ChoreoMergeUtil.createEmptyFromActivity(r);
		
		ChoreoMergeUtil.replaceActivity(s, newEmptyS);
		ChoreoMergeUtil.replaceActivity(r, newEmptyR);
	}
}
