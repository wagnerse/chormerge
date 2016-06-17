package org.bpel4chor.mergechoreography.test.choreographyMerger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.test.util.Constants;
import org.bpel4chor.model.pbd.impl.ParticipantBehaviorDescription;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.MessageExchange;
import org.eclipse.bpel.model.MessageExchanges;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.PartnerLinks;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Variable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MergerSimilarityBeforeMergeTest {
	
	private static List<String> patterns = new ArrayList<>();
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern11Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern11InvFHCHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern12Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern13Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern14Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern15Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern15BranchSuccRChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern16Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern17Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern18Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern21Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern222SendSameVarChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern222SendDiffVarsChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern23Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern23CreateInstanceChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern11Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern12Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern13Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern14WithFaultChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern14Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern15FHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern15EHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern15CHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern15THChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern21Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern22Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern23Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern24Choreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern30RisEHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern30SinCHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern30RinEHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern30SinWhileChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.asyncPattern30RinWhileChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30RisEHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30SinEHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30SRinFCTEChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30RisPickInFHChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30SinWhileChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30SinWhileMultiReplyChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.syncPattern30SinWhileMultiReplyWSDLChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.demoChoreo);
		MergerSimilarityBeforeMergeTest.patterns.add(Constants.correlationPropagatorChoreo);
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Before
	public void setUp() throws Exception {
	}
	
	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void testPBD2ScopeStructureTest() {
		for (String choreoName : MergerSimilarityBeforeMergeTest.patterns) {
			ChoreographyPackage choreoPkg = new ChoreographyPackage(choreoName);
			choreoPkg.initMergedProcess();
			for (Process pbd : choreoPkg.getPbds()) {
				// junit.textui.ResultPrinter printer = new
				// ResultPrinter(System.out);
				Scope newScope = this.getReplacingScopeForPBD(pbd, choreoPkg);
				Assert.assertNotNull(newScope);
				this.log.info("Checking similarity for PBD : " + pbd.getName() + " and <scope> : " + newScope.getName() + " ... ");
				this.checkPBD2ScopeSimilarity(pbd, newScope);
			}
		}
	}
	
	/**
	 * Find the replacing {@link Scope}-{@link Activity} in merged
	 * {@link Process} from given {@link ChoreographyPackage} with name
	 * consisting of: "Scope_" + pbd.name
	 * 
	 * @param pbd The original PBD
	 * @param pkg The {@link ChoreographyPackage} containing all data
	 * @return Found {@link Scope}-{@link Activity} or null
	 */
	private Scope getReplacingScopeForPBD(Process pbd, ChoreographyPackage pkg) {
		if ((pbd == null) || (pkg == null)) {
			throw new NullPointerException("pbd == null : " + (pbd == null) + "pkg == null" + (pkg == null));
		}
		String newName = "Scope_" + pbd.getName();
		if (pkg.getMergedProcess() != null) {
			for (Activity scope : ((Flow) pkg.getMergedProcess().getActivity()).getActivities()) {
				if (scope.getName().equals(newName)) {
					return (Scope) scope;
				}
			}
		}
		return null;
	}
	
	/**
	 * Check if given {@link Scope} equals given {@link Process} (PBD) in
	 * structure and attribute setting
	 * 
	 * @param pbd {@link ParticipantBehaviorDescription}
	 * @param scp {@link Scope} replacing the pbd
	 */
	private void checkPBD2ScopeSimilarity(Process pbd, Scope scp) {
		// Check similarity of attributes
		this.log.info("Attributes : ");
		Assert.assertEquals(pbd.getSuppressJoinFailure(), scp.getSuppressJoinFailure());
		this.log.info("=> suppressJoinFailure are equal. ");
		Assert.assertEquals(pbd.getExitOnStandardFault(), scp.getExitOnStandardFault());
		this.log.info("=> exitOnStandardFault are equal. ");
		
		// Check similarity of partnerLinks
		Assert.assertEquals(pbd.getPartnerLinks() == null, scp.getPartnerLinks() == null);
		
		if ((pbd.getPartnerLinks() != null) && (scp.getPartnerLinks() != null)) {
			Assert.assertEquals(pbd.getPartnerLinks().getChildren().size(), scp.getPartnerLinks().getChildren().size());
			for (PartnerLink plink : pbd.getPartnerLinks().getChildren()) {
				PartnerLink similarLink = this.findSimilarPartnerLink(plink.getName(), scp.getPartnerLinks());
				Assert.assertNotNull(similarLink);
				this.log.info("Checking similarity for PLink : " + plink);
				this.log.info(" and PLink : " + similarLink + " ... ");
				this.checkPLinkSimilarity(plink, similarLink);
			}
		}
		
		// Check similarity of message exchanges
		Assert.assertEquals(pbd.getMessageExchanges() == null, scp.getMessageExchanges() == null);
		
		if ((pbd.getMessageExchanges() != null) && (scp.getMessageExchanges() != null)) {
			Assert.assertEquals(pbd.getMessageExchanges().getChildren().size(), scp.getMessageExchanges().getChildren().size());
			this.log.info("Checking similarity of <messageExchanges> ... ");
			for (MessageExchange mex : pbd.getMessageExchanges().getChildren()) {
				Assert.assertNotNull(this.findSimilarMessageExchange(mex.getName(), scp.getMessageExchanges()));
			}
		}
		
		// Check similarity of variables
		Assert.assertEquals(pbd.getVariables() == null, scp.getVariables() == null);
		
		if ((pbd.getVariables() != null) && (scp.getVariables() != null)) {
			Assert.assertEquals(pbd.getVariables().getChildren().size(), scp.getVariables().getChildren().size());
			this.log.info("Checking similarity of <variables> ... ");
			for (Variable var : pbd.getVariables().getChildren()) {
				Variable similarVar = this.findSimilarElement(var.getName(), pbd.getVariables().getChildren());
				Assert.assertNotNull(similarVar);
			}
		}
		// for (Method method : Variable.class.getDeclaredMethods()) {
		// if (method.getName().startsWith("get")) {
		// System.out.println(method.getName());
		// System.out.println(method.getReturnType());
		// }
		// }
	}
	
	/**
	 * Check similarity of given {@link PartnerLink}s
	 * 
	 * @param plink1 {@link PartnerLink} 1
	 * @param plink2 {@link PartnerLink} 2
	 */
	private void checkPLinkSimilarity(PartnerLink plink1, PartnerLink plink2) {
		Assert.assertEquals(plink1.getName(), plink2.getName());
		Assert.assertEquals(plink1.getPartnerLinkType().getName(), plink2.getPartnerLinkType().getName());
		Assert.assertEquals(plink1.getMyRole().getName(), plink2.getMyRole().getName());
		Assert.assertEquals(plink1.getPartnerRole().getName(), plink2.getPartnerRole().getName());
		Assert.assertEquals(plink1.getInitializePartnerRole(), plink2.getInitializePartnerRole());
	}
	
	/**
	 * Find the {@link PartnerLink} with the given name in given
	 * {@link PartnerLinks}
	 * 
	 * @param plName {@link PartnerLink}name
	 * @param pLinks {@link PartnerLinks}
	 * @return Found {@link PartnerLink} or null
	 */
	private PartnerLink findSimilarPartnerLink(String plName, PartnerLinks pLinks) {
		for (PartnerLink plink : pLinks.getChildren()) {
			if (plink.getName().equals(plName)) {
				return plink;
			}
		}
		return null;
	}
	
	/**
	 * Find the {@link MessageExchange} with the given name in given
	 * {@link MessageExchanges}
	 * 
	 * @param mexName {@link MessageExchange}name
	 * @param mexs {@link MessageExchanges}
	 * @return Found {@link MessageExchange} or null
	 */
	private MessageExchange findSimilarMessageExchange(String mexName, MessageExchanges mexs) {
		for (MessageExchange mex : mexs.getChildren()) {
			if (mex.getName().equals(mexName)) {
				return mex;
			}
		}
		return null;
	}
	
	private <T, V> T findSimilarElement(String elName, List<T> elements) {
		Method m = null;
		try {
			for (T element : elements) {
				m = element.getClass().getMethod("getName");
				Object r = m.invoke(element);
				if (elName.equals(String.valueOf(r))) {
					return element;
				}
			}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
