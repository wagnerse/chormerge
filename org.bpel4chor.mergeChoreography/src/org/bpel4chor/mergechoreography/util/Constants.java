package org.bpel4chor.mergechoreography.util;

/**
 * Contains constants for the merge process.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 */
public interface Constants {
	
	/**
	 * Name of the {@link Empty}, all new {@link Scope}s join here
	 */
	String FH_NAME_EMPTY_CONTINUE = "continueProcess";
	/**
	 * Name postfix of every new {@link Scope}
	 */
	String FH_NAME_CASE_CATCH = "_catch";
	/**
	 * Name postfix of new {@link CatchAll} @{link Scope}
	 */
	String FH_NAME_CASE_CATCHALL = "_catchAll";
	/**
	 * Name prefix for new {@link Flow}
	 */
	String FH_NAME_FLOW = "FH_Processing_";
	/**
	 * Name prefix for new surround {@link Scope}
	 */
	String FH_NAME_NEW_SUR_SCOPE = "FH_SurScope_";
	/**
	 * Name prefix for new {@link TerminationHandler} {@link Scope}
	 */
	String TH_NAME_NEW_SCOPE = "TH_Scope_";
	/**
	 * Name for PreProcessing {@link Scope} that represent an alternative {@link Invoke}
	 */
	String NAME_SCOPE_PRE_PROCESSING = "PRE_PROCESSING_SI_";
	/**
	 * Name prefix for the PBD-{@link Scope}
	 */
	String PREFIX_NAME_PBD_SCOPE = "Scope_";
	/**
	 * Name prefix for {@link CompensationHandler} isScopeCompleted
	 */
	String PREFIX_NAME_CH_ISCOMPLETED = "isScopeCompleted_";
	/**
	 * Name prefix for {@link CompensationHandler}s new surrounding
	 * {@link Scope}
	 */
	String PREFIX_NAME_CH_SUR_SCOPE = "CH_SurScope_";
	/**
	 * Name prefix for {@link CompensationHandler}s new surrounding {@link Flow}
	 */
	String PREFIX_NAME_CH_SUR_FLOW = "CH_SurFlow_";
	/**
	 * Name prefix for {@link CompensationHandler}s {@link Link}s between
	 * {@link Scope} and his compensation {@link Scope}
	 */
	String PREFIX_NAME_CH_LINK = "CH_link_";
	
	// XPATH constants
	String XPATH_BOOLEAN_TRUE = "true()";
	String XPATH_BOOLEAN_FALSE = "false()";
}
