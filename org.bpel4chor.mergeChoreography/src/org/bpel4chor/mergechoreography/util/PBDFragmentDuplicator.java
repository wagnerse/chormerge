package org.bpel4chor.mergechoreography.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.CatchAll;
import org.eclipse.bpel.model.Compensate;
import org.eclipse.bpel.model.CompensateScope;
import org.eclipse.bpel.model.CompensationHandler;
import org.eclipse.bpel.model.CompletionCondition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Correlation;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Else;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.EventHandler;
import org.eclipse.bpel.model.Exit;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.FromPart;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Links;
import org.eclipse.bpel.model.MessageExchange;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnEvent;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.OpaqueActivity;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Rethrow;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Sources;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Targets;
import org.eclipse.bpel.model.TerminationHandler;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.ToPart;
import org.eclipse.bpel.model.Validate;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.Wait;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.util.WSDLUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Message;

import de.uni_stuttgart.iaas.bpel.model.utilities.FragmentDuplicator;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

/**
 * The PBDFragmentDuplicator helps copying PBD Communication Activities
 * (Receive, Invoke, Reply, ForEach), for the rest the normal
 * {@link FragmentDuplicator} is used
 * 
 * @since Oktober 15, 2012
 * @author Peter Debicki
 */
public class PBDFragmentDuplicator {
	
	protected static Logger log = Logger.getLogger(PBDFragmentDuplicator.class);
	public static PBDFragmentDuplicatorExtension pbdFragmentDuplicatorExtension = new PBDFragmentDuplicatorExtension();
	
	private static ChoreographyPackage pkg = null;
	
	
	/**
	 * Copy the original activity and return a new one.
	 * 
	 * <p>
	 * 1. Note that this copy is not recursively, because we just need a copy of
	 * the activity currently given, not its children.
	 * 
	 * @param origAct The original activity
	 * @param fragProc The fragment process
	 * @param fragDefn The fragment definition
	 * @return The new activity
	 */
	public static Activity copyActivity(Activity origAct) {
		
		PBDFragmentDuplicator.log.info("Calling copyActivity(Activity origAct) for origAct => " + origAct);
		
		if (origAct == null) {
			throw new NullPointerException("argument is null");
		}
		
		Activity newActivity = null;
		
		if (origAct instanceof Receive) {
			newActivity = PBDFragmentDuplicator.copyActivity((Receive) origAct);
		} else if (origAct instanceof Invoke) {
			newActivity = PBDFragmentDuplicator.copyActivity((Invoke) origAct);
		} else if (origAct instanceof Reply) {
			newActivity = PBDFragmentDuplicator.copyActivity((Reply) origAct);
		} else if (origAct instanceof Scope) {
			newActivity = PBDFragmentDuplicator.copyActivity((Scope) origAct);
		} else if (origAct instanceof Flow) {
			newActivity = PBDFragmentDuplicator.copyActivity((Flow) origAct);
		} else if (origAct instanceof ForEach) {
			newActivity = PBDFragmentDuplicator.copyActivity((ForEach) origAct);
		} else if (origAct instanceof If) {
			newActivity = PBDFragmentDuplicator.copyActivity((If) origAct);
		} else if (origAct instanceof Pick) {
			newActivity = PBDFragmentDuplicator.copyActivity((Pick) origAct);
		} else if (origAct instanceof RepeatUntil) {
			newActivity = PBDFragmentDuplicator.copyActivity((RepeatUntil) origAct);
		} else if (origAct instanceof Sequence) {
			newActivity = PBDFragmentDuplicator.copyActivity((Sequence) origAct);
		} else if (origAct instanceof While) {
			newActivity = PBDFragmentDuplicator.copyActivity((While) origAct);
		} else if (origAct instanceof Assign) {
			newActivity = PBDFragmentDuplicator.copyActivity((Assign) origAct);
		} else if (origAct instanceof Compensate) {
			newActivity = PBDFragmentDuplicator.copyActivity((Compensate) origAct);
		} else if (origAct instanceof CompensateScope) {
			newActivity = PBDFragmentDuplicator.copyActivity((CompensateScope) origAct);
		} else if (origAct instanceof Empty) {
			newActivity = PBDFragmentDuplicator.copyActivity((Empty) origAct);
		} else if (origAct instanceof Exit) {
			newActivity = PBDFragmentDuplicator.copyActivity((Exit) origAct);
		} else if (origAct instanceof OpaqueActivity) {
			newActivity = PBDFragmentDuplicator.copyActivity((OpaqueActivity) origAct);
		} else if (origAct instanceof Rethrow) {
			newActivity = PBDFragmentDuplicator.copyActivity((Rethrow) origAct);
		} else if (origAct instanceof Throw) {
			newActivity = PBDFragmentDuplicator.copyActivity((Throw) origAct);
		} else if (origAct instanceof Validate) {
			newActivity = PBDFragmentDuplicator.copyActivity((Validate) origAct);
		} else if (origAct instanceof Wait) {
			newActivity = PBDFragmentDuplicator.copyActivity((Wait) origAct);
		}
		
		// If origAct is instance of PartnerActivity add it to old2New Map
		String wsuID = null;
		if (origAct.getElement() != null)
			if (origAct.getElement().getAttribute("wsu:id") != null)
				wsuID = origAct.getElement().getAttribute("wsu:id");
		if (((wsuID) != null) && (!wsuID.equals(""))) {
			PBDFragmentDuplicator.pkg.addOld2NewRelation(origAct.getElement()
					.getAttribute("wsu:id"), newActivity);

			// We also set the name if the newActivity to the wsu:id to make it
			// unique
			newActivity.setName(origAct.getElement().getAttribute("wsu:id"));
		}

		return newActivity;
	}
	
	/**
	 * Copy the flow and return a new one
	 * <p>
	 * <b>Note</b>: only the flow structure is copied, the children activities
	 * will NOT be copied together.
	 * 
	 * @param act The flow activity
	 * @return The new flow activity
	 */
	public static Flow copyActivity(Flow act) {
		
		if (act == null) {
			throw new NullPointerException("argument is null");
		}
		
		Flow newFlow = BPELFactory.eINSTANCE.createFlow();
		FragmentDuplicator.copyStandardAttributes(act, newFlow);
		
		// // Insert newFlow in our pbd2MergdFlows-Map
		// PBDFragmentDuplicator.log.info("Adding <flow>-<flow> relation for : "
		// + act + " , and : " + newFlow);
		// PBDFragmentDuplicator.pkg.getPbd2MergedFlows().put(act, newFlow);
		
		Links links = BPELFactory.eINSTANCE.createLinks();
		newFlow.setLinks(links);
		
		// Copy Links
		if (act.getLinks() != null) {
			for (Link oldLink : act.getLinks().getChildren()) {
				Link newLink = BPELFactory.eINSTANCE.createLink();
				newLink.setName(oldLink.getName());
				newFlow.getLinks().getChildren().add(newLink);
				// Add <link>-to-<link> relation to our
				// pbd2MergdLinks-Map
				PBDFragmentDuplicator.pkg.getPbd2MergedLinks().put(oldLink, newLink);
			}
		}
		
		// Copy Children
		for (Activity subAct : act.getActivities()) {
			Activity newActivity = PBDFragmentDuplicator.copyActivity(subAct);
			newFlow.getActivities().add(newActivity);
			// Resolve Links to containing Flow
			// ChoreoMergeUtil.resolveLinksToOwnerFlow(newActivity);
		}
		
		PBDFragmentDuplicator.copyStandardElements(act, newFlow);
		return newFlow;
	}
	
	/**
	 * Copy the original reply activity and return a new one.
	 * 
	 * @param origAct
	 * @return
	 */
	public static Receive copyActivity(Receive origAct) {
		
		if (origAct == null) {
			return null;
		}
		
		Receive newReceive = BPELFactory.eINSTANCE.createReceive();
		FragmentDuplicator.copyStandardAttributes(origAct, newReceive);
		PBDFragmentDuplicator.copyStandardElements(origAct, newReceive);
		
		// createInstance-Attribute
		if (origAct.getCreateInstance() != null) {
			newReceive.setCreateInstance(origAct.getCreateInstance());
		}
		
		// variable
		if (origAct.getVariable() != null) {
			newReceive.setVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(origAct.getVariable()));
		}
		
		// correlations
		if ((origAct.getCorrelations() != null) && (origAct.getCorrelations().getChildren().size() > 0)) {
			newReceive.setCorrelations(BPELFactory.eINSTANCE.createCorrelations());
			for (Correlation correlation : origAct.getCorrelations().getChildren()) {
				Correlation newCorrelation = PBDFragmentDuplicator.copyCorrelation(correlation);
				newReceive.getCorrelations().getChildren().add(newCorrelation);
			}
		}
		
		// message exchange
		if (origAct.getMessageExchange() != null) {
			newReceive.setMessageExchange(origAct.getMessageExchange());
		}
		
		// fromParts
		if ((origAct.getFromParts() != null) && (origAct.getFromParts().getChildren().size() > 0)) {
			newReceive.setFromParts(BPELFactory.eINSTANCE.createFromParts());
			for (FromPart fromPart : origAct.getFromParts().getChildren()) {
				FromPart newFromPart = PBDFragmentDuplicator.copyFromPart(fromPart);
				newReceive.getFromParts().getChildren().add(newFromPart);
			}
		}
		
		return newReceive;
	}
	
	/**
	 * Copy the original invoke activity and return a new one.
	 * 
	 * @param origAct
	 * @param fragProc
	 * @param fragDefn
	 * @return
	 */
	public static Invoke copyActivity(Invoke origAct) {
		
		if (origAct == null) {
			return null;
		}
		
		Invoke newInvoke = BPELFactory.eINSTANCE.createInvoke();
		
		FragmentDuplicator.copyStandardAttributes(origAct, newInvoke);
		PBDFragmentDuplicator.copyStandardElements(origAct, newInvoke);
		
		// inputVariable
		if (origAct.getInputVariable() != null) {
			newInvoke.setInputVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(origAct.getInputVariable()));
		}
		
		// outputVariable
		if (origAct.getOutputVariable() != null) {
			newInvoke.setOutputVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(origAct.getOutputVariable()));
		}
		
		// correlations
		if ((origAct.getCorrelations() != null) && (origAct.getCorrelations().getChildren().size() > 0)) {
			newInvoke.setCorrelations(BPELFactory.eINSTANCE.createCorrelations());
			for (Correlation correlation : origAct.getCorrelations().getChildren()) {
				Correlation newCorrelation = PBDFragmentDuplicator.copyCorrelation(correlation);
				newInvoke.getCorrelations().getChildren().add(newCorrelation);
			}
		}
		
		// FaultHandlers
		if ((origAct.getFaultHandler() != null) && ((origAct.getFaultHandler().getCatch().size() > 0) || (origAct.getFaultHandler().getCatchAll() != null))) {
			FaultHandler handler = BPELFactory.eINSTANCE.createFaultHandler();
			for (Catch cat : origAct.getFaultHandler().getCatch()) {
				Catch newCatch = PBDFragmentDuplicator.copyCatch(cat);
				handler.getCatch().add(newCatch);
			}
			newInvoke.setFaultHandler(handler);
			
			if (origAct.getFaultHandler().getCatchAll() != null) {
				CatchAll pbdCatAll = origAct.getFaultHandler().getCatchAll();
				CatchAll newCatAll = PBDFragmentDuplicator.copyCatchAll(pbdCatAll);
				newInvoke.getFaultHandler().setCatchAll(newCatAll);
			}
		}
		
		// CompensationHandlers
		if (origAct.getCompensationHandler() != null) {
			newInvoke.setCompensationHandler(PBDFragmentDuplicator.copyCompensationHandler(origAct.getCompensationHandler()));
		}
		
		// fromParts
		if ((origAct.getFromParts() != null) && (origAct.getFromParts().getChildren().size() > 0)) {
			newInvoke.setFromParts(BPELFactory.eINSTANCE.createFromParts());
			for (FromPart fromPart : origAct.getFromParts().getChildren()) {
				FromPart newFromPart = PBDFragmentDuplicator.copyFromPart(fromPart);
				newInvoke.getFromParts().getChildren().add(newFromPart);
			}
		}
		
		// toParts
		if ((origAct.getToParts() != null) && (origAct.getToParts().getChildren().size() > 0)) {
			newInvoke.setToParts(BPELFactory.eINSTANCE.createToParts());
			for (ToPart toPart : origAct.getToParts().getChildren()) {
				ToPart newToPart = PBDFragmentDuplicator.copyToPart(toPart);
				newInvoke.getToParts().getChildren().add(newToPart);
			}
		}
		
		return newInvoke;
	}
	
	/**
	 * Copy the original reply activity and return a new one.
	 * 
	 * @param origAct
	 * @return
	 */
	public static Reply copyActivity(Reply origAct) {
		
		if (origAct == null) {
			return null;
		}
		
		Reply newReply = BPELFactory.eINSTANCE.createReply();
		FragmentDuplicator.copyStandardAttributes(origAct, newReply);
		PBDFragmentDuplicator.copyStandardElements(origAct, newReply);
		
		// variable
		if (origAct.getVariable() != null) {
			newReply.setVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(origAct.getVariable()));
		}
		
		// faultName
		if (origAct.getFaultName() != null) {
			newReply.setFaultName(FragmentDuplicator.copyQName(origAct.getFaultName()));
		}
		
		// message exchange
		if (origAct.getMessageExchange() != null) {
			newReply.setMessageExchange(FragmentDuplicator.copyMessageExchange(origAct.getMessageExchange()));
		}
		
		// correlations
		if ((origAct.getCorrelations() != null) && (origAct.getCorrelations().getChildren().size() > 0)) {
			newReply.setCorrelations(BPELFactory.eINSTANCE.createCorrelations());
			for (Correlation correlation : origAct.getCorrelations().getChildren()) {
				Correlation newCorrelation = PBDFragmentDuplicator.copyCorrelation(correlation);
				newReply.getCorrelations().getChildren().add(newCorrelation);
			}
		}
		
		// toParts
		if ((origAct.getToParts() != null) && (origAct.getToParts().getChildren().size() > 0)) {
			newReply.setToParts(BPELFactory.eINSTANCE.createToParts());
			for (ToPart toPart : origAct.getToParts().getChildren()) {
				ToPart newToPart = PBDFragmentDuplicator.copyToPart(toPart);
				newReply.getToParts().getChildren().add(newToPart);
			}
		}
		
		return newReply;
	}
	
	/**
	 * Copy the given {@link Scope}
	 * 
	 * @param origScope the {@link Scope} to be copied
	 * @param origProcess the {@link Process} from the {@link Scope} to be
	 *            copied
	 * 
	 * @return new {@link Scope}
	 */
	public static Scope copyActivity(Scope origScope) {
		
		if (origScope == null) {
			throw new NullPointerException("argument is null");
		}
		
		Scope newScope = BPELFactory.eINSTANCE.createScope();
		
		if (origScope.getIsolated() != null) {
			newScope.setIsolated(origScope.getIsolated());
		}
		
		if (origScope.getExitOnStandardFault() != null) {
			newScope.setExitOnStandardFault(origScope.getExitOnStandardFault());
		}
		
		// Copy Variables
		if ((origScope.getVariables() != null) && (origScope.getVariables().getChildren().size() > 0)) {
			newScope.setVariables(BPELFactory.eINSTANCE.createVariables());
			for (Variable var : origScope.getVariables().getChildren()) {
				Variable newVar = PBDFragmentDuplicator.copyVariable(var);
				newScope.getVariables().getChildren().add(newVar);
				// Add <variable>-to-<variable> relation to our
				// pbd2MergdVars-Map
				PBDFragmentDuplicator.pkg.getPbd2MergedVars().put(var, newVar);
			}
		}
		
		// Copy PartnerLinks
		if ((origScope.getPartnerLinks() != null) && (origScope.getPartnerLinks().getChildren().size() > 0)) {
			newScope.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
			for (PartnerLink pLink : origScope.getPartnerLinks().getChildren()) {
				PartnerLink newLink = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyPartnerLink(pLink);
				newScope.getPartnerLinks().getChildren().add(newLink);
			}
		}
		
		// Copy MessageExchanges
		if ((origScope.getMessageExchanges() != null) && (origScope.getMessageExchanges().getChildren().size() > 0)) {
			newScope.setMessageExchanges(BPELFactory.eINSTANCE.createMessageExchanges());
			for (MessageExchange mex : origScope.getMessageExchanges().getChildren()) {
				MessageExchange newMex = FragmentDuplicator.copyMessageExchange(mex);
				newScope.getMessageExchanges().getChildren().add(newMex);
			}
		}
		
		// Copy CorrelationSets
		if ((origScope.getCorrelationSets() != null) && (origScope.getCorrelationSets().getChildren().size() > 0)) {
			newScope.setCorrelationSets(BPELFactory.eINSTANCE.createCorrelationSets());
			for (CorrelationSet corSet : origScope.getCorrelationSets().getChildren()) {
				CorrelationSet newCorSet = FragmentDuplicator.copyCorrelationSet(corSet);
				newScope.getCorrelationSets().getChildren().add(newCorSet);
			}
		}
		
		// Copy FaultHandlers
		if ((origScope.getFaultHandlers() != null) && ((origScope.getFaultHandlers().getCatch().size() > 0) || (origScope.getFaultHandlers().getCatchAll() != null))) {
			FaultHandler handler = BPELFactory.eINSTANCE.createFaultHandler();
			for (Catch cat : origScope.getFaultHandlers().getCatch()) {
				Catch newCatch = PBDFragmentDuplicator.copyCatch(cat);
				handler.getCatch().add(newCatch);
			}
			newScope.setFaultHandlers(handler);
			
			if (origScope.getFaultHandlers().getCatchAll() != null) {
				CatchAll pbdCatAll = origScope.getFaultHandlers().getCatchAll();
				CatchAll newCatAll = PBDFragmentDuplicator.copyCatchAll(pbdCatAll);
				newScope.getFaultHandlers().setCatchAll(newCatAll);
			}
		}
		
		// Copy EventHandlers
		if ((origScope.getEventHandlers() != null) && ((origScope.getEventHandlers().getAlarm().size() > 0) || (origScope.getEventHandlers().getEvents().size() > 0))) {
			EventHandler handler = BPELFactory.eINSTANCE.createEventHandler();
			for (OnAlarm alarm : origScope.getEventHandlers().getAlarm()) {
				OnAlarm newAlarm = PBDFragmentDuplicator.copyOnAlarm(alarm);
				handler.getAlarm().add(newAlarm);
			}
			
			for (OnEvent event : origScope.getEventHandlers().getEvents()) {
				OnEvent newEvent = PBDFragmentDuplicator.copyOnEvent(event);
				handler.getEvents().add(newEvent);
			}
			newScope.setEventHandlers(handler);
		}
		
		// Copy CompensationHandlers
		if (origScope.getCompensationHandler() != null) {
			newScope.setCompensationHandler(PBDFragmentDuplicator.copyCompensationHandler(origScope.getCompensationHandler()));
		}
		
		// Copy TerminationHandlers
		if (origScope.getTerminationHandler() != null) {
			newScope.setTerminationHandler(PBDFragmentDuplicator.copyTerminationHandler(origScope.getTerminationHandler()));
		}
		
		// Copy Activity from the Fragment Process to the new Scope
		Activity newActivity = PBDFragmentDuplicator.copyActivity(origScope.getActivity());
		newScope.setActivity(newActivity);
		
		FragmentDuplicator.copyStandardAttributes(origScope, newScope);
		PBDFragmentDuplicator.copyStandardElements(origScope, newScope);
		return newScope;
	}
	
	/**
	 * Copy the {@link PartnerLink}s from given {@link Process} to given
	 * {@link Scope}
	 * 
	 * @param origProc The {@link Process} containing the {@link PartnerLink}s
	 * @param newScope The {@link Scope} to copy the {@link PartnerLink}s to
	 */
	public static void copyPartnerLinksToScope(Process origProc, Scope newScope) {
		if ((origProc.getPartnerLinks() != null) && (origProc.getPartnerLinks().getChildren().size() > 0)) {
			if (newScope.getPartnerLinks() == null) {
				newScope.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
			}
			for (PartnerLink link : origProc.getPartnerLinks().getChildren()) {
				PartnerLink newLink = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyPartnerLink(link);
				newScope.getPartnerLinks().getChildren().add(newLink);
			}
		}
	}
	/**
	 * Get a new copy of the given variable, including the message.
	 * 
	 * @param variable
	 * @return
	 */
	public static Variable copyVariable(Variable origVar) {
		if (origVar == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Variable newVar = FragmentDuplicator.copyVariable(origVar);
		// messageType
		Message origMsg = origVar.getMessageType();
		if (origMsg != null) {
			// Now we search for the corresponding MessageType in the wsdl
			Definition defSearched = PBDFragmentDuplicator.pkg.getPbd2wsdl().get(ChoreoMergeUtil.getProcessOfElement(origVar));
			// CHECK: pruefen warum die wsdl nicht kopiert wird und ob es daran
			// liegt, dass hier der wert wieder auf null gesetzt wird obwohl
			// richtig ausgelesen wurde.
			Message refMsg = null;
			// CHECK: try to find another wsdl
			if (defSearched == null) {
				for (Definition def : PBDFragmentDuplicator.pkg.getWsdls()) {
					refMsg = WSDLUtil.resolveMessage(def, origMsg.getQName());
					if (refMsg != null)
						break;
				}
				if (refMsg == null) {
					// last resort: guess WSDL file name from PBD name
					String origMsgQNameStr = origMsg.getQName().toString();
					String processToBeSearched = origMsgQNameStr.substring(
							origMsgQNameStr.lastIndexOf('/') + 1,
							origMsgQNameStr.indexOf('}'));

					Process targetProc = pkg.choreographyPackageExtension
							.getPBDByName(processToBeSearched);

					defSearched = PBDFragmentDuplicator.pkg.getPbd2wsdl().get(
							targetProc);
					refMsg = WSDLUtil.resolveMessage(defSearched, origMsg.getQName());
				}
			} else {
				refMsg = WSDLUtil.resolveMessage(defSearched, origMsg.getQName());
			}
			newVar.setMessageType(refMsg);
		}
		
		return newVar;
	}
	
	/**
	 * Copy {@link MessageExchange}s from given {@link Process} to given
	 * {@link Scope}
	 * 
	 * @param origProc The {@link Process} containing the
	 *            {@link MessageExchange}s
	 * @param newScope The {@link Scope} to copy the {@link MessageExchange}s to
	 */
	public static void copyMessageExchangesToScope(Process origProc, Scope newScope) {
		if ((origProc.getMessageExchanges() != null) && (origProc.getMessageExchanges().getChildren().size() > 0)) {
			if (newScope.getMessageExchanges() == null) {
				newScope.setMessageExchanges(BPELFactory.eINSTANCE.createMessageExchanges());
			}
			for (MessageExchange mex : origProc.getMessageExchanges().getChildren()) {
				MessageExchange newMex = FragmentDuplicator.copyMessageExchange(mex);
				newScope.getMessageExchanges().getChildren().add(newMex);
			}
		}
	}
	
	/**
	 * Copy {@link CorrelationSet}s from given {@link Process} to given
	 * {@link Scope}
	 * 
	 * @param origProc The {@link Process} containing the {@link CorrelationSet}
	 * 
	 * @param newScope The {@link Scope} to copy the {@link CorrelationSet}s to
	 * 
	 * @param chorPack The {@link ChoreographyPackage} to add the new
	 *            {@link CorrelationSet}s to
	 */
	public static void copyCorrelationSetsToScope(Process origProc, Scope newScope) {
		if ((origProc.getCorrelationSets() != null) && (origProc.getCorrelationSets().getChildren().size() > 0)) {
			if (newScope.getCorrelationSets() == null) {
				newScope.setCorrelationSets(BPELFactory.eINSTANCE.createCorrelationSets());
			}
			for (CorrelationSet corSet : origProc.getCorrelationSets().getChildren()) {
				CorrelationSet newCorSet = FragmentDuplicator.copyCorrelationSet(corSet);
				newScope.getCorrelationSets().getChildren().add(newCorSet);
			}
		}
	}
	
	/**
	 * Copy {@link Variable}s from given {@link Process} to given {@link Scope}
	 * 
	 * @param origProc The {@link Process} containing the {@link Variable}
	 * 
	 * @param newScope The {@link Scope} to copy the {@link Variable}s to
	 * 
	 * @param chorPack The {@link ChoreographyPackage} to add the new
	 *            {@link Variable}s to
	 */
	public static void copyVariablesToScope(Process origProc, Scope newScope) {
		if ((origProc.getVariables() != null) && (origProc.getVariables().getChildren().size() > 0)) {
			if (newScope.getVariables() == null) {
				newScope.setVariables(BPELFactory.eINSTANCE.createVariables());
			}
			for (Variable variable : origProc.getVariables().getChildren()) {
				Variable newVariable = PBDFragmentDuplicator.copyVariable(variable);
				newScope.getVariables().getChildren().add(newVariable);
				// Add <variable>-to-<variable> relation to our
				// pbd2MergdVars-Map
				PBDFragmentDuplicator.pkg.getPbd2MergedVars().put(variable, newVariable);
			}
		}
	}
	
	/**
	 * Copy {@link FaultHandler}s from given {@link Process} to given
	 * {@link Scope}
	 * 
	 * @param origProc The {@link Process} containing the {@link FaultHandler}s
	 * 
	 * @param newScope The {@link Scope} to copy the {@link FaultHandler}s to
	 * 
	 * @param chorPack The {@link ChoreographyPackage} to add the new
	 *            {@link FaultHandler}s to
	 */
	public static void copyFHToScope(Process origProc, Scope newScope) {
		if ((origProc.getFaultHandlers() != null) && ((origProc.getFaultHandlers().getCatch().size() > 0) || (origProc.getFaultHandlers().getCatchAll() != null))) {
			FaultHandler handler = BPELFactory.eINSTANCE.createFaultHandler();
			for (Catch cat : origProc.getFaultHandlers().getCatch()) {
				Catch newCatch = PBDFragmentDuplicator.copyCatch(cat);
				handler.getCatch().add(newCatch);
			}
			newScope.setFaultHandlers(handler);
			
			if (origProc.getFaultHandlers().getCatchAll() != null) {
				CatchAll pbdCatAll = origProc.getFaultHandlers().getCatchAll();
				CatchAll newCatAll = PBDFragmentDuplicator.copyCatchAll(pbdCatAll);
				newScope.getFaultHandlers().setCatchAll(newCatAll);
			}
			
		}
	}
	
	/**
	 * Copy {@link EventHandler}s from given {@link Process} to given
	 * {@link Scope}
	 * 
	 * @param origProc The {@link Process} containing the {@link EventHandler}s
	 * 
	 * @param newScope The {@link Scope} to copy the {@link EventHandler}s to
	 * 
	 * @param chorPack The {@link ChoreographyPackage} to add the new
	 *            {@link EventHandler}s to
	 */
	public static void copyEHToScope(Process origProc, Scope newScope) {
		if ((origProc.getEventHandlers() != null) && ((origProc.getEventHandlers().getAlarm().size() > 0) || (origProc.getEventHandlers().getEvents().size() > 0))) {
			EventHandler handler = BPELFactory.eINSTANCE.createEventHandler();
			for (OnAlarm alarm : origProc.getEventHandlers().getAlarm()) {
				OnAlarm newAlarm = PBDFragmentDuplicator.copyOnAlarm(alarm);
				handler.getAlarm().add(newAlarm);
			}
			
			for (OnEvent event : origProc.getEventHandlers().getEvents()) {
				OnEvent newEvent = PBDFragmentDuplicator.copyOnEvent(event);
				handler.getEvents().add(newEvent);
			}
			newScope.setEventHandlers(handler);
		}
	}
	
	/**
	 * Copy all activities from the given PBD to the new merged Process Note: If
	 * we have name collisions, we check the new name into a map
	 * 
	 * @param pbd The PBD to be copied over
	 * @param chorPack The {@link ChoreographyPackage} containing all
	 *            choreography data
	 */
	public static void copyVarsAndActitivies(Process pbd) {
		PBDFragmentDuplicator.log.log(Level.INFO, "Copying Variables and Activities from PBD : " + pbd.getName());
		
		Scope newScope = BPELFactory.eINSTANCE.createScope();
		newScope.setName(getNewPBDNameForScope(pbd));
		

		// Todo Added code for renaming scopes when there is participant set in
		// topology
		PBDFragmentDuplicatorExtension.updateScopeName(newScope);
		// End of added code
		if (pbd.getExitOnStandardFault() != null) {
			newScope.setExitOnStandardFault(pbd.getExitOnStandardFault());
		}
		if (pbd.getSuppressJoinFailure() != null) {
			newScope.setSuppressJoinFailure(pbd.getSuppressJoinFailure());
		}
		
		// Copy newScope into MergedProcess Flow
		((Flow) PBDFragmentDuplicator.pkg.getMergedProcess().getActivity()).getActivities().add(newScope);
		
		// Copy PartnerLinks
		PBDFragmentDuplicator.copyPartnerLinksToScope(pbd, newScope);
		
		// Copy MessageExchanges
		PBDFragmentDuplicator.copyMessageExchangesToScope(pbd, newScope);
		
		// Copy CorrelationSets
		PBDFragmentDuplicator.copyCorrelationSetsToScope(pbd, newScope);
		
		// Copy Variables
		PBDFragmentDuplicator.copyVariablesToScope(pbd, newScope);
		
		// Copy FaultHandlers
		PBDFragmentDuplicator.copyFHToScope(pbd, newScope);
		if (!(ChoreoMergeUtil.hasNPCatchAllFH(pbd))) {
			if (newScope.getFaultHandlers() == null) {
				newScope.setFaultHandlers(BPELFactory.eINSTANCE.createFaultHandler());
			}
			newScope.getFaultHandlers().setCatchAll(ChoreoMergeUtil.createNPCatchAll());
		}
		
		// Copy EventHandlers
		PBDFragmentDuplicator.copyEHToScope(pbd, newScope);
		
		// Copy Activity from the Fragment Process to the new Scope
		Activity newActivity = PBDFragmentDuplicator.copyActivity(pbd.getActivity());
		newScope.setActivity(newActivity);
		
		/**
		 * TODO Added code
		 */
		pbdFragmentDuplicatorExtension.mergedProcessScopeMap.put(
				newScope.getName(), newScope);
	}
	
	/**
	 * Creates a new name for PBD-{@link Scope}
	 * 
	 * @param pbd PBD name will be used as postfix
	 * @return
	 */
	public static String getNewPBDNameForScope(Process pbd) {
		return Constants.PREFIX_NAME_PBD_SCOPE + pbd.getName();
	}
	
	/**
	 * Set the {@link ChoreographyPackage} for the {@link PBDFragmentDuplicator}
	 * 
	 * @param pkg
	 */
	public static void setPkg(ChoreographyPackage pkg) {
		PBDFragmentDuplicator.pkg = pkg;
	}
	
	/**
	 * Copy the {@link Catch} {@link FaultHandler}
	 * 
	 * @param oldCatch
	 * @return
	 */
	public static Catch copyCatch(Catch oldCatch) {
		if (oldCatch == null) {
			return null;
		}
		Catch newCatch = BPELFactory.eINSTANCE.createCatch();
		// CHECK: Variable was missing if variable was define in catch, care if
		// variable is set with copyVariable all
		// other parameter must be set after copyVariable, if not
		// FaultMessageType could be null
		Variable catchVar = ChoreoMergeUtil.resolveVariableInMergedProcess(oldCatch.getFaultVariable());
		if (catchVar == null && oldCatch.getFaultVariable() != null) {
			catchVar = PBDFragmentDuplicator.copyVariable(oldCatch.getFaultVariable());
		}
		newCatch.setFaultVariable(catchVar);
		newCatch.setDocumentation(oldCatch.getDocumentation());
		newCatch.setElement(oldCatch.getElement());
		newCatch.setEnclosingDefinition(oldCatch.getEnclosingDefinition());
		newCatch.setFaultElement(oldCatch.getFaultElement());
		newCatch.setFaultMessageType(oldCatch.getFaultMessageType());
		newCatch.setFaultName(oldCatch.getFaultName());
		newCatch.setActivity(PBDFragmentDuplicator.copyActivity(oldCatch.getActivity()));
		return newCatch;
	}
	
	/**
	 * Copy the {@link CatchAll} {@link FaultHandler}
	 * 
	 * @param oldCatchAll
	 * @return
	 */
	public static CatchAll copyCatchAll(CatchAll oldCatchAll) {
		if (oldCatchAll == null) {
			return null;
		}
		CatchAll newCatchAll = BPELFactory.eINSTANCE.createCatchAll();
		newCatchAll.setDocumentation(oldCatchAll.getDocumentation());
		newCatchAll.setDocumentationElement(oldCatchAll.getDocumentationElement());
		newCatchAll.setElement(oldCatchAll.getElement());
		newCatchAll.setEnclosingDefinition(oldCatchAll.getEnclosingDefinition());
		newCatchAll.setActivity(PBDFragmentDuplicator.copyActivity(oldCatchAll.getActivity()));
		return newCatchAll;
	}
	
	/**
	 * Copy the {@link OnAlarm} {@link EventHandler}
	 * 
	 * @param oldOnAlarm
	 * @return
	 */
	public static OnAlarm copyOnAlarm(OnAlarm oldOnAlarm) {
		if (oldOnAlarm == null) {
			return null;
		}
		OnAlarm newAlarm = BPELFactory.eINSTANCE.createOnAlarm();
		newAlarm.setDocumentation(oldOnAlarm.getDocumentation());
		newAlarm.setDocumentationElement(oldOnAlarm.getDocumentationElement());
		newAlarm.setElement(oldOnAlarm.getElement());
		newAlarm.setEnclosingDefinition(oldOnAlarm.getEnclosingDefinition());
		newAlarm.setFor(oldOnAlarm.getFor());
		newAlarm.setRepeatEvery(oldOnAlarm.getRepeatEvery());
		newAlarm.setUntil(oldOnAlarm.getUntil());
		newAlarm.setActivity(PBDFragmentDuplicator.copyActivity(oldOnAlarm.getActivity()));
		return newAlarm;
	}
	
	/**
	 * Copy the {@link OnEvent} {@link EventHandler}
	 * 
	 * @param oldOnEvent
	 * @return
	 */
	public static OnEvent copyOnEvent(OnEvent oldOnEvent) {
		if (oldOnEvent == null) {
			return null;
		}
		OnEvent newEvent = BPELFactory.eINSTANCE.createOnEvent();
		newEvent.setCorrelations(oldOnEvent.getCorrelations());
		newEvent.setCorrelationSets(oldOnEvent.getCorrelationSets());
		newEvent.setDocumentation(oldOnEvent.getDocumentation());
		newEvent.setDocumentationElement(oldOnEvent.getDocumentationElement());
		newEvent.setElement(oldOnEvent.getElement());
		newEvent.setEnclosingDefinition(oldOnEvent.getEnclosingDefinition());
		newEvent.setFromParts(oldOnEvent.getFromParts());
		newEvent.setMessageExchange(oldOnEvent.getMessageExchange());
		newEvent.setMessageType(oldOnEvent.getMessageType());
		newEvent.setOperation(oldOnEvent.getOperation());
		newEvent.setPartnerLink(oldOnEvent.getPartnerLink());
		newEvent.setPortType(oldOnEvent.getPortType());
		
		// !!! The Variable of the <onEvent>-branch constitutes an implicit
		// declaration in the associated <scope> so we need a copy !!
		newEvent.setVariable(PBDFragmentDuplicator.copyVariable(oldOnEvent.getVariable()));
		// newEvent.setVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(oldOnEvent.getVariable()));
		newEvent.setXSDElement(oldOnEvent.getXSDElement());
		newEvent.setActivity(PBDFragmentDuplicator.copyActivity(oldOnEvent.getActivity()));
		
		// If oldOnEvent has wsu:id copy it to our map
		String wsuID = oldOnEvent.getElement().getAttribute("wsu:id");
		if (((wsuID) != null) && (!wsuID.equals(""))) {
			PBDFragmentDuplicator.pkg.addOld2NewRelation(oldOnEvent.getElement().getAttribute("wsu:id"), newEvent);
		}
		
		return newEvent;
	}
	
	/**
	 * Copy given {@link CompensationHandler}
	 * 
	 * @param origCH {@link CompensationHandler}
	 * @return Copy of given {@link CompensationHandler}
	 */
	public static CompensationHandler copyCompensationHandler(CompensationHandler origCH) {
		if (origCH == null) {
			return null;
		}
		CompensationHandler newCH = BPELFactory.eINSTANCE.createCompensationHandler();
		newCH.setDocumentation(origCH.getDocumentation());
		newCH.setDocumentationElement(origCH.getDocumentationElement());
		newCH.setElement(origCH.getElement());
		newCH.setEnclosingDefinition(origCH.getEnclosingDefinition());
		newCH.setActivity(PBDFragmentDuplicator.copyActivity(origCH.getActivity()));
		return newCH;
	}
	
	/**
	 * Copy given {@link TerminationHandler}
	 * 
	 * @param origTH {@link TerminationHandler}
	 * @return Copy of given {@link TerminationHandler}
	 */
	public static TerminationHandler copyTerminationHandler(TerminationHandler origTH) {
		if (origTH == null) {
			return null;
		}
		TerminationHandler newTH = BPELFactory.eINSTANCE.createTerminationHandler();
		newTH.setDocumentation(origTH.getDocumentation());
		newTH.setDocumentationElement(origTH.getDocumentationElement());
		newTH.setElement(origTH.getElement());
		newTH.setEnclosingDefinition(origTH.getEnclosingDefinition());
		newTH.setActivity(PBDFragmentDuplicator.copyActivity(origTH.getActivity()));
		return newTH;
	}
	
	/**
	 * Copy given {@link ForEach}
	 * 
	 * @param act {@link ForEach}
	 * @return Copy of given {@link ForEach}
	 */
	public static ForEach copyActivity(ForEach act) {
		if (act == null) {
			return null;
		}
		ForEach newForEach = BPELFactory.eINSTANCE.createForEach();
		FragmentDuplicator.copyStandardAttributes(act, newForEach);
		PBDFragmentDuplicator.copyStandardElements(act, newForEach);
		newForEach.setCounterName(ChoreoMergeUtil.resolveVariableInMergedProcess(act.getCounterName()));
		newForEach.setParallel(act.getParallel());
		newForEach.setStartCounterValue(FragmentDuplicator.copyExpression(act.getStartCounterValue()));
		newForEach.setFinalCounterValue(FragmentDuplicator.copyExpression(act.getFinalCounterValue()));
		if (act.getCompletionCondition() != null) {
			newForEach.setCompletionCondition(FragmentDuplicator.copyCompletionCondition(act.getCompletionCondition()));
		}
		newForEach.setActivity(PBDFragmentDuplicator.copyActivity(act
				.getActivity()));

		ArrayList<String> finalCounterValueList = new ArrayList<String>();
		finalCounterValueList = pbdFragmentDuplicatorExtension
				.containsNestedForEach(newForEach, finalCounterValueList);
		String finalCounterValue = "";
		for (int i = 0; i < finalCounterValueList.size(); i++) {

			if (!pbdFragmentDuplicatorExtension.isInteger(finalCounterValueList
					.get(i))
					|| !pbdFragmentDuplicatorExtension.isInteger(newForEach
							.getFinalCounterValue().getBody().toString())) {
				if (finalCounterValue.length() == 0) {
					finalCounterValue = finalCounterValueList.get(i)
							+ "*"
							+ newForEach.getFinalCounterValue().getBody()
									.toString();
				} else {
					finalCounterValue = "*"
							+ finalCounterValueList.get(i)
							+ "*"
							+ newForEach.getFinalCounterValue().getBody()
									.toString();
				}

			} else {
				if (finalCounterValue.length() == 0) {
					finalCounterValue = ""
							+ (Integer.parseInt(finalCounterValueList.get(i)) * Integer
									.parseInt(newForEach.getFinalCounterValue()
											.getBody().toString()));
				} else {
					finalCounterValue = ""
							+ (Integer.parseInt(finalCounterValue)
									* Integer.parseInt(finalCounterValueList
											.get(i)) * Integer
										.parseInt(newForEach
												.getFinalCounterValue()
												.getBody().toString()));
				}
			}
		}
		if (finalCounterValue.length() > 0) {
			System.out.println("-----------------Final COunter Value:   "
					+ finalCounterValue + "\tForEach: " + newForEach.getName());
			newForEach.getFinalCounterValue().setBody(finalCounterValue);
		}
		// Added code
		// I assume that each ForEach name will be unique
		// newForEach .getFinalCounterValue().getBody().toString()

		/*
		 * System.out.println("--------- Foreach: " + newForEach.getName() +
		 * "\tParent is ForEach: " + checkParentIsForEach(act));
		 */

		pbdFragmentDuplicatorExtension.processForEachMap.put(
				newForEach.getName(), newForEach);// currentProcess.getName()
		// + ","
		// End of Added code
		return newForEach;
	}
	
	/**
	 * Copy given {@link If}
	 * 
	 * @param act {@link If}
	 * @return Copy of given {@link If}
	 */
	public static If copyActivity(If act) {
		if (act == null) {
			return null;
		}
		If newIf = BPELFactory.eINSTANCE.createIf();
		FragmentDuplicator.copyStandardAttributes(act, newIf);
		PBDFragmentDuplicator.copyStandardElements(act, newIf);
		newIf.setCondition(FragmentDuplicator.copyCondition(act.getCondition()));
		if (act.getElse() != null) {
			Else newElse = BPELFactory.eINSTANCE.createElse();
			newElse.setActivity(PBDFragmentDuplicator.copyActivity(act.getElse().getActivity()));
			newIf.setElse(newElse);
		}
		if ((act.getElseIf() != null) && (act.getElseIf().size() > 0)) {
			for (ElseIf elseIf : act.getElseIf()) {
				ElseIf newElseIf = BPELFactory.eINSTANCE.createElseIf();
				newElseIf.setCondition(FragmentDuplicator.copyCondition(elseIf.getCondition()));
				newElseIf.setActivity(PBDFragmentDuplicator.copyActivity(elseIf.getActivity()));
				newIf.getElseIf().add(newElseIf);
			}
		}
		newIf.setActivity(PBDFragmentDuplicator.copyActivity(act.getActivity()));
		return newIf;
	}
	
	/**
	 * Copy given {@link Pick}
	 * 
	 * @param act {@link Pick}
	 * @return Copy of given {@link Pick}
	 */
	public static Pick copyActivity(Pick act) {
		if (act == null) {
			return null;
		}
		Pick newPick = BPELFactory.eINSTANCE.createPick();
		FragmentDuplicator.copyStandardAttributes(act, newPick);
		PBDFragmentDuplicator.copyStandardElements(act, newPick);
		
		if (act.getCreateInstance() != null) {
			newPick.setCreateInstance(act.getCreateInstance());
		}
		
		// Copy <onAlarm>s
		if ((act.getAlarm() != null) && (act.getAlarm().size() > 0)) {
			for (OnAlarm onAlarm : act.getAlarm()) {
				OnAlarm newOnAlarm = PBDFragmentDuplicator.copyOnAlarm(onAlarm);
				newPick.getAlarm().add(newOnAlarm);
			}
		}
		
		// Copy <onMessage>s
		for (OnMessage onMessage : act.getMessages()) {
			newPick.getMessages().add(PBDFragmentDuplicator.copyOnMessage(onMessage));
		}
		
		return newPick;
	}
	
	/**
	 * Copy given {@link OnMessage}
	 * 
	 * @param onMessage {@link OnMessage}
	 * @return Copy of given {@link OnMessage}
	 */
	public static OnMessage copyOnMessage(OnMessage onMessage) {
		if (onMessage == null) {
			return null;
		}
		OnMessage newOnMessage = BPELFactory.eINSTANCE.createOnMessage();
		
		newOnMessage.setVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(onMessage.getVariable()));
		newOnMessage.setMessageExchange(FragmentDuplicator.copyMessageExchange(onMessage.getMessageExchange()));
		newOnMessage.setActivity(PBDFragmentDuplicator.copyActivity(onMessage.getActivity()));
		
		// fromParts
		if ((onMessage.getFromParts() != null) && (onMessage.getFromParts().getChildren().size() > 0)) {
			newOnMessage.setFromParts(BPELFactory.eINSTANCE.createFromParts());
			for (FromPart fromPart : onMessage.getFromParts().getChildren()) {
				FromPart newFromPart = PBDFragmentDuplicator.copyFromPart(fromPart);
				newOnMessage.getFromParts().getChildren().add(newFromPart);
			}
		}
		
		// correlations
		if ((onMessage.getCorrelations() != null) && (onMessage.getCorrelations().getChildren().size() > 0)) {
			newOnMessage.setCorrelations(BPELFactory.eINSTANCE.createCorrelations());
			for (Correlation correlation : onMessage.getCorrelations().getChildren()) {
				Correlation newCorrelation = PBDFragmentDuplicator.copyCorrelation(correlation);
				newOnMessage.getCorrelations().getChildren().add(newCorrelation);
			}
		}
		
		// If onMessage has wsu:id copy it to our map
		String wsuID = onMessage.getElement().getAttribute("wsu:id");
		if (((wsuID) != null) && (!wsuID.equals(""))) {
			PBDFragmentDuplicator.pkg.addOld2NewRelation(onMessage.getElement().getAttribute("wsu:id"), newOnMessage);
		}
		
		return newOnMessage;
	}
	
	/**
	 * Copy given {@link FromPart}
	 * 
	 * @param fromPart {@link FromPart}
	 * @return Copy of given {@link FromPart}
	 */
	public static FromPart copyFromPart(FromPart fromPart) {
		if (fromPart == null) {
			return null;
		}
		FromPart newFromPart = BPELFactory.eINSTANCE.createFromPart();
		newFromPart.setPart(FragmentDuplicator.copyPart(fromPart.getPart()));
		newFromPart.setToVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(fromPart.getToVariable()));
		return newFromPart;
	}
	
	/**
	 * Copy given {@link ToPart}
	 * 
	 * @param fromPart {@link ToPart}
	 * @return Copy of given {@link ToPart}
	 */
	public static ToPart copyToPart(ToPart toPart) {
		if (toPart == null) {
			return null;
		}
		ToPart newToPart = BPELFactory.eINSTANCE.createToPart();
		newToPart.setPart(FragmentDuplicator.copyPart(toPart.getPart()));
		newToPart.setFromVariable(ChoreoMergeUtil.resolveVariableInMergedProcess(toPart.getFromVariable()));
		return toPart;
	}
	
	/**
	 * Copy given {@link Correlation}
	 * 
	 * @param origCorrelations {@link Correlation}
	 * @return Copy of given {@link Correlation}
	 */
	public static Correlation copyCorrelation(Correlation origCorrelation) {
		if (origCorrelation == null) {
			return null;
		}
		
		Correlation newCorrelation = BPELFactory.eINSTANCE.createCorrelation();
		newCorrelation.setInitiate(origCorrelation.getInitiate());
		newCorrelation.setPattern(origCorrelation.getPattern());
		newCorrelation.setSet(origCorrelation.getSet());
		
		return newCorrelation;
	}
	
	/**
	 * Copy given {@link RepeatUntil}
	 * 
	 * @param act {@link RepeatUntil}
	 * @return Copy of given {@link RepeatUntil}
	 */
	public static RepeatUntil copyActivity(RepeatUntil act) {
		if (act == null) {
			return null;
		}
		RepeatUntil newRepeatUntil = BPELFactory.eINSTANCE.createRepeatUntil();
		FragmentDuplicator.copyStandardAttributes(act, newRepeatUntil);
		PBDFragmentDuplicator.copyStandardElements(act, newRepeatUntil);
		newRepeatUntil.setCondition(FragmentDuplicator.copyCondition(act.getCondition()));
		newRepeatUntil.setActivity(PBDFragmentDuplicator.copyActivity(act.getActivity()));
		return newRepeatUntil;
	}

	/**
	 * Copy given {@link Sequence}
	 * 
	 * @param act {@link Sequence}
	 * @return Copy of given {@link Sequence}
	 */
	public static Sequence copyActivity(Sequence act) {
		if (act == null) {
			return null;
		}
		Sequence newSequence = BPELFactory.eINSTANCE.createSequence();
		FragmentDuplicator.copyStandardAttributes(act, newSequence);
		PBDFragmentDuplicator.copyStandardElements(act, newSequence);
		for (Activity activity : act.getActivities()) {
			newSequence.getActivities().add(PBDFragmentDuplicator.copyActivity(activity));
		}
		return newSequence;
	}
	
	/**
	 * Copy given {@link While}
	 * 
	 * @param act {@link While}
	 * @return Copy of given {@link While}
	 */
	public static While copyActivity(While act) {
		if (act == null) {
			return null;
		}
		While newWhile = BPELFactory.eINSTANCE.createWhile();
		FragmentDuplicator.copyStandardAttributes(act, newWhile);
		PBDFragmentDuplicator.copyStandardElements(act, newWhile);
		newWhile.setCondition(FragmentDuplicator.copyCondition(act.getCondition()));
		newWhile.setActivity(PBDFragmentDuplicator.copyActivity(act.getActivity()));
		return newWhile;
	}
	
	/**
	 * Copy the original assign activity and return a new one.
	 * <p>
	 * Note that to setup the configuration of the new assign activity, the
	 * referred variable must be resolved from the fragment process/definition.
	 * 
	 * @param act
	 * @return
	 */
	public static Assign copyActivity(Assign act) {
		
		if (act == null) {
			return null;
		}
		
		Assign newAssign = null;
		
		newAssign = BPELFactory.eINSTANCE.createAssign();
		FragmentDuplicator.copyStandardAttributes(act, newAssign);
		
		if (act.getValidate() != null) {
			newAssign.setValidate(act.getValidate());
		}
		
		List<Copy> copyList = act.getCopy();
		for (Copy copy : copyList) {
			newAssign.getCopy().add(PBDFragmentDuplicator.copyCopy(copy));
		}
		
		PBDFragmentDuplicator.copyStandardElements(act, newAssign);
		return newAssign;
	}
	
	/**
	 * Copy given {@link Copy}
	 * 
	 * @param copy {@link Copy}
	 * @return Copy of given {@link Copy}
	 */
	public static Copy copyCopy(Copy copy) {
		
		if (copy == null) {
			throw new NullPointerException("argument is null.");
		}
		
		Copy newCopy = BPELFactory.eINSTANCE.createCopy();
		newCopy.setFrom(PBDFragmentDuplicator.copyFrom(copy.getFrom()));
		newCopy.setTo(PBDFragmentDuplicator.copyTo(copy.getTo()));
		
		return newCopy;
		
	}
	
	/**
	 * Copy given {@link From}
	 * 
	 * @param from {@link From}
	 * @return Copy of given {@link From}
	 */
	public static From copyFrom(From from) {
		
		if (from == null) {
			throw new NullPointerException("argument is null.");
		}
		
		From newFrom = BPELFactory.eINSTANCE.createFrom();
		
		if (from.getLiteral() != null) {
			newFrom.setLiteral(from.getLiteral());
		}
		if (from.getType() != null) {
			newFrom.setType(from.getType());
		}
		if (from.getVariable() != null) {
			// CHECK Variable could be defined as catch attribute so we have to
			// copy this variable if it is not found by resolve Methode
			Variable var = ChoreoMergeUtil.resolveVariableInMergedProcess(from.getVariable());
			if (var == null && from.getVariable() != null) {
				var = FragmentDuplicator.copyVariable(from.getVariable());
			}
			newFrom.setVariable(var);
		}
		if (from.getPart() != null) {
			newFrom.setPart(FragmentDuplicator.copyPart(from.getPart()));
		}
		if (from.getProperty() != null) {
			newFrom.setProperty(FragmentDuplicator.copyProperty(from.getProperty()));
		}
		if (from.getQuery() != null) {
			newFrom.setQuery(FragmentDuplicator.copyQuery(from.getQuery()));
		}
		if (from.getLiteral() != null) {
			newFrom.setLiteral(from.getLiteral());
		}
		if (from.getExpression() != null) {
			newFrom.setExpression(FragmentDuplicator.copyExpression(from.getExpression()));
		}
		if (from.getPartnerLink() != null) {
			newFrom.setPartnerLink(FragmentDuplicator.copyPartnerLink(from.getPartnerLink()));
		}
		return newFrom;
	}
	
	/**
	 * Copy given {@link To}
	 * 
	 * @param to {@link To}
	 * @return Copy of given {@link To}
	 */
	public static To copyTo(To to) {
		
		if (to == null) {
			throw new NullPointerException("argument is null.");
		}
		
		To newTo = BPELFactory.eINSTANCE.createTo();
		
		if (to.getVariable() != null) {
			// CHECK Variable could be defined as catch attribute so we have to
			// copy this variable if it is not found by resolve Methode
			Variable var = ChoreoMergeUtil.resolveVariableInMergedProcess(to.getVariable());
			if (var == null && to.getVariable() != null) {
				var = FragmentDuplicator.copyVariable(to.getVariable());
			}
			newTo.setVariable(var);
		}
		if (to.getPart() != null) {
			newTo.setPart(FragmentDuplicator.copyPart(to.getPart()));
		}
		if (to.getProperty() != null) {
			newTo.setProperty(FragmentDuplicator.copyProperty(to.getProperty()));
		}
		if (to.getQuery() != null) {
			newTo.setQuery(FragmentDuplicator.copyQuery(to.getQuery()));
		}
		if (to.getExpression() != null) {
			newTo.setExpression(FragmentDuplicator.copyExpression(to.getExpression()));
		}
		if (to.getPartnerLink() != null) {
			newTo.setPartnerLink(PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyPartnerLink(to.getPartnerLink()));
		}
		return newTo;
	}
	
	/**
	 * Copy the {@link Sources} to given {@link Activity}
	 * 
	 * @param origSources The original {@link Sources}
	 * @param container The container {@link Activity} of the new sources
	 */
	public static void copySources(Sources origSources, Activity container) {
		if ((origSources == null) || (container == null)) {
			throw new NullPointerException("argument is null. origSources == null:" + (origSources == null) + " container == null:" + (container == null));
		}
		
		if (container.getSources() == null) {
			Sources newSources = BPELFactory.eINSTANCE.createSources();
			container.setSources(newSources);
		}
		
		for (Source origSource : origSources.getChildren()) {
			
			Source newSource = BPELFactory.eINSTANCE.createSource();
			
			newSource.setLink(ChoreoMergeUtil.resolveLinkInMergedProcess(origSource.getLink()));
			if (origSource.getTransitionCondition() != null) {
				newSource.setTransitionCondition(FragmentDuplicator.copyCondition(origSource.getTransitionCondition()));
			}
			newSource.setActivity(container);
		}
	}
	
	/**
	 * Copy the {@link Targets} to given {@link Activity}
	 * 
	 * @param origTargets The original {@link Targets}
	 * @param container The container {@link Activity} of the new Targets
	 * @return A new targets
	 */
	public static void copyTargets(Targets origTargets, Activity container) {
		
		if ((origTargets == null) || (container == null)) {
			throw new NullPointerException("argument is null. origTargets == null:" + (origTargets == null) + " container == null:" + (container == null));
		}
		
		if (container.getTargets() == null) {
			container.setTargets(BPELFactory.eINSTANCE.createTargets());
		}
		Targets newTargets = container.getTargets();
		
		// a Targets contains joinCondition AND a list of target
		if (origTargets.getJoinCondition() != null) {
			newTargets.setJoinCondition(FragmentDuplicator.copyCondition(origTargets.getJoinCondition()));
		}
		
		for (Target origTarget : origTargets.getChildren()) {
			// a target contains link and the container activity
			Target newTarget = BPELFactory.eINSTANCE.createTarget();
			
			if (origTarget.getLink() != null) {
				newTarget.setLink(ChoreoMergeUtil.resolveLinkInMergedProcess(origTarget.getLink()));
			}
			newTarget.setActivity(container);
		}
		
	}
	
	/**
	 * Copy the given {@link Compensate}
	 * 
	 * @param act {@link Compensate}
	 * @return Copy of given {@link Compensate}
	 */
	public static Compensate copyActivity(Compensate act) {
		if (act == null) {
			return null;
		}
		Compensate newCompensate = BPELFactory.eINSTANCE.createCompensate();
		FragmentDuplicator.copyStandardAttributes(act, newCompensate);
		PBDFragmentDuplicator.copyStandardElements(act, newCompensate);
		return newCompensate;
	}
	
	/**
	 * Copy the standard elements from original activity into the new one.
	 * 
	 * <p>
	 * Each activity has optional containers "sources" and "targets", which
	 * contain standard elements "source" and "target" respectively.
	 * 
	 * @param origAct The original activity
	 * @param newAct The new activity
	 */
	public static void copyStandardElements(Activity origAct, Activity newAct) {
		
		if ((origAct == null) || (newAct == null)) {
			throw new NullPointerException("Null parameter error! origAct==null:" + (origAct == null) + " newAct==null:" + (newAct == null));
		}
		
		// sources
		if (origAct.getSources() != null) {
			PBDFragmentDuplicator.copySources(origAct.getSources(), newAct);
		}
		
		// targets
		if (origAct.getTargets() != null) {
			PBDFragmentDuplicator.copyTargets(origAct.getTargets(), newAct);
		}
	}
	
	/**
	 * Copy the given {@link CompensateScope}
	 * 
	 * @param act {@link CompensateScope}
	 * @return Copy of given {@link CompensateScope}
	 */
	public static CompensateScope copyActivity(CompensateScope act) {
		if (act == null) {
			return null;
		}
		CompensateScope newCScope = BPELFactory.eINSTANCE.createCompensateScope();
		FragmentDuplicator.copyStandardAttributes(act, newCScope);
		PBDFragmentDuplicator.copyStandardElements(act, newCScope);
		if (act.getTarget() != null) {
			newCScope.setTarget(act.getTarget());
		}
		return newCScope;
	}
	
	/**
	 * Copy given {@link Empty}
	 * 
	 * @param act {@link Empty}
	 * @return Copy of given {@link Empty}
	 */
	public static Empty copyActivity(Empty act) {
		if (act == null) {
			return null;
		}
		Empty newEmpty = BPELFactory.eINSTANCE.createEmpty();
		FragmentDuplicator.copyStandardAttributes(act, newEmpty);
		PBDFragmentDuplicator.copyStandardElements(act, newEmpty);
		return newEmpty;
	}
	
	/**
	 * Copy given {@link Exit}
	 * 
	 * @param act {@link Exit}
	 * @return Copy of given {@link Exit}
	 */
	public static Exit copyActivity(Exit act) {
		if (act == null) {
			return null;
		}
		Exit newExit = BPELFactory.eINSTANCE.createExit();
		FragmentDuplicator.copyStandardAttributes(act, newExit);
		PBDFragmentDuplicator.copyStandardElements(act, newExit);
		return newExit;
	}
	
	/**
	 * Copy given {@link OpaqueActivity}
	 * 
	 * @param act {@link OpaqueActivity}
	 * @return Copy of given {@link OpaqueActivity}
	 */
	public static OpaqueActivity copyActivity(OpaqueActivity act) {
		if (act == null) {
			return null;
		}
		OpaqueActivity newAct = BPELFactory.eINSTANCE.createOpaqueActivity();
		PBDFragmentDuplicator.copyStandardElements(act, newAct);
		FragmentDuplicator.copyStandardAttributes(act, newAct);
		return newAct;
	}
	
	/**
	 * Copy the given {@link Rethrow}
	 * 
	 * @param act {@link Rethrow}
	 * @return Copy of given {@link Rethrow}
	 */
	public static Rethrow copyActivity(Rethrow act) {
		if (act == null) {
			return null;
		}
		Rethrow newRethrow = BPELFactory.eINSTANCE.createRethrow();
		FragmentDuplicator.copyStandardAttributes(act, newRethrow);
		PBDFragmentDuplicator.copyStandardElements(act, newRethrow);
		return newRethrow;
	}
	
	/**
	 * Copy given {@link Throw}
	 * 
	 * @param act {@link Throw}
	 * @return Copy of given {@link Throw}
	 */
	public static Throw copyActivity(Throw act) {
		if (act == null) {
			return null;
		}
		Throw newThrow = BPELFactory.eINSTANCE.createThrow();
		FragmentDuplicator.copyStandardAttributes(act, newThrow);
		PBDFragmentDuplicator.copyStandardElements(act, newThrow);
		return newThrow;
	}
	
	/**
	 * Copy given {@link Validate}
	 * 
	 * @param act {@link Validate}
	 * @return Copy of given {@link Validate}
	 */
	public static Validate copyActivity(Validate act) {
		if (act == null) {
			return null;
		}
		Validate newValidate = BPELFactory.eINSTANCE.createValidate();
		FragmentDuplicator.copyStandardAttributes(act, newValidate);
		PBDFragmentDuplicator.copyStandardElements(act, newValidate);
		if (act.getVariables() != null) {
			for (Variable var : act.getVariables()) {
				newValidate.getVariables().add(ChoreoMergeUtil.resolveVariableInMergedProcess(var));
			}
		}
		return newValidate;
	}
	
	/**
	 * Copy given {@link Wait}
	 * 
	 * @param act {@link Wait}
	 * @return Copy of given {@link Wait}
	 */
	public static Wait copyActivity(Wait act) {
		if (act == null) {
			return null;
		}
		Wait newWait = BPELFactory.eINSTANCE.createWait();
		FragmentDuplicator.copyStandardAttributes(act, newWait);
		PBDFragmentDuplicator.copyStandardElements(act, newWait);
		if (act.getFor() != null) {
			newWait.setFor(FragmentDuplicator.copyExpression(act.getFor()));
		}
		if (act.getUntil() != null) {
			newWait.setUntil(FragmentDuplicator.copyExpression(act.getUntil()));
		}
		return newWait;
	}
	
}
