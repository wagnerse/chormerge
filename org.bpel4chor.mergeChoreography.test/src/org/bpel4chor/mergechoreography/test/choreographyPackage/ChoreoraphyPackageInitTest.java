package org.bpel4chor.mergechoreography.test.choreographyPackage;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.test.util.Constants;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class ChoreoraphyPackageInitTest {
	
	private static List<String> patterns = new ArrayList<>();
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern11Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern11InvFHCHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern12Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern13Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern14Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern15Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern15BranchSuccRChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern16Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern17Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern21Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern222SendDiffVarsChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern222SendSameVarChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern23Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern23CreateInstanceChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern30RinEHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern30RinWhileChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern30RisEHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern30SinCHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.asyncPattern30SinWhileChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern11Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern12Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern13Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern14Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern14WithFaultChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern15CHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern15EHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern15FHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern15THChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern21Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern22Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern23Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern24Choreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30RisEHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30RisPickInFHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30SinEHChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30SinWhileChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30SinWhileMultiReplyChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30SinWhileMultiReplyWSDLChoreo);
		ChoreoraphyPackageInitTest.patterns.add(Constants.syncPattern30SRinFCTEChoreo);
		
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	public void testChoreographyPackageInit() {
		for (String choreoName : ChoreoraphyPackageInitTest.patterns) {
			ChoreographyPackage choreoPkg = new ChoreographyPackage(choreoName);
			Assert.assertNotNull(choreoPkg.getGrounding());
			Assert.assertNotNull(choreoPkg.getTopology());
			Assert.assertEquals(choreoPkg.getPbds().size(), choreoPkg.getWsdls().size());
			Assert.assertEquals(choreoPkg.getPbd2wsdl().size(), choreoPkg.getPbds().size());
			Assert.assertEquals(choreoPkg.getPbd2wsdl().size(), choreoPkg.getWsdls().size());
		}
		
	}
	
}
