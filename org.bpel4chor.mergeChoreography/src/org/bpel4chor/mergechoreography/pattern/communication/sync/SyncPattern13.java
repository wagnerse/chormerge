package org.bpel4chor.mergechoreography.pattern.communication.sync;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Variable;

public class SyncPattern13 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected MLEnvironment envReply;
	
	
	public SyncPattern13(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
		super(envSend, pkg);
		this.envReply = envReply;
	}
	
	@Override
	public void merge() {
		
		Invoke s = (Invoke) this.env.getS();
		Receive rec = (Receive) this.env.getR();
		Reply repl = (Reply) this.envReply.getS();
		
		// Propagate possible correlationSet initializations
		ChoreoMergeUtil.propagateCorrelInit(s);
		ChoreoMergeUtil.propagateCorrelInit(rec);
		ChoreoMergeUtil.propagateCorrelInit(repl);
		
		// First we uplift vRec used by rec and vReply used by s into
		// the process<scope> of the merged process
		Variable vRec = ChoreoMergeUtil.resolveVariable(rec.getVariable().getName(), rec);
		this.log.info("vRec => " + vRec);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vRec, this.pkg.getMergedProcess());
		
		// Create new <assign> replacing the <invoke> s
		Assign newAssignS = ChoreoMergeUtil.createAssignFromSendAct(s, vRec);
		
		// Create new <empty> replacing <receive> rec
		Empty newEmptyRec = ChoreoMergeUtil.createEmptyFromActivity(rec);
		// Create new <empty> replacing the <reply> repl
		Empty newEmptyRepl = ChoreoMergeUtil.createEmptyFromActivity(repl);
		
		// Create new <link> in MergedFlow connection newAssignS and
		// newEmptyRec
		Link newLinkSend = BPELFactory.eINSTANCE.createLink();
		newLinkSend.setName(newAssignS.getName() + "TO" + newEmptyRec.getName());
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkSend);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkSend, newAssignS);
		
		// Combine joinCondition in newEmptyRec if needed
		ChoreoMergeUtil.combineJCWithLink(newEmptyRec, newLinkSend);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkSend, newEmptyRec);
		
		// // Replace old activities (s, rec, repl)
		ChoreoMergeUtil.replaceActivity(s, newAssignS);
		ChoreoMergeUtil.replaceActivity(rec, newEmptyRec);
		ChoreoMergeUtil.replaceActivity(repl, newEmptyRepl);
		
	}
}
