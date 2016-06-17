package org.bpel4chor.mergechoreography.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.CatchAll;
import org.eclipse.bpel.model.CompletionCondition;
import org.eclipse.bpel.model.CorrelationSet;
import org.eclipse.bpel.model.Else;
import org.eclipse.bpel.model.ElseIf;
import org.eclipse.bpel.model.EventHandler;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Links;
import org.eclipse.bpel.model.MessageExchange;
import org.eclipse.bpel.model.OnAlarm;
import org.eclipse.bpel.model.OnEvent;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.wst.wsdl.Definition;

import de.uni_stuttgart.iaas.bpel.model.utilities.FragmentDuplicator;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

public class PBDFragmentDuplicatorExtension {
	private static ChoreographyPackage pkg = null;
	// hash map which sotres (processName:ForEach)
	public static Map<String, ForEach> processForEachMap = new HashMap<String, ForEach>();
	public static Map<String, Scope> mergedProcessScopeMap = new HashMap<String, Scope>();

	public void setPkg(ChoreographyPackage pkg) {
		this.pkg = pkg;
	}

	/**
	 * Copy original sequence activity without its children activities, and
	 * return ew one.
	 * 
	 * @param act
	 * @return
	 */
	public static Sequence copySequenceActivityWOChildren(Sequence act) {
		if (act == null) {
			return null;
		}
		Sequence newSequence = BPELFactory.eINSTANCE.createSequence();
		FragmentDuplicator.copyStandardAttributes(act, newSequence);
		PBDFragmentDuplicator.copyStandardElements(act, newSequence);
		return newSequence;
	}

	/**
	 * 
	 * @return the hash map containing forEachs per each pbd name.
	 */
	public static Map<String, ForEach> getProcessForEachMap() {
		return processForEachMap;
	}

	// key is (forEachActivityName), value is (ForEachActivity)
	public static void setProcessForEachMap(
			Map<String, ForEach> processForEachMap) {
		processForEachMap = processForEachMap;
	}

	/**
	 * Copy the flow and return a new one
	 * <p>
	 * <b>Note</b>: only the flow structure is copied, the children activities
	 * will NOT be copied together.
	 * 
	 * @param act
	 *            The flow activity
	 * @return The new flow activity
	 */
	public static Flow copyFlowActivityWOChildren(Flow act) {

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
				pkg.getPbd2MergedLinks().put(oldLink, newLink);
			}
		}

		PBDFragmentDuplicator.copyStandardElements(act, newFlow);
		return newFlow;
	}

	/**
	 * 
	 * Copy the original scope activity, without its containing children, and
	 * return new one
	 * 
	 * @param origScope
	 * @param flagLastFE
	 * @return
	 */
	public static Scope copyScopeActivityWOChildren(Scope origScope,
			boolean flagLastFE) {

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
		if ((origScope.getVariables() != null)
				&& (origScope.getVariables().getChildren().size() > 0)) {
			newScope.setVariables(BPELFactory.eINSTANCE.createVariables());
			for (Variable var : origScope.getVariables().getChildren()) {
				System.out.println("Copying variable: " + var.getName()
						+ "\tScope name: " + origScope.getName());
				Variable newVar = PBDFragmentDuplicator.copyVariable(var);
				newScope.getVariables().getChildren().add(newVar);
				// Add <variable>-to-<variable> relation to our
				// pbd2MergdVars-Map

			}
		}

		// Copy PartnerLinks
		if ((origScope.getPartnerLinks() != null)
				&& (origScope.getPartnerLinks().getChildren().size() > 0)) {
			newScope.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
			for (PartnerLink pLink : origScope.getPartnerLinks().getChildren()) {
				// PartnerLink newLink = PBDFragmentDuplicator
				// .copyPartnerLink(pLink);
				PartnerLink newLink = EcoreUtil.copy(pLink); //.clone();
				newScope.getPartnerLinks().getChildren().add(newLink);
			}
		}

		// Copy MessageExchanges
		if ((origScope.getMessageExchanges() != null)
				&& (origScope.getMessageExchanges().getChildren().size() > 0)) {
			newScope.setMessageExchanges(BPELFactory.eINSTANCE
					.createMessageExchanges());
			for (MessageExchange mex : origScope.getMessageExchanges()
					.getChildren()) {
				MessageExchange newMex = FragmentDuplicator
						.copyMessageExchange(mex);
				newScope.getMessageExchanges().getChildren().add(newMex);
			}
		}

		// Copy CorrelationSets
		if ((origScope.getCorrelationSets() != null)
				&& (origScope.getCorrelationSets().getChildren().size() > 0)) {
			newScope.setCorrelationSets(BPELFactory.eINSTANCE
					.createCorrelationSets());
			for (CorrelationSet corSet : origScope.getCorrelationSets()
					.getChildren()) {
				CorrelationSet newCorSet = FragmentDuplicator
						.copyCorrelationSet(corSet);
				newScope.getCorrelationSets().getChildren().add(newCorSet);
			}
		}

		// Copy FaultHandlers
		if ((origScope.getFaultHandlers() != null)
				&& ((origScope.getFaultHandlers().getCatch().size() > 0) || (origScope
						.getFaultHandlers().getCatchAll() != null))) {
			FaultHandler handler = BPELFactory.eINSTANCE.createFaultHandler();
			for (Catch cat : origScope.getFaultHandlers().getCatch()) {
				Catch newCatch = PBDFragmentDuplicator.copyCatch(cat);
				handler.getCatch().add(newCatch);
			}
			newScope.setFaultHandlers(handler);

			if (origScope.getFaultHandlers().getCatchAll() != null) {
				CatchAll pbdCatAll = origScope.getFaultHandlers().getCatchAll();
				CatchAll newCatAll = PBDFragmentDuplicator
						.copyCatchAll(pbdCatAll);
				newScope.getFaultHandlers().setCatchAll(newCatAll);
			}
		}

		// Copy EventHandlers
		if ((origScope.getEventHandlers() != null)
				&& ((origScope.getEventHandlers().getAlarm().size() > 0) || (origScope
						.getEventHandlers().getEvents().size() > 0))) {
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
		if (origScope.getCompensationHandler() != null && flagLastFE) {
			newScope.setCompensationHandler(PBDFragmentDuplicator
					.copyCompensationHandler(origScope.getCompensationHandler()));
		}

		// Copy TerminationHandlers
		if (origScope.getTerminationHandler() != null) {
			newScope.setTerminationHandler(PBDFragmentDuplicator
					.copyTerminationHandler(origScope.getTerminationHandler()));
		}

		FragmentDuplicator.copyStandardAttributes(origScope, newScope);
		PBDFragmentDuplicator.copyStandardElements(origScope, newScope);

		return newScope;
	}

	/**
	 * Get a new copy of the given partnerLink, including the partnerLinkType,
	 * role, portType.
	 * 
	 * @param origPartnerLink
	 * @return
	 */
	public static PartnerLink copyPartnerLink(PartnerLink origPartnerLink) {

		if (origPartnerLink == null) {
			throw new NullPointerException("argument is null.");
		}

		PartnerLink newPL = FragmentDuplicator.copyPartnerLink(origPartnerLink);

		// Get the corresponding wsdl-File for automatic import-creation in new
		// merged process
		Definition defSearched = pkg.getPbd2wsdl().get(
				ChoreoMergeUtil.getProcessOfElement(origPartnerLink));
		
		PartnerLinkType plType = null;
		
		if (defSearched == null) {
			for (Definition def : pkg.getWsdls()) {
				plType = MyWSDLUtil.findPartnerLinkType(def, newPL
						.getPartnerLinkType().getName());
				if (plType != null)
					break;
			}
			if (plType == null) {
				// TODO check if last resort works (compare to method
				// copyVariables)
				// pkg.
				String origPLName = origPartnerLink.getName();
				System.out.println("-- PLName: " + origPLName);
				// String processToBeSearched = origMsgQNameStr.substring(
				// origMsgQNameStr.lastIndexOf('/') + 1,
				// origMsgQNameStr.indexOf('}'));
				//
				// Process targetProc = pkg.getPBDByName(processToBeSearched);
				//
				// defSearched = PBDFragmentDuplicator.pkg.getPbd2wsdl().get(
				// targetProc);
				// if (defSearched == null)
				// System.out.println("DefSearched is STILL  NULL!!!");
				plType = MyWSDLUtil.findPartnerLinkType(defSearched, newPL
						.getPartnerLinkType().getName());
			}
		} else {
			plType = MyWSDLUtil.findPartnerLinkType(defSearched, newPL
					.getPartnerLinkType().getName());			
		}
		newPL.setPartnerLinkType(plType);
		
		return newPL;

		
	}
	
	/**
	 * Copy all activities from the given PBD to the new merged Process Note: If
	 * we have name collisions, we check the new name into a map
	 * 
	 * @param pbd
	 *            The PBD to be copied over
	 * @param forEachOfCalle
	 * @param chorPack
	 *            The {@link ChoreographyPackage} containing all choreography
	 *            data
	 */
	public static void copyVarsAndActitiviesDynamicMIP(Process pbd,
			ForEach forEachOfCalle) {
		PBDFragmentDuplicator.log.log(Level.INFO,
				"Copying Variables and Activities from PBD : " + pbd.getName());

		// Scope mainScope = BPELFactory.eINSTANCE.createScope();
		// mainScope.setName("Scope_" + pbd.getName());
		Scope mainScope = BPELFactory.eINSTANCE.createScope();
		mainScope.setName("Scope_" + pbd.getName());
		updateScopeName(mainScope);
		Scope newScope = BPELFactory.eINSTANCE.createScope();
		newScope.setName("MIP_INSTANCE_Scope_" + pbd.getName());
		// Todo Added code for renaming scopes when there is participant set in
		// topology
		// updateScopeName(newScope);
		// End of added code

		if (pbd.getExitOnStandardFault() != null) {
			newScope.setExitOnStandardFault(pbd.getExitOnStandardFault());
			// mainScope.setExitOnStandardFault(pbd.getExitOnStandardFault());

		}
		if (pbd.getSuppressJoinFailure() != null) {
			newScope.setSuppressJoinFailure(pbd.getSuppressJoinFailure());
			// mainScope.setSuppressJoinFailure(pbd.getSuppressJoinFailure());
		}
		// Copy newScope into MergedProcess Flow
		((Flow) pkg.getMergedProcess().getActivity()).getActivities().add(
				mainScope);
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
				newScope.setFaultHandlers(BPELFactory.eINSTANCE
						.createFaultHandler());
			}
			newScope.getFaultHandlers().setCatchAll(
					ChoreoMergeUtil.createNPCatchAll());
		}

		// Copy EventHandlers
		PBDFragmentDuplicator.copyEHToScope(pbd, newScope);

		ForEach forEachActivity = null;
		try {
			if (forEachOfCalle.getSuppressJoinFailure() != null) {
				/**
				 * TODO
				 * 
				 * Should created loop always be parallel ?
				 * 
				 * What about completion condition ? Should it be always false ?
				 * 
				 * What about SuppressJoinFailure ??
				 * 
				 */
				forEachActivity = createNewForEachActivity(
						forEachOfCalle.getName(),
						forEachOfCalle.getSuppressJoinFailure(),
						forEachOfCalle.getCounterName(),
						true,// forEachOfCalle.getParallel()
						forEachOfCalle.getStartCounterValue(),
						forEachOfCalle.getFinalCounterValue(), null,
						pbd.getActivity(), newScope);
				// forEachOfCalle.getCompletionCondition()
			} else {
				forEachActivity = createNewForEachActivity(
						forEachOfCalle.getName(),// "ProcessMergedForEach"
						false,
						forEachOfCalle.getCounterName(),
						true,// forEachOfCalle.getParallel()
						forEachOfCalle.getStartCounterValue(),
						forEachOfCalle.getFinalCounterValue(), null,
						pbd.getActivity(), newScope);
				// forEachOfCalle.getCompletionCondition()
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			// System.exit(0);
		}

		mainScope.setActivity(forEachActivity);
		mergedProcessScopeMap.put(mainScope.getName(), mainScope);

	}

	/**
	 * 
	 * Set correct/unique scope name to the given scope. Let's say merged
	 * process already contains, SCope_1, Scope_2, then given newScope will
	 * have name Scope_3
	 * 
	 * @param newScope
	 */
	public static void updateScopeName(Scope newScope) {
		int maxScopeNumber = 0;
		EList<Activity> activityList = ((Flow) pkg.getMergedProcess()
				.getActivity()).getActivities();
		for (int i = 0; i < activityList.size(); i++) {
			Activity scope = activityList.get(i);
			if (scope.getName().equals(newScope.getName())) {
				String scopeName = newScope.getName();
				if (isInteger(scopeName
						.substring(scopeName.lastIndexOf("_") + 1))) {
					if (maxScopeNumber < Integer.parseInt(scopeName
							.substring(scopeName.lastIndexOf("_") + 1))) {
						maxScopeNumber = Integer.parseInt(scopeName
								.substring(scopeName.lastIndexOf("_") + 1));
						newScope.setName(newScope.getName().substring(0,
								newScope.getName().lastIndexOf("_") + 1)
								+ (++maxScopeNumber));
					}
				} else {
					newScope.setName(newScope.getName() + "_" + 1);

				}

			}
		}
	}

	public static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException nfe) {
		}
		return false;
	}

	/**
	 * 
	 * This function checks whether given forEach contains nested forEach loop.
	 * 
	 * This function is not needed for this thesis
	 * 
	 * @param parentForEach
	 * @param finalCounterValueList
	 * @return
	 */
	public static ArrayList<String> containsNestedForEach(
			EObject parentForEach, ArrayList<String> finalCounterValueList) {
		// Receive res = null;
		List<EObject> children = parentForEach.eContents();
		for (EObject obj : children) {
			if (obj instanceof ForEach) {
				ForEach act = (ForEach) obj;

				if (finalCounterValueList == null) {
					finalCounterValueList = new ArrayList<String>();

				}
				finalCounterValueList.add(act.getFinalCounterValue().getBody()
						.toString());
				System.out.println("-------------- ForEach: " + act.getName());
			}

		}
		for (EObject obj : children) {
			/*
			 * if (finalCounterValueList == null) { finalCounterValueList = new
			 * ArrayList<String>();
			 * 
			 * }
			 * 
			 * finalCounterValueList.addAll(containsNestedForEach(obj,
			 * finalCounterValueList));
			 */
			containsNestedForEach(obj, finalCounterValueList);

		}
		return finalCounterValueList;
	}

	/**
	 * 
	 * 
	 * @param forEachActivitName
	 * @param suppressJoinFailure
	 *            ="yes|no"?
	 * @param counterName
	 * @param parallel
	 * @param startCounterValueExpression
	 * @param finalCounterValueExpression
	 * @param completionCondition
	 * @param nextActivity
	 *            - 1st activity of pbd to be copied inside forEach
	 * @return
	 */
	public static ForEach createNewForEachActivity(String forEachActivitName,
			boolean suppressJoinFailure, Variable counterName,
			boolean parallel, Expression startCounterValueExpression,
			Expression finalCounterValueExpression,
			CompletionCondition completionCondition, Activity firstActivity,
			Scope forEachScope) {

		ForEach newForEach = BPELFactory.eINSTANCE.createForEach();
		// FragmentDuplicator.copyStandardAttributes(act, newForEach);

		newForEach.setName(forEachActivitName);// "ProcessMergedForEach"
		// 2. suppressionJoinFailure="yes|no"?
		newForEach.setSuppressJoinFailure(suppressJoinFailure);

		// PBDFragmentDuplicator.copyStandardElements(act, newForEach);
		// No source or target is required for newly created and the only
		// ForEach activity in merged process for handling Dynamic MIP
		// Instantiation

		newForEach.setCounterName(ChoreoMergeUtil
				.resolveVariableInMergedProcess(newForEach.getCounterName()));

		/*
		 * Variable variable = BPELFactory.eINSTANCE.createVariable(); // TODO:
		 * How to facade this ? variable.setName(counterName); QName qName = new
		 * QName(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001, "unsignedInt");
		 * XSDTypeDefinition type = new XSDTypeDefinitionProxy(null, qName);//
		 * getResource().getURI(), variable.setType(type);
		 */
		newForEach.setCounterName(counterName);

		newForEach.setParallel(parallel);

		newForEach.setStartCounterValue(FragmentDuplicator
				.copyExpression(startCounterValueExpression));
		newForEach.setFinalCounterValue(FragmentDuplicator
				.copyExpression(finalCounterValueExpression));

		if (completionCondition != null) {
			newForEach.setCompletionCondition(FragmentDuplicator
					.copyCompletionCondition(completionCondition));
		}
		if (firstActivity != null && firstActivity != forEachScope) {//

			forEachScope.setActivity(PBDFragmentDuplicator
					.copyActivity(firstActivity));

			newForEach.setActivity(forEachScope);
		} else if (firstActivity == forEachScope) {
			newForEach.setActivity(forEachScope);
		}

		return newForEach;
	}

	/**
	 * 
	 * Create new dynamic container/scope
	 * 
	 * @param origScope
	 * @return
	 */
	public static Scope createNewDynamicScopeActivity(Scope origScope) {
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
		if ((origScope.getVariables() != null)
				&& (origScope.getVariables().getChildren().size() > 0)) {
			newScope.setVariables(BPELFactory.eINSTANCE.createVariables());
			newScope.getVariables().getChildren()
					.addAll(origScope.getVariables().getChildren());
		}
		// Copy PartnerLinks
		if ((origScope.getPartnerLinks() != null)
				&& (origScope.getPartnerLinks().getChildren().size() > 0)) {
			newScope.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());

			newScope.getPartnerLinks().getChildren()
					.addAll(origScope.getPartnerLinks().getChildren());

		}

		// Copy MessageExchanges
		if ((origScope.getMessageExchanges() != null)
				&& (origScope.getMessageExchanges().getChildren().size() > 0)) {
			newScope.setMessageExchanges(BPELFactory.eINSTANCE
					.createMessageExchanges());
			newScope.getMessageExchanges().getChildren()
					.addAll(origScope.getMessageExchanges().getChildren());

		}

		// Copy CorrelationSets
		if ((origScope.getCorrelationSets() != null)
				&& (origScope.getCorrelationSets().getChildren().size() > 0)) {
			newScope.setCorrelationSets(BPELFactory.eINSTANCE
					.createCorrelationSets());
			newScope.getCorrelationSets().getChildren()
					.addAll(origScope.getCorrelationSets().getChildren());

		}

		// Copy FaultHandlers
		if ((origScope.getFaultHandlers() != null)
				&& ((origScope.getFaultHandlers().getCatch().size() > 0) || (origScope
						.getFaultHandlers().getCatchAll() != null))) {

			newScope.setFaultHandlers(BPELFactory.eINSTANCE
					.createFaultHandler());
			newScope.getFaultHandlers().getCatch()
					.addAll(origScope.getFaultHandlers().getCatch());

			if (origScope.getFaultHandlers().getCatchAll() != null) {
				CatchAll pbdCatAll = origScope.getFaultHandlers().getCatchAll();
				CatchAll newCatAll = PBDFragmentDuplicator
						.copyCatchAll(pbdCatAll);
				newScope.getFaultHandlers().setCatchAll(newCatAll);
			}
		}

		// Copy EventHandlers
		if ((origScope.getEventHandlers() != null)
				&& ((origScope.getEventHandlers().getAlarm().size() > 0) || (origScope
						.getEventHandlers().getEvents().size() > 0))) {
			EventHandler handler = BPELFactory.eINSTANCE.createEventHandler();

			if (origScope.getEventHandlers().getAlarm().size() != 0)
				handler.getAlarm().addAll(
						origScope.getEventHandlers().getAlarm());
			if (origScope.getEventHandlers().getEvents().size() != 0)
				handler.getEvents().addAll(
						origScope.getEventHandlers().getEvents());

			newScope.setEventHandlers(handler);
		}

		// Copy CompensationHandlers
		if (origScope.getCompensationHandler() != null) {

			newScope.setCompensationHandler(PBDFragmentDuplicator
					.copyCompensationHandler(origScope.getCompensationHandler()));
		}

		// Copy TerminationHandlers
		if (origScope.getTerminationHandler() != null) {
			newScope.setTerminationHandler(PBDFragmentDuplicator
					.copyTerminationHandler(origScope.getTerminationHandler()));
		}

		// Copy Activity from the Fragment Process to the new Scope

		ForEach firstActivityOfOriginalScope = (ForEach) origScope
				.getActivity();
		Activity newActivity = createNewForEachActivity(
				firstActivityOfOriginalScope.getName(),
				firstActivityOfOriginalScope.getSuppressJoinFailure(),
				firstActivityOfOriginalScope.getCounterName(), true,
				firstActivityOfOriginalScope.getStartCounterValue(),
				firstActivityOfOriginalScope.getFinalCounterValue(),
				firstActivityOfOriginalScope.getCompletionCondition(),
				firstActivityOfOriginalScope.getActivity(),
				(Scope) firstActivityOfOriginalScope.getActivity());

		newScope.setActivity(newActivity);

		FragmentDuplicator.copyStandardAttributes(origScope, newScope);
		PBDFragmentDuplicator.copyStandardElements(origScope, newScope);

		return newScope;
	}

	/**
	 * 
	 * Create new static scope/container
	 * 
	 * @param origScope
	 * @return
	 */
	public static Scope createNewStaticScopeActivity(Scope origScope) {
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
		if ((origScope.getVariables() != null)
				&& (origScope.getVariables().getChildren().size() > 0)) {
			newScope.setVariables(BPELFactory.eINSTANCE.createVariables());
			newScope.getVariables().getChildren()
					.addAll(origScope.getVariables().getChildren());
		}
		// Copy PartnerLinks
		if ((origScope.getPartnerLinks() != null)
				&& (origScope.getPartnerLinks().getChildren().size() > 0)) {
			newScope.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());

			newScope.getPartnerLinks().getChildren()
					.addAll(origScope.getPartnerLinks().getChildren());

		}

		// Copy MessageExchanges
		if ((origScope.getMessageExchanges() != null)
				&& (origScope.getMessageExchanges().getChildren().size() > 0)) {
			newScope.setMessageExchanges(BPELFactory.eINSTANCE
					.createMessageExchanges());
			newScope.getMessageExchanges().getChildren()
					.addAll(origScope.getMessageExchanges().getChildren());

		}

		// Copy CorrelationSets
		if ((origScope.getCorrelationSets() != null)
				&& (origScope.getCorrelationSets().getChildren().size() > 0)) {
			newScope.setCorrelationSets(BPELFactory.eINSTANCE
					.createCorrelationSets());
			newScope.getCorrelationSets().getChildren()
					.addAll(origScope.getCorrelationSets().getChildren());

		}

		// Copy FaultHandlers
		if ((origScope.getFaultHandlers() != null)
				&& ((origScope.getFaultHandlers().getCatch().size() > 0) || (origScope
						.getFaultHandlers().getCatchAll() != null))) {

			newScope.setFaultHandlers(BPELFactory.eINSTANCE
					.createFaultHandler());
			newScope.getFaultHandlers().getCatch()
					.addAll(origScope.getFaultHandlers().getCatch());

			/*
			 * newScope.getFaultHandlers().setCatchAll(
			 * BPELFactory.eINSTANCE.createCatchAll());
			 */

			if (origScope.getFaultHandlers().getCatchAll() != null) {
				CatchAll pbdCatAll = origScope.getFaultHandlers().getCatchAll();
				CatchAll newCatAll = PBDFragmentDuplicator
						.copyCatchAll(pbdCatAll);
				newScope.getFaultHandlers().setCatchAll(newCatAll);
			}
		}

		// Copy EventHandlers
		if ((origScope.getEventHandlers() != null)
				&& ((origScope.getEventHandlers().getAlarm().size() > 0) || (origScope
						.getEventHandlers().getEvents().size() > 0))) {
			EventHandler handler = BPELFactory.eINSTANCE.createEventHandler();

			if (origScope.getEventHandlers().getAlarm().size() != 0)
				handler.getAlarm().addAll(
						origScope.getEventHandlers().getAlarm());
			if (origScope.getEventHandlers().getEvents().size() != 0)
				handler.getEvents().addAll(
						origScope.getEventHandlers().getEvents());

			newScope.setEventHandlers(handler);
		}

		// Copy CompensationHandlers
		if (origScope.getCompensationHandler() != null) {
			/*
			 * newScope.setCompensationHandler(BPELFactory.eINSTANCE
			 * .createCompensationHandler());
			 */
			newScope.setCompensationHandler(PBDFragmentDuplicator
					.copyCompensationHandler(origScope.getCompensationHandler()));
		}

		// Copy TerminationHandlers
		if (origScope.getTerminationHandler() != null) {
			newScope.setTerminationHandler(PBDFragmentDuplicator
					.copyTerminationHandler(origScope.getTerminationHandler()));
		}

		// Copy Activity from the Fragment Process to the new Scope

		ForEach firstActivityOfOriginalScope = (ForEach) origScope
				.getActivity();
		Activity newActivity = createNewForEachActivity(
				firstActivityOfOriginalScope.getName(),
				firstActivityOfOriginalScope.getSuppressJoinFailure(),
				firstActivityOfOriginalScope.getCounterName(), true,
				firstActivityOfOriginalScope.getStartCounterValue(),
				firstActivityOfOriginalScope.getFinalCounterValue(),
				firstActivityOfOriginalScope.getCompletionCondition(),
				firstActivityOfOriginalScope.getActivity(),
				(Scope) firstActivityOfOriginalScope.getActivity());

		newScope.setActivity(newActivity);

		FragmentDuplicator.copyStandardAttributes(origScope, newScope);
		PBDFragmentDuplicator.copyStandardElements(origScope, newScope);

		return newScope;
	}

	/**
	 * Copy original If activity without its children and return new one
	 * 
	 * @param act
	 * @return
	 */
	public static If copyIfActivityWOChildren(If act) {
		if (act == null) {
			return null;
		}
		If newIf = BPELFactory.eINSTANCE.createIf();
		FragmentDuplicator.copyStandardAttributes(act, newIf);
		PBDFragmentDuplicator.copyStandardElements(act, newIf);
		newIf.setCondition(FragmentDuplicator.copyCondition(act.getCondition()));
		if (act.getElse() != null) {
			Else newElse = BPELFactory.eINSTANCE.createElse();
			newElse.setActivity(PBDFragmentDuplicator.copyActivity(act
					.getElse().getActivity()));
			newIf.setElse(newElse);
		}
		if ((act.getElseIf() != null) && (act.getElseIf().size() > 0)) {
			for (ElseIf elseIf : act.getElseIf()) {
				ElseIf newElseIf = BPELFactory.eINSTANCE.createElseIf();
				newElseIf.setCondition(FragmentDuplicator.copyCondition(elseIf
						.getCondition()));
				newElseIf.setActivity(PBDFragmentDuplicator.copyActivity(elseIf
						.getActivity()));
				newIf.getElseIf().add(newElseIf);
			}
		}
		return newIf;
	}

}
