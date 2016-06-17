package org.bpel4chor.mergechoreography.util;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Catch;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.TerminationHandler;
import org.eclipse.bpel.model.Variable;

import de.uni_stuttgart.iaas.bpel.model.utilities.FragmentDuplicator;

/**
 * This class processes the {@link FaultHandler}. It generates a new
 * {@link Scope} and puts the {@link Activity} from the {@link FaultHandler}
 * into it. After that it generates {@link Link}s to connect the old
 * {@link FaultHandler} with the new {@link Scope}.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class FaultHandlerUtil implements Constants {
	
	/**
	 * Creates {@link Scope}s and {@link Link}s for the {@link FaultHandler}
	 * 
	 * @param oldFaultHandler {@link FaultHandler} that should be processed
	 * @param oldActivityName old name of the {@link Activity}
	 * @param newSurScope in this {@link Scope} some new {@link Variable}s with
	 *            the values from the old {@link Catch} or {@link CatchAll} will
	 *            be stored so that they can be accessed by the new
	 *            {@link Scope}s
	 * @param emptyContinue the join {@link Activity} for all new {@link Scopes}
	 * @param newSurFlow in which the new {@link TerminationHandler}-
	 *            {@link Scope} will be put
	 */
	public static void processFaultHandler(FaultHandler oldFaultHandler, String oldActivityName, Scope newSurScope, Empty emptyContinue, Flow newSurFlow) {
		if (oldFaultHandler.getCatch() != null) {
			// catch case
			for (Catch cat : oldFaultHandler.getCatch()) {
				if (FCTEUtil.checkIfActivityOrSubActivityhaveIncomingSource(cat)) {
					Link catchToScope = FCTEUtil.createNewLink();
					
					Scope newCatchScope = addNewScopeForCatchOrCatchAllCase(newSurFlow, emptyContinue, catchToScope, cat.getActivity(), createNameForCatch(oldActivityName));
					
					/**
					 * add empty activity with Source from catchToFhScope
					 * (outgoing link = source)<br>
					 * this activity must set after add the old activity to new
					 * scope
					 */
					cat.setActivity(FCTEUtil.createNewEmptyWithSourceLink(catchToScope));
					
					// process catch Var
					processCatchVar(cat, newCatchScope, newSurScope);
				}
			}
		}
		if (oldFaultHandler.getCatchAll() != null && FCTEUtil.checkIfActivityOrSubActivityhaveIncomingSource(oldFaultHandler.getCatchAll())) {
			// catchAll case
			Link catchToScope = FCTEUtil.createNewLink();
			addNewScopeForCatchOrCatchAllCase(newSurFlow, emptyContinue, catchToScope, oldFaultHandler.getCatchAll().getActivity(), createNameForScopeCatchAll(oldActivityName));
			/**
			 * add empty activity with Source from catchToFhScope (outgoing link
			 * = source)<br>
			 * this activity must set after add the old activity to new scope
			 */
			oldFaultHandler.getCatchAll().setActivity(FCTEUtil.createNewEmptyWithSourceLink(catchToScope));
		}
	}
	
	/**
	 * Creates a new name for the new {@link CatchAll} {@link Scope}:<br>
	 * <code>oldActivityName + "_catchAll"</code>
	 * 
	 * @param oldActivityName name of the old {@link Activity}
	 * @return
	 */
	private static String createNameForScopeCatchAll(String oldActivityName) {
		return oldActivityName + FH_NAME_CASE_CATCHALL;
	}
	
	/**
	 * Creates a new name for the new {@link Catch} {@link Scope}:<br>
	 * <code>oldActivityName + "_catch"</code>
	 * 
	 * @param oldActivityName name of the old {@link Activity}
	 * @return
	 */
	private static String createNameForCatch(String oldActivityName) {
		return oldActivityName + FH_NAME_CASE_CATCH;
	}
	
	/**
	 * Adds a new {@link Variable} to the surrounding {@link Scope}
	 * <code>newSurScope</code> after that it creates a {@link Sequence} and
	 * adds a new {@link Assign} with a {@link Copy} from the fault
	 * {@link Variable} to new {@link Variable}. The {@link Sequence} will be
	 * added to current {@link Catch} and the {@link Activity} from
	 * {@link Catch} will be added to the {@link Sequence} .<br>
	 * After that a {@link Variable} with the name from {@link Catch}-
	 * {@link Variable} will be created in the catch {@link Scope}
	 * <code>catchScope</code> and a {@link Sequence} with {@link Assign} and
	 * {@link Copy} will be created too.
	 * 
	 * @param currentCat current {@link Catch} that contains information of the
	 *            {@link Variable}s that should be mapped
	 * @param catchScope the corresponding {@link Scope} that gets the new
	 *            {@link Assign}
	 * @param newSurScope the surrounding {@link Scope} in which the mapping
	 *            {@link Variable} will be added
	 */
	private static void processCatchVar(Catch currentCat, Scope catchScope, Scope newSurScope) {
		if (currentCat.getFaultVariable() != null) {
			/** first create new var in newSurScope to save catchVarValue */
			Variable var = FragmentDuplicator.copyVariable(currentCat.getFaultVariable());
			var.setName(var.getName() + "_" + FCTEUtil.getUUID());
			FCTEUtil.initScopeVar(newSurScope);
			newSurScope.getVariables().getChildren().add(var);
			
			/**
			 * add new Sequence with assign (faultvar to new created var) to
			 * currentCat
			 */
			Assign newAssign = BPELFactory.eINSTANCE.createAssign();
			newAssign.getCopy().add(ChoreoMergeUtil.createCopy(currentCat.getFaultVariable(), var));
			Sequence seq = BPELFactory.eINSTANCE.createSequence();
			seq.getActivities().add(newAssign);
			seq.getActivities().add(currentCat.getActivity());
			// replace Activity with new Sequence
			currentCat.setActivity(seq);
			
			/** create new Variable with oldName and it to new Scope */
			Variable scopeVar = FragmentDuplicator.copyVariable(currentCat.getFaultVariable());
			FCTEUtil.initScopeVar(catchScope);
			catchScope.getVariables().getChildren().add(scopeVar);
			// add assign with copy to Scope
			Assign copyAssignCatchScope = BPELFactory.eINSTANCE.createAssign();
			copyAssignCatchScope.getCopy().add(ChoreoMergeUtil.createCopy(var, scopeVar));
			Sequence seqScope = BPELFactory.eINSTANCE.createSequence();
			seqScope.getActivities().add(copyAssignCatchScope);
			seqScope.getActivities().add(catchScope.getActivity());
			catchScope.setActivity(seqScope);
		}
	}
	
	/**
	 * Adds new {@link Scope} for {@link Catch} or {@link CatchAll} case to
	 * {@link Flow} with all parameters and connect it to the old {@link Scope}
	 * 
	 * @param flow in which the new {@link FaultHandler}- {@link Scope} will be
	 *            put
	 * @param emptyContinue the join {@link Activity} for all new {@link Scope}s
	 * @param catchToScope {@link Link} from old {@link Catch} to new
	 *            {@link Scope}
	 * @param catchActivity old {@link Activity}s from old {@link Catch}
	 * @param newScopeName
	 * @return returns the new created {@link Scope}
	 */
	private static Scope addNewScopeForCatchOrCatchAllCase(Flow flow, Empty emptyContinue, Link catchToScope, Activity catchActivity, String newScopeName) {
		flow.getLinks().getChildren().add(catchToScope);
		/** catchToFhScope */
		Scope catScope = BPELFactory.eINSTANCE.createScope();
		catScope.setName(newScopeName);
		// move activity to new scope
		catScope.setActivity(catchActivity);
		// add target from catchToFhScope scope (incoming link = target)
		ChoreoMergeUtil.createTarget4LinkInActivity(catchToScope, catScope);
		
		// add to flow
		flow.getActivities().add(catScope);
		
		/** fhScopeToEmptyContinue */
		Link scopeToEmptyContinue = FCTEUtil.createNewLink();
		flow.getLinks().getChildren().add(scopeToEmptyContinue);
		// Target from fhScopeToEmptyContinue
		ChoreoMergeUtil.createTarget4LinkInActivity(scopeToEmptyContinue, emptyContinue);
		
		// Source from fhScopeToEmptyContinue
		ChoreoMergeUtil.createSource4LinkInActivity(scopeToEmptyContinue, catScope);
		
		/**
		 * root scope must not have a CompensationHandler<br>
		 * bpel specification page 125
		 */
		FCTEUtil.createEmptyAndSetToCompensationHandler(catScope);
		return catScope;
	}
	
}
