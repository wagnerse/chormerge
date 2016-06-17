package org.bpel4chor.mergechoreography.util.model;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.bpel.model.Activity;
import org.eclipse.bpel.model.BPELFactory;
import org.eclipse.bpel.model.Flow;
import org.eclipse.bpel.model.Scope;

/**
 * Data class to generate the compensation {@link Flow}.<br>
 * <br>
 * Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter Berger
 * 
 */
public class TreeNode {
	
	private Set<TreeNode> parents = null;
	/**
	 * Could be a {@link Flow} or a {@link Scope}.
	 */
	private Activity currentNodeValue = null;
	/**
	 * Only {@link Flow}s have childs.
	 */
	private Set<TreeNode> directChilds = null;
	private TreeNode successor = null;
	private Set<TreeNode> linkSuccessorSet = null;
	
	/**
	 * Compensation {@link Activity}. Used to cache the new {@link Scope}/
	 * {@link Flow} if the compensation will be build up. {@link Scope} for
	 * {@link Scope}s and {@link Flow} for {@link Flow}s
	 */
	private Activity cpActivity = null;
	/**
	 * Used as a flag which will be true if a {@link TreeNode} is added to the
	 * root{@link Flow} or a nested {@link Flow}.
	 */
	private boolean alreadyAdded = false;
	
	
	public TreeNode(Activity currentNodeValue) {
		directChilds = new HashSet<>();
		parents = new HashSet<>();
		linkSuccessorSet = new HashSet<>();
		this.currentNodeValue = currentNodeValue;
	}
	
	/**
	 * @param child
	 */
	public void addChild(TreeNode child) {
		directChilds.add(child);
	}
	
	/**
	 * @param child
	 */
	public void addParent(TreeNode parent) {
		parents.add(parent);
	}
	
	/**
	 * @param child
	 */
	public Set<TreeNode> getChilds() {
		return directChilds;
	}
	
	/**
	 * @return the cpActivity
	 */
	public Activity getCpActivity() {
		if (cpActivity == null) {
			if (currentNodeValue instanceof Scope) {
				cpActivity = BPELFactory.eINSTANCE.createScope();
			} else if (currentNodeValue instanceof Flow) {
				cpActivity = BPELFactory.eINSTANCE.createFlow();
			}
		}
		return cpActivity;
	}
	
	/**
	 * @return the currentNodeValue
	 */
	public Activity getCurrentNodeValue() {
		return currentNodeValue;
	}
	
	public TreeNode getLeaf() {
		return getLeaf(this);
	}
	
	private TreeNode getLeaf(TreeNode tn) {
		TreeNode result = tn;
		if (tn.getSuccessor() != null) {
			result = getLeaf(tn.getSuccessor());
		}
		return result;
	}
	
	/**
	 * @return the parents
	 */
	public Set<TreeNode> getParents() {
		return parents;
	}
	
	/**
	 * @return the successor
	 */
	public TreeNode getSuccessor() {
		return successor;
	}
	
	/**
	 * @return the alreadyAdded
	 */
	public boolean isAlreadyAdded() {
		return alreadyAdded;
	}
	
	/**
	 * @param alreadyAdded the alreadyAdded to set
	 */
	public void setAlreadyAdded(boolean alreadyAdded) {
		this.alreadyAdded = alreadyAdded;
	}
	
	/**
	 * @param cpActivity the cpActivity to set
	 */
	public void setCpActivity(Activity cpActivity) {
		this.cpActivity = cpActivity;
	}
	
	/**
	 * @param currentNodeValue the currentNodeValue to set
	 */
	public void setCurrentNodeValue(Activity currentNodeValue) {
		this.currentNodeValue = currentNodeValue;
	}
	
	/**
	 * @param successor the successor to set
	 */
	public void setSuccessor(TreeNode successor) {
		this.successor = successor;
	}
	
	/**
	 * @return the linkSuccessorSet
	 */
	public Set<TreeNode> getLinkSuccessorSet() {
		return linkSuccessorSet;
	}
	
	/**
	 * 
	 * @param linkSuccessor
	 */
	public void addLinkSuccessor(TreeNode linkSuccessor) {
		this.linkSuccessorSet.add(linkSuccessor);
	}
}