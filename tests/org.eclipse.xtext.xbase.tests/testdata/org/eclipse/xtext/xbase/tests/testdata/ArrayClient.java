/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.tests.testdata;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ArrayClient {

	public String[] toStringArray(String s1, String s2) {
		return new String[] { s1 , s2 };
	}
	
	public int[] toIntArray(int i1, int i2) {
		return new int[] { i1 , i2 };
	}
	
	public void inplaceToUpperCase(String[] values) {
		for(int i = 0; i < values.length; i++) {
			values[i] = values[i].toUpperCase();
		}
	}
	
	public void inplaceSwap(Object[] values) {
		if (values.length != 2)
			throw new IllegalArgumentException("length must be 2");
		Object first = values[0];
		values[0] = values[1];
		values[1] = first;
	}
	
	public void inplaceAdd10(int[] values) {
		for(int i = 0; i < values.length; i++) {
			values[i] = values[i] + 10;
		}
	}
	
}
