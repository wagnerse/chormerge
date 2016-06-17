package org.bpel4chor.mergechoreography.util.model;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.TerminationHandler;

/**
 * Data Class to tell which handler should be processed.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class ScopeHandlerBean {
	
	private boolean terminationHandler = false;
	private boolean faultHandler = false;
	private boolean pbdScope = false;
	
	
	/**
	 * It is a flag which tells if this {@link Scope} have an associated PBD.
	 * 
	 * @return the pbdScope
	 */
	public boolean isPbdScope() {
		return pbdScope;
	}
	
	/**
	 * @param pbdScope the pbdScope to set
	 */
	public void setPbdScope(boolean pbdScope) {
		this.pbdScope = pbdScope;
	}
	
	/**
	 * It is a flag which tells if any {@link Activity} in the
	 * {@link TerminationHandler} has a {@link Target} and this {@link Target}
	 * is connected with a {@link Source} outside of the
	 * {@link TerminationHandler}.
	 * 
	 * @return true if {@link TerminationHandler} contains {@link Target} that
	 *         is connected to a {@link Source} outside of the
	 *         {@link TerminationHandler}<br>
	 *         otherwise false
	 */
	public boolean isTerminationHandler() {
		return terminationHandler;
	}
	
	/**
	 * @param terminationHandler the terminationHandler to set
	 */
	public void setTerminationHandler(boolean terminationHandler) {
		this.terminationHandler = terminationHandler;
	}
	
	/**
	 * It is a flag which tells if any {@link Activity} in the
	 * {@link FaultHandler} has a {@link Target} and this {@link Target} is
	 * connected with a {@link Source} outside of the {@link FaultHandler}.
	 * 
	 * @return true if {@link FaultHandler} contains {@link Target} that is
	 *         connected to a {@link Source} outside of the {@link FaultHandler}<br>
	 *         otherwise false
	 */
	public boolean isFaultHandler() {
		return faultHandler;
	}
	
	/**
	 * @param faultHandler the faultHandler to set
	 */
	public void setFaultHandler(boolean faultHandler) {
		this.faultHandler = faultHandler;
	}
	
	/**
	 * @return true if {@link Scope} should be processed<br>
	 *         otherwise false
	 */
	public boolean processScope() {
		return isFaultHandler() || isTerminationHandler();
	}
}
