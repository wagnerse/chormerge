package org.bpel4chor.mergechoreography.matcher.communication.async;

import java.util.ArrayList;
import java.util.List;

import javax.wsdl.Part;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.matcher.communication.AsyncMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern15;
import org.bpel4chor.mergechoreography.pattern.communication.async.AsyncPattern17;
import org.bpel4chor.mergechoreography.pattern.conditions.Condition;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.mergechoreography.util.MLEnvironmentAnalyzer;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Variable;
import org.eclipse.emf.ecore.EObject;

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
public class AsyncMatcher17 implements AsyncMatcher {
	
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
		
		this.pkg = pkg;
		
		// clear previous results
		this.results.clear();
		
		// Test Call to our new LinkEnvironmentAnalyzer
		MLEnvironmentAnalyzer analyzer = new MLEnvironmentAnalyzer(link, pkg);
		this.env = analyzer.getEnvironment();
		
		// Set MessageLink to visited
		pkg.addVisitedLink(link);
		
		// Pass the Information to the Merge Pattern
		this.pattern = new AsyncPattern17(this.env, pkg);
		
		// Check if <invoke> s is the second element inside a <sequence> with
		// suppressJoinFailure=no
		Invoke s = (Invoke) this.env.getS();
		Receive r = null;
		Sequence seqS = null;
		Variable statusVar = null;
		Part statusVarPart = null;
		Scope surScope = null;
		String linkName = null;
		Sequence seqFHS = null;
		Invoke sFH = null;
		boolean noOtherSendingMLinks = false;
		MessageLink fhMlink = null;
		boolean receiverSideMatch = false;
		
		EObject sContainer = s.eContainer();
		
		if ((sContainer instanceof Sequence) && (((Sequence) sContainer).isSetSuppressJoinFailure()) && (((Sequence) sContainer).getSuppressJoinFailure() == false)) {
			Sequence seq1 = (Sequence) sContainer;
			
			// Check if sequence has just one <target>
			if ((seq1.getTargets() != null) && (seq1.getTargets().getChildren().size() == 1)) {
				seqS = seq1;
				linkName = seqS.getTargets().getChildren().get(0).getLink().getName();
				
				// Check if s is second element in seqS with preceding <assign>
				// activity
				if ((seqS.getActivities().indexOf(s) == 1) && (seqS.getActivities().get(0) instanceof Assign)) {
					Assign preS = (Assign) seqS.getActivities().get(0);
					
					// Check if preS has only one <copy> with
					// <from>true()</from><to>statusVar</to>
					if ((preS.getCopy().size() == 1)) {
						Copy copy = preS.getCopy().get(0);
						if (copy.getFrom().getExpression().getBody().equals("true()") && (copy.getTo().getVariable() != null) && (copy.getTo().getVariable().getName().equals(s.getInputVariable().getName()))) {
							statusVar = copy.getTo().getVariable();
							if (copy.getTo().getPart() != null) {
								statusVarPart = copy.getTo().getPart();
							}
						}
					}
				}
				
				// Check if seqS is surrounded by <scope> with FaultHandler and
				// one
				// <target>
				EObject seqSContainer = seqS.eContainer();
				
				if (seqSContainer instanceof Scope) {
					
					Scope scpSeqS = (Scope) seqSContainer;
					// Check if <scope> has FaultHandler defined for
					// bpel:joinFailure
					if ((scpSeqS.getFaultHandlers() != null) && (scpSeqS.getFaultHandlers().getCatch().size() == 1)) {
						Catch fh = scpSeqS.getFaultHandlers().getCatch().get(0);
						QName newQName = new QName("", "joinFailure", "");
						if (fh.getFaultName().equals(newQName)) {
							surScope = scpSeqS;
							
							// Check if fh has a <sequence> with another
							// combination of <assign>/<invoke>
							if (fh.getActivity() instanceof Sequence) {
								seqFHS = (Sequence) fh.getActivity();
								if (seqFHS.getActivities().size() == 2) {
									// Check if first activity in seqFHS is
									// <assign>
									if (seqFHS.getActivities().get(0) instanceof Assign) {
										Assign preSFH = (Assign) seqFHS.getActivities().get(0);
										
										// Check if preSFH has only one <copy>
										// with
										// <from>false()</from><to>statusVar</to>
										if ((preSFH.getCopy().size() == 1)) {
											Copy copy = preSFH.getCopy().get(0);
											if (copy.getFrom().getExpression().getBody().equals("false()") && (copy.getTo().getVariable() != null) && (copy.getTo().getVariable().getName().equals(s.getInputVariable().getName()))) {
												
												// Check if second activity in
												// seqFHS is <invoke> using the
												// same variable as s
												if (seqFHS.getActivities().get(1) instanceof Invoke) {
													Invoke fhSeq = (Invoke) seqFHS.getActivities().get(1);
													if (fhSeq.getInputVariable().getName().equals(s.getInputVariable().getName())) {
														sFH = fhSeq;
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		
		// Check that r is a <receive>
		if (this.env.getR() instanceof Receive) {
			r = (Receive) this.env.getR();
			
			// Find the other MLink with sendActivity sFH and r as
			// receiveActivity
			if (sFH != null) {
				// If there's more than one, it's not the right pattern !
				List<MessageLink> mLinks = new ArrayList<>();
				for (MessageLink messageLink : pkg.getTopology().getMessageLinks()) {
					if (!pkg.isLinkVisited(messageLink)) {
						if (ChoreoMergeUtil.resolveActivity(messageLink.getReceiveActivity()) == r) {
							mLinks.add(messageLink);
						}
					}
				}
				if (mLinks.size() == 1) {
					noOtherSendingMLinks = true;
					fhMlink = mLinks.get(0);
					// Check if r has just one <source> with the same name as
					// linkName and the
					// <transitionCondition>="$rVar.statusVarPart=true()"
					if ((r.getSources() != null) && (r.getSources().getChildren().size() == 1)) {
						Source source = r.getSources().getChildren().get(0);
						if (source.getLink().getName().equals(linkName) && (source.getTransitionCondition() != null)) {
							org.eclipse.bpel.model.Condition cond = source.getTransitionCondition();
							String comp = "$" + r.getVariable().getName() + "." + statusVarPart.getName() + "=true()";
							if (cond.getBody().equals(comp)) {
								receiverSideMatch = true;
							}
						}
					}
				}
			}
		}
		
		if ((r != null) && (seqS != null) && (statusVar != null) && (surScope != null) && (seqFHS != null) && (sFH != null) && noOtherSendingMLinks && receiverSideMatch && (fhMlink != null) && (linkName != null)) {
			this.pkg.addVisitedLink(fhMlink);
			MLEnvironmentAnalyzer analyzerFH = new MLEnvironmentAnalyzer(fhMlink, this.pkg);
			((AsyncPattern17) this.pattern).setFhMlinkEnv(analyzerFH.getEnvironment());
			((AsyncPattern17) this.pattern).setFhMlink(fhMlink);
		}
		
		// Set the conditions
		Condition cond1 = new Condition(seqS != null);
		Condition cond2 = new Condition(statusVar != null);
		Condition cond3 = new Condition(surScope != null);
		Condition cond4 = new Condition(seqFHS != null);
		Condition cond5 = new Condition(sFH != null);
		Condition cond6 = new Condition(fhMlink != null);
		Condition cond7 = new Condition(noOtherSendingMLinks);
		Condition cond8 = new Condition(receiverSideMatch);
		Condition cond9 = new Condition(linkName != null);
		
		this.results.add(cond1.evaluate());
		this.log.info("AsyncMatcher 1.7 s is in a <sequence> with suppressJoinFailure=no set : " + cond1.evaluate());
		this.results.add(cond2.evaluate());
		this.log.info("AsyncMatcher 1.7 with one preceding <assign> copying true() to variable : " + cond2.evaluate());
		this.results.add(cond3.evaluate());
		this.log.info("AsyncMatcher 1.7 surrounding <scope> has <catch>-Fault Handler for bpel:joinFailure : " + cond3.evaluate());
		this.results.add(cond4.evaluate());
		this.log.info("AsyncMatcher 1.7 <catch> has <sequence> : " + cond4.evaluate());
		this.results.add(cond5.evaluate());
		this.log.info("AsyncMatcher 1.7 sFH is in <sequence> in <catch> after <assign> copying false() to same variable : " + cond5.evaluate());
		this.results.add(cond6.evaluate());
		this.log.info("AsyncMatcher 1.7 Other Message Link with sFH as sendActivity is : " + cond6.evaluate());
		this.results.add(cond7.evaluate());
		this.log.info("AsyncMatcher 1.7 There are no other message Links with receiveActivity r : " + cond7.evaluate());
		this.results.add(cond8.evaluate());
		this.log.info("AsyncMatcher 1.7 Receiving r has one <source> with check for true() in variable : " + cond8.evaluate());
		this.results.add(cond9.evaluate());
		this.log.info("AsyncMatcher 1.7 seqS use same linkName as <source> of r : " + cond9.evaluate());
		
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
