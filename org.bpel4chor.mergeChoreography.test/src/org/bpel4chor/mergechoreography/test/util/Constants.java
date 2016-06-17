package org.bpel4chor.mergechoreography.test.util;

import java.io.File;

/**
 * Some constants for the UnitTests of the ChoreographyMerger
 * 
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class Constants {
	
	// The outputPath for the merged choreographies
	// public static String mergeOutputPath =
	// "E:\\Arbeit\\Diplom\\EclipseBPELWS\\MeineTestChoreos\\bpelContent\\MergeOutput";
	public static String mergeOutputPath = "/Users/wagnerse/Documents/Diss/Prototype/temp";
	
	// path to source Files for bpel4chor Faulthandler Scenario
	public static String pathToFhSzenarioBpel4chor = "E:\\workspace_bpeldesigner\\BpelToBpel4Chor\\Szenario?\\bpel4chor\\";
	public static String pathToFhZipFiles = new File(new File("").getAbsolutePath() + File.separator + "input" + File.separator + "bpel4chorFaulthandler").getAbsolutePath();
	
	// path to check files
	public static String pathToCheckFiles = new File(new File("").getAbsolutePath() + File.separator + "input" + File.separator + "checkFiles" + File.separator).getAbsolutePath();;
	
	private static File baseDir = new File(new File("").getAbsolutePath() + File.separator + "input" + File.separator + "bpel4chorChoreographies");
	
	public static String asyncPattern11Choreo = Constants.baseDir + File.separator + "ASP11.zip";
	public static String asyncPattern11InvFHCHChoreo = Constants.baseDir + File.separator + "ASP11InvFHCH.zip";
	public static String asyncPattern12Choreo = Constants.baseDir + File.separator + "ASP12.zip";
	public static String asyncPattern13Choreo = Constants.baseDir + File.separator + "ASP13.zip";
	public static String asyncPattern14Choreo = Constants.baseDir + File.separator + "ASP14.zip";
	public static String asyncPattern15Choreo = Constants.baseDir + File.separator + "ASP15.zip";
	public static String asyncPattern15BranchSuccRChoreo = Constants.baseDir + File.separator + "ASP15BranchSuccR.zip";
	public static String asyncPattern16Choreo = Constants.baseDir + File.separator + "ASP16.zip";
	public static String asyncPattern17Choreo = Constants.baseDir + File.separator + "ASP17.zip";
	public static String asyncPattern18Choreo = Constants.baseDir + File.separator + "ASP18.zip";
	public static String asyncPattern21Choreo = Constants.baseDir + File.separator + "ASP21.zip";
	public static String asyncPattern222SendSameVarChoreo = Constants.baseDir + File.separator + "ASP222SendSameVar.zip";
	public static String asyncPattern222SendDiffVarsChoreo = Constants.baseDir + File.separator + "ASP222SendDiffVars.zip";
	public static String asyncPattern23Choreo = Constants.baseDir + File.separator + "ASP23.zip";
	public static String asyncPattern23CreateInstanceChoreo = Constants.baseDir + File.separator + "ASP23CreateInstance.zip";
	public static String asyncPattern30RisEHChoreo = Constants.baseDir + File.separator + "ASP30RisEH.zip";
	public static String asyncPattern30SinCHChoreo = Constants.baseDir + File.separator + "ASP30SinCH.zip";
	public static String asyncPattern30RinEHChoreo = Constants.baseDir + File.separator + "ASP30RinEH.zip";
	public static String asyncPattern30SinWhileChoreo = Constants.baseDir + File.separator + "ASP30SinWhile.zip";
	public static String asyncPattern30RinWhileChoreo = Constants.baseDir + File.separator + "ASP30RinWhile.zip";
	public static String syncPattern11Choreo = Constants.baseDir + File.separator + "SP11.zip";
	public static String syncPattern12Choreo = Constants.baseDir + File.separator + "SP12.zip";
	public static String syncPattern13Choreo = Constants.baseDir + File.separator + "SP13.zip";
	public static String syncPattern14WithFaultChoreo = Constants.baseDir + File.separator + "SP14WithFault.zip";
	public static String syncPattern14Choreo = Constants.baseDir + File.separator + "SP14.zip";
	public static String syncPattern15FHChoreo = Constants.baseDir + File.separator + "SP15FH.zip";
	public static String syncPattern15EHChoreo = Constants.baseDir + File.separator + "SP15EH.zip";
	public static String syncPattern15CHChoreo = Constants.baseDir + File.separator + "SP15CH.zip";
	public static String syncPattern15THChoreo = Constants.baseDir + File.separator + "SP15TH.zip";
	public static String syncPattern21Choreo = Constants.baseDir + File.separator + "SP21.zip";
	public static String syncPattern22Choreo = Constants.baseDir + File.separator + "SP22.zip";
	public static String syncPattern23Choreo = Constants.baseDir + File.separator + "SP23.zip";
	public static String syncPattern24Choreo = Constants.baseDir + File.separator + "SP24.zip";
	public static String syncPattern25Choreo = Constants.baseDir + File.separator + "SP25.zip";
	public static String syncPattern30RisEHChoreo = Constants.baseDir + File.separator + "SP30RisEH.zip";
	public static String syncPattern30SinEHChoreo = Constants.baseDir + File.separator + "SP30SinEH.zip";
	public static String syncPattern30SRinFCTEChoreo = Constants.baseDir + File.separator + "SP30SRinFCTE.zip";
	public static String syncPattern30RisPickInFHChoreo = Constants.baseDir + File.separator + "SP30RisPickInFH.zip";
	public static String syncPattern30SinWhileChoreo = Constants.baseDir + File.separator + "SP30SinWhile.zip";
	public static String syncPattern30SinWhileMultiReplyChoreo = Constants.baseDir + File.separator + "SP30SinWhileMultiReply.zip";
	public static String syncPattern30SinWhileMultiReplyWSDLChoreo = Constants.baseDir + File.separator + "SP30SinWhileMultiReplyWSDL.zip";
	public static String demoChoreo = Constants.baseDir + File.separator + "DemoChoreo.zip";
	public static String correlationPropagatorChoreo = Constants.baseDir + File.separator + "CorrelationPropagator.zip";
	public static String multiInstanceChoreo = Constants.baseDir + File.separator + "multiinstance" + File.separator + "MIP.zip";
}
