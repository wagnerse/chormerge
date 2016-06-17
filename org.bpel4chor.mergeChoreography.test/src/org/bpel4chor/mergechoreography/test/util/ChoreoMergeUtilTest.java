package org.bpel4chor.mergechoreography.test.util;

import org.bpel4chor.mergechoreography.ChoreographyMerger;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.model.topology.impl.Topology;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.CatchAll;
import org.eclipse.bpel.model.Compensate;
import org.eclipse.bpel.model.CompensationHandler;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnEvent;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.TerminationHandler;
import org.eclipse.bpel.model.Variable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ChoreoMergeUtilTest {
	
	private static Flow flow = null;
	
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ChoreoMergeUtilTest.flow = BPELFactory.eINSTANCE.createFlow();
		ChoreoMergeUtilTest.flow.setName("TestFlow1");
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	public void addLinkToFlowTest() {
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName("link1");
		ChoreoMergeUtil.addLinkToFlow(ChoreoMergeUtilTest.flow, newLink);
		Assert.assertNotNull(ChoreoMergeUtilTest.flow.getLinks());
		Assert.assertTrue(ChoreoMergeUtilTest.flow.getLinks().getChildren().get(0) == newLink);
		
		this.thrown.expect(RuntimeException.class);
		ChoreoMergeUtil.addLinkToFlow(ChoreoMergeUtilTest.flow, newLink);
	}
	
	@Test
	public void addSourceToActivityTest() {
		ChoreoMergeUtilTest.flow = BPELFactory.eINSTANCE.createFlow();
		Source newSource = BPELFactory.eINSTANCE.createSource();
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName("outboundLink1");
		newSource.setLink(newLink);
		ChoreoMergeUtil.addSourceToActivity(ChoreoMergeUtilTest.flow, newSource);
		Assert.assertNotNull(ChoreoMergeUtilTest.flow.getSources());
		Assert.assertTrue(ChoreoMergeUtilTest.flow.getSources().getChildren().get(0) == newSource);
	}
	
	@Test
	public void addTargetToActivityTest() {
		ChoreoMergeUtilTest.flow = BPELFactory.eINSTANCE.createFlow();
		Target newTarget = BPELFactory.eINSTANCE.createTarget();
		Link newLink = BPELFactory.eINSTANCE.createLink();
		newLink.setName("inboundLink1");
		newTarget.setLink(newLink);
		ChoreoMergeUtil.addTargetToActivity(ChoreoMergeUtilTest.flow, newTarget);
		Assert.assertNotNull(ChoreoMergeUtilTest.flow.getTargets());
		Assert.assertTrue(ChoreoMergeUtilTest.flow.getTargets().getChildren().get(0) == newTarget);
	}
	
	@Test
	public void combineJCWithLinkTest() {
		ChoreoMergeUtilTest.flow = BPELFactory.eINSTANCE.createFlow();
		Link newLink1 = BPELFactory.eINSTANCE.createLink();
		newLink1.setName("link1");
		Link newLink2 = BPELFactory.eINSTANCE.createLink();
		newLink2.setName("link2");
		Target newTarget1 = BPELFactory.eINSTANCE.createTarget();
		newTarget1.setLink(newLink1);
		Target newTarget2 = BPELFactory.eINSTANCE.createTarget();
		newTarget2.setLink(newLink2);
		ChoreoMergeUtil.addTargetToActivity(ChoreoMergeUtilTest.flow, newTarget1);
		ChoreoMergeUtil.addTargetToActivity(ChoreoMergeUtilTest.flow, newTarget2);
		Link newLink3 = BPELFactory.eINSTANCE.createLink();
		newLink3.setName("link3");
		ChoreoMergeUtil.combineJCWithLink(ChoreoMergeUtilTest.flow, newLink3);
		Assert.assertEquals("$link3 and ($link1 or $link2)", ChoreoMergeUtilTest.flow.getTargets().getJoinCondition().getBody().toString());
	}
	
	@Test
	public void createAssignFromInvokeTest() {
		Invoke inv = BPELFactory.eINSTANCE.createInvoke();
		Variable varSend = BPELFactory.eINSTANCE.createVariable();
		varSend.setName("varSend");
		Variable varRec = BPELFactory.eINSTANCE.createVariable();
		varRec.setName("varRec");
		inv.setInputVariable(varSend);
		Assign assignFromInvoke = ChoreoMergeUtil.createAssignFromInvoke(inv, varRec);
		Assert.assertEquals(varSend, assignFromInvoke.getCopy().get(0).getFrom().getVariable());
		Assert.assertEquals(varRec, assignFromInvoke.getCopy().get(0).getTo().getVariable());
	}
	
	@Test
	public void createAssignFromSendActTest() {
		Invoke inv = BPELFactory.eINSTANCE.createInvoke();
		Reply reply = BPELFactory.eINSTANCE.createReply();
		Variable varSend = BPELFactory.eINSTANCE.createVariable();
		varSend.setName("varSend");
		Variable varRec = BPELFactory.eINSTANCE.createVariable();
		varRec.setName("varRec");
		inv.setInputVariable(varSend);
		Assign assignFromInvoke = ChoreoMergeUtil.createAssignFromSendAct(inv, varRec);
		Assert.assertEquals(varSend, assignFromInvoke.getCopy().get(0).getFrom().getVariable());
		Assert.assertEquals(varRec, assignFromInvoke.getCopy().get(0).getTo().getVariable());
		reply.setVariable(varSend);
		Assign assignFromReply = ChoreoMergeUtil.createAssignFromSendAct(reply, varRec);
		Assert.assertEquals(varSend, assignFromReply.getCopy().get(0).getFrom().getVariable());
		Assert.assertEquals(varRec, assignFromReply.getCopy().get(0).getTo().getVariable());
	}
	
	@Test
	public void createEmptyFromActivityTest() {
		Empty newEmpty = ChoreoMergeUtil.createEmptyFromActivity(ChoreoMergeUtilTest.flow);
		Assert.assertEquals(ChoreoMergeUtilTest.flow.getName(), newEmpty.getName());
	}
	
	@Test
	public void hasNPCatchAllFHTest() {
		Process process = BPELFactory.eINSTANCE.createProcess();
		process.setFaultHandlers(BPELFactory.eINSTANCE.createFaultHandler());
		CatchAll newCatchAll = BPELFactory.eINSTANCE.createCatchAll();
		process.getFaultHandlers().setCatchAll(newCatchAll);
		Assert.assertTrue(ChoreoMergeUtil.hasNPCatchAllFH(process));
	}
	
	@Test
	public void createNPCatchAllTest() {
		CatchAll npCatchAll = ChoreoMergeUtil.createNPCatchAll();
		Assert.assertTrue(npCatchAll.getActivity() instanceof Compensate);
	}
	
	@Test
	public void setGetActivityFCTEHandlerTest() {
		Empty newEmpty = BPELFactory.eINSTANCE.createEmpty();
		Catch newCatch = BPELFactory.eINSTANCE.createCatch();
		ChoreoMergeUtil.setActivityForFCTEHandler(newCatch, newEmpty);
		Assert.assertNotNull(newCatch.getActivity());
		Assert.assertNotNull(ChoreoMergeUtil.getActivityFromFCTEHandler(newCatch));
		CatchAll newCatchAll = BPELFactory.eINSTANCE.createCatchAll();
		ChoreoMergeUtil.setActivityForFCTEHandler(newCatchAll, newEmpty);
		Assert.assertNotNull(newCatchAll.getActivity());
		Assert.assertNotNull(ChoreoMergeUtil.getActivityFromFCTEHandler(newCatchAll));
		CompensationHandler newCH = BPELFactory.eINSTANCE.createCompensationHandler();
		ChoreoMergeUtil.setActivityForFCTEHandler(newCH, newEmpty);
		Assert.assertNotNull(newCH.getActivity());
		Assert.assertNotNull(ChoreoMergeUtil.getActivityFromFCTEHandler(newCH));
		TerminationHandler newTH = BPELFactory.eINSTANCE.createTerminationHandler();
		ChoreoMergeUtil.setActivityForFCTEHandler(newTH, newEmpty);
		Assert.assertNotNull(newTH.getActivity());
		Assert.assertNotNull(ChoreoMergeUtil.getActivityFromFCTEHandler(newTH));
		OnEvent newOE = BPELFactory.eINSTANCE.createOnEvent();
		ChoreoMergeUtil.setActivityForFCTEHandler(newOE, newEmpty);
		Assert.assertNotNull(newOE.getActivity());
		Assert.assertNotNull(ChoreoMergeUtil.getActivityFromFCTEHandler(newOE));
		OnAlarm newOA = BPELFactory.eINSTANCE.createOnAlarm();
		ChoreoMergeUtil.setActivityForFCTEHandler(newOA, newEmpty);
		Assert.assertNotNull(newOA.getActivity());
		Assert.assertNotNull(ChoreoMergeUtil.getActivityFromFCTEHandler(newOA));
	}
	
	@Test
	public void getFCTEHandlerOfActivityTest() {
		Empty newEmpty = BPELFactory.eINSTANCE.createEmpty();
		Scope newScope = BPELFactory.eINSTANCE.createScope();
		newScope.setActivity(newEmpty);
		Catch newCatch = BPELFactory.eINSTANCE.createCatch();
		newCatch.setActivity(newScope);
		Assert.assertEquals(newCatch, ChoreoMergeUtil.getFCTEHandlerOfActivity(newEmpty));
	}
	
	@Test
	public void getProcessOfActivityTest() {
		Process proc = BPELFactory.eINSTANCE.createProcess();
		Empty newEmpty = BPELFactory.eINSTANCE.createEmpty();
		Scope newScope = BPELFactory.eINSTANCE.createScope();
		newScope.setActivity(newEmpty);
		proc.setActivity(newScope);
		Assert.assertEquals(proc, ChoreoMergeUtil.getProcessOfActivity(newEmpty));
		
	}
	
	@Test
	public void topologyResolverTest() {
		ChoreographyMerger choreographyMerger = new ChoreographyMerger(Constants.asyncPattern11Choreo);
		Topology top = choreographyMerger.getChoreographyPackage().getTopology();
		Assert.assertNotNull(ChoreoMergeUtil.getTypeByName("p1Type", top));
		Assert.assertNull(ChoreoMergeUtil.getTypeByName("testy", top));
	}
}
