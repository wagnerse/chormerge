package org.bpel4chor.mergechoreography.pattern;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.util.MLEnvironment;

/**
 * Base class for Merge Patterns
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public abstract class MergePattern {
	
	/** The Choreograohy Package */
	protected ChoreographyPackage pkg;
	
	/** The Information about the analyzed environment of the link */
	protected MLEnvironment env;
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	
	public MergePattern(MLEnvironment env, ChoreographyPackage pkg) {
		this.pkg = pkg;
		this.env = env;
	}
	
	public abstract void merge();
	
}
