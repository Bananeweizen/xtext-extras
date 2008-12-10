/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.util;

import java.util.Arrays;
import java.util.Iterator;

import junit.framework.TestCase;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class MappingIteratorTest extends TestCase implements Function<String, String> {

	public String exec(String param) {
		return param + "->" + param;
	}
	
	public void testEmptyIter() {
		Iterator<String> iter = createMappingIterator(new String[]{});
		assertFalse(iter.hasNext());
	}
	
	public void testSingleElementIter() {
		Iterator<String> iter = createMappingIterator("a");
		assertTrue(iter.hasNext());
		assertEquals("a->a", iter.next());
		assertFalse(iter.hasNext());
	}
	
	public void testMultiElementIter() {
		Iterator<String> iter = createMappingIterator("a", "b", "c");
		assertEquals("a->a", iter.next());
		assertEquals("b->b", iter.next());
		assertEquals("c->c", iter.next());
		assertFalse(iter.hasNext());
	}
	
	public void testNullIter() {
		try {
			new MappingIterator<String, String>(null, this);
			fail("NPE expected");
		} catch(NullPointerException e) {
			// expected
		}
	}
	
	public void testNullFunction() {
		try {
			new MappingIterator<String, String>(createMappingIterator("a"), null);
		} catch(NullPointerException e) {
			// expected
		}
	}
	
	private Iterator<String> createMappingIterator(String ...strings) {
		return new MappingIterator<String, String>(Arrays.asList(strings).iterator(), this);
	}

}
