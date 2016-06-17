package org.bpel4chor.mergechoreography.pattern.conditions;

/**
 * Condition class for checking special link conditions
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class Condition {
	
	protected final boolean result;
	
	
	public Condition(boolean result) {
		this.result = result;
	}
	
	public boolean evaluate() {
		return this.result;
	}
}
