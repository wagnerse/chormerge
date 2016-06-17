package org.bpel4chor.mergechoreography;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bpel4chor.mergechoreography.util.PBDFragmentDuplicator;
import org.bpel4chor.model.topology.impl.Participant;
import org.bpel4chor.model.topology.impl.ParticipantSet;
import org.eclipse.bpel.model.ForEach;
import org.eclipse.bpel.model.Process;
import org.eclipse.emf.ecore.EObject;

public class ChoreographyPackageExtension {
	public ChoreographyMergerExtension choreographyMergerExtension;
	ChoreographyPackage choreographyPackage;

	public ChoreographyPackageExtension(ChoreographyPackage choreographyPackage) {
		this.choreographyPackage = choreographyPackage;
	}

	/**
	 * This function copies same variables and Activities to similar scopes in
	 * merged process. Similar scopes are the scopes which has same scope name ,
	 * but different scope IDs. Scope_1 and Scop_2 are similar scopes, and
	 * created during MIP static/dynamic MIP instantiation.
	 */
	public void handleSameParticipantTypeParticipants() {
		Map<String, Integer> participantCountMap = new HashMap<String, Integer>();
		updateParticipantCounts(participantCountMap);
		for (Process process : choreographyPackage.getPbds()) {
			int numOfParticipants = 0;
			if (participantCountMap.containsKey(process.getName()))
				numOfParticipants = participantCountMap.get(process.getName());
			if (choreographyMergerExtension.countInvokesNOTInForeach != 0)
				numOfParticipants += (choreographyMergerExtension.countInvokesNOTInForeach - 1);
			for (int i = 0; i < numOfParticipants; i++) {
				PBDFragmentDuplicator.copyVarsAndActitivies(process);
			}

		}

	}

	/**
	 * Update number of participants in hash map by counting number of
	 * participants in topology artifact.
	 * 
	 * @param participantCountMap
	 */
	public void updateParticipantCounts(Map<String, Integer> participantCountMap) {
		List<Participant> participants = choreographyPackage.getTopology()
				.getParticipants();
		int participantCount = 0;
		for (int i = 0; i < participants.size(); i++) {

			if (participantCountMap.containsKey(participants.get(i).getType()))
				continue;
			for (int j = 0; j < participants.size(); j++) {
				if (participants.get(i).getType()
						.equals(participants.get(j).getType())) {
					participantCount += 1;
				}
			}
			participantCountMap.put(
					getPBDDescriptionNameFromParticipantType(participants
							.get(i).getType()), participantCount);
			participantCount = 0;
		}

	}

	/**
	 * 
	 * Get PBD name from participant type in topology artifact
	 * 
	 * @param participantType
	 * @return
	 */
	public String getPBDDescriptionNameFromParticipantType(
			String participantType) {
		if (participantType == null || participantType.length() == 0) {
			return null;
		}

		for (int i = 0; i < choreographyPackage.getTopology()
				.getParticipantTypes().size(); i++) {
			if (participantType.equals(choreographyPackage.getTopology()
					.getParticipantTypes().get(i).getName())) {
				return choreographyPackage.getTopology().getParticipantTypes()
						.get(i).getParticipantBehaviorDescription()
						.getLocalPart();
			}
		}
		return null;

	}

	/**
	 * Get PBD namefrom participant name in topology artifact
	 * 
	 * @param participantName
	 * @return
	 */
	public String getPBDDescriptionNameFromParticipantName(
			String participantName) {
		if (participantName == null || participantName.length() == 0) {
			return null;
		}

		List<Participant> participants = new ArrayList<Participant>();
		participants
				.addAll(choreographyPackage.getTopology().getParticipants());
		String participantType = "";
		for (int i = 0; i < choreographyPackage.getTopology()
				.getParticipantSets().size(); i++) {
			ParticipantSet pSet = choreographyPackage.getTopology()
					.getParticipantSets().get(i);
			participants.addAll(pSet.getParticipantList());

			int index = 0;

			while (pSet.getParticipantSetList() != null
					&& index < pSet.getParticipantSetList().size()) {
				participants.addAll(pSet.getParticipantSetList().get(index)
						.getParticipantList());
				index += 1;
			}

		}

		for (int i = 0; i < participants.size(); i++) {
			if (participants.get(i).getName().equals(participantName)) {
				participantType = participants.get(i).getType();
			}
		}
		if (participantType.length() > 0) {
			return getPBDDescriptionNameFromParticipantType(participantType);
		}
		return null;

	}

	private static int checkIndex = -2;

	/**
	 * This function handles MIP Instantiation scenario when there is
	 * participantSet or not.
	 */
	public void handleParticipantSetMerge() {
		// Check if there exist participatnSet in topology artifact
		if (choreographyPackage.getTopology().getParticipantSets().size() == 0) {
			/**
			 * 
			 * There is no participantSet in topology.xml, so just copy all
			 * process fragments to separate scopes into the new merged Process
			 * flow
			 */
			handleSameParticipantTypeParticipants();

		} else {
			/**
			 * There exists participantSet in topology artifact. Check whether
			 * this is dynamic or Static MIP instantiation and follow
			 * corresponding step
			 * 
			 */

			ArrayList<String> participantSetPBDNames = getParticipantSetPBDNames();

			for (Process process : choreographyPackage.getPbds()) {
				checkIndex = PBDisMemberOfParticipantSet(process.getName(),
						participantSetPBDNames);
				if (checkIndex == -1) {

					handleSameParticipantTypeParticipants();
				} else {

					/**
					 * TODO
					 * 
					 * Not part of this thesis, can be modified later
					 * 
					 * If some forEach resides inside another forEach we don't
					 * need to create separate container for nested forEach
					 * 
					 * So remove the forEach residing inside of another forEach
					 * from next operations
					 * 
					 * 
					 */
					for (ForEach curForEach : PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
							.getProcessForEachMap().values()) {

						if (checkParentIsForEach(curForEach)) {
							for (String forEachName : PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
									.getProcessForEachMap().keySet()) {

								ForEach forEach = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
										.getProcessForEachMap()
										.get(forEachName);
								if (forEach == curForEach) {
									PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
											.getProcessForEachMap().remove(
													forEachName);
									for (int i = 0; i < participantSetPBDNames
											.size(); i++) {
										if (participantSetPBDNames.get(i)
												.contains(curForEach.getName())) {
											participantSetPBDNames
													.remove(participantSetPBDNames
															.get(i));
											break;
										}
									}

								}

							}

						}

					}

					do {
						try {

							if (isInteger(PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
									.getProcessForEachMap()
									.get(participantSetPBDNames.get(checkIndex)
											.substring(
													participantSetPBDNames.get(
															checkIndex)
															.indexOf(",") + 1))
									.getFinalCounterValue().getBody()
									.toString())) {
								/**
								 * Static MIP instantiation
								 * 
								 * If finalCounterValue is number/integer, then
								 * this is Static MIP instantiation
								 * 
								 * If there are several participants of the same
								 * type, then also Static MIP Instantiation
								 * 
								 */

								ForEach currentForeach = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
										.getProcessForEachMap()
										.get(participantSetPBDNames.get(
												checkIndex).substring(
												participantSetPBDNames.get(
														checkIndex)
														.indexOf(",") + 1));
								int upperLimit = Integer
										.parseInt(currentForeach
												.getFinalCounterValue()
												.getBody().toString());

								for (int k = 0; k < upperLimit; k++) {
									PBDFragmentDuplicator
											.copyVarsAndActitivies(process);
								}
							} else {
								/**
								 * Dynamic MIP Instantiation
								 * 
								 * 
								 * FinalCounterValue is variable (doesn't have
								 * number value), -> then this is Dynamic MIP
								 * Instantiation
								 * 
								 */

								ForEach currentForEach = PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
										.getProcessForEachMap()
										.get(participantSetPBDNames.get(
												checkIndex).substring(
												participantSetPBDNames.get(
														checkIndex)
														.indexOf(",") + 1));
								int upperLimit = 1;
								for (int i = 0; i < upperLimit; i++)
									PBDFragmentDuplicator.pbdFragmentDuplicatorExtension
											.copyVarsAndActitiviesDynamicMIP(
													process, currentForEach);

							}
						} catch (Exception e) {
							System.out.println(e.getMessage());
							e.printStackTrace();
						}
						// CheckIndex is helper variable for keeping track of
						// processed PBDs
						checkIndex = PBDisMemberOfParticipantSet(
								process.getName(), participantSetPBDNames);
					} while (checkIndex != -1);
				}
			}

		}

	}

	/**
	 * 
	 * Check whether given forEach's parent is also forEach activity
	 * 
	 * @param currentForEach
	 * @return
	 */
	public static boolean checkParentIsForEach(EObject currentForEach) {

		EObject parent = null;
		if (currentForEach != null)
			parent = currentForEach.eContainer();

		if (parent instanceof ForEach) {
			System.out.println("Parent: " + ((ForEach) parent).getName());
			return true;
		} else if (parent != null) {
			return checkParentIsForEach(parent);
		}

		return false;
	}

	/**
	 * Get first forEach parent
	 * 
	 * @param currentForEach
	 * @return
	 */
	public static ForEach getFirstForEachParent(EObject currentForEach) {

		EObject parent = null;
		if (currentForEach != null)
			parent = currentForEach.eContainer();

		if (parent instanceof ForEach) {

			return (ForEach) parent;
		} else if (parent != null) {
			return (ForEach) getFirstForEachParent(parent);
		}

		return null;
	}

	/**
	 * 
	 * Get all the pbd names which are described in participantSet in topology
	 * artifact
	 * 
	 * @return
	 */
	public ArrayList<String> getParticipantSetPBDNames() {
		ArrayList<String> participantSetPBDNames = new ArrayList<String>();
		for (int i = 0; i < choreographyPackage.getTopology()
				.getParticipantSets().size(); i++) {
			ParticipantSet ps = choreographyPackage.getTopology()
					.getParticipantSets().get(i);

			String psType = ps.getType();
			String participantPBDName = "";
			for (int j = 0; j < choreographyPackage.getTopology()
					.getParticipantTypes().size(); j++) {
				String participantTypeName = choreographyPackage.getTopology()
						.getParticipantTypes().get(i).getName();
				if (participantTypeName.equals(psType)) {
					participantPBDName = choreographyPackage.getTopology()
							.getParticipantTypes().get(i)
							.getParticipantBehaviorDescription().getLocalPart();
					break;

				}

			}

			for (int j = 0; j < ps.getForEach().size(); j++) {
				participantSetPBDNames.add(participantPBDName + ","
						+ ps.getForEach().get(j).getLocalPart());//

			}

		}
		return participantSetPBDNames;
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
	 * Check whether given pbd is member of participatnSet
	 * 
	 * @param pbdName
	 * @param participantSetPBDNames
	 * @return
	 */
	public static int PBDisMemberOfParticipantSet(String pbdName,
			ArrayList<String> participantSetPBDNames) {
		if (participantSetPBDNames.size() == 0)
			return -1;

		for (int i = 0; i < participantSetPBDNames.size(); i++) {
			if (pbdName.equals(participantSetPBDNames.get(i).substring(0,
					participantSetPBDNames.get(i).indexOf(","))))
				if (i > checkIndex)
					return i;
		}
		// checkIndex = -1;
		return -1;
	}

	/**
	 * Find the PBD with the given Name
	 * 
	 * @param pbdName
	 *            The name of the searched PBD
	 * @return pbd or null
	 */
	public Process getPBDByName(String pbdName) {
		for (Process pbd : choreographyPackage.getPbds()) {
			if (pbd.getName().equals(pbdName)) {
				return pbd;
			}
		}
		return null;
	}
}
