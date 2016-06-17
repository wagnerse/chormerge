package org.bpel4chor.mergechoreography.util;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELExtensibleElement;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.PartnerActivity;
import org.eclipse.bpel.model.Receive;

/**
 * 
 * Analyzer Class for determining the surrounding activities of a
 * {@link MessageLink}
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class MLEnvironmentAnalyzer implements Serializable {
	
	private static final long serialVersionUID = -8831266707756914781L;
	
	/**
	 * The concerned MessageLink(s), max. 2 included in case of sync
	 * communication
	 */
	protected MessageLink mLink = null;
	/** The ChoreographyPackage holding all data */
	protected ChoreographyPackage pkg;
	
	protected Logger logger = Logger.getLogger(this.getClass().getPackage().getName());
	
	
	public MLEnvironmentAnalyzer(MessageLink link, ChoreographyPackage pkg) {
		super();
		this.mLink = link;
		this.pkg = pkg;
	}
	
	/**
	 * Returns the {@link MLEnvironment} of the given {@link MessageLink}
	 * 
	 * @return {@link MLEnvironment}
	 */
	public MLEnvironment getEnvironment() {
		
		MLEnvironment environment = new MLEnvironment();
		
		PartnerActivity s = null;
		Activity r = null;
		
		// get send- and receive-activity
		this.logger.info("ml.SendActivity => " + this.mLink.getSendActivity());
		this.logger.info("pkg => " + this.pkg);
		this.logger.info("ml.ReceiveActivity => " + this.mLink.getReceiveActivity());
		
		// Get sendActivity from mLink
		s = (PartnerActivity) ChoreoMergeUtil.resolveActivity(this.mLink.getSendActivity());
		
		// Get receiveActivity from mLink
		BPELExtensibleElement recAct = ChoreoMergeUtil.resolveActivity(this.mLink.getReceiveActivity());
		if ((recAct instanceof Receive) || (recAct instanceof Invoke)) {
			r = (Activity) recAct;
		} else if (recAct instanceof OnMessage) {
			// We need to get the <pick> containing the <onMessage>
			r = (Activity) recAct.eContainer();
		}
		
		environment.setS(s);
		environment.setR(r);
		
		// Analyze environment before invoke
		List<Activity> beforeInv = ChoreoMergeUtil.getPreceedingActivities(s);
		if (beforeInv != null) {
			environment.getPreS().addAll(beforeInv);
		}
		
		List<Activity> afterInv = ChoreoMergeUtil.getSucceedingActivities(s);
		if (afterInv != null) {
			environment.getSuccS().addAll(afterInv);
		}
		
		// Analyze environment before receive
		List<Activity> beforeRec = ChoreoMergeUtil.getPreceedingActivities(r);
		if (beforeRec != null) {
			environment.getPreR().addAll(beforeRec);
		}
		
		// Analyze environment after receive
		List<Activity> afterRec = ChoreoMergeUtil.getSucceedingActivities(r);
		if (afterRec != null) {
			environment.getSuccR().addAll(afterRec);
		}
		
		// generate output for debug
		this.logger.info("MLEnvorinmentAnalyzer for ml => " + this.mLink.getName());
		this.logger.info("PreS => " + ChoreoMergeUtil.getTextOfList(environment.getPreS()));
		this.logger.info("s => " + s.getName());
		this.logger.info("SuccS => " + ChoreoMergeUtil.getTextOfList(environment.getSuccS()));
		this.logger.info("PreR => " + ChoreoMergeUtil.getTextOfList(environment.getPreR()));
		this.logger.info("r => " + r.getName());
		this.logger.info("SuccR => " + ChoreoMergeUtil.getTextOfList(environment.getSuccR()));
		
		return environment;
	}
}
