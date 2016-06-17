package org.bpel4chor.mergechoreography.pattern.communication.async;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.model.topology.impl.MessageLink;
import org.bpel4chor.utils.WSUIDGenerator;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.If;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.OnMessage;
import org.eclipse.bpel.model.Pick;
import org.eclipse.bpel.model.Scope;
import org.eclipse.bpel.model.Throw;
import org.eclipse.bpel.model.Variable;

/**
 * Pattern for Merging Simple Invoke
 * 
 * Copyright 2012 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class AsyncPattern23 extends MergePattern {
	
	/** List of {@link MessageLink}s */
	private List<MessageLink> mLinks = new ArrayList<>();
	
	/**
	 * List of Information about the analyzed environment of the
	 * {@link MessageLink}s
	 */
	private List<MLEnvironment> linkEnvs = new ArrayList<>();
	
	
	public AsyncPattern23(MLEnvironment env, ChoreographyPackage pkg) {
		super(env, pkg);
	}
	
	@Override
	public void merge() {
		// Invoke s = (Invoke) this.env.getS();
		Pick r = (Pick) this.env.getR();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(r);
		
		// List of already uplifted variables
		List<Variable> uplifted = new ArrayList<>();
		
		// List of <onMessage>-branches used for choreography-internal
		// communication
		Map<Invoke, OnMessage> omsInternal = this.getInternalOnMessages();
		
		// Now we create a new guard variable vR_activated in process<scope> of
		// the merged process and initialize it inline with false()
		Variable guard = ChoreoMergeUtil.createGuardVariableInMergedProcess(r.getName() + WSUIDGenerator.getId() + "_activated");
		
		// create <scope> replacing r
		Scope newScope = ChoreoMergeUtil.createScopeFromPickWithoutOM(r, new ArrayList<>(omsInternal.values()), guard);
		
		for (Map.Entry<Invoke, OnMessage> oms : omsInternal.entrySet()) {
			OnMessage rOM = oms.getValue();
			Invoke s = oms.getKey();
			
			// Propagate possible correlationSet initializations
			ChoreoMergeUtil.propagateCorrelInit(s);
			
			// First we uplift the vR used by rOM into the process<scope> of the
			// merged process
			Variable vR = ChoreoMergeUtil.resolveVariable(rOM.getVariable().getName(), r);
			this.log.info("vR => " + vR);
			if (!uplifted.contains(vR)) {
				ChoreoMergeUtil.upliftVariableToProcessScope(vR, this.pkg.getMergedProcess());
				uplifted.add(vR);
			}
			
			// Now we create <if>s for every sending MessageLink with an
			// <assign> A with
			// <from>true()<from><to>vR_Activivated<from>vS</from><to>vR</>
			// as long vR_activated=false(). A is connected with a link to a
			// <throw> for ns:pickFault1 inside the <flow> of scpPick.
			If newIf = ChoreoMergeUtil.createIfFromInvoke(s, guard, vR);
			
			// Get <assign> inside newIf
			Assign newAssign = (Assign) newIf.getActivity();
			
			ChoreoMergeUtil.replaceActivity(s, newIf);
			
			// create new <throw> for receiving <onMessage> rOM
			// create <throw>
			QName qName = new QName(this.pkg.getMergedProcess().getTargetNamespace(), ChoreoMergeUtil.resolveWSU_ID(rOM) + "Fault");
			Throw newThrow = ChoreoMergeUtil.createThrowForUserDefinedFault(qName);
			
			// Add newThrow to <flow> inside newScope
			((Flow) newScope.getActivity()).getActivities().add(newThrow);
			
			// Create new <link> in MergedFlow connecting <assign> in newIf and
			// newThrow
			Link newLink = BPELFactory.eINSTANCE.createLink();
			newLink.setName(newIf.getName() + "TO" + newScope.getName());
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
			ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssign);
			ChoreoMergeUtil.createTarget4LinkInActivity(newLink, newThrow);
			
		}
		
		ChoreoMergeUtil.replaceActivity(r, newScope);
		
	}
	
	/**
	 * Get {@link List} of choreography-internally communicating
	 * {@link OnMessage}-branches
	 * 
	 * @return {@link List} of {@link OnMessage}-branches
	 */
	private Map<Invoke, OnMessage> getInternalOnMessages() {
		Map<Invoke, OnMessage> omsInternal = new HashMap<Invoke, OnMessage>();
		for (MessageLink mLink : this.mLinks) {
			OnMessage rOM = (OnMessage) ChoreoMergeUtil.resolveActivity(mLink.getReceiveActivity());
			Invoke s = (Invoke) ChoreoMergeUtil.resolveActivity(mLink.getSendActivity());
			omsInternal.put(s, rOM);
		}
		
		return omsInternal;
	}
	
	/**
	 * Add new {@link MessageLink} to succLinks
	 * 
	 * @param link new {@link MessageLink}
	 */
	public void addMessageLink(MessageLink link) {
		this.mLinks.add(link);
	}
	
	/**
	 * Add new {@link MLEnvironment} to succEnvs
	 * 
	 * @param mlEnv
	 */
	public void addMLinkEnv(MLEnvironment mlEnv) {
		this.linkEnvs.add(mlEnv);
	}
	
	public List<MessageLink> getmLinks() {
		return this.mLinks;
	}
	
	public List<MLEnvironment> getLinkEnvs() {
		return this.linkEnvs;
	}
	
}
