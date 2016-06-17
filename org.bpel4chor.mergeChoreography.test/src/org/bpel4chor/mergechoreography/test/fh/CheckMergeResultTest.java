package org.bpel4chor.mergechoreography.test.fh;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;

import org.bpel4chor.mergechoreography.test.util.Constants;
import org.bpel4chor.mergechoreography.tester.CheckMergeResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for CheckMergeResult.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class CheckMergeResultTest {
	private int szenario = 17;
	private String pathToMergedProcess = "E:\\TestResult\\Szenario"+szenario+"\\ProcessMerged.bpel";
	private String pathToCheckProcess = Constants.pathToCheckFiles + File.separator + "Szenario"+szenario+".bpel";
	
	
	@Test
	public void checkMergeResult() {
		FileSystem fs = FileSystems.getDefault();
		boolean result = new CheckMergeResult(fs.getPath(pathToMergedProcess), fs.getPath(pathToCheckProcess)).isProcessCorrect();
		Assert.assertTrue(result);
	}
}
