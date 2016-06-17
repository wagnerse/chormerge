package org.bpel4chor.mergechoreography.matcher.communication.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.matcher.communication.AsyncMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern11;
import org.bpel4chor.mergechoreography.pattern.conditions.Condition;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.OnEvent;

/**
 * Matcher Class for Matching BPEL Process Behavior (Async)
 * 
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class AsyncMatcher30 implements AsyncMatcher {
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	/** The {@link MLEnvironment} of the given {@link MessageLink} */
	private MLEnvironment env;
	
	/** The {@link AsyncPattern11} of this {@link AsyncMatcher} */
	private MergePattern pattern = null;
	
	/** The results of the evaluation of conditions */
	private List<Boolean> results = new ArrayList<>();
	
	
	/**
	 * Method for detecting matching merge Pattern
	 * 
	 * @param link {@link MessageLink} to be analyzed
	 * @param pkg The {@link ChoreographyPackage} holding all data
	 * @return {@link CommunicationPattern} to be applied
	 */
	@Override
	public MergePattern match(MessageLink link, ChoreographyPackage pkg) {
		
		// clear previous results
		this.results.clear();
		
		// Add link to list of visited Links
		pkg.addVisitedLink(link);
		
		// Resolve send- and receiveActivity
		BPELExtensibleElement s = ChoreoMergeUtil.resolveActivity(link.getSendActivity());
		BPELExtensibleElement r = ChoreoMergeUtil.resolveActivity(link.getReceiveActivity());
		
		// s is an <invoke>, check if it is inside an EH or FH
		
		// check if r is an EH
		if (r instanceof OnEvent) {
			this.log.info("r is <onEvent> !! : " + r);
			Condition cond = new Condition(true);
			this.results.add(cond.evaluate());
			pkg.addNMML(link);
		}
		
		// check if s is in an EH or CH
		if (ChoreoMergeUtil.isElementInCEHandler(s)) {
			this.log.info("s is in EventHandler or CompensationHandler !! : " + s);
			Condition cond = new Condition(true);
			this.results.add(cond.evaluate());
			pkg.addNMML(link);
		}
		
		// check if r is FCTE-Handler
		// if (ChoreoMergeUtil.isElementInFCTEHandler(r)) {
		// this.log.info("r is in FCTE-Handler !! : " + r);
		// Condition cond = new Condition(true);
		// this.results.add(cond.evaluate());
		// pkg.addNMML(link);
		// }
		
		// check if s or r are in a Loop
		if (ChoreoMergeUtil.isElementInLoop(s) || ChoreoMergeUtil.isElementInLoop(r)) {
			this.log.info("s and/or r are in a Loop !! Is s in Loop : " + ChoreoMergeUtil.isElementInLoop(s) + " . Is r in Loop : " + ChoreoMergeUtil.isElementInLoop(r));
			Condition cond = new Condition(true);
			this.results.add(cond.evaluate());
			pkg.addNMML(link);
		}
		
		return this.pattern;
		
	}
	
	@Override
	public List<Boolean> evaluateConditions() {
		return this.results;
	}
	
	@Override
	public MergePattern getPattern() {
		return this.pattern;
	}
}
