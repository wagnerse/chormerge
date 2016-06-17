package org.bpel4chor.mergechoreography.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.CompensateScope;
import org.eclipse.bpel.model.CompensationHandler;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * This class contains methods for PreProcessing. <br>
 * To use EcoreUtil following this steps in Eclipse:<br>
 * Project Properties -> Java Build Path -> Libraries -> Plug-in Dependencies ->
 * org.eclipse.emf.ecore_2.8.3.jar -> Access rules -> Edit -> Add ->
 * Resolution=Accessible; Rule Pattern=org/eclipse/emf/ecore/util/**<br>
 * or use the eclipse internal Preferences:<br>
 * Windows -> Preferences -> Java -> Compiler -> Errors/Warnings -> Deprecated
 * and restricted API -> Forbidden reference (access rules) = Warning<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class MergePreProcessor implements Constants {
	
	/**
	 * Starts PreProcessing of merge process.
	 * 
	 * @param choreographyPackage contains information for PreProcessing
	 */
	public static void startPreProcessing(ChoreographyPackage choreographyPackage) {
		// alternative Invokes
		startPreprocessingForInvokes(choreographyPackage.getPbds());
		// ### EXTEND PreProcessing code here EXTEND ###
	}
	
	/**
	 * Searches for {@link Invoke}s with {@link FaultHandler} or
	 * {@link CompensationHandler} and generates new {@link Scope}s which are
	 * equivalent to their {@link Invoke}s.<br>
	 * BPEL specification page 86/87.
	 * 
	 * @param pbdList contains the {@link Process}es with {@link Invoke}s
	 */
	private static void startPreprocessingForInvokes(List<Process> pbdList) {
		TreeIterator<?> iterator = null;
		Object oElement = null;
		List<Invoke> invokeList = null;
		HashMap<Activity, Set<CompensateScope>> csMapper = null;
		Invoke invoke = null;
		CompensateScope cs = null;
		for (Process p : pbdList) {
			invokeList = new ArrayList<>();
			csMapper = new HashMap<>();
			// find all Invokes, do not change anything in current process
			// iterator
			iterator = p.eAllContents();
			while (iterator.hasNext()) {
				oElement = iterator.next();
				if (oElement instanceof Invoke) {
					invoke = (Invoke) oElement;
					if (invoke.getFaultHandler() != null || invoke.getCompensationHandler() != null) {
						invokeList.add(invoke);
					}
				} else if (oElement instanceof CompensateScope) {
					// CompensateScope need to be updated with the new Scope
					cs = (CompensateScope) oElement;
					Set<CompensateScope> csSet = csMapper.get(cs.getTarget());
					if (csSet == null) {
						csSet = new HashSet<>();
						csMapper.put(cs.getTarget(), csSet);
					}
					csSet.add(cs);
				}
			}
			// replace all Invokes with alternative Invoke
			for (Invoke inv : invokeList) {
				createAlternativeInvoke(inv, csMapper.get(inv));
			}
		}
	}
	
	/**
	 * Creates a new alternative {@link Scope} from the given {@link Invoke} and
	 * updates the associated {@link CompensateScope}s.
	 * 
	 * @param invoke an alternative will be created
	 * @param csSet includes all {@link CompensateScope}s that must be updated
	 *            to the new {@link Scope}
	 */
	private static void createAlternativeInvoke(Invoke invoke, Set<CompensateScope> csSet) {
		Scope scope = BPELFactory.eINSTANCE.createScope();
		scope.setName(createSurroundingScopeName());
		scope.setSources(invoke.getSources());
		scope.setTargets(invoke.getTargets());
		if (invoke.getFaultHandler() != null) {
			// 1. this is necessary because the old intern FaultHandler blocks
			// the simple setActivity with UnmodifiedRandomAccessList<br>
			// 2. can not use PBDFragmentDuplicatior because it changes the DOM
			// for the mergeProcess (wsu:id will be removed)
			scope.setFaultHandlers(EcoreUtil.copy(invoke.getFaultHandler()));
		}
		
		if (invoke.getCompensationHandler() != null) {
			scope.setCompensationHandler(EcoreUtil.copy(invoke.getCompensationHandler()));
		}
		
		// set new Activity
		Invoke newActivity = EcoreUtil.copy(invoke);
		newActivity.setFaultHandler(null);
		newActivity.setCompensationHandler(null);
		scope.setActivity(newActivity);
		// replace Activity
		ChoreoMergeUtil.replaceActivity(invoke, scope);
		
		// update old CompensateScopes
		if (csSet != null) {
			for (CompensateScope cs : csSet) {
				cs.setTarget(scope.getName());
				cs.setTarget(scope);
			}
		}
	}
	
	/**
	 * Creates a name for the surrounding {@link Scope}.
	 * 
	 * @return
	 */
	private static String createSurroundingScopeName() {
		return NAME_SCOPE_PRE_PROCESSING + FCTEUtil.getUUID();
	}
}
