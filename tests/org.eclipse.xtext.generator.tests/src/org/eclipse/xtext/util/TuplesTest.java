package org.eclipse.xtext.util;

import junit.framework.TestCase;

public class TuplesTest extends TestCase {
	
	public void testPair() throws Exception {
		Pair<String, String> p1 = Tuples.create("foo", "bar");
		Pair<String, String> p2 = Tuples.create("foo", null);
		Pair<String, String> p3 = Tuples.create(null, "bar");
		Pair<String, String> p4 = Tuples.create("bar", "foo");
		Pair<String, String> p5 = Tuples.create(null,null);
		
		assertEquals(p1, Tuples.create("foo", "bar"));
		assertFalse(p1.equals(p2));
		assertFalse(p1.equals(p3));
		assertFalse(p1.equals(p4));
		assertFalse(p1.equals(p5));
		assertFalse(p1.equals(null));
	}
	
	public void testTriple() throws Exception {
		assertFalse(Tuples.create("foo", "bar", "stuff").equals(Tuples.create("foo", "bar")));
		assertFalse(Tuples.create("foo", "bar").equals(Tuples.create("foo", "bar", "stuff")));
		
		Triple<String, String, String> p1 = Tuples.create("foo", "bar","honk");
		Triple<String, String, String> p2 = Tuples.create("foo", null,"honk");
		Triple<String, String, String> p3 = Tuples.create(null, "bar","honk");
		Triple<String, String, String> p6 = Tuples.create("foo", "bar",null);
		Triple<String, String, String> p4 = Tuples.create("bar", "foo","honk");
		Triple<String, String, String> p5 = Tuples.create(null,null,null);
		
		assertEquals(p1, Tuples.create("foo", "bar","honk"));
		assertFalse(p1.equals(p2));
		assertFalse(p1.equals(p3));
		assertFalse(p1.equals(p4));
		assertFalse(p1.equals(p5));
		assertFalse(p1.equals(p6));
		assertFalse(p1.equals(null));
	}
}
