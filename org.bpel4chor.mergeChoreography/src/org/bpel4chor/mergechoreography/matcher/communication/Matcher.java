package org.bpel4chor.mergechoreography.matcher.communication;

import org.bpel4chor.mergechoreography.pattern.MergePattern;

public interface Matcher extends Evaluator {
	
	public MergePattern getPattern();
}
