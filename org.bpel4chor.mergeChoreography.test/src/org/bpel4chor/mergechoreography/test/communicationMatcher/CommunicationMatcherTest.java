package org.bpel4chor.mergechoreography.test.communicationMatcher;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.bpel4chor.mergechoreography.ChoreographyMerger;
import org.bpel4chor.mergechoreography.matcher.communication.CommunicationMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern11;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern12;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern13;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern14;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern15;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern16;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern17;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern18;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern21;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern22;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern23;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern11;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern12;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern13;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern14;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern21;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern23;
import org.bpel4chor.mergechoreography.pattern.communication.sync.SyncPattern24;
import org.bpel4chor.mergechoreography.test.util.Constants;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CommunicationMatcherTest {
	
	private static Map<String, Class<? extends MergePattern>> patterns = new HashMap<>();
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern11Choreo, AsyncPattern11.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern11InvFHCHChoreo, AsyncPattern11.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern12Choreo, AsyncPattern12.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern13Choreo, AsyncPattern13.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern14Choreo, AsyncPattern14.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern15Choreo, AsyncPattern15.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern15BranchSuccRChoreo, AsyncPattern15.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern16Choreo, AsyncPattern16.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern17Choreo, AsyncPattern17.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern18Choreo, AsyncPattern18.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern21Choreo, AsyncPattern21.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern222SendSameVarChoreo, AsyncPattern22.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern222SendDiffVarsChoreo, AsyncPattern22.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern23Choreo, AsyncPattern23.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern23CreateInstanceChoreo, AsyncPattern23.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern11Choreo, SyncPattern11.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern12Choreo, SyncPattern12.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern13Choreo, SyncPattern13.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern14WithFaultChoreo, SyncPattern14.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern14Choreo, SyncPattern14.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern15FHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern15EHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern15CHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern15THChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern21Choreo, SyncPattern21.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern22Choreo, SyncPattern21.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern23Choreo, SyncPattern23.class);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern24Choreo, SyncPattern24.class);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern30RisEHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern30SinCHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern30RinEHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern30SinWhileChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.asyncPattern30RinWhileChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30RisEHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30SinEHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30SRinFCTEChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30RisPickInFHChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30SinWhileChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30SinWhileMultiReplyChoreo, null);
		CommunicationMatcherTest.patterns.put(Constants.syncPattern30SinWhileMultiReplyWSDLChoreo, null);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	public void test() {
		for (Map.Entry<String, Class<? extends MergePattern>> entry : CommunicationMatcherTest.patterns.entrySet()) {
			ChoreographyMerger choreographyMerger = new ChoreographyMerger(entry.getKey());
			
			choreographyMerger.getChoreographyPackage().initMergedProcess();
			
			// Now check the MessageLinks and merge
			CommunicationMatcher matcher = new CommunicationMatcher();
			for (MessageLink link : choreographyMerger.getChoreographyPackage().getTopology().getMessageLinks()) {
				if (!choreographyMerger.getChoreographyPackage().isLinkVisited(link)) {
					MergePattern pattern = matcher.match(link, choreographyMerger.getChoreographyPackage());
					Class<? extends MergePattern> searchedOne = entry.getValue();
					if (searchedOne != null) {
						Assert.assertTrue(searchedOne.isInstance(pattern));
					} else {
						Assert.assertNull(pattern);
					}
				}
			}
		}
	}
	
}
