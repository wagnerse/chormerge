package org.bpel4chor.mergechoreography;

import java.io.Serializable;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.matcher.communication.CommunicationMatcher;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.MergePostProcessor;
import org.bpel4chor.mergechoreography.util.MergePreProcessor;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;

/**
 * The BPEL Choreography Merger read in a BPEL4Chor Choreography, merges the
 * processes and then generates BPEL and the associated WSDL Files.
 * 
 * @since Aug 1, 2012
 * @author Peter Debicki
 */
@SuppressWarnings("restriction")
public class ChoreographyMerger implements Serializable {
	
	private static final long serialVersionUID = -6483525635701914879L;
	
	/** The Choreography Package */
	private ChoreographyPackage choreographyPackage;
	public ChoreographyMergerExtension choreographyMergerExtension;
	public ChoreographyPackageExtension choreographyPackageExtension;
	
	protected Logger log = Logger.getLogger(this.getClass().getPackage().getName());
	
	static {
		// setup the extension to factory map, so that the proper
		// ResourceFactory can be used to read the file.
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("pbd", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
		
	}
	
	
	/**
	 * Constructor for ChoreographyMerger
	 * 
	 * @param fileName The Zip-File containing the BPEL4Chor-Choreography
	 */
	public ChoreographyMerger(String fileName) {
		
		// Here we go the read in Choreo
		this.choreographyPackage = new ChoreographyPackage(fileName);
		
		/**
		 * TODO Added code here
		 */
		this.choreographyMergerExtension = new ChoreographyMergerExtension(this.choreographyPackage, this.log);
		this.choreographyPackageExtension = new ChoreographyPackageExtension(this.choreographyPackage);
		this.choreographyPackage.setChoreographyPackageExtension(this.choreographyPackageExtension);
		this.choreographyPackage.setChoreographyMergerExtension(this.choreographyMergerExtension);
	}
	
	/**
	 * The main merge method, returns new merged BPEL Process (executable not
	 * abstract!) made from the given choreography
	 * 
	 * @param fileName The name under which the new Process.zip should be saved
	 * @return ZipFile containing the new merged BPEL Process incl. WSDLs
	 */
	public ZipFile merge(String fileName) {
		// PreProcessing PBDs (alternative invokes)
		MergePreProcessor.startPreProcessing(this.choreographyPackage);
		// Create New initial executable BPEL Process and
		// copy all vars and activities into it
		this.mergeChoreography();
		// PostProcessing of the merged process
		MergePostProcessor.startPostProcessing(this.choreographyPackage);
		this.choreographyPackage.saveMergedChoreography(fileName);
		
		ZipFile temp = null;
		return temp;
	}
	
	/**
	 * Method for initializing and merging the new merged BPEL process
	 * 
	 */
	private void mergeChoreography() {
		// Iinitialize the merged BPEL Process
		this.choreographyPackage.initMergedProcess();
		
		// Now check the MessageLinks and merge
		CommunicationMatcher matcher = new CommunicationMatcher();
		for (MessageLink link : this.choreographyPackage.getTopology().getMessageLinks()) {
			if (!this.choreographyPackage.isLinkVisited(link)) {
				MergePattern pattern = matcher.match(link, this.choreographyPackage);
				// if pattern==null we have a reply-Link or a
				// Non-Mergeable-Message-Link
				if (pattern != null) {
					pattern.merge();
				}
			}
		}
		
		// After merging configure remaining communication activities from
		// NMML
		
		this.choreographyMergerExtension.configureNMMLActivities2();
		if (this.choreographyMergerExtension.createdNewPartnerLinksForNMML) {
			this.choreographyMergerExtension.copyNewPartnerLinksOfNMMLToForEachScope();
		}
		this.choreographyMergerExtension.handleSeveralCreateInstanceYesCases();
		this.choreographyMergerExtension.updateScopeNames();
		this.choreographyMergerExtension.addNewScopesToMap();
		this.choreographyMergerExtension.configureNMMLActivities2();
		if (this.choreographyMergerExtension.createdNewPartnerLinksForNMML) {
			this.choreographyMergerExtension.copyNewPartnerLinksOfNMMLToForEachScope();
		}
		
		try {
			this.choreographyMergerExtension.updateScopeNames();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		// After merging configure remaining communication activities from NMML
		this.choreographyMergerExtension.performLoopFragmentation();
	}
	
	/**
	 * Get {@link ChoreographyPackage}
	 * 
	 * @return {@link ChoreographyPackage}
	 */
	public ChoreographyPackage getChoreographyPackage() {
		return this.choreographyPackage;
	}
	
}
