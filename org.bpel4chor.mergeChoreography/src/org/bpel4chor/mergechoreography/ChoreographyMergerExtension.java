package org.bpel4chor.mergechoreography;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.PBDFragmentDuplicator;
import org.bpel4chor.mergechoreography.util.PBDFragmentDuplicatorExtension;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.model.topology.impl.Participant;
import org.bpel4chor.model.topology.impl.ParticipantType;
import org.bpel4chor.utils.BPEL4ChorUtil;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.CatchAll;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.FaultHandler;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.OnEvent;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.PartnerActivity;
import org.eclipse.bpel.model.PartnerLink;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Sources;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Targets;
import org.eclipse.bpel.model.partnerlinktype.PartnerLinkType;
import org.eclipse.bpel.model.partnerlinktype.Role;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.wst.wsdl.Definition;
import org.eclipse.wst.wsdl.Operation;
import org.eclipse.wst.wsdl.PortType;

import de.uni_stuttgart.iaas.bpel.model.utilities.FragmentDuplicator;
import de.uni_stuttgart.iaas.bpel.model.utilities.MyWSDLUtil;

public class ChoreographyMergerExtension {
	
	private ChoreographyPackage choreographyPackage;
	protected Logger log;
	
	
	public ChoreographyMergerExtension(ChoreographyPackage choreographyPackage, Logger log) {
		this.choreographyPackage = choreographyPackage;
		this.log = log;
	}
	
	
	/**
	 * countInvokesNOTInForeach - Variable for holding number of invokes NOT
	 * inside forEach activities
	 */
	public static int countInvokesNOTInForeach = 0;
	/**
	 * invokeCounts - Variable for holding number of invokes inside all
	 * forEachsactivities
	 */
	public static Map<String, Integer> invokeCountsInsideForEachs = new HashMap<String, Integer>();
	/**
	 * this variable is used for checking if new Partner Links were created for
	 * NMML
	 */
	public static boolean createdNewPartnerLinksForNMML = false;
	/**
	 * List containing all links residing in merged process's flow activity
	 */
	public static ArrayList<Link> linksList = new ArrayList<Link>();
	
	/**
	 * Contains all the containers/scopes generated in merged process
	 */
	public static ArrayList<Scope> scopeList = new ArrayList<Scope>();
	
	// wsuIDs of inokve activities not iside forEach
	public ArrayList<String> wsuIdOfInvokesnotInsideForEach = new ArrayList<String>();
	
	protected Map<Activity, Activity> copyActOrgActsPairs = new HashMap<>();
	
	
	/**
	 * This function performs loop fragmentation on created containers in merged
	 * process
	 */
	public void performLoopFragmentation() {
		try {
			// Read and store all the links residing in top flow activity of
			// merged process
			this.intializeLinksFromMergedProcess();
			// Read and store all scopes/containers created in merged process
			this.initializeScopeActivities();
			
			//
			/**
			 * This hash map of forEach loops contains all the forEach
			 * containers in scope
			 * 
			 * HashMap<scopeName,HasMap<containerId,forEachContainer>
			 */
			HashMap<String, HashMap<Integer, ForEach>> hmFEs = new HashMap<String, HashMap<Integer, ForEach>>();
			
			boolean flagLastFE = false;
			// for (int i = scopeList.size() - 1; i > 0; i--) {
			for (int i = 0; i < ChoreographyMergerExtension.scopeList.size(); i++) {
				flagLastFE = false;
				Scope scope = ChoreographyMergerExtension.scopeList.get(i);
				
				// This linkedList keeps the list of all activities till the
				// first communication activity inside FE fragment. lstObjs's
				// last element will be the communication element
				LinkedList<EObject> lstObjs = new LinkedList<EObject>();
				
				// Initially, before loop fragmentation, scope will contain only
				// one forEach fragment (as we don't support nested forEach)
				ForEach forEach = this.findForEachActivityInsideDynamicMIP(scope);
				
				// TODO Quick Hack: Avoids FE-Fragmentation in case no FE exists
				if (forEach == null) {
					continue;
				}
				
				ArrayList<String> prevCommActivityNameList = new ArrayList<String>();
				
				// this variable keeps track of the last processed/found
				// communication activity inside FE
				// with the help of this variable FE is searched for next
				// communication activity coming after after previously
				// processed communicating activity
				String prevCommActName = "";
				do {
					// if lstObjs contains some activities - the container is
					// not empty container, then its last element will be
					// communication activity
					if ((lstObjs != null) && (lstObjs.size() > 0)) {
						prevCommActName = ((Activity) lstObjs.getLast()).getName();
					}
					lstObjs.clear();
					
					// In the last iteration lstObjs will contain all the
					// activities comming after the last communicating activity
					// in FE
					if ((lstObjs != null) && (lstObjs.size() > 0) && !this.isCommActivity(lstObjs.getLast())) {
						
						flagLastFE = true;
					}
					
					// After finding the delimeter - the communicating activity,
					// divide the FE into 2 FE:
					// 1) FE containing all the activities before communicating
					// activities
					// 2) FE containign communicating activity
					this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, forEach, prevCommActivityNameList);
					
					// add processed communicating activity name to the
					// processed communicating activity names list
					if (lstObjs.getLast() instanceof Assign) {
						prevCommActivityNameList.add(((Assign) lstObjs.getLast()).getName());
					} else if (lstObjs.getLast() instanceof Empty) {
						prevCommActivityNameList.add(((Empty) lstObjs.getLast()).getName());
					}
					
					// First element of FE is scope activity
					Scope scopeOrigFE = (Scope) forEach.getActivity();
					
					// Initialize new FE which will contain all the activities
					// preceding
					// communicating activity in original FE-in the FE before
					// loop fragmentation
					ForEach newPreForEach = BPELFactory.eINSTANCE.createForEach();
					newPreForEach.setName(forEach.getName());
					Scope newPreScope = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyScopeActivityWOChildren(scopeOrigFE, flagLastFE);
					newPreForEach.setActivity(newPreScope);
					
					// Initialize new FE for holding communicating activity from
					// original FE
					ForEach newCommForEach = BPELFactory.eINSTANCE.createForEach();
					newCommForEach.setName(forEach.getName());
					Scope newCommScope = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyScopeActivityWOChildren(scopeOrigFE, flagLastFE);
					newCommForEach.setActivity(newCommScope);
					
					// Temporary object which helps for knowing when pre FE
					// exists, when to finish fragmenting FE in each iteration
					TerminateFlag terminate = new TerminateFlag();
					terminate.finished = false;
					terminate.preExists = false;
					
					// Create new preceding and communicating FE fragments
					this.createNewPreAndCommFEFragments(scopeOrigFE.getActivity(), newPreScope, newCommScope, lstObjs.getLast(), prevCommActName, terminate);
					
					// Iterator<Activity> actCopiesIter =
					// this.copyActOrgActsPairs.keySet().iterator();
					//
					// while (actCopiesIter.hasNext()) {
					// Activity activityCopy = actCopiesIter.next();
					// Activity orgActivity =
					// this.copyActOrgActsPairs.get(activityCopy);
					//
					// activityCopy.setSources(orgActivity.getSources());
					// activityCopy.setTargets(orgActivity.getTargets());
					// }
					
					// If the last element of lstObjs is communicating activity,
					if (this.isCommActivity(lstObjs.getLast())) {
						// If preceding FE exists
						if (terminate.preExists) {
							HashMap<Integer, ForEach> hm = null;
							if (hmFEs.containsKey(scope.getName())) {
								hm = hmFEs.get(scope.getName());
								
								// Set the name of pre FE to the original FE
								// name+"_"+ next integer for creating FE name
								// unique
								newPreForEach.setName(newPreForEach.getName() + "_" + (hm.size() + 1));
								hm.put(hm.size() + 1, newPreForEach);
								newCommForEach.setName(newCommForEach.getName() + "_" + (hm.size() + 1));
								hm.put(hm.size() + 1, newCommForEach);
							} else {
								// Set the name of pre FE to the original FE
								// name+"_1"
								hm = new HashMap<Integer, ForEach>();
								hm.put(1, newPreForEach);
								newPreForEach.setName(newPreForEach.getName() + "_1");
								hm.put(2, newCommForEach);
								hmFEs.put(scope.getName(), hm);
								newCommForEach.setName(newCommForEach.getName() + "_2");
							}
							
						} else {
							// Pre FE does not exist, so only create
							// communicating FE
							HashMap<Integer, ForEach> hm = null;
							if (hmFEs.containsKey(scope.getName())) {
								hm = hmFEs.get(scope.getName());
								newCommForEach.setName(newCommForEach.getName() + "_" + (hm.size() + 1));
								hm.put(hm.size() + 1, newCommForEach);
							} else {
								hm = new HashMap<Integer, ForEach>();
								newCommForEach.setName(newCommForEach.getName() + "_1");
								hm.put(1, newCommForEach);
								hmFEs.put(scope.getName(), hm);
							}
							
						}
					} else {
						// The last element of listObjs is not communicating
						// activity
						// so this is the last FE need to be creaetd - which
						// will contain all the activities after the last
						// communicating activity
						HashMap<Integer, ForEach> hm = null;
						if (hmFEs.containsKey(scope.getName())) {
							hm = hmFEs.get(scope.getName());
							newCommForEach.setName(newCommForEach.getName() + "_" + (hm.size() + 1));
							hm.put(hm.size() + 1, newCommForEach);
						} else {
							hm = new HashMap<Integer, ForEach>();
							newCommForEach.setName(newCommForEach.getName() + "_2");
							hm.put(2, newCommForEach);
							hmFEs.put(scope.getName(), hm);
						}
					}
				} while (this.isCommActivity(lstObjs.getLast()));
				
				// Delete Links from FE fragment if FE does contain neither the
				// source nor the target of link
				this.deleteExtraLinksAfterFragmentation(scope.getName(), hmFEs);
				
				// Update the changes in the scope container
				scope = this.modifyScopeWithNewFEs(scope, hmFEs);
				ChoreographyMergerExtension.scopeList.set(i, scope);
				scope = ChoreographyMergerExtension.scopeList.get(i);
				
				// Link status propagation - find broken links, and apply link
				// status propagation technique
				this.handleBrokenLinks(scope, hmFEs);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param scope - scope/container in the merged process
	 * @param hmFEs - hash map of all the FEs after fragmentation in the merged
	 *            process {scopeName:{1:<forEach>}}
	 * 
	 *            This function finds the links which are broken, and then
	 *            applies link Status propagation technique to the FE fragments
	 *            containing broken links.
	 * 
	 *            Broken link is the link which has source and target activities
	 *            in two different FE fragments after loop fragmentation
	 * 
	 *            Link Status Propagation is creating Scope+AssignTrue,
	 *            FaultHandler+AssignFalse, Control link from (Scope+AssignTrue)
	 *            to (FaultHandler+AssignFalse) in source FE. In target FE Empty
	 *            activity and Control link from (Empty) to (Target) activity of
	 *            broken link.
	 */
	public void handleBrokenLinks(Scope scope, HashMap<String, HashMap<Integer, ForEach>> hmFEs) {
		
		// Get and store all the links which are broken
		HashMap<String, HashMap<String, HashMap<String, Activity>>> mapOfLinks = this.initializeLinkDetailsMap(scope.getName(), hmFEs);
		
		Iterator<String> scopeIt = mapOfLinks.keySet().iterator();
		while (scopeIt.hasNext()) {
			HashMap<String, HashMap<String, Activity>> linkDetailsMap = mapOfLinks.get(scopeIt.next());
			Iterator<String> linkIt = linkDetailsMap.keySet().iterator();
			while (linkIt.hasNext()) {
				String linkName = linkIt.next();
				HashMap<String, Activity> linkDet = linkDetailsMap.get(linkName);
				Activity source = this.findActivityByName(scope, linkDet.get("Source").getName());
				Activity target = this.findActivityByName(scope, linkDet.get("Target").getName());
				
				// Apply Loop Status Propagation echnique to source FE
				this.createSrcPartOfBrokenLink(source);
				
				// Apply Loop Status Propagation echnique to target FE
				this.createTrgPartOfBrokenLink(target);
				
			}
			
		}
		
	}
	
	/**
	 * 
	 * @param source - this is the soruce activity of broken link
	 * 
	 *            This function applies Link status propagation to the source FE
	 *            fragment. Source fragment is the FE fragment which contains
	 *            source of the broken link.
	 * 
	 *            New Scope is created. Scope's supressJoinFailure is set to
	 *            false.New AssignTrue is created added to that scope. New
	 *            FaultHandler is created. new AssignFalse is created and added
	 *            to that Fault Handler. Then this Fault Handler is added to
	 *            Scope. Then this is Scope is added to source FE fragment. New
	 *            Control link is created from (Source) of broken link to
	 *            (Scope) in source FE.
	 */
	public void createSrcPartOfBrokenLink(Activity source) {
		Scope scope = BPELFactory.eINSTANCE.createScope();
		scope.setName("scopeForBrokenLink");
		scope.setSuppressJoinFailure(false);
		Assign assignTrue = BPELFactory.eINSTANCE.createAssign();
		assignTrue.setName("assignTrue");
		/**
		 * TODO Create variable of MAP type to hold link status value, and set
		 * its value to true
		 */
		scope.setActivity(assignTrue);
		FaultHandler fh = BPELFactory.eINSTANCE.createFaultHandler();
		Assign assignFalse = BPELFactory.eINSTANCE.createAssign();
		assignFalse.setName("assignFalse");
		CatchAll catchAll = BPELFactory.eINSTANCE.createCatchAll();
		catchAll.setActivity(assignFalse);
		fh.setCatchAll(catchAll);
		scope.setFaultHandlers(fh);
		// assignFalse
		/**
		 * TODO Create variable of MAP type to hold link status value, and set
		 * its value to false
		 */
		Link linkFromSourcetoNewScope = BPELFactory.eINSTANCE.createLink();
		linkFromSourcetoNewScope.setName("linkSourceToNewScope");
		Source sourceOfLink = BPELFactory.eINSTANCE.createSource();
		sourceOfLink.setActivity(source);
		sourceOfLink.setLink(linkFromSourcetoNewScope);
		linkFromSourcetoNewScope.getSources().add(sourceOfLink);
		Target targetOfLink = BPELFactory.eINSTANCE.createTarget();
		targetOfLink.setActivity(assignTrue);
		linkFromSourcetoNewScope.getTargets().add(targetOfLink);
		Flow flow = ((Flow) source.eContainer());
		flow.getLinks().getChildren().add(linkFromSourcetoNewScope);
		flow.getActivities().add(scope);
	}
	
	/**
	 * 
	 * @param target - is the target activity of the broken link
	 * 
	 *            This function applies link status propagation to the target FE
	 *            fragment. New Empty activity is created. New Control link is
	 *            created from (Empty) activity to (Target) activity. Then this
	 *            empty activity and link is added to target FE.
	 */
	public void createTrgPartOfBrokenLink(Activity target) {
		Empty empty = BPELFactory.eINSTANCE.createEmpty();
		empty.setName("BrokenLinkEmpty");
		Link linkFromEmptytoTrg = BPELFactory.eINSTANCE.createLink();
		linkFromEmptytoTrg.setName("linkEmptyToTarget");
		Source sourceOfLink = BPELFactory.eINSTANCE.createSource();
		sourceOfLink.setActivity(empty);
		sourceOfLink.setLink(linkFromEmptytoTrg);
		linkFromEmptytoTrg.getSources().add(sourceOfLink);
		Target targetOfLink = BPELFactory.eINSTANCE.createTarget();
		targetOfLink.setActivity(target);
		linkFromEmptytoTrg.getTargets().add(targetOfLink);
		((Flow) target.eContainer()).getActivities().add(empty);
		((Flow) target.eContainer()).getLinks().getChildren().add(linkFromEmptytoTrg);
		/**
		 * TODO Implement function for reading link status value from map
		 * variable
		 */
		
	}
	
	/**
	 * 
	 * @param scopeName - the scope/container in merged process
	 * @param hmFEs - hash map which contains FE fragments of each scope
	 * @return the hash map which is hash map storing data scopeName -> linkName
	 *         -> linkDetails.
	 * 
	 *         This hash map will only contain details of broken links.
	 * 
	 *         link details are source the link, target of the link,
	 *         LinkSourceFE which is the source FE, LinkTargetFE which is the
	 *         target FE
	 */
	public HashMap<String, HashMap<String, HashMap<String, Activity>>> initializeLinkDetailsMap(String scopeName, HashMap<String, HashMap<Integer, ForEach>> hmFEs) {
		HashMap<Integer, ForEach> newFEFragments = hmFEs.get(scopeName);
		
		Iterator<ForEach> it = newFEFragments.values().iterator();
		// scopeName -> linkName -> linkDetails
		HashMap<String, HashMap<String, HashMap<String, Activity>>> mapOfLinks = new HashMap<String, HashMap<String, HashMap<String, Activity>>>();
		HashMap<String, HashMap<String, Activity>> linkDet = new HashMap<String, HashMap<String, Activity>>();
		boolean flagBrokenLink = false;
		while (it.hasNext()) {
			ForEach fe = it.next();
			Flow flow = this.findFlowActivityInsideFE(fe);
			EList<Link> flowLinks = flow.getLinks().getChildren();
			for (Link l : flowLinks) {
				Activity src = this.getSourceActNameFromFlow(flow, l.getName());
				Activity trg = this.getTargetActNameFromFlow(flow, l.getName());
				HashMap<String, Activity> hm = null;
				if ((mapOfLinks.get(scopeName) != null) && (mapOfLinks.get(scopeName).get(l.getName()) != null)) {
					hm = mapOfLinks.get(scopeName).get(l.getName());
				} else {
					hm = new HashMap<String, Activity>();
				}
				
				if (src != null) {
					hm.put("Source", src);
					hm.put("LinkSourceFE", fe);
				}
				if (trg != null) {
					hm.put("Target", trg);
					hm.put("LinkTargetFE", fe);
				}
				if ((src == null) || (trg == null)) {
					flagBrokenLink = true;
				}
				if (flagBrokenLink) {
					linkDet.put(l.getName(), hm);
					mapOfLinks.put(scopeName, linkDet);
					flagBrokenLink = false;
				}
				
			}
		}
		return mapOfLinks;
	}
	
	/**
	 * 
	 * @param flow
	 * @param linkName
	 * @return the source activity of the link which is residing inside the
	 *         given flow
	 */
	public Activity getSourceActNameFromFlow(Flow flow, String linkName) {
		for (Activity act : flow.getActivities()) {
			if (act.getSources() != null) {
				if (act.getSources().getChildren().get(0).getLink().getName().equals(linkName)) {
					return act.getSources().getChildren().get(0).getActivity();
				}
			}
			
		}
		return null;
		
	}
	
	/**
	 * 
	 * @param flow
	 * @param linkName
	 * @return the atrget activity of the link which is residing inside the
	 *         given flow
	 */
	public Activity getTargetActNameFromFlow(Flow flow, String linkName) {
		for (Activity act : flow.getActivities()) {
			if (act.getTargets() != null) {
				if (act.getTargets().getChildren().get(0).getLink().getName().equals(linkName)) {
					return act.getTargets().getChildren().get(0).getActivity();
				}
			}
			
		}
		return null;
		
	}
	
	/**
	 * 
	 * @param scopeName - scope/container in merged process
	 * @param hmFEs - hash map containing FE fragments per scope
	 * 
	 *            This function deletes the links from the FE fragments, if FE
	 *            fragment contains neither the source nor the target of the
	 *            link
	 */
	public void deleteExtraLinksAfterFragmentation(String scopeName, HashMap<String, HashMap<Integer, ForEach>> hmFEs) {
		HashMap<Integer, ForEach> newFEFragments = hmFEs.get(scopeName);
		Iterator<ForEach> it = newFEFragments.values().iterator();
		while (it.hasNext()) {
			ForEach fe = it.next();
			Flow flow = this.findFlowActivityInsideFE(fe);
			EList<Link> flowLinks = flow.getLinks().getChildren();
			ArrayList<String> allLinkNamesInFlow = new ArrayList<String>();
			for (Link l : flowLinks) {
				allLinkNamesInFlow.add(l.getName());
			}
			ArrayList<String> linksofFlowActivities = new ArrayList<String>();
			for (EObject obj : flow.eContents()) {
				if (obj instanceof Activity) {
					Activity act = (Activity) obj;
					
					if ((act.getSources() != null) && (act.getSources().getChildren() != null)) {
						for (Source s : act.getSources().getChildren()) {
							linksofFlowActivities.add(s.getLink().getName());
						}
					}
					if ((act.getTargets() != null) && (act.getTargets().getChildren() != null)) {
						for (Target t : act.getTargets().getChildren()) {
							linksofFlowActivities.add(t.getLink().getName());
						}
					}
				}
			}
			for (int i = 0; i < allLinkNamesInFlow.size(); i++) {
				if (!linksofFlowActivities.contains(allLinkNamesInFlow.get(i))) {
					flowLinks.remove(i);
					allLinkNamesInFlow.remove(i);
					i = -1;
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @param oldScope - scope/container in merged process before loop
	 *            fragmentation
	 * @param hmFEs - hash map of FE fragments per scope
	 * @return the updated Scope which contains FE fragmetns after loop
	 *         fragmentation technique is applied
	 * 
	 *         This function updates the scope with the new FE fragments after
	 *         loop fragmentation has been applied.
	 */
	public Scope modifyScopeWithNewFEs(Scope oldScope, HashMap<String, HashMap<Integer, ForEach>> hmFEs) {
		Scope newScope = oldScope.clone(); // oldscope.clone(), or use the EMF
											// copy
		// method;
		ForEach fe = this.findForEachActivityInsideDynamicMIP(newScope);
		
		if (fe.eContainer() instanceof Sequence) {
			Sequence parent = (Sequence) fe.eContainer();
			parent.getActivities().remove(fe);
			HashMap<Integer, ForEach> newFEFragments = hmFEs.get(newScope.getName());
			Iterator<ForEach> it = newFEFragments.values().iterator();
			while (it.hasNext()) {
				ForEach f = it.next();
				parent.getActivities().add(f);
			}
			
		} else if (fe.eContainer() instanceof Flow) {
			Flow parent = (Flow) fe.eContainer();
			parent.getActivities().remove(fe);
			HashMap<Integer, ForEach> newFEFragments = hmFEs.get(newScope.getName());
			Iterator<ForEach> it = newFEFragments.values().iterator();
			while (it.hasNext()) {
				ForEach f = it.next();
				parent.getActivities().add(f);
			}
		} else if (fe.eContainer() instanceof Scope) {
			
			Scope parent = (Scope) fe.eContainer();
			parent.setActivity(null);
			Flow newFlow = BPELFactory.eINSTANCE.createFlow();
			newFlow.setName("DynamicMIPFlow");
			HashMap<Integer, ForEach> newFEFragments = hmFEs.get(newScope.getName());
			Iterator<ForEach> it = newFEFragments.values().iterator();
			while (it.hasNext()) {
				ForEach f = it.next();
				newFlow.getActivities().add(f);
			}
			parent.setActivity(newFlow);
		}
		return newScope;
	}
	
	
	// commActName : linkName-Src/Trg
	// ArrayList<HashMap<String, String>> commActivitiesList = new
	// ArrayList<HashMap<String, String>>();
	
	/**
	 * 
	 * Temporary class which helps for processing, does not contain any
	 * important data.
	 * 
	 */
	class TerminateFlag {
		
		public boolean finished = false;
		public boolean preExists = false;
		public boolean startAddingActivities = false;
	}
	
	
	/**
	 * 
	 * @param firstActOfOrigFE first activity of original ForEach loop
	 * @param pre - main activity (can be Flow, Sequence, Flow) of FE which is
	 *            preceding communicating FE
	 * @param comm - main activity (can be Flow, Sequence, Flow) of
	 *            communicating FE
	 * @param currCommActObj - current communication activity used to fragment
	 *            original ForEach
	 * @param prevCommActName - the list of processed/used communication
	 *            activities for fragmentation
	 * @param terminate - the boolean flag which let's to know when to stop
	 *            fragmentation for each communication activity
	 * 
	 * 
	 *            This function creates 2 FE fragments from 1 original FE
	 *            fragment: preceding and communicating.
	 * 
	 *            This function keeps parent structure when applying loop
	 *            fragmentation.
	 */
	public void createNewPreAndCommFEFragments(EObject firstActOfOrigFE, EObject pre, EObject comm, EObject currCommActObj, String prevCommActName, TerminateFlag terminate) {
		
		/*
		 * Atomic ativities do just reside within one FE-Fragment hence they do
		 * not have to be duplicated as they do only reside within one
		 * FE-Fragment. Structured activities may have to be placed into several
		 * FE-Fragments, if their body contains a link violating activity, e.g.
		 * If has to be split in If1 and If2 where If2 is in FE1 and If2 is in
		 * FE2
		 */
		
		if (terminate.finished) {
			return;
		}
		Activity firstAct = (Activity) firstActOfOrigFE;
		// Check the first activity of original FE, and create corresponding
		// activity in preceding and communicating FEs.
		// If 1st activity of original FE is Flow, then make first activity of
		// preceding and communicating FEs also Flow activity
		if (firstAct instanceof Flow) {
			EList<Activity> children = ((Flow) firstAct).getActivities();
			Flow newFlowPre = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyFlowActivityWOChildren((Flow) firstAct);
			Flow newFlowComm = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyFlowActivityWOChildren((Flow) firstAct);
			
			if (pre instanceof Scope) {
				Scope preScope = (Scope) pre;
				preScope.setActivity(newFlowPre);
				Scope commScope = (Scope) comm;
				commScope.setActivity(newFlowComm);
			} else if (pre instanceof Flow) {
				Flow preFlow = (Flow) pre;
				preFlow.getActivities().add(newFlowPre);
				Flow commFlow = (Flow) comm;
				commFlow.getActivities().add(newFlowComm);
				
			} else if (pre instanceof Sequence) {
				Sequence preSequence = (Sequence) pre;
				preSequence.getActivities().add(newFlowPre);
				Sequence commSequence = (Sequence) comm;
				commSequence.getActivities().add(newFlowComm);
			} else if (pre instanceof If) {
				If preIf = (If) pre;
				preIf.setActivity(newFlowPre);
				If commIf = (If) comm;
				commIf.setActivity(newFlowComm);
			}
			
			List<Activity> childrenList = new CopyOnWriteArrayList<Activity>(children);
			
			Iterator<Activity> childreIter = childrenList.iterator();
			while (childreIter.hasNext()) {
				// for (EObject obj : children) {
				// if (!finished)
				EObject obj = childreIter.next();
				this.createNewPreAndCommFEFragments(obj, newFlowPre, newFlowComm, currCommActObj, prevCommActName, terminate);
			}
			
		}
		// If 1st activity of original FE is Sequence, then make first activity
		// of
		// preceding and communicating FEs also Sequence activity
		else if (firstAct instanceof Sequence) {
			EList<Activity> children = ((Sequence) firstAct).getActivities();
			Sequence newSequencePre = PBDFragmentDuplicatorExtension.copySequenceActivityWOChildren((Sequence) firstAct);// BPELFactory.eINSTANCE.createSequence();
			Sequence newSequenceComm = PBDFragmentDuplicatorExtension.copySequenceActivityWOChildren((Sequence) firstAct);// BPELFactory.eINSTANCE.createSequence();
			
			if (pre instanceof Scope) {
				Scope preScope = (Scope) pre;
				preScope.setActivity(newSequencePre);
				Scope commScope = (Scope) comm;
				commScope.setActivity(newSequenceComm);
				for (EObject obj : children) {
					this.createNewPreAndCommFEFragments(obj, newSequencePre, newSequenceComm, currCommActObj, prevCommActName, terminate);
				}
			}
			
			if (pre instanceof Flow) {
				Flow preFlow = (Flow) pre;
				preFlow.getActivities().add(newSequencePre);
				Flow commFlow = (Flow) comm;
				commFlow.getActivities().add(newSequenceComm);
			} else if (pre instanceof Sequence) {
				Sequence preSequence = (Sequence) pre;
				preSequence.getActivities().add(newSequencePre);
				Sequence commSequence = (Sequence) comm;
				commSequence.getActivities().add(newSequenceComm);
			} else if (pre instanceof If) {
				If preIf = (If) pre;
				preIf.setActivity(newSequencePre);
				If commIf = (If) comm;
				commIf.setActivity(newSequenceComm);
			}
			for (EObject obj : children) {
				this.createNewPreAndCommFEFragments(obj, newSequencePre, newSequenceComm, currCommActObj, prevCommActName, terminate);
			}
		}
		// If 1st activity of original FE is Scope, then make first activity of
		// preceding and communicating FEs also Scope activity
		else if (firstAct instanceof Scope) {
			
			Scope newScopePre = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyScopeActivityWOChildren((Scope) firstAct, true);// BPELFactory.eINSTANCE.createScope();
			Scope newScopeComm = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyScopeActivityWOChildren((Scope) firstAct, true);// BPELFactory.eINSTANCE.createScope();
			if (pre instanceof Flow) {
				Flow preFlow = (Flow) pre;
				preFlow.getActivities().add(newScopePre);
				Flow commFlow = (Flow) comm;
				commFlow.getActivities().add(newScopeComm);
			} else if (pre instanceof Sequence) {
				Sequence preSequence = (Sequence) pre;
				preSequence.getActivities().add(newScopePre);
				Sequence commSequence = (Sequence) comm;
				commSequence.getActivities().add(newScopeComm);
			} else if (pre instanceof If) {
				If preIf = (If) pre;
				preIf.setActivity(newScopePre);
				If commIf = (If) comm;
				commIf.setActivity(newScopeComm);
			}
			Activity child = ((Scope) firstAct).getActivity();
			this.createNewPreAndCommFEFragments(child, newScopePre, newScopeComm, currCommActObj, prevCommActName, terminate);
			
		}
		// If 1st activity of original FE is If, then make first activity of
		// preceding and communicating FEs also If activity
		else if (firstAct instanceof If) {
			If newIfPre = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyIfActivityWOChildren((If) firstAct);
			If newIfComm = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyIfActivityWOChildren((If) firstAct);
			if (pre instanceof Flow) {
				Flow preFlow = (Flow) pre;
				preFlow.getActivities().add(newIfPre);
				Flow commFlow = (Flow) comm;
				commFlow.getActivities().add(newIfComm);
			} else if (pre instanceof Sequence) {
				Sequence preSequence = (Sequence) pre;
				preSequence.getActivities().add(newIfPre);
				Sequence commSequence = (Sequence) comm;
				commSequence.getActivities().add(newIfComm);
			} else if (pre instanceof If) {
				If preIf = (If) pre;
				preIf.setActivity(newIfPre);
				If commIf = (If) comm;
				commIf.setActivity(newIfComm);
			}
			Activity child = ((If) firstAct).getActivity();
			this.createNewPreAndCommFEFragments(child, newIfPre, newIfComm, currCommActObj, prevCommActName, terminate);
		} else {
			// This condition helps to skip all the activities previously added
			// to previously created pre and comm FE fragments.
			// In Other words this condition enables addition of new activities
			// which are between current communicating activity and previous
			// communicating activity.
			if (firstAct.getName().equals(prevCommActName)) {
				terminate.startAddingActivities = true;
				return;
			}
			
			if (terminate.startAddingActivities || (prevCommActName.length() == 0)) {
				Activity commAct = (Activity) currCommActObj;
				// If the activity to be added is communicating activity (Assign
				// containing Sources or Empty activity containing Targets )
				if (firstAct.getName().equals(commAct.getName())) {
					// Add this communicating activity only to communicating FE
					
					if (currCommActObj instanceof Assign) {
						Assign newCommAssign = ((Assign) currCommActObj).clone();
						// Assign newCommAssign = (Assign) currCommActObj;
						if (comm instanceof Flow) {
							Flow commFlow = (Flow) comm;
							commFlow.getActivities().add(newCommAssign);
						} else if (comm instanceof Sequence) {
							Sequence commSequence = (Sequence) comm;
							commSequence.getActivities().add(newCommAssign);
						} else if (comm instanceof If) {
							If commIf = (If) comm;
							commIf.setActivity(newCommAssign);
						}
					} else if (currCommActObj instanceof Empty) {
						Empty newCommEmpty = ((Empty) currCommActObj).clone();
						// Empty newCommEmpty = (Empty) currCommActObj;
						if (comm instanceof Flow) {
							Flow commFlow = (Flow) comm;
							commFlow.getActivities().add(newCommEmpty);
						} else if (comm instanceof Sequence) {
							Sequence commSequence = (Sequence) comm;
							commSequence.getActivities().add(newCommEmpty);
						} else if (comm instanceof If) {
							If commIf = (If) comm;
							commIf.setActivity(newCommEmpty);
						}
					}
					terminate.finished = true;
					return;
				} else {
					// If the activity to be added is not communicating
					// activity, the nadd it only to rpeceding FE fragment. Not
					// communicating activities can be Invoke, Sequence, If,
					// Receive, Empty (without Targets), Assign (without
					// Sources).
					if (firstAct instanceof Invoke) {
						if (!terminate.preExists) {
							terminate.preExists = true;
						}
						Invoke newPreInvoke = ((Invoke) firstAct).clone();
						// .clone();
						// Invoke newPreInvoke = (Invoke) firstAct;
						if (pre instanceof Flow) {
							Flow preFlow = (Flow) pre;
							preFlow.getActivities().add(newPreInvoke);
							
						} else if (pre instanceof Sequence) {
							Sequence preSequence = (Sequence) pre;
							preSequence.getActivities().add(newPreInvoke);
							
						} else if (pre instanceof If) {
							If preIf = (If) pre;
							preIf.setActivity(newPreInvoke);
							
						}
						
					} else if (firstAct instanceof Receive) {
						if (!terminate.preExists) {
							terminate.preExists = true;
						}
						Receive newPreReceive = ((Receive) firstAct).clone();
						// Receive newPreReceive = (Receive) firstAct;
						if (pre instanceof Flow) {
							Flow preFlow = (Flow) pre;
							preFlow.getActivities().add(newPreReceive);
							
						} else if (pre instanceof Sequence) {
							Sequence preSequence = (Sequence) pre;
							preSequence.getActivities().add(newPreReceive);
							
						} else if (pre instanceof If) {
							If preIf = (If) pre;
							preIf.setActivity(newPreReceive);
							
						}
					} else if (firstAct instanceof Empty) {
						if (!terminate.preExists) {
							terminate.preExists = true;
						}
						Empty newPreEmpty = ((Empty) firstAct).clone();
						// Empty newPreEmpty = (Empty) firstAct;
						if (pre instanceof Flow) {
							Flow preFlow = (Flow) pre;
							preFlow.getActivities().add(newPreEmpty);
							
						} else if (pre instanceof Sequence) {
							Sequence preSequence = (Sequence) pre;
							preSequence.getActivities().add(newPreEmpty);
							
						} else if (pre instanceof If) {
							If preIf = (If) pre;
							preIf.setActivity(newPreEmpty);
							
						}
						
					} else if (firstAct instanceof Assign) {
						if (!terminate.preExists) {
							terminate.preExists = true;
						}
						Assign newPreAssign = ((Assign) firstAct).clone();
						// Assign newPreAssign = (Assign) firstAct;
						if (pre instanceof Flow) {
							Flow preFlow = (Flow) pre;
							preFlow.getActivities().add(newPreAssign);
							
						} else if (pre instanceof Sequence) {
							Sequence preSequence = (Sequence) pre;
							preSequence.getActivities().add(newPreAssign);
							
						} else if (pre instanceof If) {
							If preIf = (If) pre;
							preIf.setActivity(newPreAssign);
							
						}
						
					}
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @param obj - the activity to be checked
	 * @return return if the given activity is communicating activity or not
	 * 
	 *         The communicating activity is either Assign activity containign
	 *         Sources , or Empty activity containing Targets.
	 */
	public boolean isCommActivity(EObject obj) {
		// TODO FIXME I should not return true just because I have source
		// elements
		if (obj instanceof Assign) {
			// && obj instanceof Sources
			List<EObject> children = obj.eContents();
			for (EObject o : children) {
				if (o instanceof Sources) {
					return true;
				}
			}
		} else if (obj instanceof Empty) {
			// && obj instanceof Targets
			List<EObject> children = obj.eContents();
			for (EObject o : children) {
				if (o instanceof Targets) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * 
	 * @param lstObjs - the linked list which will contain all the activities
	 *            till the communicating activity
	 * @param startObj - is the 1st activity of FE fragment
	 * @param prevCommActNameList - the list of already processed communicating
	 *            activities
	 * @return the list of all activities inside FE till the first communicating
	 *         activity which is not processed
	 */
	public LinkedList<EObject> newGetActivitiesTillFirstCommActFromForEach(LinkedList<EObject> lstObjs, EObject startObj, ArrayList<String> prevCommActNameList) {
		// This function traverses all the activities and their children insde
		// FE fragment (in other words which are children of startObj) till it
		// finds the non-processed communicating activity, and then returns the
		// list contating all the activities till the first non-processed
		// communicating activity
		
		List<EObject> childActivities = startObj.eContents();
		
		if (startObj instanceof ForEach) {
			Activity act = ((ForEach) startObj).getActivity();
			lstObjs.addLast(act);
			childActivities = act.eContents();
		} else if (startObj instanceof Sequence) {
			lstObjs.addLast(startObj);
			List<Activity> childActs = ((Sequence) startObj).getActivities();
			for (Activity child : childActs) {
				return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, child, prevCommActNameList);
			}
			
		} else if (startObj instanceof Flow) {
			lstObjs.addLast(startObj);
			List<Activity> childActs = ((Flow) startObj).getActivities();
			for (Activity child : childActs) {
				return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, child, prevCommActNameList);
			}
			
		} else {
			lstObjs.addLast(startObj);
		}
		if ((childActivities.size() == 0) || !(startObj instanceof Activity)) {
			EObject parent = startObj.eContainer();
			childActivities = parent.eContents();
			for (int i = 0; i < childActivities.size(); i++) {
				EObject o = childActivities.get(i);
				if (o.equals(startObj)) {
					if ((i + 1) < childActivities.size()) {
						return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, childActivities.get(i + 1), prevCommActNameList);
					}
				}
			}
		}
		for (EObject obj : childActivities) {
			
			if ((startObj instanceof Assign) && (obj instanceof Sources)) {
				if (lstObjs.getLast() instanceof Sources) {
					lstObjs.removeLast();
				}
				
				Assign assignAct = (Assign) startObj;
				
				if (!prevCommActNameList.contains(assignAct.getName())) {
					
					return lstObjs;
				} else {
					List<EObject> childActivities2 = startObj.eContainer().eContents();
					for (int i = 0; i < childActivities2.size(); i++) {
						EObject o = childActivities2.get(i);
						if (o instanceof Activity) {
							if (((Activity) o).getName().equals(((Activity) startObj).getName())) {
								
								return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, childActivities2.get(i + 1), prevCommActNameList);
							}
						}
					}
				}
			}
			if ((startObj instanceof Empty) && (obj instanceof Targets)) {
				
				if (lstObjs.getLast() instanceof Targets) {
					lstObjs.removeLast();
				}
				Empty emptyAct = (Empty) startObj;
				
				if (!prevCommActNameList.contains(emptyAct.getName())) {
					
					return lstObjs;
				} else {
					
					List<EObject> childActivities2 = startObj.eContainer().eContents();
					for (int i = 0; i < childActivities2.size(); i++) {
						EObject o = childActivities2.get(i);
						if (o instanceof Activity) {
							if (((Activity) o).getName().equals(((Activity) startObj).getName())) {
								
								return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, childActivities2.get(i + 1), prevCommActNameList);
							}
						}
					}
					
				}
			}
			if (obj instanceof Activity) {
				lstObjs.addLast(obj);
				if (obj.eContents() != null) {
					List<EObject> children = obj.eContents();
					for (EObject obj1 : children) {
						if (obj1 instanceof Activity) {
							return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, obj1, prevCommActNameList);//
						}
					}
				}
			} else {
				EObject parent = startObj.eContainer();
				List<EObject> childActivities3 = parent.eContents();
				for (int i = 0; i < childActivities3.size(); i++) {
					EObject o = childActivities3.get(i);
					if (o.equals(startObj)) {
						if ((i + 1) < childActivities3.size()) {
							return this.newGetActivitiesTillFirstCommActFromForEach(lstObjs, childActivities3.get(i + 1), prevCommActNameList);
						}
					}
				}
			}
			
		}
		
		return null;
	}
	
	/**
	 * This function gets and stores all the containers/scopes in merged process
	 */
	public void initializeScopeActivities() {
		Process mergedProcess = this.choreographyPackage.getMergedProcess();
		Activity firstActivity = mergedProcess.getActivity();
		List<EObject> childActivities = firstActivity.eContents();
		for (EObject obj : childActivities) {
			if (obj instanceof Scope) {
				Scope scope = (Scope) obj;
				ChoreographyMergerExtension.scopeList.add(scope);
			}
		}
		
	}
	
	/**
	 * This function gets and stores all the links which are children of top
	 * flow activity of FE fragment.
	 */
	public void intializeLinksFromMergedProcess() {
		
		Process mergedProcess = this.choreographyPackage.getMergedProcess();
		Activity firstActivity = mergedProcess.getActivity();
		List<EObject> childActivities = firstActivity.eContents();
		Flow flow = (Flow) firstActivity;
		if (flow.getLinks() != null) {
			for (Link link : flow.getLinks().getChildren()) {
				ChoreographyMergerExtension.linksList.add(link);
			}
		} else {
			for (EObject e : flow.eContents()) {
				System.out.println(e.getClass());
			}
		}
		
	}
	
	/**
	 * Helper method. Storing newly created scopes in map for further
	 * processing.
	 */
	public void addNewScopesToMap() {
		Process mergedProcess = this.choreographyPackage.getMergedProcess();
		
		Activity firstActivity = mergedProcess.getActivity();
		EObject parentOfScopes = this.getAllContainerScopesParent(firstActivity);
		List<EObject> scopeObjects = parentOfScopes.eContents();
		for (EObject scopeObj : scopeObjects) {
			
			if (scopeObj instanceof Scope) {
				Scope scope = (Scope) scopeObj;
				PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.mergedProcessScopeMap.put(scope.getName(), scope);
			}
		}
		
	}
	
	/**
	 * This function updates scope/container names in merged process. Basically
	 * this function handles correct number indexing attached to the name of
	 * container scopes. Scope_1, Scope_2, ..., Scope_N
	 */
	public void updateScopeNames() {
		Process mergedProcess = this.choreographyPackage.getMergedProcess();
		ArrayList<Scope> listOfScopes = new ArrayList<Scope>();
		
		Activity firstActivity = mergedProcess.getActivity();
		EObject obj = this.getAllContainerScopesParent(firstActivity);
		
		for (EObject o : obj.eContents()) {
			if (o instanceof Scope) {
				Scope scope = (Scope) o;
				// System.out.println("Adding scope: " + scope.getName());
				listOfScopes.add(scope);
			}
		}
		
		for (int i = 0; i < listOfScopes.size(); i++) {
			for (int j = i + 1; j < listOfScopes.size(); j++) {
				if (listOfScopes.get(i).getName().equals(listOfScopes.get(j).getName())) {
					Scope scopeToBeChanged = listOfScopes.get(j);
					String scopeName = scopeToBeChanged.getName();
					String newScopeName = scopeName.substring(0, scopeName.lastIndexOf("_") + 1) + (1 + Integer.parseInt(scopeName.substring(scopeName.lastIndexOf("_") + 1)));
					scopeToBeChanged.setName(newScopeName);
					i = 0;
					j = i + 1;
					
				}
			}
		}
		
	}
	
	/**
	 * 
	 * @param obj - merged process's first activity
	 * @return return the parent of all container scoeps in merged process
	 */
	public EObject getAllContainerScopesParent(EObject obj) {
		if (obj instanceof Scope) {
			return obj.eContainer();
		}
		
		for (EObject o : obj.eContents()) {
			if (o instanceof Scope) {
				return obj;
			}
			
		}
		for (int i = 0; i < obj.eContents().size(); i++) {
			return this.getAllContainerScopesParent(obj.eContents().get(i));
		}
		return null;
	}
	
	/**
	 * In order supporint g MIP instantiation, each partner link needs to be
	 * copied into each corresponding FE fragment.Because multiple instances of
	 * the same processes will access same partner link at the same time during
	 * run time.
	 * 
	 * 
	 */
	public void copyNewPartnerLinksOfNMMLToForEachScope() {
		/**
		 * Implement here copying partner links from higher scope (which are
		 * generated for NMML) into scope inside forEach activity
		 * 
		 * 
		 * 1) Check if there is participantSet
		 * 
		 * 2) if there is, then get highest Scope of forEach activity and copy
		 * new partner links there from merged process's Scope_Airline scope
		 * 
		 */
		try {
			if (this.choreographyPackage.getTopology().getParticipantSets().size() > 0) {
				Process mergedProcess = this.choreographyPackage.getMergedProcess();
				Activity firstActivity = mergedProcess.getActivity();
				EObject containerSub = firstActivity;
				ArrayList<String> participatnSetPBDNames = this.choreographyPackage.choreographyPackageExtension.getParticipantSetPBDNames();
				
				EList<EObject> objs = containerSub.eContents();
				boolean continueFlag = true;
				for (EObject objOfFLow : objs) {
					continueFlag = true;
					if (objOfFLow instanceof Scope) {
						
						Scope scopeOfPS_PBD = (Scope) objOfFLow;
						
						int index = this.checkScopeActivityNameVSParticipantSetPBDNames(scopeOfPS_PBD.getName(), participatnSetPBDNames);
						if (index != -1) {
							containerSub = scopeOfPS_PBD;
							objs = containerSub.eContents();
							for (EObject objOfScope : objs) {
								{
									if (!continueFlag) {
										break;
									}
									if (objOfScope instanceof ForEach) {
										containerSub = objOfScope;
										objs = containerSub.eContents();
										for (EObject objOfForEach : objs) {
											if (!continueFlag) {
												break;
											}
											if (objOfForEach instanceof Scope) {
												Scope scopeOfForEach = (Scope) objOfForEach;
												scopeOfForEach.getPartnerLinks().getChildren().addAll(scopeOfPS_PBD.getPartnerLinks().getChildren());
												continueFlag = false;
											}
										}
										
									}
								}
							}
						}
					}
					
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 
	 * @param curObj - can be scope activity
	 * @param actName - activity name of searched activity
	 * @return the activity with given activity name residing in the given scope
	 */
	public Invoke findInvokeActivity(EObject curObj, String actName) {
		Invoke res = null;
		List<EObject> children = curObj.eContents();
		for (EObject obj : children) {
			if (obj instanceof Invoke) {
				Invoke act = (Invoke) obj;
				if (act.getElement().getAttribute("wsu:id").equals(actName) || act.getName().equals(actName)) {
					return act;
				}
				
			}
			
		}
		for (EObject obj : children) {
			res = this.findInvokeActivity(obj, actName);
			if (res != null) {
				return res;
			}
		}
		return res;
	}
	
	/**
	 * 
	 * @param curObj - can be scope activity
	 * @param actName - activity name of searched activity
	 * @return the activity with given activity name residing in the given scope
	 */
	public Pick findPickActivity(EObject curObj, String actName) {
		Pick res = null;
		List<EObject> children = curObj.eContents();
		for (EObject obj : children) {
			if (obj instanceof Pick) {
				Pick act = (Pick) obj;
				if (act.getElement().getAttribute("wsu:id").equals(actName)) {
					return act;
				}
				
			}
			
		}
		for (EObject obj : children) {
			res = this.findPickActivity(obj, actName);
			if (res != null) {
				return res;
			}
		}
		return res;
	}
	
	/**
	 * Configure remaining communication activities from NMML
	 */
	public void configureNMMLActivities2() {
		if (this.choreographyPackage.getNMML().size() > 0) {
			this.log.info("Following Message Links couldn't be merged : ");
			for (MessageLink ml : this.choreographyPackage.getNMML()) {
				
				// If sendActivity is <reply> skip the link, it will be
				// configured after the <receive>
				if (ChoreoMergeUtil.resolveActivity(ml.getSendActivity()) instanceof Reply) {
					continue;
				}
				
				// Grounding-Topology-WSDL-Informations
				this.log.info("ML : " + ml);
				org.bpel4chor.model.grounding.impl.MessageLink grndMl = BPEL4ChorUtil.resolveGroundingMessageLinkByName(this.choreographyPackage.getGrounding(), ml.getName());
				this.log.info("The corresponding Grounding Message Link is : " + grndMl);
				Participant sender = BPEL4ChorUtil.resolveParticipant(this.choreographyPackage.getTopology(), ml.getSender());
				Participant receiver = BPEL4ChorUtil.resolveParticipant(this.choreographyPackage.getTopology(), ml.getReceiver());
				
				this.log.info("Sender Participant is : " + sender);
				this.log.info("Receiver Participant is : " + receiver);
				ParticipantType sendPT = ChoreoMergeUtil.resolveParticipantType(this.choreographyPackage.getTopology(), sender);
				ParticipantType recPT = ChoreoMergeUtil.resolveParticipantType(this.choreographyPackage.getTopology(), receiver);
				this.log.info("Sender ParticipantType is : " + sendPT);
				this.log.info("Receiver ParticipantType is : " + recPT);
				Process sendPBD = this.choreographyPackage.choreographyPackageExtension.getPBDByName(sendPT.getParticipantBehaviorDescription().getLocalPart());
				Process recPBD = this.choreographyPackage.choreographyPackageExtension.getPBDByName(recPT.getParticipantBehaviorDescription().getLocalPart());
				this.log.info("Sender PBD is : " + sendPBD);
				this.log.info("Receiver PBD is : " + recPBD);
				Definition sendDef = this.choreographyPackage.getPbd2wsdl().get(sendPBD);
				Definition recDef = this.choreographyPackage.getPbd2wsdl().get(recPBD);
				
				this.log.info("Sender WSDL : " + sendDef);
				this.log.info("Receiver WSDL : " + recDef);
				
				PortType recPortType = MyWSDLUtil.findPortType(recDef, grndMl.getPortType().getLocalPart());
				// CHECK: again only one wsdl is permitted
				if (recPortType == null) {
					for (Definition def : this.choreographyPackage.getWsdls()) {
						recPortType = MyWSDLUtil.findPortType(def, grndMl.getPortType().getLocalPart());
						if (recPortType != null) {
							break;
						}
					}
				}
				this.log.info("Receiver PortType is : " + recPortType);
				
				Operation recOperation = MyWSDLUtil.resolveOperation(recDef, recPortType.getQName(), grndMl.getOperation());
				this.log.info("Receiver Operation is : " + recOperation);
				Role recPLRole = MyWSDLUtil.findPartnerLinkTypeRole(recDef, recPortType);
				// CHECK: again ...
				if (recPLRole == null) {
					for (Definition def : this.choreographyPackage.getWsdls()) {
						recPLRole = MyWSDLUtil.findPartnerLinkTypeRole(def, recPortType);
						if (recPLRole != null) {
							break;
						}
					}
				}
				this.log.info("PartnerLinkType-Role which supports PortType above is : " + recPLRole);
				
				// Technical Activity configuration
				BPELExtensibleElement sendAct = ChoreoMergeUtil.resolveActivity(ml.getSendActivity());
				if (sendAct.eContainer() == null) {
					// QUICK HACK :)
					// The parent is null for some reason, we cannot figure out
					// Search the invoke from the parent, returns an equal
					// object, but not the same
					sendAct = this.findInvokeActivity(sendPBD.getActivity(), ((Invoke) sendAct).getName());
				}
				// System.out.println();
				BPELExtensibleElement recAct = ChoreoMergeUtil.resolveActivity(ml.getReceiveActivity());
				
				Invoke s = (Invoke) sendAct;
				BPELExtensibleElement r = null;
				if (recAct instanceof Receive) {
					r = recAct;
				} else if (recAct instanceof OnMessage) {
					r = (Activity) recAct.eContainer();
				} else if (recAct instanceof OnEvent) {
					if (recAct.eContainer() instanceof Process) {
						// If we have an <onEvent> of <process>
						r = (Process) recAct.eContainer().eContainer();
					} else {
						// If we have an <onEvent> of <scope>
						r = (Scope) recAct.eContainer().eContainer();
					}
					
				}
				
				Scope scpS = ChoreoMergeUtil.getHighestScopeOfActivity(s);
				
				ArrayList<Scope> similarScopes = this.haveSimilarScopeContainers(scpS, PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.mergedProcessScopeMap);
				
				BPELExtensibleElement scpR = null; // ChoreoMergeUtil.getHighestScopeOfActivity(r);
				if (r instanceof Process) {
					scpR = r;
				} else if (r instanceof Scope) {
					scpR = r;
				} else {
					
					scpR = ChoreoMergeUtil.getHighestScopeOfActivity((Activity) r);
					
				}
				
				String recName = (r instanceof Process ? ((Process) r).getName() : ((Activity) r).getName());
				
				// Set PartnerLinks, Operation and PortType for s
				if (scpS.getPartnerLinks() == null) {
					scpS.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
				}
				PartnerLink newPLS = BPELFactory.eINSTANCE.createPartnerLink();
				newPLS.setName(s.getName() + "TO" + recName + "PLS");
				newPLS.setPartnerLinkType((PartnerLinkType) recPLRole.eContainer());
				newPLS.setPartnerRole(recPLRole);
				scpS.getPartnerLinks().getChildren().add(newPLS);
				s.setPartnerLink(newPLS);
				s.setOperation(recOperation);
				s.setPortType(recPortType);
				
				PartnerLink newPLR = BPELFactory.eINSTANCE.createPartnerLink();
				newPLR.setName(s.getName() + "TO" + recName + "PLR");
				newPLR.setPartnerLinkType((PartnerLinkType) recPLRole.eContainer());
				newPLR.setMyRole(recPLRole);
				
				if (scpR instanceof Process) {
					Process proc = (Process) scpR;
					if (proc.getPartnerLinks() == null) {
						proc.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
					}
					proc.getPartnerLinks().getChildren().add(newPLR);
				} else {
					Scope scp = (Scope) scpR;
					if (scp.getPartnerLinks() == null) {
						scp.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
					}
					scp.getPartnerLinks().getChildren().add(newPLR);
					
				}
				
				if (recAct instanceof Receive) {
					((PartnerActivity) recAct).setPartnerLink(newPLR);
					((PartnerActivity) recAct).setOperation(recOperation);
					((PartnerActivity) recAct).setPortType(recPortType);
				} else if (recAct instanceof OnMessage) {
					OnMessage om = (OnMessage) recAct;
					om.setPartnerLink(newPLR);
					om.setOperation(recOperation);
					om.setPortType(recPortType);
				} else if (recAct instanceof OnEvent) {
					OnEvent oe = (OnEvent) recAct;
					oe.setPartnerLink(newPLR);
					oe.setOperation(recOperation);
					oe.setPortType(recPortType);
				}
				
				if (!ChoreoMergeUtil.isInvokeAsync(s)) {
					// Find the <reply>ing links for s in NMML
					for (MessageLink messageLink : this.choreographyPackage.getNMML()) {
						if (messageLink.getReceiveActivity().equals(s.getName())) {
							Reply repl = (Reply) ChoreoMergeUtil.resolveActivity(messageLink.getSendActivity());
							repl.setPartnerLink(newPLR);
							repl.setOperation(recOperation);
							repl.setPortType(recPortType);
						}
					}
				}
				
				for (int i = 0; i < similarScopes.size(); i++) {
					Scope invokeSimilarScope = similarScopes.get(i);
					if (invokeSimilarScope.getPartnerLinks() == null) {
						invokeSimilarScope.setPartnerLinks(BPELFactory.eINSTANCE.createPartnerLinks());
						
					}
					
					// Copy PartnerLinks
					for (PartnerLink pLink : scpS.getPartnerLinks().getChildren()) {
						PartnerLink newLink = FragmentDuplicator.copyPartnerLink(pLink);
						if (!ChoreographyMergerExtension.containsPartnerLink(pLink, invokeSimilarScope.getPartnerLinks().getChildren())) {
							invokeSimilarScope.getPartnerLinks().getChildren().add(newLink);
						}
					}
					
				}
			}
			ChoreographyMergerExtension.createdNewPartnerLinksForNMML = true;
		}
		
	}
	
	/**
	 * 
	 * @param pLink
	 * @param listOfPLinks
	 * @return true if the list contains given partner link, otherwise return
	 *         false
	 */
	public static boolean containsPartnerLink(PartnerLink pLink, EList<PartnerLink> listOfPLinks) {
		for (PartnerLink pl : listOfPLinks) {
			if (pl.getName().equals(pLink.getName())) {
				return true;
			}
		}
		return false;
		
	}
	
	/**
	 * 
	 * @param str - scope name
	 * @return - cechk whteher scope name contains number or not
	 */
	public static boolean scopeNameHasNumber(String str) {
		try {
			Integer.parseInt(str.substring(str.lastIndexOf("_") + 1));
			return true;
		} catch (NumberFormatException nfe) {
		}
		return false;
	}
	
	/**
	 * 
	 * @param currentScope
	 * @param mergedProcessAllScopes
	 * @return
	 * 
	 *         This function checks whethere there are similar scopes in the
	 *         merged process after MIP instantiation scenarios. Similar scopes
	 *         means same scope names but different indexes in the end. Scope_1
	 *         and Scope_2 are similar Scopes. but ScopeA_1 and ScopeB_2 are not
	 *         similar scopes.
	 */
	public ArrayList<Scope> haveSimilarScopeContainers(Scope currentScope, Map<String, Scope> mergedProcessAllScopes) {
		String processed_scope_name = currentScope.getName();
		ArrayList<Scope> listOfSimilarScopes = new ArrayList<Scope>();
		Iterator<Entry<String, Scope>> itMergedProcessAllScopes = mergedProcessAllScopes.entrySet().iterator();
		while (itMergedProcessAllScopes.hasNext()) {
			Entry<String, Scope> mergedProcess_scope = itMergedProcessAllScopes.next();
			String mergedProcess_scope_name = mergedProcess_scope.getKey();
			if (ChoreographyMergerExtension.scopeNameHasNumber(processed_scope_name)) {
				if (ChoreographyMergerExtension.scopeNameHasNumber(mergedProcess_scope_name)) {
					if ((processed_scope_name.contains(mergedProcess_scope_name.substring(0, mergedProcess_scope_name.lastIndexOf("_") + 1)) && !processed_scope_name.equals(mergedProcess_scope_name)) || (mergedProcess_scope_name.contains(processed_scope_name.substring(0, processed_scope_name.lastIndexOf("_") + 1)) && !processed_scope_name.equals(mergedProcess_scope_name))) {
						listOfSimilarScopes.add(mergedProcess_scope.getValue());
					}
				} else {
					if ((processed_scope_name.contains(mergedProcess_scope_name) && !processed_scope_name.equals(mergedProcess_scope_name)) || (mergedProcess_scope_name.contains(processed_scope_name.substring(0, processed_scope_name.lastIndexOf("_") + 1)) && !processed_scope_name.equals(mergedProcess_scope_name))) {
						listOfSimilarScopes.add(mergedProcess_scope.getValue());
					}
				}
			} else {
				if (ChoreographyMergerExtension.scopeNameHasNumber(mergedProcess_scope_name)) {
					if ((processed_scope_name.contains(mergedProcess_scope_name.substring(0, mergedProcess_scope_name.lastIndexOf("_") + 1)) && !processed_scope_name.equals(mergedProcess_scope_name)) || (mergedProcess_scope_name.contains(processed_scope_name) && !processed_scope_name.equals(mergedProcess_scope_name))) {
						listOfSimilarScopes.add(mergedProcess_scope.getValue());
					}
				}
			}
			
		}
		
		return listOfSimilarScopes;
		
	}
	
	public void handleSeveralCreateInstanceYesCases() {
		/**
		 * All receiving activities including receive, onMessage of Pick
		 */
		//
		List<MessageLink> messageLinks = this.choreographyPackage.getTopology().getMessageLinks();
		Map<String, ArrayList<String>> receiveActivitiesMap = new HashMap<String, ArrayList<String>>();
		for (MessageLink msgLink : messageLinks) {
			String pbdName = this.choreographyPackage.choreographyPackageExtension.getPBDDescriptionNameFromParticipantName(msgLink.getReceiver());
			if (receiveActivitiesMap.containsKey(pbdName)) {
				receiveActivitiesMap.get(pbdName).add(msgLink.getReceiveActivity());
				
			} else {
				ArrayList<String> recActList = new ArrayList<String>();
				recActList.add(msgLink.getReceiveActivity());
				receiveActivitiesMap.put(pbdName, recActList);
				
			}
		}
		try {
			List<Process> pbds = new ArrayList<Process>();
			List<Process> processedPbds = new ArrayList<Process>();
			pbds.addAll(this.choreographyPackage.getPbds());
			Map<String, ArrayList<String>> newInstanceCreatingRecActMap = new HashMap<String, ArrayList<String>>();
			
			/**
			 * Get all receive/Pick activities where createInstance="yes"
			 * 
			 * newInstanceCreatingRecActMap contains pbd name and receive
			 * activity name
			 * 
			 */
			
			for (Process pbd : pbds) {
				
				System.out.println("Pbd: " + pbd.getName());
				if (receiveActivitiesMap.containsKey(pbd.getName())) {
					ArrayList<String> recActivites = receiveActivitiesMap.get(pbd.getName());
					
					for (int j = 0; j < recActivites.size(); j++) {
						String curRecAct = recActivites.get(j);
						
						boolean createInstance = this.checkReceiveActivityCreateInstanceYes(pbd, curRecAct);
						
						if (createInstance) {
							
							if (newInstanceCreatingRecActMap.containsKey(pbd.getName())) {
								newInstanceCreatingRecActMap.get(pbd.getName()).add(curRecAct);
								
							} else {
								
								ArrayList<String> recActList = new ArrayList<String>();
								recActList.add(curRecAct);
								newInstanceCreatingRecActMap.put(pbd.getName(), recActList);
							}
						}
						
					}
				}
				
			}
			
			/**
			 * TODO
			 * 
			 * get wsuId of all those invoke activities which are calling
			 * receive/pick activities (with attribute createInstance="yes")
			 * 
			 */
			
			Map<String, ArrayList<String>> newInstanceCreatingInvokeActMap = new HashMap<String, ArrayList<String>>();
			
			for (MessageLink mL : this.choreographyPackage.getTopology().getMessageLinks()) {
				
				for (int i = 0; i < pbds.size(); i++) {
					Process pbd = pbds.get(i);
					if (newInstanceCreatingRecActMap.get(pbd.getName()) == null) {
						continue;
					}
					
					for (String recActivity : newInstanceCreatingRecActMap.get(pbd.getName())) {
						if (mL.getReceiveActivity().equals(recActivity)) {
							
							if (newInstanceCreatingInvokeActMap.get(pbd.getName()) != null) {
								if (!newInstanceCreatingInvokeActMap.get(pbd.getName()).contains(mL.getSendActivity())) {
									newInstanceCreatingInvokeActMap.get(pbd.getName()).add(mL.getSendActivity());
									
								}
							} else {
								ArrayList<String> invokeNames = new ArrayList<String>();
								invokeNames.add(mL.getSendActivity());
								newInstanceCreatingInvokeActMap.put(pbd.getName(), invokeNames);
								
							}
							
						}
					}
					
				}
				
			}
			
			/**
			 * Get activity from choreographyPackage.mergedProcess getOld2New()
			 * 
			 * And then check if parent is forEach or not
			 * 
			 * Count numbers
			 * 
			 */
			pbds.clear();
			pbds.addAll(this.choreographyPackage.getPbds());
			// invokeCounts will keep for each forEach number of invokes inside
			boolean firstTime = true;
			for (Process pbd : pbds) {
				if (newInstanceCreatingInvokeActMap.get(pbd.getName()) == null) {
					continue;
				}
				for (String invokeWsuId : newInstanceCreatingInvokeActMap.get(pbd.getName())) {
					Activity invokeActivity = (Activity) this.choreographyPackage.getOld2New().get(invokeWsuId);
					if (this.choreographyPackage.choreographyPackageExtension.checkParentIsForEach(invokeActivity)) {
						// countInvokesInForeach += 1;
						ForEach parentForEach = this.choreographyPackage.choreographyPackageExtension.getFirstForEachParent(invokeActivity);
						
						/**
						 * Check if forEach's parent is also forEach then add
						 * number of invokes inside it to parents numOfInvokes
						 * value
						 * 
						 */
						
						if (this.choreographyPackage.choreographyPackageExtension.checkParentIsForEach(parentForEach)) {
							ForEach parentParentForEach = this.choreographyPackage.choreographyPackageExtension.getFirstForEachParent(parentForEach);
							if (ChoreographyMergerExtension.invokeCountsInsideForEachs.containsKey(parentParentForEach.getName())) {
								if (firstTime) {
									firstTime = false;
								} else {
									ChoreographyMergerExtension.invokeCountsInsideForEachs.put(parentParentForEach.getName(), ChoreographyMergerExtension.invokeCountsInsideForEachs.get(parentParentForEach.getName()).intValue() + 1);
								}
							} else {
								ChoreographyMergerExtension.invokeCountsInsideForEachs.put(parentParentForEach.getName(), 1);
							}
							
						} else {
							
							if (ChoreographyMergerExtension.invokeCountsInsideForEachs.containsKey(parentForEach.getName())) {
								
								ChoreographyMergerExtension.invokeCountsInsideForEachs.put(parentForEach.getName(), ChoreographyMergerExtension.invokeCountsInsideForEachs.get(parentForEach.getName()).intValue() + 1);
								
							} else {
								ChoreographyMergerExtension.invokeCountsInsideForEachs.put(parentForEach.getName(), 1);
							}
						}
						
					} else {
						this.wsuIdOfInvokesnotInsideForEach.add(invokeWsuId);
						ChoreographyMergerExtension.countInvokesNOTInForeach += 1;
						System.out.println("Invoke Activity: " + invokeActivity.getName() + "\tCount: " + ChoreographyMergerExtension.countInvokesNOTInForeach);
					}
					
				}
				
			}
			// This function below is not part of this thesis, even though it is
			// implemented.
			// createExtraContinaersForExtraInvokes();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This function creates extra MIP containers for each extra invoke
	 * activity, which is not inside FE fragment.
	 */
	public void createExtraContinaersForExtraInvokes() {
		
		Process mergedProcess = this.choreographyPackage.getMergedProcess();
		Activity firstActivity = mergedProcess.getActivity();
		EObject obj = firstActivity;
		List<EObject> scopes = obj.eContents();
		
		boolean hasExecuted = false;
		for (EObject scopeObj : scopes) {
			Scope scope = (Scope) scopeObj;
			List<EObject> scopeChildrenForEach = scope.eContents();
			String scopeName = "";
			if (scope.getName().indexOf("_") == scope.getName().lastIndexOf("_")) {
				scopeName = scope.getName().substring(scope.getName().indexOf("_") + 1);
			} else {
				scopeName = scope.getName().substring(scope.getName().indexOf("_") + 1, scope.getName().indexOf("_", scope.getName().indexOf("_") + 1));
			}
			if (scopeChildrenForEach.size() == 2) {
				ForEach curForEach = (ForEach) scopeChildrenForEach.get(0);
				int count = ChoreographyMergerExtension.invokeCountsInsideForEachs.get(curForEach.getName()) - 1;
				Process pbd = this.getPbdFromPbdName(scopeName);
				for (int i = 0; i < count; i++) {
					ForEach currentForEach = (ForEach) scope.getActivity();
					PBDFragmentDuplicator.pbdFragmentDuplicatorExtension.copyVarsAndActitiviesDynamicMIP(pbd, currentForEach);
				}
			} else {
				if (!hasExecuted) {
					for (String wsuId : this.wsuIdOfInvokesnotInsideForEach) {
						Process pbd = this.getPBDFromWsuIdOfInvoke(wsuId);
						for (int i = 0; i < ChoreographyMergerExtension.countInvokesNOTInForeach; i++) {
							PBDFragmentDuplicator.copyVarsAndActitivies(pbd);
						}
						hasExecuted = true;
					}
				}
			}
			
		}
	}
	
	/**
	 * 
	 * @param wsuIdOfInvoke
	 * @return the process in which the invoke activity with given wsuID resides
	 */
	public Process getPBDFromWsuIdOfInvoke(String wsuIdOfInvoke) {
		MessageLink requiredMsgLink = null;
		for (MessageLink mL : this.choreographyPackage.getTopology().getMessageLinks()) {
			if (mL.getSendActivity().equals(wsuIdOfInvoke)) {
				requiredMsgLink = mL;
				break;
			}
		}
		String requiredPbdName = this.choreographyPackage.choreographyPackageExtension.getPBDDescriptionNameFromParticipantName(requiredMsgLink.getReceiver());
		return this.getPbdFromPbdName(requiredPbdName);
		
	}
	
	/**
	 * 
	 * @param pbdName
	 * @return - the process with the given pbd name
	 */
	public Process getPbdFromPbdName(String pbdName) {
		List<Process> pbds = this.choreographyPackage.getPbds();
		for (Process pbd : pbds) {
			if (pbd.getName().equals(pbdName)) {
				return pbd;
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param forEachActivity
	 * @return - check if the given forEach activity ahs another/nested forEach
	 *         activity, then return it , otherwise return null.
	 */
	public ForEach checkHasForEachChild(EObject forEachActivity) {
		
		if (forEachActivity == null) {
			return null;
		}
		
		List<EObject> children = forEachActivity.eContents();
		for (EObject child : children) {
			if (child instanceof ForEach) {
				return (ForEach) child;
			}
		}
		
		for (EObject child : children) {
			return this.checkHasForEachChild(child);
		}
		return null;
	}
	
	/**
	 * 
	 * @param pbd - process which is participating in choreography
	 * @param recActname - recevie activity name
	 * @return check whether the receive activity with given name, residing in
	 *         the given pbd, has createInstance attribtue value yes or no.
	 */
	public boolean checkReceiveActivityCreateInstanceYes(Process pbd, String recActname) {
		Activity firstActivity = pbd.getActivity();
		EObject container = firstActivity.eContainer();
		container = firstActivity;
		
		Receive recAct = this.findReceiveActivity(container, recActname);
		if (recAct != null) {
			if (recAct.getCreateInstance()) {
				return true;
			}
		}
		
		Pick pickAct = this.findPickActivity(container, recActname);
		if (pickAct != null) {
			if (pickAct.getCreateInstance()) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param curObj - is the scope activity representing MIP instantiation
	 * @return the forEach activity insde MIP container
	 */
	public ForEach findForEachActivityInsideDynamicMIP(EObject curObj) {
		if (curObj == null) {
			return null;
		}
		ForEach forEach = null;
		List<EObject> children = curObj.eContents();
		for (EObject obj : children) {
			if (obj instanceof ForEach) {
				ForEach act = (ForEach) obj;
				return act;
			}
			
		}
		for (EObject obj : children) {
			forEach = this.findForEachActivityInsideDynamicMIP(obj);
			if (forEach != null) {
				return forEach;
			}
		}
		return forEach;
	}
	
	/**
	 * 
	 * @param curObj - is the scope activity representing MIP instantiation
	 * @param foundForEachs - already foudn forEach activities
	 * @return the forEach activity inside give scope, which is not found (which
	 *         name is not in foundForEachs list)
	 */
	public ForEach findForEachActivityInsideMIPScope(EObject curObj, ArrayList<String> foundForEachs) {
		if (curObj == null) {
			return null;
		}
		ForEach forEach = null;
		List<EObject> children = curObj.eContents();
		for (EObject obj : children) {
			if (obj instanceof ForEach) {
				ForEach act = (ForEach) obj;
				if (!foundForEachs.contains(act.getName())) {
					return act;
				}
			}
			
		}
		for (EObject obj : children) {
			forEach = this.findForEachActivityInsideMIPScope(obj, foundForEachs);
			if (forEach != null) {
				return forEach;
			}
		}
		return forEach;
	}
	
	/**
	 * 
	 * @param curObjFE - is forEach activity
	 * @return return the flow activitiy residing in the given FE
	 */
	public Flow findFlowActivityInsideFE(EObject curObjFE) {
		Flow res = null;
		List<EObject> children = curObjFE.eContents();
		for (EObject obj : children) {
			if (obj instanceof Flow) {
				Flow act = (Flow) obj;
				
				return act;
				
			}
			
		}
		for (EObject obj : children) {
			res = this.findFlowActivityInsideFE(obj);
			if (res != null) {
				return res;
			}
		}
		return res;
	}
	
	/**
	 * 
	 * @param curObj - scope activity
	 * @param actName - the searched activity name
	 * @return the activity with given activity name residing in the given scope
	 */
	public Activity findActivityByName(EObject curObj, String actName) {
		Activity res = null;
		List<EObject> children = curObj.eContents();
		for (EObject obj : children) {
			if (obj instanceof Activity) {
				Activity act = (Activity) obj;
				if ((act.getName() != null) && act.getName().equals(actName)) {
					return act;
				}
				
			}
			
		}
		for (EObject obj : children) {
			res = this.findActivityByName(obj, actName);
			if (res != null) {
				return res;
			}
		}
		return res;
	}
	
	/**
	 * 
	 * @param curObj - can be scope activity
	 * @param actName - activity name of searched activity
	 * @return the activity with given activity name residing in the given scope
	 */
	public Receive findReceiveActivity(EObject curObj, String actName) {
		Receive res = null;
		List<EObject> children = curObj.eContents();
		for (EObject obj : children) {
			if (obj instanceof Receive) {
				Receive act = (Receive) obj;
				if (act.getElement().getAttribute("wsu:id").equals(actName)) {
					return act;
				}
				
			}
			
		}
		for (EObject obj : children) {
			res = this.findReceiveActivity(obj, actName);
			if (res != null) {
				return res;
			}
		}
		return res;
	}
	
	/**
	 * 
	 * @param scopeName
	 * @param participantSetPBDNames
	 * @return return the index next to scope name. if scope name is Scope_1,
	 *         the nreturns 1. If Scope name does not contain any of PBD names,
	 *         then return -1.
	 */
	public int checkScopeActivityNameVSParticipantSetPBDNames(String scopeName, ArrayList<String> participantSetPBDNames) {
		for (int i = 0; i < participantSetPBDNames.size(); i++) {
			String pbdName = participantSetPBDNames.get(i).substring(0, participantSetPBDNames.get(i).indexOf(","));
			if (scopeName.contains(pbdName)) {
				return i;
			}
		}
		return -1;
	}
}
