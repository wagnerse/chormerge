package org.bpel4chor.mergechoreography.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;

import org.apache.log4j.Logger;
import org.eclipse.bpel.model.Variable;

/**
 * Helper class for XPath expression examination
 * 
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class XPathUtil {
	
	private static Logger log = Logger.getLogger(XPathUtil.class);
	
	
	/**
	 * Replace all occurrences of oldVarName in xpathExpr with newVarName
	 * 
	 * @param xpathExpr The {@link XPath} expression String
	 * @param oldVarName The old variable name
	 * @param newVarName The new variable name
	 * @return {@link XPath} expression String with replaced {@link Variable}
	 *         names
	 */
	public static String replaceVariableName(String xpathExpr, String oldVarName, String newVarName) {
		// First replace normal Variable occurrence
		String newExpr = xpathExpr.replaceAll(Pattern.quote("$" + oldVarName) + "(?!\\w{1})\\W??", "\\$" + newVarName);
		// Now replace all occurrences in bpel:getVariableProperty
		newExpr = newExpr.replaceAll("bpel:getVariableProperty\\(\"" + oldVarName + "\"", "bpel:getVariableProperty(\"" + newVarName + "\"");
		newExpr = newExpr.replaceAll("bpel:getVariableProperty\\('" + oldVarName + "'", "bpel:getVariableProperty('" + newVarName + "'");
		return newExpr;
	}
	
	/**
	 * Replace all occurrences of oldVarName in xpathExpr with jc
	 * 
	 * @param xpathExpr The {@link XPath} expression String
	 * @param oldVarName The old variable name
	 * @param expr The new jc
	 * @return {@link XPath} expression String with replaced {@link Variable}
	 *         names
	 */
	public static String replaceVariableByExpr(String xpathExpr, String oldVarName, String expr) {
		// First replace normal Variable occurrence
		XPathUtil.log.info("replaceVariableByExpr xpathExpr : " + xpathExpr);
		XPathUtil.log.info("replaceVariableByExpr oldVarName : " + oldVarName);
		XPathUtil.log.info("replaceVariableByExpr jc : " + expr);
		String newExpr = xpathExpr.replaceAll(Pattern.quote("$" + oldVarName) + "(?!\\w{1})\\W??", "(" + Matcher.quoteReplacement(expr) + ")");
		return newExpr;
	}
	
	/**
	 * Get all {@link Variable} names used in given xpathExpr
	 * 
	 * @param xpathExpr The {@link XPath} expression String to examine
	 * @return {@link List} of all used {@link Variable}names
	 */
	public static List<String> getUsedVariabeNames(String xpathExpr) {
		Set<String> varNames = new HashSet<>();
		// Normal $variablename occurrence
		String pattern1 = Pattern.quote("$") + "(\\w+|_+)" + "(?!\\w{1})" + "\\W??";
		// variable name occurrences in bpel:getVariableProperty
		String pattern2 = "bpel:getVariableProperty\\(\"(\\w+|_+)\"";
		String pattern3 = "bpel:getVariableProperty\\('(\\w+|_+)'";
		for (MatchResult r : XPathUtil.findMatches(pattern1, xpathExpr)) {
			varNames.add(r.group().substring(1, r.group().length()));
		}
		for (MatchResult r : XPathUtil.findMatches(pattern2, xpathExpr)) {
			varNames.add(r.group().substring(r.group().indexOf("\"") + 1, r.group().lastIndexOf("\"")));
		}
		for (MatchResult r : XPathUtil.findMatches(pattern3, xpathExpr)) {
			varNames.add(r.group().substring(r.group().indexOf("'") + 1, r.group().lastIndexOf("'")));
		}
		
		return new ArrayList<>(varNames);
	}
	
	/**
	 * Find matches of given regex pattern in given string
	 * 
	 * @param pattern regex pattern
	 * @param s The string to examine
	 * @return {@link List} of matches
	 */
	private static Iterable<MatchResult> findMatches(String pattern, CharSequence s) {
		List<MatchResult> results = new ArrayList<MatchResult>();
		
		for (Matcher m = Pattern.compile(pattern).matcher(s); m.find();) {
			results.add(m.toMatchResult());
		}
		
		return results;
	}
	
}
