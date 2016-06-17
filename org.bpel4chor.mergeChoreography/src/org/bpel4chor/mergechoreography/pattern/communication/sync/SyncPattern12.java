package org.bpel4chor.mergechoreography.pattern.communication.sync;

import java.util.List;

import org.bpel4chor.mergechoreography.ChoreographyPackage;
import org.bpel4chor.mergechoreography.pattern.MergePattern;
import org.bpel4chor.mergechoreography.util.ChoreoMergeUtil;
import org.bpel4chor.mergechoreography.util.MLEnvironment;
import org.bpel4chor.utils.WSUIDGenerator;
import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.Assign;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Empty;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Invoke;
import org.eclipse.bpel.model.Link;
import org.eclipse.bpel.model.Receive;
import org.eclipse.bpel.model.Reply;
import org.eclipse.bpel.model.Sequence;
import org.eclipse.bpel.model.Source;
import org.eclipse.bpel.model.Target;
import org.eclipse.bpel.model.Variable;

public class SyncPattern12 extends MergePattern {
	
	/** The Information about the analyzed environment of the link */
	protected MLEnvironment envReply;
	
	
	public SyncPattern12(MLEnvironment envSend, MLEnvironment envReply, ChoreographyPackage pkg) {
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
		Variable vReply = ChoreoMergeUtil.resolveVariable(s.getOutputVariable().getName(), s);
		this.log.info("vRec => " + vRec);
		this.log.info("vReply => " + vReply);
		
		ChoreoMergeUtil.upliftVariableToProcessScope(vRec, this.pkg.getMergedProcess());
		ChoreoMergeUtil.upliftVariableToProcessScope(vReply, this.pkg.getMergedProcess());
		
		// Create new <assign> replacing the <invoke> s
		Assign newAssignS = ChoreoMergeUtil.createAssignFromSendAct(s, vRec);
		// Create new <sequence> surrounding newAssignS
		Sequence newSeq = BPELFactory.eINSTANCE.createSequence();
		// Set <targets> and <sources> newAssignS to newSeq
		newSeq.setSources(newAssignS.getSources());
		newSeq.setTargets(newAssignS.getTargets());
		// Create new <empty> after newAssignS for synchronization of the
		// responding link
		Empty newEmptyReply = BPELFactory.eINSTANCE.createEmpty();
		newSeq.getActivities().add(newAssignS);
		newSeq.getActivities().add(newEmptyReply);
		
		// TODO: examine <joinCondition>s in succ(r) !!
		// TODO: examine <transitionCondition>s of r for variables other than vR
		// Get all <sources> of r
		// CHECK: null
		if (rec.getSources() != null) {
			List<Source> sourcesOfR = rec.getSources().getChildren();
			
			// Remove every link from sourcesOfR from owning <flow>
			for (Source source : sourcesOfR) {
				ChoreoMergeUtil.removeLinkFromFlow(ChoreoMergeUtil.findLinkOwnerFlow(rec, source.getLink().getName()), source.getLink());
			}
			
			// Remove every <target> from every activity in succ(rec)
			for (Activity activity : this.env.getSuccR()) {
				for (Source source : sourcesOfR) {
					Target target = ChoreoMergeUtil.findTargetInActivity(activity, source.getLink().getName());
					if (target != null) {
						ChoreoMergeUtil.removeTargetFromActivity(activity, target);
					}
				}
			}
		}
		
		// Create new <source> for every activity in succ(rec)
		for (Activity activity : this.env.getSuccR()) {
			Link newLink = BPELFactory.eINSTANCE.createLink();
			newLink.setName(newAssignS.getName() + "TO" + activity.getName());
			
			// Add newLink to MergedFlow
			ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLink);
			
			// Add new <source> for newLink in newAssign
			ChoreoMergeUtil.createSource4LinkInActivity(newLink, newAssignS);
			
			// Add new <target> for newLink in activity
			ChoreoMergeUtil.createTarget4LinkInActivity(newLink, activity);
		}
		
		// Create new <assign> replacing the <reply> repl
		Assign newAssignRepl = ChoreoMergeUtil.createAssignFromSendAct(repl, vReply);
		
		// Create new <link> in MergedFlow connection newAssignRepl and
		// newEmptyReply
		Link newLinkReply = BPELFactory.eINSTANCE.createLink();
		newLinkReply.setName(newAssignRepl.getName() + "TO" + (newEmptyReply.getName() != null ? newEmptyReply.getName() : "Empty" + WSUIDGenerator.getId()));
		ChoreoMergeUtil.addLinkToFlow((Flow) this.pkg.getMergedProcess().getActivity(), newLinkReply);
		ChoreoMergeUtil.createSource4LinkInActivity(newLinkReply, newAssignRepl);
		ChoreoMergeUtil.createTarget4LinkInActivity(newLinkReply, newEmptyReply);
		
		// // Replace old activities (s, repl) and remove rec
		ChoreoMergeUtil.replaceActivity(s, newSeq);
		ChoreoMergeUtil.removeActivityFromContainer(rec);
		ChoreoMergeUtil.replaceActivity(repl, newAssignRepl);
		
	}
}
