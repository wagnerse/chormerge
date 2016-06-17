package org.bpel4chor.mergechoreography.matcher.communication.sync;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.matcher.communication.AsyncMatcher;
import org.bpel4chor.mergechoreography.matcher.communication.SyncMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern11;
import org.bpel4chor.mergechoreography.pattern.conditions.Condition;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.OnEvent;

/**
 * Matcher Class for Matching BPEL Process Behavior (Sync)
 * 
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class SyncMatcher30 implements SyncMatcher {
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	/** The {@link MLEnvironment} of the given {@link MessageLink} mlSend */
	private MLEnvironment envSend;
	
	/** The {@link MLEnvironment} of the given {@link MessageLink} mReply */
	private MLEnvironment envReply;
	
	/** The {@link AsyncPattern11} of this {@link AsyncMatcher} */
	private MergePattern pattern = null;
	
	/** The results of the evaluation of conditions */
	private List<Boolean> results = new ArrayList<>();
	
	/** The ChoreographyPackage containing all information */
	private ChoreographyPackage pkg;
	
	
	@Override
	public List<Boolean> evaluateConditions() {
		return this.results;
	}
	
	@Override
	public MergePattern match(MessageLink mlSend, MessageLink mlReply, ChoreographyPackage pkg) {
		
		this.pkg = pkg;
		
		// clear previous results
		this.results.clear();
		
		// Add links to list of visited Links
		pkg.addVisitedLink(mlSend);
		pkg.addVisitedLink(mlReply);
		
		// Resolve send- and receiveActivity
		BPELExtensibleElement s = ChoreoMergeUtil.resolveActivity(mlSend.getSendActivity());
		BPELExtensibleElement r = ChoreoMergeUtil.resolveActivity(mlSend.getReceiveActivity());
		
		// s is an <invoke>, check if it is inside an EH or FH
		
		// check if r is an EH
		if (r instanceof OnEvent) {
			this.log.info("r is <onEvent> !! : " + r);
			Condition cond = new Condition(true);
			this.results.add(cond.evaluate());
			pkg.addNMML(mlSend);
			pkg.addNMML(mlReply);
		}
		
		// check if r is FCTE-Handler
		// if (ChoreoMergeUtil.isElementInFCTEHandler(s) ||
		// ChoreoMergeUtil.isElementInFCTEHandler(r)) {
		// this.log.info("s and/or r are in FCTE-Handler !! Is s in FCTE-Handler : "
		// + ChoreoMergeUtil.isElementInFCTEHandler(s) +
		// " . Is r in FCTE-Handler : " +
		// ChoreoMergeUtil.isElementInFCTEHandler(r));
		// Condition cond = new Condition(true);
		// this.results.add(cond.evaluate());
		// pkg.addNMML(mlSend);
		// pkg.addNMML(mlReply);
		// }
		
		// check if s or r are in a Loop
		if (ChoreoMergeUtil.isElementInLoop(s) || ChoreoMergeUtil.isElementInLoop(r)) {
			this.log.info("s and/or r are in a Loop !! Is s in Loop : " + ChoreoMergeUtil.isElementInLoop(s) + " . Is r in Loop : " + ChoreoMergeUtil.isElementInLoop(r));
			Condition cond = new Condition(true);
			this.results.add(cond.evaluate());
			pkg.addNMML(mlSend);
			pkg.addNMML(mlReply);
		}
		
		// check if one condition is true, if so find other replying Message
		// Links and add them to NMML too
		if (this.results.size() > 0) {
			this.findOtherReplyingMessageLinks(mlSend);
		}
		
		return this.pattern;
	}
	
	/**
	 * Find other replying {@link MessageLink} as the already known
	 * 
	 * @param ml {@link MessageLink} with sendActivity
	 */
	private void findOtherReplyingMessageLinks(MessageLink ml) {
		List<MessageLink> replyLinks = new ArrayList<>();
		for (MessageLink messageLink : this.pkg.getTopology().getMessageLinks()) {
			if (!this.pkg.isLinkVisited(messageLink)) {
				if (messageLink.getReceiveActivity().equals(ml.getSendActivity())) {
					this.log.info("Found replying message link for ml: " + ml.getName() + " with sendActivity : " + ml.getSendActivity());
					this.log.info("=>: " + messageLink.getName());
					replyLinks.add(messageLink);
					this.pkg.addVisitedLink(messageLink);
					this.pkg.addNMML(messageLink);
				}
			}
		}
	}
	
	@Override
	public MergePattern getPattern() {
		return this.pattern;
	}
	
}
