package org.bpel4chor.mergechoreography.util;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.util.model.TreeNode;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.CompensationHandler;
import org.eclipse.bpel.model.Condition;
import org.eclipse.bpel.model.Copy;
import org.eclipse.bpel.model.EventHandler;
import org.eclipse.bpel.model.Expression;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.From;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.RepeatUntil;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.TerminationHandler;
import org.eclipse.bpel.model.To;
import org.eclipse.bpel.model.Variable;
import org.eclipse.bpel.model.While;
import org.eclipse.bpel.model.util.BPELConstants;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xsd.XSDFactory;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.util.XSDConstants;

/**
 * This class processes the {@link CompensationHandler}. It generates a new
 * compensation {@link Flow} for a given {@link Scope}.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class CompensationHandlerUtil implements Constants {
	
	private static Logger log = Logger.getLogger(CompensationHandlerUtil.class);
	
	
	/**
	 * Creates an alternative compensation for the given {@link Scope}.
	 * 
	 * @param pbdScope {@link Scope} which will get a new compensation
	 *            {@link Flow}.
	 */
	public static void processCompensation(Scope pbdScope) {
		log.debug("Compensation logic startet for Scope=" + pbdScope.getName());
		
		HashMap<Activity, TreeNode> mapperATn = new HashMap<>();
		// build compensation tree
		TreeNode root = new TreeNode(pbdScope);
		createCompensationNode(root, mapperATn);
		// create compensation Scope
		Scope cpSurScope = BPELFactory.eINSTANCE.createScope();
		cpSurScope.setName(createSurroundingScopeName());
		cpSurScope.setVariables(BPELFactory.eINSTANCE.createVariables());
		ChoreoMergeUtil.replaceActivity(pbdScope, cpSurScope);
		// flow that contains the old Scope and the new compensation flow
		Flow cpFlow = BPELFactory.eINSTANCE.createFlow();
		cpFlow.getActivities().add(pbdScope);
		// assign for init vars
		Assign assign = BPELFactory.eINSTANCE.createAssign();
		// sequence contains flow and assign for init vars
		Sequence sequence = BPELFactory.eINSTANCE.createSequence();
		sequence.getActivities().add(assign);
		sequence.getActivities().add(cpFlow);
		// add sequence to cpSurScope
		cpSurScope.setActivity(sequence);
		// create compensation tree
		cpFlow.getActivities().add(createCompensationFlow(root, cpSurScope, assign, mapperATn));
	}
	
	/**
	 * Creates the new alternative compensation {@link Flow}.
	 * 
	 * @param root {@link TreeNode} which starts the pbdScope.
	 * @param cpSurScope {@link Scope} that contains the compensation
	 *            {@link Flow} and the {@link Scope} which needs an alternative
	 *            compensation
	 * @param assign {@link Assign} that initialize all isScopeCompleted
	 *            Variables to <code>XPATH_BOOLEAN_FALSE</code>
	 * @param mapperATn contains a {@link HashMap} that contains a mapping
	 *            between the current {@link Activity} and the associated
	 *            {@link TreeNode}.
	 * @return the alternative compensation {@link Flow}
	 */
	private static Flow createCompensationFlow(TreeNode root, Scope cpSurScope, Assign assign, HashMap<Activity, TreeNode> mapperATn) {
		Flow cpFlow = BPELFactory.eINSTANCE.createFlow();
		cpFlow.setLinks(BPELFactory.eINSTANCE.createLinks());
		cpFlow.setName(createSurroundingFlowName());
		
		Scope rootScope = BPELFactory.eINSTANCE.createScope();
		rootScope.setActivity(BPELFactory.eINSTANCE.createEmpty());
		root.setCpActivity(rootScope);
		// first add all sources and targets
		for (TreeNode tn : mapperATn.values()) {
			//printFlow(cpFlow);
			if (tn.getCurrentNodeValue() instanceof Flow) {
				Flow flow = (Flow) tn.getCpActivity();
				// add all children to flow
				for (TreeNode child : tn.getChilds()) {
					addChildrenToFlow(flow, child);
				}
			} else if (tn.getCurrentNodeValue() instanceof Scope) {
				createCompensationScope(cpFlow, cpSurScope, assign, tn);
			}
			// only add it if its not already added in a Flow, if not the old
			// pointer to the Flow will be removed
			if (!tn.isAlreadyAdded()) {
				cpFlow.getActivities().add(tn.getCpActivity());
			}
		}
		return cpFlow;
	}
	
	/**
	 * Adds all children to the compensation {@link Flow}.
	 * 
	 * @param flow a new compensation {@link Flow}.
	 * @param child this child will be added to the new compensation
	 *            {@link Flow}.
	 */
	private static void addChildrenToFlow(Flow flow, TreeNode child) {
		flow.getActivities().add(child.getCpActivity());
		child.setAlreadyAdded(true);
		if (child.getSuccessor() != null) {
			addChildrenToFlow(flow, child.getSuccessor());
		}
	}
	
	/**
	 * Prints the given {@link Flow}.
	 * 
	 * @param flow that should be printed.
	 */
	@SuppressWarnings("unused")
	private static void printFlow(Flow flow) {
		String tmp = "";
		for (Activity act : flow.getActivities()) {
			tmp += act.getName() + ";";
		}
		System.err.println(tmp);
	}
	
	/**
	 * Creates and adds a compensation Scope to the alternative compensation
	 * tree.
	 * 
	 * @param rootFlow to this {@link Flow} the new compensation {@link Scope}
	 *            will be added
	 * @param cpSurScope {@link Scope} that contains the compensation
	 *            {@link Flow} and the {@link Scope} which needs an alternative
	 *            compensation
	 * @param assign {@link Assign} that initialize all isScopeCompleted
	 *            Variables to <code>XPATH_BOOLEAN_FALSE</code>
	 * @param cpTN {@link TreeNode} which contains information for compensation
	 */
	private static void createCompensationScope(Flow rootFlow, Scope cpSurScope, Assign assign, TreeNode cpTN) {
		// create variable
		Variable isCompleted = createIsCompletedVariale();
		// add and init variable
		cpSurScope.getVariables().getChildren().add(isCompleted);
		assign.getCopy().add(createCopyForVariableIsCompleted(isCompleted, false));
		
		// scope that should be used to create compensation scope
		Scope scope = (Scope) cpTN.getCurrentNodeValue();
		
		// create compensation Scope
		Scope result = (Scope) cpTN.getCpActivity();
		result.setName(createCompensationScopeName(scope.getName()));
		// if default CompensationHandler is installed-> set Empty Activity
		Activity act = null;
		if (scope.getCompensationHandler() == null) {
			act = BPELFactory.eINSTANCE.createEmpty();
		} else {
			act = scope.getCompensationHandler().getActivity();
		}
		result.setActivity(createIfForIsCompleted(isCompleted, act));
		// set compensation if CompensationHandler contains one
		// TODO check if compensate in fh th ch and replace them
		// set target(incoming) and source(outgoing)
		if (cpTN.getSuccessor() != null) {
			setLinkFromChildToParent(rootFlow, cpTN.getSuccessor(), cpTN);
		}
		if (!cpTN.getLinkSuccessorSet().isEmpty()) {
			for (TreeNode tn : cpTN.getLinkSuccessorSet()) {
				setLinkFromChildToParent(rootFlow, tn, cpTN);
			}
		}
		// add assign to currentScope
		Sequence sequence = BPELFactory.eINSTANCE.createSequence();
		sequence.getActivities().add(createAssignForVariableIsCompleted(isCompleted, true));
		sequence.getActivities().add(scope.getActivity());
		scope.setActivity(sequence);
	}
	
	/**
	 * Creates a {@link Link} and adds it to the given {@link Flow}. The
	 * {@link Source} (outgoing) {@link Link} will be the given
	 * <code>child</code> and the {@link Target} (incoming) {@link Link} will be
	 * the given <code>parent</code>. The {@link Scope}s that will be connected
	 * could be found in the {@link TreeNode}s.
	 * 
	 * @param rootFlow to this {@link Flow} the new {@link Link} will be added
	 * @param child {@link TreeNode} which will be the {@link Source}
	 * @param parent {@link TreeNode} which will be the {@link Target}
	 */
	private static void setLinkFromChildToParent(Flow rootFlow, TreeNode child, TreeNode parent) {
		Activity childActivity = child.getCpActivity();
		Link link = BPELFactory.eINSTANCE.createLink();
		link.setName(createLinkName());
		rootFlow.getLinks().getChildren().add(link);
		// source
		if (childActivity.getSources() == null) {
			childActivity.setSources(BPELFactory.eINSTANCE.createSources());
		}
		Source source = BPELFactory.eINSTANCE.createSource();
		source.setLink(link);
		childActivity.getSources().getChildren().add(source);
		// target
		Activity parentActivity = parent.getCpActivity();
		if (parentActivity.getTargets() == null) {
			parentActivity.setTargets(BPELFactory.eINSTANCE.createTargets());
		}
		Target target = BPELFactory.eINSTANCE.createTarget();
		target.setLink(link);
		parentActivity.getTargets().getChildren().add(target);
	}
	
	/**
	 * Creates a name for the compensation {@link Scope}s.
	 * 
	 * @param scopeName
	 * @return
	 */
	private static String createCompensationScopeName(String scopeName) {
		return scopeName + "_" + FCTEUtil.getUUID();
	}
	
	/**
	 * Creates a name for the compensation {@link Link}s.
	 * 
	 * @return
	 */
	private static String createLinkName() {
		return PREFIX_NAME_CH_LINK + FCTEUtil.getUUID();
	}
	
	/**
	 * Creates a name for the surrounding {@link Scope}.
	 * 
	 * @return
	 */
	private static String createSurroundingScopeName() {
		return PREFIX_NAME_CH_SUR_SCOPE + FCTEUtil.getUUID();
	}
	
	/**
	 * Creates a name for the surrounding {@link Flow}.
	 * 
	 * @return
	 */
	private static String createSurroundingFlowName() {
		return PREFIX_NAME_CH_SUR_FLOW + FCTEUtil.getUUID();
	}
	
	/**
	 * Builds up the compensation Tree. It processes the logic for {@link Flow}
	 * and {@link Scope} {@link Object}s.
	 * 
	 * @param root current {@link TreeNode} which should be processed
	 * @param mapperATn contains a map between {@link TreeNode}s and
	 *            {@link Activity}s to get an easy access to the
	 *            {@link TreeNode}s
	 */
	private static void createCompensationNode(TreeNode root, HashMap<Activity, TreeNode> mapperATn) {
		TreeIterator<?> iterator = root.getCurrentNodeValue().eAllContents();
		Object oElement = null;
		TreeNode tmpActivity = null;
		TreeNode iterateActivity = root;
		// trace the given tree and find all Flows and Scopes
		while (iterator.hasNext()) {
			oElement = iterator.next();
			if (isCurrentlyRestricted(oElement)) {
				// TODO CHECK compensation needed, currently deactivated
				iterator.prune();
				continue;
			} else if (oElement instanceof Scope) {
				tmpActivity = new TreeNode((Activity) oElement);
				// tmpActivity.addParent(iterateActivity);
				mapperATn.put((Activity) oElement, tmpActivity);
				createCompensationNode(tmpActivity, mapperATn);
				iterateActivity.setSuccessor(tmpActivity);
				iterateActivity = tmpActivity.getLeaf();
				iterator.prune();
				continue;
			} else if (oElement instanceof Flow) {
				tmpActivity = new TreeNode((Activity) oElement);
				// tmpActivity.addParent(iterateActivity);
				mapperATn.put((Activity) oElement, tmpActivity);
				compensateFlow(tmpActivity, mapperATn);
				iterateActivity.setSuccessor(tmpActivity);
				iterateActivity = tmpActivity.getLeaf();
				iterator.prune();
				continue;
			}
			// trace deeper do not prune here
		}
	}
	
	/**
	 * Checks if the given object is currently not implemented and restricted.
	 * 
	 * @param oElement that will be checked
	 * @return
	 */
	private static boolean isCurrentlyRestricted(Object oElement) {
		return oElement instanceof FaultHandler || oElement instanceof TerminationHandler || oElement instanceof CompensationHandler || oElement instanceof EventHandler || oElement instanceof RepeatUntil || oElement instanceof ForEach || oElement instanceof While;
	}
	
	/**
	 * Builds up the compensation Tree. It processes the logic for {@link Flow}
	 * objects.
	 * 
	 * @param flowNode current {@link TreeNode} which contains a {@link Flow}
	 *            and should be processed
	 * @param mapperATn contains a map between {@link TreeNode}s and
	 *            {@link Activity}s to get an easy access to the
	 *            {@link TreeNode}s
	 */
	private static void compensateFlow(TreeNode flowNode, HashMap<Activity, TreeNode> mapperATn) {
		Flow flow = (Flow) flowNode.getCurrentNodeValue();
		TreeIterator<?> iterator = flowNode.getCurrentNodeValue().eAllContents();
		Object oElement = null;
		TreeNode tmpActivity = null;
		// trace the given tree and find all Flows and Scopes
		while (iterator.hasNext()) {
			oElement = iterator.next();
			if (isCurrentlyRestricted(oElement)) {
				// TODO CHECK compensation needed, currently deactivated
				iterator.prune();
				continue;
			} else if (oElement instanceof Scope) {
				tmpActivity = new TreeNode((Activity) oElement);
				mapperATn.put((Activity) oElement, tmpActivity);
				flowNode.addChild(tmpActivity);
				createCompensationNode(tmpActivity, mapperATn);
				iterator.prune();
				continue;
			} else if (oElement instanceof Flow) {
				tmpActivity = new TreeNode((Activity) oElement);
				mapperATn.put((Activity) oElement, tmpActivity);
				flowNode.addChild(tmpActivity);
				compensateFlow(flowNode, mapperATn);
				iterator.prune();
				continue;
			}
			// trace deeper do not prune here
		}
		// now check links between scopes and flows
		if (flow.getLinks() != null && !flow.getLinks().getChildren().isEmpty()) {
			createFlowTree(flowNode, mapperATn);
		}
		// else {
		// // no links, parallel processing, no order needed, add all in
		// // sequence
		// TreeNode tmpNode = flowNode;
		// TreeNode flowSuccessor = flowNode.getSuccessor();
		// for (TreeNode tn : flowNode.getChilds()) {
		// tmpNode.setSuccessor(tn);
		// tmpNode = tn.getLeaf();
		// }
		// tmpNode.setSuccessor(flowSuccessor);
		// }
	}
	
	/**
	 * Checks all {@link Link}s in the {@link Flow} and connects the
	 * {@link TreeNode}s together.
	 * 
	 * @param flowNode {@link Flow} which {@link Link}s will be checked.
	 * @param mapperATn contains a map between {@link TreeNode}s and
	 *            {@link Activity}s to get an easy access to the
	 *            {@link TreeNode}s
	 */
	private static void createFlowTree(TreeNode flowNode, HashMap<Activity, TreeNode> mapperATn) {
		Flow flow = (Flow) flowNode.getCurrentNodeValue();
		// find Scope with included Activity that contains the Link -> and
		// connect the leaf of the Scope (with outgoing link) with target Scope
		// if a Scope is not connect with anyone, it runs in parallel
		
		// Activity with source(outgoing) link
		Activity actSource = null;
		Activity actTarget = null;
		// surrounding scope or flow that contains the Activity
		Activity actScopeOrFlowSource = null;
		Activity actScopeOrFlowTarget = null;
		TreeNode targetTN = null;
		TreeNode sourceTN = null;
		for (Link link : flow.getLinks().getChildren()) {
			// find Source Activity
			actSource = link.getSources().get(0).getActivity();
			actScopeOrFlowSource = getScopeOrSurroundingScope(actSource);
			// Find Target Activity
			actTarget = link.getTargets().get(0).getActivity();
			actScopeOrFlowTarget = getScopeOrSurroundingScope(actTarget);
			// get TreeNodes
			sourceTN = mapperATn.get(actScopeOrFlowSource);
			targetTN = mapperATn.get(actScopeOrFlowTarget);
			// set successor from source to Target
			// CHECK do not use leaf, special list for links needed
			// sourceTN.getLeaf().setSuccessor(targetTN);
			// TODO CHECK could it be that getScopeOrSurroundingScope are equal?
			// do not set the leaf, if another TreeNode must point to the
			// rootScope, we can not overwrite the leaf successor
			sourceTN.getLeaf().addLinkSuccessor(targetTN);
			// sourceTN.addLinkSuccessor(targetTN);
			// CHECK add parent to use Flow in compensation
			// targetTN.addParent(sourceTN);
		}
		// here are parallel Scopes or Flows in a flow, so we have to note this
		// if we build up the compensation tree
	}
	
	/**
	 * Moves up the given {@link Activity} and finds the enclosing {@link Scope}
	 * or {@link Flow}.
	 * 
	 * @param act start {@link Activity} to climb up until a {@link Scope} or
	 *            {@link Flow} is found
	 * @return
	 */
	private static Activity getScopeOrSurroundingScope(Activity act) {
		Activity result = null;
		if (act instanceof Scope) {
			result = (Scope) act;
		} else if (act instanceof Flow) {
			result = (Flow) act;
		} else {
			EObject container = null;
			container = act.eContainer();
			while (container != null && !(container instanceof Scope) && !(container instanceof Flow)) {
				container = container.eContainer();
			}
			result = (Activity) container;
		}
		return result;
	}
	
	/**
	 * Creates a name for a isCompleted-{@link Variable}.
	 * 
	 * @return
	 */
	private static String getIsCompletedVariableName() {
		return PREFIX_NAME_CH_ISCOMPLETED + FCTEUtil.getUUID();
	}
	
	/**
	 * Creates a {@link Variable} which signals if a {@link Scope} is completed
	 * or not.
	 * 
	 * @return
	 */
	private static Variable createIsCompletedVariale() {
		Variable isCompleted = BPELFactory.eINSTANCE.createVariable();
		isCompleted.setName(getIsCompletedVariableName());
		
		// XSD boolean type
		XSDTypeDefinition type = XSDFactory.eINSTANCE.createXSDSimpleTypeDefinition();
		type.setName("boolean");
		type.setTargetNamespace(XSDConstants.SCHEMA_FOR_SCHEMA_URI_2001);
		
		isCompleted.setType(type);
		
		// initialize variable
		Expression exp = BPELFactory.eINSTANCE.createExpression();
		exp.setExpressionLanguage(BPELConstants.XMLNS_XPATH_EXPRESSION_LANGUAGE);
		exp.setBody(XPATH_BOOLEAN_FALSE);
		From from = BPELFactory.eINSTANCE.createFrom();
		from.setExpression(exp);
		
		isCompleted.setFrom(from);
		return isCompleted;
	}
	
	/**
	 * Creates a {@link Assign} that contains a {@link Copy} which sets the
	 * {@link Variable} <code>isCompleted = true</code> or
	 * <code>isCompleted = false</code>
	 * 
	 * @param isCompleted {@link Variable} which needs the {@link Assign}
	 * @param setBoolean signals if {@link Variable} value should be
	 *            <code>XPATH_BOOLEAN_TRUE</code> or
	 *            <code>XPATH_BOOLEAN_FALSE</code>
	 * @return
	 */
	private static Assign createAssignForVariableIsCompleted(Variable isCompleted, boolean setBoolean) {
		// assign
		Assign assign = BPELFactory.eINSTANCE.createAssign();
		assign.getCopy().add(createCopyForVariableIsCompleted(isCompleted, setBoolean));
		
		return assign;
	}
	
	/**
	 * Creates a {@link Copy} which sets the {@link Variable}
	 * <code>isCompleted = true</code> or <code>isCompleted = false</code>
	 * 
	 * @param isCompleted {@link Variable} which needs the {@link Assign}
	 * @param setBoolean signals if {@link Variable} value should be
	 *            <code>XPATH_BOOLEAN_TRUE</code> or
	 *            <code>XPATH_BOOLEAN_FALSE</code>
	 * @return
	 */
	private static Copy createCopyForVariableIsCompleted(Variable isCompleted, boolean setBoolean) {
		// from
		Expression exp = BPELFactory.eINSTANCE.createExpression();
		exp.setExpressionLanguage(BPELConstants.XMLNS_XPATH_EXPRESSION_LANGUAGE);
		if (setBoolean) {
			exp.setBody(XPATH_BOOLEAN_TRUE);
		} else {
			exp.setBody(XPATH_BOOLEAN_FALSE);
		}
		From from = BPELFactory.eINSTANCE.createFrom();
		from.setExpression(exp);
		
		// to
		To to = BPELFactory.eINSTANCE.createTo();
		to.setVariable(isCompleted);
		
		// copy
		Copy copy = BPELFactory.eINSTANCE.createCopy();
		copy.setFrom(from);
		copy.setTo(to);
		
		return copy;
	}
	
	/**
	 * Creates an {@link If} and checks if the given {@link Variable} is
	 * <code>true</code>
	 * 
	 * @param isCompleted {@link Variable} that should be checked
	 * @param act this {@link Activity} will be set if the condition is true
	 * @return
	 */
	private static If createIfForIsCompleted(Variable isCompleted, Activity act) {
		Condition cond = BPELFactory.eINSTANCE.createCondition();
		cond.setExpressionLanguage(BPELConstants.XMLNS_XPATH_EXPRESSION_LANGUAGE);
		cond.setBody("$" + isCompleted.getName());
		
		If bpelIf = BPELFactory.eINSTANCE.createIf();
		bpelIf.setCondition(cond);
		bpelIf.setActivity(act);
		return bpelIf;
	}
}
