package org.bpel4chor.mergechoreography.matcher.communication;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.matcher.communication.async.AsyncMatcher30;
import org.bpel4chor.mergechoreography.matcher.communication.sync.SyncMatcher30;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.ClassComparator;
import org.bpel4chor.mergechoreography.util.ClassLoadingUtil;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.PartnerActivity;
import org.eclipse.bpel.model.Reply;

/**
 * Matcher Class for Matching BPEL Process Behavior
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class CommunicationMatcher implements Serializable {
	
	private static final long serialVersionUID = 471002085247211487L;
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	/** Lists of registered LinkMatcher */
	private List<AsyncMatcher> asyncMatcher = new ArrayList<>();
	private List<SyncMatcher> syncMatcher = new ArrayList<>();
	
	
	/**
	 * Method for detecting matching merge Pattern
	 * 
	 * @param mergedProcess Merged BPEL Process
	 * @param process PBD to be merged in
	 * @param pkg The {@link ChoreographyPackage} holding all data
	 * @return {@link Pattern} to be applied
	 * @throws NoApplicableMatcherFoundException
	 */
	public MergePattern match(MessageLink link, ChoreographyPackage pkg) {
		
		// Check if link.sendActivity is a sync or async call
		this.log.log(Level.INFO, "Running CommunicationMatcher .....");
		this.log.log(Level.INFO, "For MessageLink => " + link.getName());
		
		PartnerActivity send = (PartnerActivity) ChoreoMergeUtil.resolveActivity(link.getSendActivity());
		
		// Check if we have a Reply-Link
		if (send instanceof Reply) {
			return null;
		}
		
		// Check if we have Async Communication
		if (send instanceof Invoke) {
			
			if (((Invoke) send).getOutputVariable() == null) {
				// we have async communication
				this.log.info("Async Invoke found, now running AsyncMatcher ....");
				List<Matcher> matches = new ArrayList<>();
				for (AsyncMatcher matcher : this.asyncMatcher) {
					if (matcher instanceof AsyncMatcher30) {
						this.log.info("Checking asyncMatcher : "
								+ matcher.getClass().getName()
								+ " for MLink : " + link.getName());
						this.log.info("Skipping asyncMatcher : "
								+ matcher.getClass().getName()
								+ " for MLink : " + link.getName());
						continue;
					}
					this.log.info("Checking asyncMatcher : "
							+ matcher.getClass().getName() + " for MLink : "
							+ link.getName());
					matcher.match(link, pkg);
					// Check if Matcher is instanceof AsyncMatcher3.0 and just
					// one forbidden condition is true
					if ((matcher instanceof AsyncMatcher30)
							&& (this.oneConditionTrue(matcher
									.evaluateConditions()))) {
						// skip the link
						return null;
					}
					if (this.allConditionsTrue(matcher.evaluateConditions())) {
						this.log.info("AsyncMatcher => "
								+ matcher.getClass().getName()
								+ " all conditions true !!");
						matches.add(matcher);
					}
				}
				MergePattern bestMatch = this.getBestMatch(matches);
				this.log.info("AsyncMatcher best match for MLink : "
						+ link.getName()
						+ "  => "
						+ (bestMatch != null ? bestMatch.getClass().getName()
								: null));
				return bestMatch;
			} else {
				// we must have sync communication
				this.log.info("Sync Invoke found, now running SyncMatcher ....");
				List<Matcher> matches = new ArrayList<>();
				
				// find a replying Message Link
				MessageLink mlSend = link;
				MessageLink mlReply = ChoreoMergeUtil.findReplyingMessageLink(mlSend);
				
				for (SyncMatcher matcher : this.syncMatcher) {
					this.log.info("Checking syncMatcher : "
							+ matcher.getClass().getName()
							+ " for MLinkSend : " + mlSend.getName()
							+ " and MLinkReply : " + mlReply.getName());
					matcher.match(mlSend, mlReply, pkg);
					// Check if Matcher is instanceof SyncMatcher3.0 and just
					// one forbidden condition is true
					if ((matcher instanceof SyncMatcher30) && (this.oneConditionTrue(matcher.evaluateConditions()))) {
						// skip the link
						return null;
					}
					if (this.allConditionsTrue(matcher.evaluateConditions())) {
						this.log.info("SyncMatcher => "
								+ matcher.getClass().getName()
								+ " all conditions true !!");
						matches.add(matcher);
					}
				}
				MergePattern bestMatch = this.getBestMatch(matches);
				this.log.info("SyncMatcher best match for MLinkSend : "
						+ mlSend.getName()
						+ " and MLinkReply : "
						+ mlReply.getName()
						+ "  => "
						+ (bestMatch != null ? bestMatch.getClass().getName()
								: null));
				return bestMatch;
			}
		}
		return null;
	}
	
	public CommunicationMatcher() {
		this.initMatcher();
	}
	
	/**
	 * (Clear if needed) and load the Matcher into the corresponding lists
	 */
	private void initMatcher() {
		// this.asyncMatcher.clear();
		// this.syncMatcher.clear();
		try {
			Class<?>[] asyncMatchers = ClassLoadingUtil.getClasses("org.bpel4chor.mergechoreography.matcher.communication.async");
			Class<?>[] syncMatchers = ClassLoadingUtil.getClasses("org.bpel4chor.mergechoreography.matcher.communication.sync");
			Arrays.sort(syncMatchers, new ClassComparator());
			Arrays.sort(asyncMatchers, new ClassComparator());
			for (Class<?> class1 : asyncMatchers) {
				this.log.info("CommunicationMatcher: adding asyncMatcher to List: " + class1.getName());
				this.asyncMatcher.add((AsyncMatcher) class1.newInstance());
			}
			for (Class<?> class1 : syncMatchers) {
				this.log.info("CommunicationMatcher: adding syncMatcher to List: " + class1.getName());
				this.syncMatcher.add((SyncMatcher) class1.newInstance());
			}
		} catch (ClassNotFoundException | IOException | InstantiationException
				| IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check whether all results are true
	 * 
	 * @param results {@link List} of {@link Boolean}
	 * @return true or false
	 */
	private boolean allConditionsTrue(List<Boolean> results) {
		if (results.size() == 0) {
			return false;
		}
		for (Boolean result : results) {
			if (result == false) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Check whether one result is true
	 * 
	 * @param results {@link List} of {@link Boolean}
	 * @return
	 */
	private boolean oneConditionTrue(List<Boolean> results) {
		for (Boolean result : results) {
			if (result == true) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Return the best matching {@link MergePattern}
	 * 
	 * @param matches {@link List} of {@link AsyncMatcher}s
	 * @return {@link MergePattern} or null
	 */
	private MergePattern getBestMatch(List<Matcher> matches) {
		int highestMatchSize = 0;
		MergePattern bestMatch = null;
		for (Matcher matcher : matches) {
			this.log.info("getBestMatch evaluating matcher " + matcher.getClass().getCanonicalName());
			if (matcher.evaluateConditions().size() > highestMatchSize) {
				this.log.info("" + matcher.evaluateConditions().size() + " > " + highestMatchSize);
				highestMatchSize = matcher.evaluateConditions().size();
				bestMatch = matcher.getPattern();
			}
		}
		return bestMatch;
	}
}
