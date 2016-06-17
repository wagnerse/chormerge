package org.bpel4chor.mergechoreography.test.choreographyMerger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bpel4chor.mergechoreography.ChoreographyMerger;
import org.bpel4chor.mergechoreography.test.util.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChoreographyMergerTest {
	
	private static Map<String, String> patterns = new HashMap<>();
	
	private static String outputPath = Constants.mergeOutputPath;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ChoreographyMergerTest.patterns.put(Constants.asyncPattern11Choreo, "ASP11");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern11InvFHCHChoreo, "ASP11InvFHCH");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern12Choreo, "ASP12");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern13Choreo, "ASP13");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern14Choreo, "ASP14");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern15Choreo, "ASP15");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern15BranchSuccRChoreo, "ASP15BranchSuccR");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern16Choreo, "ASP16");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern17Choreo, "ASP17");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern18Choreo, "ASP18");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern21Choreo, "ASP21");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern222SendSameVarChoreo, "ASP222SendSameVar");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern222SendDiffVarsChoreo, "ASP222SendDiffVars");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern23Choreo, "ASP23");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern23CreateInstanceChoreo, "ASP23CreateInstance");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern11Choreo, "SP11");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern12Choreo, "SP12");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern13Choreo, "SP13");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern14WithFaultChoreo, "SP14WithFault");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern14Choreo, "SP14");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern15FHChoreo, "SP15FH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern15EHChoreo, "SP15EH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern15CHChoreo, "SP15CH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern15THChoreo, "SP15TH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern21Choreo, "SP21");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern22Choreo, "SP22");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern23Choreo, "SP23");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern24Choreo, "SP24");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern30RisEHChoreo, "ASP30RisEH");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern30SinCHChoreo, "ASP30SinCH");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern30RinEHChoreo, "ASP30RinEH");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern30SinWhileChoreo, "ASP30SinWhile");
//		ChoreographyMergerTest.patterns.put(Constants.asyncPattern30RinWhileChoreo, "ASP30RinWhile");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30RisEHChoreo, "SP30RisEH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30SinEHChoreo, "SP30SinEH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30SRinFCTEChoreo, "SP30SRinFCTE");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30RisPickInFHChoreo, "SP30RisPickInFH");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30SinWhileChoreo, "SP30SinWhile");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30SinWhileMultiReplyChoreo, "SP30SinWhileMultiReply");
//		ChoreographyMergerTest.patterns.put(Constants.syncPattern30SinWhileMultiReplyWSDLChoreo, "SP30SinWhileMultiReplyWSDL");
//		ChoreographyMergerTest.patterns.put(Constants.demoChoreo, "DemoChoreo");
//		ChoreographyMergerTest.patterns.put(Constants.correlationPropagatorChoreo, "CorrelationPropagator");
	//	ChoreographyMergerTest.patterns.put(Constants.multiInstanceChoreo, "Multi-Instance-Test");
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	public void test() {
		for (Map.Entry<String, String> entry : ChoreographyMergerTest.patterns.entrySet()) {
			boolean success = (new File(ChoreographyMergerTest.outputPath + java.io.File.separator + entry.getValue())).mkdir();
			ChoreographyMerger choreographyMerger = new ChoreographyMerger(entry.getKey());
			choreographyMerger.merge(ChoreographyMergerTest.outputPath + java.io.File.separator + entry.getValue() + java.io.File.separator);
		}
	}
	
}
