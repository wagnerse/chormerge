package org.bpel4chor.mergechoreography.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.util.model.ScopeHandlerBean;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.CatchAll;
import org.eclipse.bpel.model.Compensate;
import org.eclipse.bpel.model.CompensationHandler;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Links;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.TerminationHandler;
import org.eclipse.bpel.model.Variable;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;

import de.uni_stuttgart.iaas.bpel.model.utilities.ExtendedActivityIterator;

/**
 * Main class to process {@link TerminationHandler} and {@link FaultHandler}. If
 * any {@link Target} is found in the {@link TerminationHandler} or
 * {@link FaultHandler} new {@link Scope}s will be generated and {@link Links}
 * will be set so that the merged process can handle incoming {@link Links} in
 * {@link TerminationHandler} and {@link FaultHandler}<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class FCTEUtil implements Constants {
	
	private static Logger log = Logger.getLogger(FCTEUtil.class);
	
	
	/**
	 * Iterates over all PBD {@link Scope}s and installs alternative
	 * CompensationHandler
	 * 
	 * @param mergedProcess that {@link CompensationHandler} will be changed
	 */
	public static void processCompensationHandler(Process mergedProcess) {
		// process all pbdScopes only
		Activity act = mergedProcess.getActivity();
		if (act instanceof Flow) {
			Flow rootFlow = (Flow) act;
			for (Activity activity : rootFlow.getActivities()) {
				// CHECK care we will change the flow {@link Activity}s
				CompensationHandlerUtil.processCompensation((Scope) activity);
			}
		} else {
			log.warn("Process order \nMergedProcess -> Flow -> PBD Scopes\nnot given. CompensationHandler logic could not be installed.");
		}
	}
	
	/**
	 * Looks for all {@link Scope}s in <code>mergedProcess</code> and checks if
	 * any one should be processed
	 * 
	 * @param mergedProcess process that will be changed if any {@link Target}
	 *            is found which is connected with a {@link Source} outside of
	 *            the corresponding Handler
	 * @param pbds {@link List} of {@link Process}es that contains information
	 *            about if the default-{@link FaultHandler} should be installed
	 *            or an alternative that only contains an {@link Compensate}
	 */
	public static void processScopesFT(Process mergedProcess, List<Process> pbds) {
		Map<Scope, ScopeHandlerBean> mapScope = new HashMap<Scope, ScopeHandlerBean>();
		// find all Scopes to get the FaultHandler
		ExtendedActivityIterator actIterator = new ExtendedActivityIterator(mergedProcess);
		Activity nextA = null;
		Scope scope = null;
		ScopeHandlerBean shb = null;
		while (actIterator.hasNext()) {
			nextA = actIterator.next();
			shb = new ScopeHandlerBean();
			
			if (nextA instanceof Scope) {
				// before we change anything we have to check which scope should
				// be process
				scope = (Scope) nextA;
				// check if FaultHandler should be processed
				if (scope.getFaultHandlers() != null) {
					shb.setFaultHandler(checkIfActivityOrSubActivityhaveIncomingSource(scope.getFaultHandlers()));
				}
				// check if TerminationHandler should be processed
				if (scope.getTerminationHandler() != null) {
					shb.setTerminationHandler(checkIfActivityOrSubActivityhaveIncomingSource(scope.getTerminationHandler()));
				}
				if (shb.processScope()) {
					// check if Scope was PBD, necessary to set
					// CompensationHandler in surrounding Scope
					shb.setPbdScope(isScopePBDProcess(scope, pbds));
					// process Scope if necessary
					mapScope.put(scope, shb);
				}
			}
		}
		
		// needed because we can not change the scope in
		// ExtendedActivityIterator
		for (Entry<Scope, ScopeHandlerBean> entry : mapScope.entrySet()) {
			processScope(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Checks if the given {@link Scope} have an associated PBD
	 * 
	 * @param scope name of this {@link Scope} will be checked if any Process in
	 *            the list
	 * @param pbds {@link List} of {@link Process}es that will be checked
	 * 
	 * @return
	 */
	private static boolean isScopePBDProcess(Scope scope, List<Process> pbds) {
		boolean result = false;
		// a PBD scope could be in the flow=MergedFlow or could be nested with
		// other PBDs
		// if a scope was a PBD we have to identify it with name
		for (Process p : pbds) {
			// We do NOT need the following if statement as this code is called BEFORE
			// {@link org.bpel4chor.mergechoreography.util.PBDFragmentDuplicator.copyVarsAndActitivies(Process)}
			// Otherwise, we would have to do followng:
			// match all existing PBD scopes, e.g., generated at loop unrolling.
			// if (scope.getName().startsWith(PBDFragmentDuplicator.getNewPBDNameForScope(p))) {

			// only one scope for each PBD is existing
			if (PBDFragmentDuplicator.getNewPBDNameForScope(p).equals(scope.getName())) {
				// overwrite default-FaultHandler, if an error occurs do not
				// affect the other Scope-Processes
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Checks if any {@link Activity} contains a {@link Target}. If one
	 * {@link Target} exists that has a connected {@link Source} outside of the
	 * handler then it should be processed.
	 * 
	 * @param eAllContents BPEL element that should be traced and checked
	 * @return true if a {@link Target} with connected {@link Source} outside of
	 *         the handler is found
	 */
	public static boolean checkIfActivityOrSubActivityhaveIncomingSource(EObject eAllContents) {
		boolean result = false;
		TreeIterator<?> iterator = eAllContents.eAllContents();
		Object oElement = null;
		// trace the given tree
		treeIterator: while (iterator.hasNext()) {
			oElement = iterator.next();
			if (oElement instanceof FaultHandler || oElement instanceof TerminationHandler) {
				// stop this tree line, only search in Scope activities, every
				// Scope-Handler get a new search
				iterator.prune();
				continue treeIterator;
			} else if (oElement instanceof Activity) {
				// if one activity with Target was found, break search and
				// return true
				Activity act = (Activity) oElement;
				if (act.getTargets() != null && act.getTargets().getChildren().size() > 0 && isOneOfSourcesOutsideOfHandler(eAllContents, act)) {
					result = true;
					break treeIterator;
				}
			}
		}
		return result;
	}
	
	/**
	 * Inner Links are allowed but if an {@link Activity} comes from outside the
	 * handler it should be processed. Only {@link Source}s in the same handler
	 * are allowed.
	 * 
	 * @param handler current handler which is checked
	 * @param actWithTargets {@link Activity} that contains the founded
	 *            {@link Target}s
	 * @return
	 */
	private static boolean isOneOfSourcesOutsideOfHandler(EObject handler, Activity actWithTargets) {
		boolean result = false;
		Set<Activity> sourceActivityList = ChoreoMergeUtil.findSourcesOfActivity(actWithTargets);
		EObject container = null;
		for (Activity act : sourceActivityList) {
			// Climb up and check every Activity if Source is in the same
			// handler as Target
			container = act.eContainer();
			while (container != null && !(handler.equals(container))) {
				container = container.eContainer();
			}
			if (container == null || !container.equals(handler)) {
				result = true;
				break;
			}
		}
		return result;
	}
	
	/**
	 * Generates new {@link Scope}s and a {@link Flow} and connects them.
	 * 
	 * @param oldScope {@link Scope} that should be processed
	 * @param shb contains information on if {@link TerminationHandler} or
	 *            {@link FaultHandler} should be processed
	 */
	private static void processScope(Scope oldScope, ScopeHandlerBean shb) {
		Flow newSurFlow = BPELFactory.eINSTANCE.createFlow();
		newSurFlow.setLinks(BPELFactory.eINSTANCE.createLinks());
		newSurFlow.setName(getSurFlowName(oldScope.getName()));
		
		// FH surrounding scope
		Scope newSurScope = createNewSurroundingScope(oldScope, shb);
		newSurScope.setActivity(newSurFlow);
		
		// empty with Source from Scope to continue process
		Empty emptyContinue = BPELFactory.eINSTANCE.createEmpty();
		emptyContinue.setName(FH_NAME_EMPTY_CONTINUE);
		// outgoing link
		emptyContinue.setSources(oldScope.getSources());
		newSurFlow.getActivities().add(emptyContinue);
		
		/** process FaultHandler */
		if (shb.isFaultHandler()) {
			FaultHandlerUtil.processFaultHandler(oldScope.getFaultHandlers(), oldScope.getName(), newSurScope, emptyContinue, newSurFlow);
		}
		
		/** process TerminationHandler */
		if (shb.isTerminationHandler()) {
			TerminationHandlerUtil.processTerminationHandler(oldScope, emptyContinue, newSurFlow);
		}
		
		// old scope process, delete old sources and add new link to
		// emptyContinue
		Link oldScopeToEmptyContinue = createNewLink();
		newSurFlow.getLinks().getChildren().add(oldScopeToEmptyContinue);
		ChoreoMergeUtil.createTarget4LinkInActivity(oldScopeToEmptyContinue, emptyContinue);
		// replace scope with NewSurScope
		ChoreoMergeUtil.replaceActivity(oldScope, newSurScope);
		// delete old sources and add new source
		oldScope.setSources(null);
		ChoreoMergeUtil.createSource4LinkInActivity(oldScopeToEmptyContinue, oldScope);
		// add oldScope to newSurFlow
		newSurFlow.getActivities().add(oldScope);
	}
	
	/**
	 * Creates a name for the surrounding {@link Flow}.
	 * 
	 * @param currentScopeName which will be used as postfix
	 * @return
	 */
	private static String getSurFlowName(String currentScopeName) {
		return FH_NAME_FLOW + currentScopeName;
	}
	
	/**
	 * Creates a new {@link Link} with name as UUID.
	 * 
	 * @return
	 */
	public static Link createNewLink() {
		Link link = BPELFactory.eINSTANCE.createLink();
		link.setName(getUUID());
		return link;
	}
	
	/**
	 * Returns random UUID as {@link String}
	 * 
	 * @return
	 */
	public static synchronized String getUUID() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Creates an {@link Empty} {@link Activity} and adds a new {@link Source}-
	 * {@link Link}
	 * 
	 * @param link which will be set as new {@link Link}
	 * 
	 * @return
	 */
	public static Empty createNewEmptyWithSourceLink(Link link) {
		Empty em = BPELFactory.eINSTANCE.createEmpty();
		ChoreoMergeUtil.createSource4LinkInActivity(link, em);
		return em;
	}
	
	/**
	 * Initialize {@link Variable}s from the given {@link Scope}
	 * 
	 * @param scope whose {@link Variable}s should be initialized
	 */
	public static void initScopeVar(Scope scope) {
		if (scope.getVariables() == null) {
			scope.setVariables(BPELFactory.eINSTANCE.createVariables());
		}
	}
	
	/**
	 * Creates a new surrounding {@link Scope} that is needed to access
	 * {@link Variable}s from the new {@link FaultHandler}-{@link Scope}s
	 * 
	 * @param scope contains Variables that should be lifted up
	 * @param shb contains information on if the given {@link Scope} have an
	 *            associated PBD
	 * @return
	 */
	private static Scope createNewSurroundingScope(Scope scope, ScopeHandlerBean shb) {
		Scope newSurScope = BPELFactory.eINSTANCE.createScope();
		newSurScope.setName(createSurroundingScopeName(scope.getName()));
		// copy Variables, we need it to use them in the new scopes for every
		// catch case
		newSurScope.setVariables(scope.getVariables());
		// we dont need them anymore in the processScope
		scope.setVariables(null);
		// only compensate if scope was PBD!
		if (shb.isPbdScope()) {
			newSurScope.setFaultHandlers(BPELFactory.eINSTANCE.createFaultHandler());
			newSurScope.getFaultHandlers().setCatchAll(ChoreoMergeUtil.createNPCatchAll());
		}
		return newSurScope;
	}
	
	/**
	 * Creates a name for the surrounding {@link Scope}.
	 * 
	 * @param currentScopeName which will be used as postfix
	 * @return
	 */
	private static String createSurroundingScopeName(String currentScopeName) {
		return FH_NAME_NEW_SUR_SCOPE + currentScopeName;
	}
	
	/**
	 * Creates an {@link Empty} and adds it to the {@link CatchAll} from the
	 * given {@link Scope}. If {@link CatchAll} exists it will be overwritten.
	 * With this method you can overwrite the default {@link FaultHandler}.
	 * 
	 * @param scope that will get a new {@link FaultHandler}
	 */
	public static void createEmptyAndSetToCatchAll(Scope scope) {
		scope.setFaultHandlers(BPELFactory.eINSTANCE.createFaultHandler());
		CatchAll ca = BPELFactory.eINSTANCE.createCatchAll();
		ca.setActivity(BPELFactory.eINSTANCE.createEmpty());
		scope.getFaultHandlers().setCatchAll(ca);
	}
	
	/**
	 * Creates an {@link Empty} and adds it to the {@link CompensationHandler}
	 * from the given {@link Scope}. If {@link CompensationHandler} exists it
	 * will be overwritten. With this method you can overwrite the default
	 * {@link CompensationHandler}.
	 * 
	 * @param scope that will get a new {@link CompensationHandler}
	 */
	public static void createEmptyAndSetToCompensationHandler(Scope scope) {
		CompensationHandler ch = BPELFactory.eINSTANCE.createCompensationHandler();
		ch.setActivity(BPELFactory.eINSTANCE.createEmpty());
		scope.setCompensationHandler(ch);
	}
}
