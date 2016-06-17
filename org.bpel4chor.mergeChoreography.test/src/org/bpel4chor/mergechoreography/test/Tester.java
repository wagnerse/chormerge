package org.bpel4chor.mergechoreography.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.utils.BPEL4ChorReader;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELPlugin;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Process;
import org.eclipse.bpel.model.resource.BPELResource;
import org.eclipse.bpel.model.resource.BPELResourceFactoryImpl;
import org.eclipse.bpel.model.resource.BPELWriter;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.wst.wsdl.internal.util.WSDLResourceFactoryImpl;
import org.eclipse.xsd.util.XSDResourceFactoryImpl;

import de.uni_stuttgart.iaas.bpel.model.utilities.MyBPELUtils;
import de.uni_stuttgart.iaas.bpel.model.utilities.Utility;

public class Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputPath = "E:\\Arbeit\\Diplom\\EclipseBPELWS\\MeineTestChoreos\\bpelContent\\EmptyOptimizer\\";
		Map<String, String> args2 = new HashMap<>();
		
//		args2.put("EmptyOpt1.bpel", outputPath);
//		args2.put("EmptyOptCase1.bpel", outputPath);
//		args2.put("EmptyOptCase2.bpel", outputPath);
		args2.put("EmptyOptCase3.bpel", outputPath);
		
		
		for (Map.Entry<String, String> entry : args2.entrySet()) {
			Process proc1 = BPEL4ChorReader.readBPEL(entry.getValue()+entry.getKey());
			
			System.out.println(proc1.getName());
			
			Activity eAct = MyBPELUtils.resolveActivity("Empty1", proc1);
			
			System.out.println(Utility.dumpEE(eAct));
			
			ChoreoMergeUtil.optimizeEmpty((Empty) eAct);
			
			
			try {
				print(proc1);
				writeBPEL(proc1, new FileOutputStream(new File(outputPath+"optimized"+entry.getKey())));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		
		
		

		

	}
	
	/**
	 * Print the BPEL process to console
	 * <p>
	 * Note that this method can only print out the processes that contain no
	 * WSDL imports.
	 * 
	 * @param process
	 * @throws IOException
	 */
	public static void print(Process process) throws IOException {
		// init
		BPELPlugin bpelPlugin = new BPELPlugin();
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("bpel", new BPELResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("wsdl", new WSDLResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xsd", new XSDResourceFactoryImpl());
		
		boolean useNSPrefix = false;
		ResourceSet resourceSet = new ResourceSetImpl();
		// "processName.bpel" is only a work-around to bypass the need of URI in
		// BPELResource
		URI uri = URI.createFileURI(process.eResource().getURI().toFileString());
		BPELResource resource = (BPELResource) resourceSet.createResource(uri);
		resource.setOptionUseNSPrefix(useNSPrefix);
		resource.getContents().add(process);
		
		Map argsMap = new HashMap();
		// this args map prevents a NPE in the release 1.0
		argsMap.put("", "");
		resource.save(System.out, argsMap);
	}
	
	/**
	 * Write the BPEL process to BPEL file using BPELResource
	 * 
	 * @param process
	 * @param outputStream
	 * @throws IOException
	 */
	public static void writeBPEL(Process process, OutputStream outputStream) throws IOException {
		
		//
		// BPELResource is just needed for providing bpel process to the
		// BPEL/PBD writer, the URI is not used, we use "any.bpel" to bypass the
		// Exception because of no URI presents.
		//
		boolean useNSPrefix = false;
		ResourceSet resourceSet = new ResourceSetImpl();
		URI uri = URI.createFileURI(process.eResource().getURI().toFileString());
		BPELResource resource = (BPELResource) resourceSet.createResource(uri);
		resource.setOptionUseNSPrefix(useNSPrefix);
		resource.getContents().add(process);
		
		BPELWriter writer = new BPELWriter();
		Map args = new HashMap();
		
		args.put("", "");
		writer.write(resource, outputStream, args);
	}

}
