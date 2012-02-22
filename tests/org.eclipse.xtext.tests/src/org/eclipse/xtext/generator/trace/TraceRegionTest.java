/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.trace;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.generator.trace.AbstractStatefulTraceRegion;
import org.eclipse.xtext.generator.trace.AbstractTraceRegion;
import org.eclipse.xtext.generator.trace.LocationData;
import org.eclipse.xtext.generator.trace.TraceRegion;
import org.eclipse.xtext.util.TextRegionWithLineInformation;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Objects;
import com.google.common.collect.Iterators;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@SuppressWarnings("all")
public class TraceRegionTest extends Assert {

	@Test
	public void testConstructor() {
		TraceRegion region = new TraceRegion(0, 1, 0, 0, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		assertEquals(0, region.getMyOffset());
		assertEquals(1, region.getMyLength());
		assertEquals(2, region.getMergedLocationData().getOffset());
		assertEquals(3, region.getMergedLocationData().getLength());
		assertEquals(URI.createURI("uri"), region.getAssociatedPath());
		assertEquals("project", region.getAssociatedProjectName());
		assertNull(region.getParent());
		assertTrue(region.getNestedRegions().isEmpty());
	}
	
	@Test
	public void testConstructorWithParent() {
		TraceRegion parent = new TraceRegion(0, 1, 0, 0, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion region = new TraceRegion(0, 1, 0, 0, 2, 3, 0, 0, parent, null, null);
		assertEquals(URI.createURI("uri"), region.getAssociatedPath());
		assertEquals("project", region.getAssociatedProjectName());
		assertEquals(parent, region.getParent());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorInvalidArgs_01() {
		new TraceRegion(-1, 0, 0, 0, 0, 0, 0, 0, null, URI.createURI("uri"), "project");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorInvalidArgs_02() {
		new TraceRegion(0, -1, 0, 0, 0, 0, 0, 0, null, URI.createURI("uri"), "project");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorInvalidArgs_03() {
		new TraceRegion(0, 0, -1, 0, 0, 0, 0, 0, null, URI.createURI("uri"), "project");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorInvalidArgs_04() {
		new TraceRegion(0, 0, 0, -1, 0, 0, 0, 0, null, URI.createURI("uri"), "project");
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructorInvalidArgs_05() {
		new TraceRegion(0, 0, 0, 0, 0, 0, 0, 0, null, null, "project");
	}
	
	public void testConstructor_NoProject() {
		TraceRegion region = new TraceRegion(0, 0, 0, 0, 0, 0, 0, 0, null, URI.createURI("uri"), null);
		assertEquals("<unknown>", region.getAssociatedProjectName());
	}
	
	public void assertEquals(Iterator<? extends AbstractTraceRegion> iterator1, Iterator<? extends AbstractTraceRegion> iterator2) {
		while (iterator1.hasNext()) {
			assertTrue("iterator2.hasNext", iterator2.hasNext());
			AbstractTraceRegion o1 = iterator1.next();
			AbstractTraceRegion o2 = iterator2.next();
			assertEquals(o1, o2);
			assertEquals(o1.getMyLineNumber(), o2.getMyLineNumber());
			assertEquals(o1.getMyEndLineNumber(), o2.getMyEndLineNumber());
		}
		assertFalse("iterator2.hasNext", iterator2.hasNext());
	}
	
	@Test
	public void testLeafIterator_NoChildren() {
		TraceRegion region = new TraceRegion(0, 1, 1, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		Iterator<AbstractTraceRegion> iter = region.leafIterator();
		assertEquals(Collections.singleton(region).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_OneChild() {
		TraceRegion parent = new TraceRegion(0, 1, 1, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion region = new TraceRegion(0, 1, 1, 2, 2, 3, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = parent.leafIterator();
		assertEquals(Collections.singleton(region).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_GrandChild() {
		TraceRegion root = new TraceRegion(0, 1, 1, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion parent = new TraceRegion(0, 1, 1, 2, 2, 3, 0, 0, root, null, null);
		TraceRegion region = new TraceRegion(0, 1, 1, 2, 2, 3, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = root.leafIterator();
		assertEquals(Collections.singleton(region).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_TwoChildren_NoGaps() {
		TraceRegion parent = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion first = new TraceRegion(0, 1, 0, 1, 2, 3, 0, 0, parent, null, null);
		TraceRegion second = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = parent.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_OneChild_LeftGap() {
		final TraceRegion parent = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		AbstractTraceRegion first = new AbstractStatefulTraceRegion(new TextRegionWithLineInformation(0, 1, 0, 1), new LocationData(2, 3, 0, 0, null, null), parent) {};
		TraceRegion second = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = parent.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_OneChild_RightGap() {
		final TraceRegion parent = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		AbstractTraceRegion first = new TraceRegion(0, 1, 0, 1, 3, 4, 0, 0, parent, null, null);
		AbstractTraceRegion second = new AbstractStatefulTraceRegion(new TextRegionWithLineInformation(1, 1, 1, 2), new LocationData(2, 3, 0, 0, null, null), parent) {};
		Iterator<AbstractTraceRegion> iter = parent.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_OneGrandChild_LeftGap() {
		final TraceRegion root = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		AbstractTraceRegion first = new AbstractStatefulTraceRegion(new TextRegionWithLineInformation(0, 1, 0, 1), new LocationData(2, 3, 0, 0, null, null), root) {};
		TraceRegion parent = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, root, null, null);
		TraceRegion second = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = root.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_OneGrandChild_RightGap() {
		final TraceRegion root = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion parent = new TraceRegion(0, 1, 0, 1, 3, 4, 0, 0, root, null, null);
		TraceRegion first = new TraceRegion(0, 1, 0, 1, 3, 4, 0, 0, parent, null, null);
		AbstractTraceRegion second = new AbstractStatefulTraceRegion(new TextRegionWithLineInformation(1, 1, 1, 2), new LocationData(2, 3, 0, 0, null, null), root) {};
		Iterator<AbstractTraceRegion> iter = root.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_TwoGrandChildren_NoGaps_01() {
		TraceRegion root = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion parent = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, root, null, null);
		TraceRegion first = new TraceRegion(0, 1, 0, 1, 2, 3, 0, 0, parent, null, null);
		TraceRegion second = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = root.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_TwoGrandChildren_NoGaps_02() {
		TraceRegion root = new TraceRegion(0, 2, 0, 2, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion firstParent = new TraceRegion(0, 1, 0, 1, 2, 3, 0, 0, root, null, null);
		TraceRegion first = new TraceRegion(0, 1, 0, 1, 2, 3, 0, 0, firstParent, null, null);
		TraceRegion secondParent = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, root, null, null);
		TraceRegion second = new TraceRegion(1, 1, 1, 2, 3, 4, 0, 0, secondParent, null, null);
		Iterator<AbstractTraceRegion> iter = root.leafIterator();
		assertEquals(Arrays.asList(first, second).iterator(), iter);
	}
	
	@Test
	public void testLeafIterator_TwoChildren_WithGaps() {
		final TraceRegion parent = new TraceRegion(0, 3, 0, 3, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion first = new TraceRegion(0, 1, 0, 1, 2, 3, 0, 0, parent, null, null);
		AbstractTraceRegion second = new AbstractStatefulTraceRegion(new TextRegionWithLineInformation(1, 1, 1, 2), new LocationData(2, 3, 0, 0, null, null), parent) {};
		AbstractTraceRegion third = new TraceRegion(2, 1, 2, 3, 3, 4, 0, 0, parent, null, null);
		Iterator<AbstractTraceRegion> iter = parent.leafIterator();
		assertEquals(Arrays.asList(first, second, third).iterator(), iter);
	}

	@Test
	public void testAnnotate_01() {
		TraceRegion region = new TraceRegion(0, 1, 0, 0, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		assertEquals("<2:3[a]", region.getAnnotatedString("a"));
	}
	
	@Test
	public void testAnnotate_02() {
		TraceRegion region = new TraceRegion(1, 1, 0, 0, 2, 3, 0, 0, null, URI.createURI("uri"), "project");
		assertEquals("a<2:3[b]c", region.getAnnotatedString("abc"));
	}
	
	@Test
	public void testAnnotate_03() {
		TraceRegion parent = new TraceRegion(0, 4, 0, 0, 1, 2, 0, 0, null, URI.createURI("uri"), "project");
		new TraceRegion(0, 1, 0, 0, 3, 4, 0, 0, parent, null, null);
		new TraceRegion(2, 1, 0, 0, 5, 6, 0, 0, parent, null, null);
		new TraceRegion(3, 1, 0, 0, 7, 8, 0, 0, parent, null, null);
		assertEquals("<1:2[<3:4[a]b<5:6[c]<7:8[d]]e", parent.getAnnotatedString("abcde"));
	}
	
	@Test
	public void testAnnotate_04() {
		TraceRegion root = new TraceRegion(0, 4, 0, 0, 1, 2, 0, 0, null, URI.createURI("uri"), "project");
		TraceRegion parent = new TraceRegion(1, 2, 0, 0, 3, 4, 0, 0, root, null, null);
		new TraceRegion(2, 1, 0, 0, 5, 6, 0, 0, parent, null, null);
		assertEquals("<1:2[a<3:4[b<5:6[c]]d]e", root.getAnnotatedString("abcde"));
	}
}
