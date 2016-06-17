package org.bpel4chor.mergechoreography.test;

import org.bpel4chor.mergechoreography.test.choreographyMerger.ChoreographyMergerTest;
import org.bpel4chor.mergechoreography.test.choreographyPackage.ChoreoraphyPackageInitTest;
import org.bpel4chor.mergechoreography.test.communicationMatcher.CommunicationMatcherTest;
import org.bpel4chor.mergechoreography.test.util.ChoreoMergeUtilTest;
import org.bpel4chor.mergechoreography.test.util.XPathUtilTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ChoreoraphyPackageInitTest.class, CommunicationMatcherTest.class, ChoreoMergeUtilTest.class, ChoreographyMergerTest.class, XPathUtilTest.class})
public class AllTests {
	
}
