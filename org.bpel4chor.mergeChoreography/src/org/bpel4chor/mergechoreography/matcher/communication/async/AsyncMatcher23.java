package org.bpel4chor.mergechoreography.matcher.communication.async;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.matcher.communication.AsyncMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern11;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern23;
import org.bpel4chor.mergechoreography.pattern.conditions.Condition;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.mergechoreography.util.MLEnvironmentAnalyzer;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.model.topology.impl.Topology;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Scope;

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
public class AsyncMatcher23 implements AsyncMatcher {
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	/** The {@link MLEnvironment} of the given {@link MessageLink} */
	private MLEnvironment env;
	
	/** The {@link AsyncPattern11} of this {@link AsyncMatcher} */
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
		
		this.pkg = pkg;
		
		// clear previous results
		this.results.clear();
		
		// Test Call to our new LinkEnvironmentAnalyzer
		MLEnvironmentAnalyzer analyzer = new MLEnvironmentAnalyzer(link, pkg);
		this.env = analyzer.getEnvironment();
		
		// Set MessageLink to visited
		pkg.addVisitedLink(link);
		
		// Pass the Information to the Merge Pattern
		this.pattern = new AsyncPattern23(this.env, pkg);
		
		((AsyncPattern23) this.pattern).addMessageLink(link);
		((AsyncPattern23) this.pattern).addMLinkEnv(analyzer.getEnvironment());
		
		// Check for other Message Links sending to same <pick> bit distinct
		// <onMessage>-branches
		if (this.env.getR() instanceof Pick) {
			// r is a <pick> analyse other Message Links for sendActivities to
			// <onMessage>-branches of r
			this.analyseOtherMLinks((Pick) this.env.getR());
		}
		
		// Set the conditions
		Condition cond1 = new Condition(this.env.getR() instanceof Pick);
		Condition cond2 = new Condition(((AsyncPattern23) this.pattern).getmLinks().size() > 1);
		
		this.results.add(cond1.evaluate());
		this.log.info("AsyncMatcher 2.3 R() instanceof Pick : " + cond1.evaluate());
		this.results.add(cond2.evaluate());
		this.log.info("AsyncMatcher 2.3 R() There exist other MessageLinks sending to the same Pick and same <onMessage>-branch : " + cond2.evaluate());
		if (cond2.evaluate()) {
			this.log.info("These MessageLinks are : ");
			for (MessageLink mLink : ((AsyncPattern23) this.pattern).getmLinks()) {
				this.log.info(" => " + mLink.getName());
			}
		}
		
		return this.pattern;
		
	}
	
	/**
	 * Analyse the other {@link MessageLink}s from {@link Topology}
	 * 
	 * @param rec The {@link Pick} to check for other {@link Invoke}s
	 */
	private void analyseOtherMLinks(Pick rec) {
		
		for (MessageLink mLink : this.pkg.getTopology().getMessageLinks()) {
			if (!(this.pkg.isLinkVisited(mLink))) {
				// Test if receiveActivity of mLink is rec
				BPELExtensibleElement recOther = ChoreoMergeUtil.resolveActivity(mLink.getReceiveActivity());
				if ((recOther instanceof OnMessage) && (((OnMessage) recOther).eContainer() == rec)) {
					// Test if sendActivity of mLink is in another <scope> than
					// the other already known sendActivities
					if (this.isNotInSameScope((Invoke) ChoreoMergeUtil.resolveActivity(mLink.getSendActivity()))) {
						// Test if recOther is the same <onMessage>-branch as
						// the already known
						if (((AsyncPattern23) this.pattern).getmLinks().get(0).getReceiveActivity().equals(mLink.getReceiveActivity())) {
							this.pkg.addVisitedLink(mLink);
							MLEnvironmentAnalyzer analyzerSucc = new MLEnvironmentAnalyzer(mLink, this.pkg);
							((AsyncPattern23) this.pattern).addMLinkEnv(analyzerSucc.getEnvironment());
							((AsyncPattern23) this.pattern).addMessageLink(mLink);
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * Check whether given {@link Invoke} is in another {@link Scope} than the
	 * already known
	 * 
	 * @param invoke {@link Invoke} to check
	 * @return true or false
	 */
	private boolean isNotInSameScope(Invoke invoke) {
		for (MLEnvironment mlEnv : ((AsyncPattern23) this.pattern).getLinkEnvs()) {
			Invoke otherInv = (Invoke) mlEnv.getS();
			if (ChoreoMergeUtil.getHighestScopeOfActivity(otherInv) == ChoreoMergeUtil.getHighestScopeOfActivity(invoke)) {
				return false;
			}
		}
		return true;
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
