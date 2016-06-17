package org.bpel4chor.mergechoreography.exceptions;

/**
 * Exception from reading in zipfile containing the choreography
 * 
 * @since Aug 14, 2012
 * @author Peter Debicki
 */
public class IncompleteZipFileException extends Exception {
	
	private static final long serialVersionUID = -1499313122894972031L;
	
	
	public IncompleteZipFileException() {
		super();
	}
	
	public IncompleteZipFileException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public IncompleteZipFileException(String message) {
		super(message);
	}
	
	public IncompleteZipFileException(Throwable cause) {
		super(cause);
	}
}
