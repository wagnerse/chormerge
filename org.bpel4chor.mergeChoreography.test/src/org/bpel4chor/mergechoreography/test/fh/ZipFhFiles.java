package org.bpel4chor.mergechoreography.test.fh;

import java.io.IOException;

import org.bpel4chor.mergechoreography.test.util.Constants;
import org.junit.Test;

import de.uni_stuttgart.iaas.bpel.model.utilities.ZipUtil;

/**
 * 
 * @author Peter Berger
 * 
 */
public class ZipFhFiles {

	@Test
	public void testZipFiles() throws IOException {
		for (int i = 17; i < 18; i++)
			ZipUtil.zip(Constants.pathToFhSzenarioBpel4chor.replace("?", "" + i), Constants.pathToFhZipFiles,
					"Szenario" + i + ".zip");
	}
}
