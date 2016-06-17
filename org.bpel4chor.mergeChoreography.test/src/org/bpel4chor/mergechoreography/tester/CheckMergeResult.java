package org.bpel4chor.mergechoreography.tester;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.bpel4chor.utils.BPEL4ChorConstants;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * This class contains logic to compare two {@link Process}es. All
 * {@link Activity} names will be checked with Java Regex. If any name or
 * {@link Process} sequence do not match it will return false.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class CheckMergeResult {
	
	private static Logger log = Logger.getLogger(CheckMergeResult.class);
	
	/**
	 * Path to process merged after consolidation.
	 */
	private Path processMerged = null;
	/**
	 * Path to check file.
	 */
	private Path processCheck = null;
	
	private Process pMerged = null;
	private Process pCheck = null;
	
	
	/**
	 * Constructor
	 * 
	 * @param processMerged
	 * @param processCheck
	 */
	public CheckMergeResult(Path processMerged, Path processCheck) {
		this.processMerged = processMerged;
		this.processCheck = processCheck;
	}
	
	/**
	 * Checks if processMerged is equal processCheck.
	 * 
	 * @return true if its correct<br>
	 *         false otherwise
	 */
	public boolean isProcessCorrect() {
		initObjects();
		return checkProcesses();
	}
	
	/**
	 * Checks if the process was correct generated.
	 */
	public boolean checkProcesses() {
		boolean result = true;
		// check if flow MergedFlow is in process
		if (!pMerged.getActivity().getName().equals("MergedFlow")) {
			log.info("ProcessMerged lacks Flow 'MergedFlow'.");
			result = false;
		}
		if (!pCheck.getActivity().getName().equals("MergedFlow")) {
			throw new CheckMergeException("Process file 'CheckMerged' lacks Flow 'MergedFlow': " + processCheck.getFileName());
		}
		TreeIterator<?> iteratorMP = pMerged.getActivity().eAllContents();
		TreeIterator<?> iteratorCP = pCheck.getActivity().eAllContents();
		Object oElementMP = null;
		Object oElementCP = null;
		// trace the given tree
		while (iteratorCP.hasNext() && iteratorMP.hasNext() && result) {
			oElementCP = iteratorCP.next();
			oElementMP = iteratorMP.next();
			if (oElementCP instanceof Scope)
				log.info("scope");
			// check if class are equal
			if (oElementCP.getClass().equals(oElementMP.getClass())) {
				result = checkActivityConditions(oElementMP, oElementCP);
			} else {
				// prune until iteratorCP and iteratorMP are equal
				iteratorMP.prune();
				nextIterator: while (iteratorMP.hasNext()) {
					oElementMP = iteratorMP.next();
					if (oElementCP.getClass().equals(oElementMP.getClass())) {
						break nextIterator;
					}
					iteratorMP.prune();
				}
				// check if are equal or if iteratorMP do not have anymore
				// elements
				if (!oElementCP.getClass().equals(oElementMP.getClass())) {
					result = false;
					// print wrong Assertion
					printReverseTrace(((EObject) oElementCP));
					break;
				} else if (result) {
					result = checkActivityConditions(oElementMP, oElementCP);
				}
			}
		}
		return result;
	}
	
	/**
	 * Checks if some conditions are correct.
	 * 
	 * @param oElementMP
	 * @param oElementCP
	 * @return
	 */
	private boolean checkActivityConditions(Object oElementMP, Object oElementCP) {
		boolean result = true;
		if (oElementMP instanceof Activity && oElementCP instanceof Activity) {
			Activity actMP = (Activity) oElementMP;
			Activity actCP = (Activity) oElementCP;
			
			// check name of Activity
			if (actCP.getName() != null && !actCP.getName().isEmpty()) {
				// name is null
				if (actMP.getName() == null) {
					log.info("Wrong Activity name. Expected: " + actCP.getName() + " Got: null");
					result = false;
				}
				Pattern pattern = Pattern.compile(actCP.getName());
				Matcher matcher = pattern.matcher(actMP.getName());
				// if no matches found stop
				if (!matcher.matches()) {
					log.info("No name matches. Expected: " + actCP.getName());
					result = false;
				}
			}
		}
		return result;
	}
	
	/**
	 * Prints the reverse order of the given object until process
	 * 
	 * @param eOjbect
	 */
	private void printReverseTrace(EObject eOjbect) {
		StringBuffer sb = new StringBuffer();
		EObject container = null;
		sb.append(eOjbect.getClass().getSimpleName() + " <- ");
		container = eOjbect.eContainer();
		Activity act = null;
		while (container != null && !(pCheck.equals(container))) {
			if (container instanceof Activity) {
				act = (Activity) container;
				if (act.getName() != null)
					sb.append(container.getClass().getSimpleName() + ":" + act.getName() + " <- ");
			} else {
				sb.append(container.getClass().getSimpleName() + " <- ");
			}
			container = container.eContainer();
		}
		sb.append("ProcessMerged");
		log.info("There is no match in close environment:");
		log.info(sb.toString());
	}
	
	/**
	 * Read files and initialize process objects.
	 */
	private void initObjects() {
		this.pMerged = loadProcess(this.processMerged);
		this.pCheck = loadProcess(this.processCheck);
	}
	
	/**
	 * Load EMF process objects.
	 * 
	 * @param file that should be loaded
	 * @return
	 */
	private Process loadProcess(Path file) {
		Process result = null;
		try {
			InputStream stream = Files.newInputStream(file);
			ResourceSet resourceSet = new ResourceSetImpl();
			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
			URI uri = URI.createURI(file.toUri().toString());
			BPELResource resource = (BPELResource) resourceSet.createResource(uri);
			result = BPEL4ChorReader.readBPEL(resource, stream);
			// Set Abstract Process profile !!
			result.setAbstractProcessProfile(BPEL4ChorConstants.PBD_ABSTRACT_PROCESS_PROFILE);
			log.info(file.getFileName() + " loaded.");
		} catch (Exception e) {
			log.error(e);
		}
		return result;
	}
}
