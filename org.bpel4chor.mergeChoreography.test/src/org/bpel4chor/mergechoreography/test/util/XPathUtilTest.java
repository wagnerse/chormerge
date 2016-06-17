package org.bpel4chor.mergechoreography.test.util;

import java.util.List;

import org.bpel4chor.mergechoreography.util.XPathUtil;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class XPathUtilTest {
	
	private static String testString1;
	private static String testString2;
	private static String testString3;
	private static String testString4;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		XPathUtilTest.testString1 = "//doc[contains(., $myVar) $myVar]/*/text($myVar/hallo) $myVar.test $myVar1 $myVar $myVar=$myVar bpel:getVariableProperty(\"varA\",\"b:propB\") bpel:getVariableProperty('varA','b:propB')";
		XPathUtilTest.testString2 = "//doc[contains(., $myVar) $myVar]/*/text($myVar/hallo) $myVar.test $myVar1 $myVar $myVar=$myVar bpel:getVariableProperty(\"myVar\",\"b:propB\") bpel:getVariableProperty('myVar','b:propB')";
		XPathUtilTest.testString3 = "//doc[contains(., $myVar1) $Test__Var]/*/text($halloVar3/hallo) $shitVarblablublib.test $hihihiVar233423 $joala_1 $myVar=$myVar bpel:getVariableProperty(\"Test__Var\",\"b:propB\") bpel:getVariableProperty('Test__Var','b:propB')";
		XPathUtilTest.testString4 = "//doc[contains(., $myVar1) $Test__Var]/*/text($halloVar3/hallo) $shitVarblablublib.test $hihihiVar233423 $joala_1 $myVar=$myVar bpel:getVariableProperty(\"varA\",\"b:propB\") bpel:getVariableProperty('varA','b:propB')";
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	@Test
	public void testEquality() {
		String expResultReplacement1 = "//doc[contains(., $myVar) $myVar]/*/text($myVar/hallo) $myVar.test $myVar1 $myVar $myVar=$myVar bpel:getVariableProperty(\"varNew\",\"b:propB\") bpel:getVariableProperty('varNew','b:propB')";
		String expResultReplacement2 = "//doc[contains(., $newVar) $newVar]/*/text($newVar/hallo) $newVar.test $myVar1 $newVar $newVar=$newVar bpel:getVariableProperty(\"newVar\",\"b:propB\") bpel:getVariableProperty('newVar','b:propB')";
		String expResultReplacement3 = "//doc[contains(., $myVar1) $newVar]/*/text($halloVar3/hallo) $shitVarblablublib.test $hihihiVar233423 $joala_1 $myVar=$myVar bpel:getVariableProperty(\"newVar\",\"b:propB\") bpel:getVariableProperty('newVar','b:propB')";
		String resultReplacement1 = XPathUtil.replaceVariableName(XPathUtilTest.testString1, "varA", "varNew");
		String resultReplacement2 = XPathUtil.replaceVariableName(XPathUtilTest.testString2, "myVar", "newVar");
		String resultReplacement3 = XPathUtil.replaceVariableName(XPathUtilTest.testString3, "Test__Var", "newVar");
		Assert.assertEquals(resultReplacement1, expResultReplacement1);
		Assert.assertEquals(resultReplacement2, expResultReplacement2);
		Assert.assertEquals(resultReplacement3, expResultReplacement3);
	}
	
	@Test
	public void testVariableContainmentSize() {
		List<String> varNames1 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString1);
		List<String> varNames2 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString2);
		List<String> varNames3 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString3);
		List<String> varNames4 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString4);
		Assert.assertEquals(varNames1.size(), 3);
		Assert.assertEquals(varNames2.size(), 2);
		Assert.assertEquals(varNames3.size(), 7);
		Assert.assertEquals(varNames4.size(), 8);
	}
	
	@Test
	public void testVariableContainmentNames() {
		List<String> varNames1 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString1);
		List<String> varNames2 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString2);
		List<String> varNames3 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString3);
		List<String> varNames4 = XPathUtil.getUsedVariabeNames(XPathUtilTest.testString4);
		Assert.assertTrue(varNames1.contains("myVar"));
		Assert.assertTrue(varNames1.contains("varA"));
		Assert.assertTrue(varNames1.contains("myVar1"));
		Assert.assertTrue(varNames2.contains("myVar"));
		Assert.assertTrue(varNames2.contains("myVar1"));
		Assert.assertTrue(varNames3.contains("shitVarblablublib"));
		Assert.assertTrue(varNames3.contains("Test__Var"));
		Assert.assertTrue(varNames3.contains("joala_1"));
		Assert.assertTrue(varNames3.contains("myVar"));
		Assert.assertTrue(varNames3.contains("hihihiVar233423"));
		Assert.assertTrue(varNames3.contains("myVar1"));
		Assert.assertTrue(varNames3.contains("halloVar3"));
		Assert.assertTrue(varNames4.contains("shitVarblablublib"));
		Assert.assertTrue(varNames4.contains("Test__Var"));
		Assert.assertTrue(varNames4.contains("joala_1"));
		Assert.assertTrue(varNames4.contains("myVar"));
		Assert.assertTrue(varNames4.contains("hihihiVar233423"));
		Assert.assertTrue(varNames4.contains("myVar1"));
		Assert.assertTrue(varNames4.contains("halloVar3"));
		Assert.assertTrue(varNames4.contains("varA"));
	}
	
}
