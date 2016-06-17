package org.bpel4chor.mergechoreography.matcher.communication.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.matcher.communication.AsyncMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern15;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern16;
import org.bpel4chor.mergechoreography.pattern.conditions.Condition;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.mergechoreography.util.MLEnvironmentAnalyzer;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.model.topology.impl.Topology;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;

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
public class AsyncMatcher16 implements AsyncMatcher {
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	/** The {@link MLEnvironment} of the given {@link MessageLink} */
	private MLEnvironment env;
	
	/** The {@link AsyncPattern15} of this {@link AsyncMatcher} */
	private MergePattern pattern = null;
	
	/** The results of the evaluation of conditions */
	private List<Boolean> results = new ArrayList<>();
	
	/** The ChoreographyPackage containing all information */
	private ChoreographyPackage pkg;
	
	
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
		
		this.pkg = pkg;
		
		// Test Call to our new LinkEnvironmentAnalyzer
		MLEnvironmentAnalyzer analyzer = new MLEnvironmentAnalyzer(link, pkg);
		this.env = analyzer.getEnvironment();
		
		// Set MessageLink to visited
		pkg.addVisitedLink(link);
		
		// Pass the Information to the Merge Pattern
		this.pattern = new AsyncPattern16(this.env, pkg);
		
		((AsyncPattern16) this.pattern).addMLinkEnv(analyzer.getEnvironment());
		((AsyncPattern16) this.pattern).addMessageLink(link);
		
		this.analyseOtherMLinks((Invoke) this.env.getS());
		
		AsyncPattern16 pattern16 = (AsyncPattern16) this.pattern;
		
		// Set the conditions
		Condition cond1 = new Condition(this.env.getR() instanceof Receive);
		Condition cond2 = new Condition(pattern16.getSuccLinks().size() > 1);
		
		this.results.add(cond1.evaluate());
		this.log.info("AsyncMatcher 1.6 R() instanceof Receive : " + cond1.evaluate());
		this.log.info("AsyncMatcher 1.6 Check if there are other <invoke>s following s communicating choreography-internally : ");
		this.results.add(cond2.evaluate());
		this.log.info("AsyncMatcher 1.6 succLinks.size() > 1 : " + cond2.evaluate());
		if (cond2.evaluate()) {
			this.log.info("AsyncMatcher 1.6 succLinks.size()  : " + pattern16.getSuccLinks().size());
			for (MessageLink succLink : pattern16.getSuccLinks()) {
				this.results.add(true);
				this.log.info("AsyncMatcher 1.6 succLink    : " + succLink.getName());
				this.log.info("AsyncMatcher 1.6 succS       : " + succLink.getSendActivity());
			}
		}
		
		return this.pattern;
		
	}
	
	/**
	 * Analyse the other {@link MessageLink}s from {@link Topology}
	 * 
	 * @param inv The {@link Invoke} to check for succeeding {@link Invoke}s
	 */
	private void analyseOtherMLinks(Invoke inv) {
		
		// Check if there exist other Message Links which are not visited and
		// whose sendActivity is in succ(S)
		MessageLink succLink = null;
		
		// <invoke> s
		Invoke s = inv;
		
		// possible succeeding <invoke> after s
		Invoke succS = null;
		
		for (MessageLink mLink : this.pkg.getTopology().getMessageLinks()) {
			if (!(this.pkg.isLinkVisited(mLink))) {
				if (ChoreoMergeUtil.resolveActivity(mLink.getSendActivity()) instanceof Invoke) {
					Invoke otherS = (Invoke) ChoreoMergeUtil.resolveActivity(mLink.getSendActivity());
					this.log.info("Checking mLink : " + mLink.getName() + " for s : " + s.getName() + " otherS : " + otherS.getName());
					
					// Check if sendActivity sLink from mLink is asynchronously
					// communicating
					if (otherS.getOutputVariable() == null) {
						
						// Now check if otherS is in succ(s)
						boolean isOtherSinSuccS = false;
						if (ChoreoMergeUtil.getSucceedingActivities(s) != null) {
							for (Activity succSact : ChoreoMergeUtil.getSucceedingActivities(s)) {
								if (succSact == otherS) {
									isOtherSinSuccS = true;
									break;
								}
							}
						}
						if (isOtherSinSuccS) {
							// Check if otherS is connected via link with s and
							// if
							// this <source> has no <transitionCondition> set
							Source sToOtherS = ChoreoMergeUtil.getMatchingSource(s, otherS);
							
							this.log.info("otherS is in succ(s).");
							
							boolean hasOtherSmoreTargets = false;
							boolean hasSToOtherSTCs = false;
							
							if (sToOtherS != null) {
								if (sToOtherS.getTransitionCondition() == null) {
									// Check if otherS has any other <target>s
									Target fromS = ChoreoMergeUtil.getMatchingTarget(s, otherS);
									
									if (fromS != null) {
										for (Target target : otherS.getTargets().getChildren()) {
											if (fromS != target) {
												hasOtherSmoreTargets = true;
												break;
											}
										}
									}
								} else {
									hasSToOtherSTCs = true;
								}
							} else {
								// It MUST be an activity succeeding s in a
								// sequence
								// Check if otherS has <target>s
								if ((otherS.getTargets() != null) && (otherS.getTargets().getChildren().size() > 0)) {
									hasOtherSmoreTargets = true;
								}
							}
							if (!hasOtherSmoreTargets && !hasSToOtherSTCs) {
								succS = otherS;
								succLink = mLink;
								this.pkg.addVisitedLink(mLink);
								MLEnvironmentAnalyzer analyzerSucc = new MLEnvironmentAnalyzer(mLink, this.pkg);
								((AsyncPattern16) this.pattern).addMLinkEnv(analyzerSucc.getEnvironment());
								((AsyncPattern16) this.pattern).addMessageLink(mLink);
								
								// Check following activities
								this.analyseOtherMLinks(succS);
								break;
							}
						}
					}
				}
			}
		}
		
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
