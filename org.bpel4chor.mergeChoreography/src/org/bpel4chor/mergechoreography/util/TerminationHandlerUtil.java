package org.bpel4chor.mergechoreography.util;

import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.TerminationHandler;

/**
 * This class processes the {@link TerminationHandler}. It generates a new
 * {@link Scope} and puts the {@link Activity} from the {@link TerminatHandler}
 * into it. After that it generates {@link Link}s to connect the old
 * {@link TerminationHandler} with the new {@link Scope}.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class TerminationHandlerUtil implements Constants {
	
	/**
	 * Creates {@link Scope}s and {@link Link}s for the
	 * {@link TerminationHandler}
	 * 
	 * @param oldScope the old {@link Scope} that contains the
	 *            {@link TerminationHandler}
	 * @param emptyContinue the join {@link Activity} for all new {@link Scopes}
	 * @param newSurFlow in which the new {@link TerminationHandler}-
	 *            {@link Scope} will be put
	 */
	public static void processTerminationHandler(Scope oldScope, Empty emptyContinue, Flow newSurFlow) {
		if (oldScope.getTerminationHandler() != null) {
			Link thToScope = FCTEUtil.createNewLink();
			
			newSurFlow.getLinks().getChildren().add(thToScope);
			/** thToScope */
			Scope thScope = BPELFactory.eINSTANCE.createScope();
			
			// TerminationHandler can not have a FaultHandler, we have to
			// uninstall the default FaultHandler
			FCTEUtil.createEmptyAndSetToCatchAll(thScope);
			
			thScope.setName(createNameForTHScope(oldScope));
			// move activity to new scope
			thScope.setActivity(oldScope.getTerminationHandler().getActivity());
			// add target from thToScope scope (incoming link = target)
			ChoreoMergeUtil.createTarget4LinkInActivity(thToScope, thScope);
			
			// add to flow
			newSurFlow.getActivities().add(thScope);
			
			/** thScopeToEmptyContinue */
			Link thScopeToEmptyContinue = FCTEUtil.createNewLink();
			newSurFlow.getLinks().getChildren().add(thScopeToEmptyContinue);
			// Target from thScopeToEmptyContinue
			ChoreoMergeUtil.createTarget4LinkInActivity(thScopeToEmptyContinue, emptyContinue);
			
			// Source from thScopeToEmptyContinue
			ChoreoMergeUtil.createSource4LinkInActivity(thScopeToEmptyContinue, thScope);
			
			/**
			 * root scope must not have a CompensationHandler<br>
			 * bpel specification page 125
			 */
			FCTEUtil.createEmptyAndSetToCompensationHandler(thScope);
			
			// now set a new source to the old TerminationHandler
			oldScope.getTerminationHandler().setActivity(FCTEUtil.createNewEmptyWithSourceLink(thToScope));
		}
	}
	
	/**
	 * Creates a new name for the new {@link TerminationHandler} {@link Scope}:<br>
	 * <code>"TH_Scope_" +oldScope.getName()</code>
	 * 
	 * @param oldScope gives its name as postfix
	 * @return
	 */
	private static String createNameForTHScope(Scope oldScope) {
		return TH_NAME_NEW_SCOPE + oldScope.getName();
	}
}
