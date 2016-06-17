package org.bpel4chor.mergechoreography.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Activity;

/**
 * Data Class for holding all data from surrounding activities of a
 * {@link MessageLink}
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class MLEnvironment implements Serializable {
	
	private static final long serialVersionUID = 7072180475599257326L;
	
	/** List of activities preceding the invoke on the sender side */
	private List<Activity> preS = new ArrayList<>();
	/** The send activity */
	private Activity s;
	/** List of activities succeeding the invoke on the sender side */
	private List<Activity> succS = new ArrayList<>();
	
	/** List of activities preceding the receive on the receiver side */
	private List<Activity> preR = new ArrayList<>();
	/** The receive activity */
	private Activity r;
	/** List of activities succeeding the receive on the receiver side */
	private List<Activity> succR = new ArrayList<>();
	
	
	public List<Activity> getPreS() {
		return this.preS;
	}
	
	public Activity getS() {
		return this.s;
	}
	
	public void setS(Activity s) {
		this.s = s;
	}
	
	public List<Activity> getSuccS() {
		return this.succS;
	}
	
	public List<Activity> getPreR() {
		return this.preR;
	}
	
	public Activity getR() {
		return this.r;
	}
	
	public void setR(Activity r) {
		this.r = r;
	}
	
	public List<Activity> getSuccR() {
		return this.succR;
	}
	
}
