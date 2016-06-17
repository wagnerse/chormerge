package org.bpel4chor.mergechoreography.util;

import org.bpel4chor.mergechoreography.ChoreographyPackage;

/**
 * This class contains methods for PostProcessing.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class MergePostProcessor {
	
	/**
	 * Starts PostProcessing of the merged process.
	 * 
	 * @param choreographyPackage contains information for processing
	 */
	public static void startPostProcessing(ChoreographyPackage choreographyPackage) {
		// sets an alternative CompensationHandler
		FCTEUtil.processCompensationHandler(choreographyPackage.getMergedProcess());
		// checks/processes if FaultHandler or TerminationHandler should
		// be processed
		FCTEUtil.processScopesFT(choreographyPackage.getMergedProcess(), choreographyPackage.getPbds());
		// ### EXTEND PostProcessing code here EXTEND ###
	}
}
