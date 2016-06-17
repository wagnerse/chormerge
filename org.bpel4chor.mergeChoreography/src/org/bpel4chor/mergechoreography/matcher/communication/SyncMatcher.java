package org.bpel4chor.mergechoreography.matcher.communication;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.model.topology.impl.MessageLink;

/**
 * Matcher Interface for Matching BPEL Process Behavior
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public interface SyncMatcher extends Matcher {
	
	public MergePattern match(MessageLink mlSend, MessageLink mlReply, ChoreographyPackage choreographyPackage);
	
}
