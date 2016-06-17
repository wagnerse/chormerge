package org.bpel4chor.mergechoreography.test.fh;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.bpel4chor.mergechoreography.ChoreographyMerger;
import org.bpel4chor.mergechoreography.test.util.Constants;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for ChoreographyMergerFhTest.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
@SuppressWarnings("unused")
public class ChoreographyMergerFhTest {
	
	// path to zip file, new dir name
	private static Map<String, String> patterns = new HashMap<>();
	
	private static String outputPath = Constants.mergeOutputPath;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
//		 ############# Generate all files.
		 initAllFiles();
//		 ############# Generate single file.
//		initSingleFile(8);
	}
	
	private static void initSingleFile(int change) {
		patterns.put(Constants.pathToFhZipFiles + File.separator + "Szenario" + change + ".zip", "Szenario" + change);
	}
	
	private static void initAllFiles() {
		// reads all zips and put it into patterns
		File[] fileList = new File(Constants.pathToFhZipFiles).listFiles();
		for (File f : fileList)
			patterns.put(f.getAbsolutePath(), f.getName().substring(0, f.getName().length() - 4));
	}
	
	@Test
	public void test() {
		ChoreographyMerger choreographyMerger = null;
		for (Map.Entry<String, String> entry : patterns.entrySet()) {
			// create directory
			(new File(outputPath + File.separator + entry.getValue())).mkdir();
			System.out.println(entry.getValue());
			// start merge
			choreographyMerger = new ChoreographyMerger(entry.getKey());
			choreographyMerger.merge(outputPath + File.separator + entry.getValue() + File.separator);
		}
	}
}
