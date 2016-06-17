package org.bpel4chor.mergechoreography.util;

import java.util.Comparator;

/**
 * Comparator for Class<?> Objects Copyright 2013 IAAS University of Stuttgart <br>
 * <br>
 * 
 * @author Peter.Debicki
 * 
 */
public class ClassComparator implements Comparator<Class<?>> {
	
	@Override
	public int compare(Class<?> a, Class<?> b) {
		return b.getName().compareTo(a.getName());
	}
}
