/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.tests.interpreter;

import java.util.Collections;
import java.util.Stack;

import org.eclipse.xtext.xbase.tests.AbstractXbaseTestCase;

import testdata.ExceptionSubclass;

import com.google.inject.internal.Lists;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 * @author Sven Efftinge
 */
public abstract class AbstractXbaseEvaluationTest extends AbstractXbaseTestCase {

	protected abstract void assertEvaluatesTo(Object object, String string);
	protected abstract void assertEvaluatesWithException(Class<? extends Throwable> class1, String string);
	
	public void testUpToOperator() throws Exception {
		assertEvaluatesTo(new Integer(9),"(9..13).iterator().next()");
	}
	
	public void testOverriddenLocalVariables() throws Exception {
		assertEvaluatesTo(new Integer(3),
				"{" +
				" val x = 3 " +
				" var y = 2 " +
				" {" +
				"  var x = y " +
				"  val y = 1 " +
				"  x+y" +
				"  }" +
				"}");
	}
	
	public void testOverriddenLocalVariables_0() throws Exception {
		assertEvaluatesTo(Boolean.TRUE,
				"{" +
				" var x = 1 " +
				" val y = 4 " +
				" for (y : 1..3) {" +
				"  x = x+y" +
				" }" +
				" x == y" +
				"}"
			);
	}
	
	public void testAddOnIntegers() throws Exception {
		assertEvaluatesTo(new Integer(3), "1+2");
	}

	public void testSubtractionOnIntegers() throws Exception {
		assertEvaluatesTo(new Integer(1), "2-1");
	}

	public void testMultiplicationOnIntegers() throws Exception {
		assertEvaluatesTo(new Integer(42), "6*7");
	}

	public void testDivisionOnIntegers_01() throws Exception {
		assertEvaluatesTo(new Integer(2), "6/3");
	}

	public void testDivisionOnIntegers_02() throws Exception {
		assertEvaluatesTo(new Integer(2), "7/3");
	}

	public void testModuloOnIntegers() throws Exception {
		assertEvaluatesTo(new Integer(1), "7%3");
	}

	public void testPowerOnIntegers() throws Exception {
		assertEvaluatesTo(new Integer(8), "2**3");
	}

	public void testLessThanOnIntegers_01() throws Exception {
		assertEvaluatesTo(new Boolean(true), "3<4");
	}

	public void testLessThanOnIntegers_02() throws Exception {
		assertEvaluatesTo(new Boolean(false), "4<3");
	}

	public void testGreaterThanOnIntegers_01() throws Exception {
		assertEvaluatesTo(new Boolean(true), "4>3");
	}

	public void testGreaterThanOnIntegers_02() throws Exception {
		assertEvaluatesTo(new Boolean(false), "3>4");
	}

	public void testLessEqualsThanOnIntegers_01() throws Exception {
		assertEvaluatesTo(new Boolean(true), "3<=3");
	}

	public void testLessEqualsThanOnIntegers_02() throws Exception {
		assertEvaluatesTo(new Boolean(true), "3<=4");
	}

	public void testLessEqualsThanOnIntegers_03() throws Exception {
		assertEvaluatesTo(new Boolean(false), "4<=3");
	}

	public void testGreaterEqualsThanOnIntegers_01() throws Exception {
		assertEvaluatesTo(new Boolean(true), "3>=3");
	}

	public void testGreaterEqualsThanOnIntegers_02() throws Exception {
		assertEvaluatesTo(new Boolean(true), "4>=3");
	}

	public void testGreaterEqualsThanOnIntegers_03() throws Exception {
		assertEvaluatesTo(new Boolean(false), "3>=4");
	}

	public void testOrOnBooleans_01() throws Exception {
		assertEvaluatesTo(new Boolean(true), "true||false");
	}

	public void testOrOnBooleans_02() throws Exception {
		assertEvaluatesTo(new Boolean(true), "false||true");
	}

	public void testOrOnBooleans_03() throws Exception {
		assertEvaluatesTo(new Boolean(true), "true||true");
	}

	public void testOrOnBooleans_04() throws Exception {
		assertEvaluatesTo(new Boolean(false), "false||false");
	}

	public void testAndOnBooleans_01() throws Exception {
		assertEvaluatesTo(new Boolean(true), "true&&true");
	}

	public void testAndOnBooleans_02() throws Exception {
		assertEvaluatesTo(new Boolean(false), "false&&true");
	}

	public void testAndOnBooleans_03() throws Exception {
		assertEvaluatesTo(new Boolean(false), "true&&false");
	}

	public void testAndOnBooleans_04() throws Exception {
		assertEvaluatesTo(new Boolean(false), "false&&false");
	}

	public void testNull() {
		assertEvaluatesTo(null, "null");
	}
	
	public void testNullBlockResult_01() {
		assertEvaluatesTo(null, "{ null; }");
	}
	
	public void testNullBlockResult_02() {
		assertEvaluatesTo(null, "{ 'literal'.length; null; }");
	}
	
	public void testStringLiteral_01() {
		assertEvaluatesTo("", "''");
	}
	
	public void testStringLiteral_02() {
		assertEvaluatesTo("literal", "'literal'");
	}
	
	public void testBooleanLiteral_01() {
		assertEvaluatesTo(Boolean.TRUE, "true");
	}
	
	public void testBooleanLiteral_02() {
		assertEvaluatesTo(Boolean.FALSE, "false");
	}
	
	public void testIntLiteral_01() {
		assertEvaluatesTo(53, "53");
	}
	
	public void testIntLiteral_02() {
		assertEvaluatesTo(0, "0");
	}
	
	public void testTypeLiteral_01() {
		assertEvaluatesTo(Void.class, "typeof(Void)");
	}

	public void testTypeLiteral_02() {
		assertEvaluatesTo(Void.TYPE, "typeof(void)");
	}
	
	public void testIfExpression_00() {
		assertEvaluatesTo(null, "if (0==1) 'literal'");
	}
	
	public void testIfExpression_01() {
		assertEvaluatesTo(null, "if (false) 'literal'");
	}
	
	public void testIfExpression_02() {
		assertEvaluatesTo("literal", "if (true) 'literal'");
	}
	
	public void testIfExpression_03() {
		assertEvaluatesTo("then", "if (true) 'then' else 'else'");
	}
	
	public void testIfExpression_04() {
		assertEvaluatesTo("else", "if (false) 'then' else 'else'");
	}
	
	public void testIfExpression_withThrowExpression_00() {
		assertEvaluatesWithException(NullPointerException.class, "if (false) throw new NullPointerException() else throw new NullPointerException()");
	}
	
	public void testIfExpression_withThrowExpression_01() {
		assertEvaluatesWithException(NullPointerException.class, "if (false) 'then' else throw new NullPointerException()");
	}
	
	public void testIfExpression_withThrowExpression_02() {
		assertEvaluatesTo("then", "if (true) 'then' else throw new NullPointerException()");
	}
	
	public void testVariableDeclaration_01() {
		assertEvaluatesTo("literal", "{var x = 'literal' x = x}");
	}
	
//TODO support overloading of local variables
//	
//	public void testVariableDeclaration_02() {
//		assertEvaluatesTo(true, "{var x = 'literal' { val x = true x}}");
//	}
	
	public void testFeatureCall_01() {
		assertEvaluatesTo("literal", "{var x = 'literal' x}");
	}
	
	public void testFeatureCall_02() {
		assertEvaluatesTo(Collections.singletonList(new Character('a')), "'a'.toCharArray");
	}
	
	public void testFeatureCall_03() {
		assertEvaluatesTo(Lists.newArrayList(
				new Character('a'), 
				new Character('b'),
				new Character('c')), "'abc'.toCharArray");
	}
	
	public void testFeatureCall_04() {
		assertEvaluatesTo(new Integer("literal".length()), "{ var this = 'literal' length }");
	}
	
	public void testConstructorCall_00() throws Exception {
		assertEvaluatesTo("", "new String()");
	}
	
	public void testAssignment_01() {
		assertEvaluatesTo("newValue", "{var x = 'literal' x = 'newValue'}");
	}

	public void testAssignment_02() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  prop1 = 'literal'" +
				"  prop1" +
				"}");
	}
	
	public void testAssignment_03() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  prop2 = 'literal'" +
				"  prop2" +
				"}");
	}
	
	public void testAssignment_04() {
		assertEvaluatesTo("Integer: 1", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  prop2 = 1" +
				"  prop2" +
				"}");
	}
	
	public void testAssignment_05() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  this.prop2 = 'literal'" +
				"  prop2" +
				"}");
	}
	
	public void testAssignment_06() {
		assertEvaluatesTo("Integer: 1", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  this.prop2 = 1" +
				"  prop2" +
				"}");
	}
	
	public void testAssignment_07() {
		assertEvaluatesTo(null, 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  this.prop2 = 'literal'" +
				"}");
	}
	
	public void testAssignment_08() {
		assertEvaluatesTo(null, 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  this.prop2 = 1" +
				"}");
	}
	
	public void testAssignment_09() {
		assertEvaluatesTo("Integer: 1", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  this.prop3 = 1" +
				"}");
	}
	
	public void testAssignment_10() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var this = new testdata.Properties1()" +
				"  prop1 = 'literal'" +
				"}");
	}
	
	public void testAssignment_11() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var list = new testdata.GenericType1<String>()" +
				"  list += 'literal'" +
				"  list.get()" +
				"}");
	}
	
	public void testAssignment_12() {
		assertEvaluatesTo(Boolean.TRUE, 
				"{" +
				"  var list = new testdata.GenericType1<String>()" +
				"  list += 'literal'" +
				"}");
	}
	
	public void testAssignmentInBlock_01() {
		assertEvaluatesTo("newValue", "{var x = 'literal' { x = 'newValue' } x }");
	}
	
	public void testThrowInBlock() {
		assertEvaluatesWithException(NullPointerException.class, "{ throw new NullPointerException() }");
	}
	
	public void testThrowInBlock_01() {
		assertEvaluatesWithException(NullPointerException.class, "{ 42.toString() throw new NullPointerException() }");
	}
	
	public void testForLoop_01() {
		assertEvaluatesTo(new Character('c'), 
				"{\n" +
				"  var Character result = null\n" +
				"  for(x: 'abc'.toCharArray) result = x\n" +
				"  result" +
				"}");
	}
	
	public void testForLoop_02() {
		assertEvaluatesTo(new Character('a'), 
				"{\n" +
				"  var Character result = null\n" +
				"  for(x: 'aba'.toCharArray) if (result == null) result = x\n" +
				"  result" +
				"}");
	}

	public void testForLoop_03() {
		assertEvaluatesWithException(ClassCastException.class, "for(x: 'abc' as Object as java.util.List<Character>) null");
	}
	
	public void testWhileLoop_01() {
		assertEvaluatesTo("newValue", 
				"{\n" +
				"  var value = 'oldValue'\n" +
				"  var condition = true\n" +
				"  while(condition) { value = 'newValue' condition=false }" +
				"  value" +
				"}");
	}
	
	public void testWhileLoop_02() {
		assertEvaluatesTo("oldValue", 
				"{\n" +
				"  var value = 'oldValue'\n" +
				"  var condition = false\n" +
				"  while(condition) { value = 'newValue' condition=false }" +
				"  value" +
				"}");
	}
	
	public void testDoWhileLoop_01() {
		assertEvaluatesTo("newValue", 
				"{\n" +
				"  var value = 'oldValue'\n" +
				"  var condition = true\n" +
				"  do {" +
				"	 value ='newValue'" +
				"    condition = false" +
				"  } while(condition)" +
				"  value" +
				"}");
	}
	
	public void testDoWhileLoop_02() {
		assertEvaluatesTo("newValue", 
				"{\n" +
				"  var value = 'oldValue'\n" +
				"  var condition = false\n" +
				"  do {" +
				"	 value = 'newValue'" +
				"  } while(condition)" +
				"  value" +
				"}");
	}
	
	public void testMemberFeatureCall_01() {
		assertEvaluatesTo("LITERAL", "'literal'.toUpperCase()");
	}
	
	public void testMemberFeatureCall_02() {
		assertEvaluatesTo("literal", "'literal'.toUpperCase().toLowerCase()");
	}
	
	public void testMemberFeatureCall_03() {
		assertEvaluatesTo("source", "new java.util.EventObject('source').source");
	}
	
	public void testMemberFeatureCall_04() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var fieldAccess = new testdata.FieldAccess()" +
				"  fieldAccess.stringField = 'literal'" +
				"}");
	}
	
	public void testMemberFeatureCall_05() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var fieldAccess = new testdata.FieldAccess()" +
				"  fieldAccess.stringField = 'literal'" +
				"  fieldAccess.stringField" +
				"}");
	}
	
	public void testMemberFeatureCall_06() {
		assertEvaluatesTo(null, 
				"{" +
				"  var fieldAccess = new testdata.FieldAccess()" +
				"  fieldAccess.stringField" +
				"}");
	}
	
	public void testMemberFeatureCall_07() {
		assertEvaluatesTo("finalField", 
				"new testdata.FieldAccess().finalField");
	}
	
	public void testNullSafeOperator_01() {
		assertEvaluatesWithException(NullPointerException.class, "(null as Object).toString()");
		assertEvaluatesWithException(NullPointerException.class, "(null as Object)?.toString().toString()");
	}
	
	public void testNullSafeOperator_02() {
		assertEvaluatesTo(null, "(null as Object)?.toString()");
		assertEvaluatesTo(null, "(null as Object)?.toString()?.toString()");
	}
	
	public void testSpreadOperator_01() {
		assertEvaluatesWithException(NullPointerException.class, "(null as java.util.List<Object>)*.toString()");
		assertEvaluatesWithException(ClassCastException.class, "('' as java.util.List<Object>)*.toString()");
	}
	
	public void testSpreadOperator_02() {
		assertEvaluatesTo(Lists.newArrayList(
				"A", "B", "C"), "'abc'.toCharArray*.toString*.toUpperCase");
	}
	
	public void testSwitchExpression_01() {
		assertEvaluatesTo(null, "switch 'x' { case false: 'literal' }");
	}
	
	public void testSwitchExpression_02() {
		assertEvaluatesTo("literal", "switch 'x' { case 'x': 'literal' }");
	}
	
	public void testSwitchExpression_03() {
		assertEvaluatesTo("literal", "switch 'x' { case 'y': 'foo' default : 'literal' }");
	}
	
	public void testSwitchExpression_04() {
		assertEvaluatesTo("x", "switch x:'x' { case x.length==3 : 'xxx' default : 'x' }");
	}
	
	public void testSwitchExpression_05() {
		assertEvaluatesTo(null, "switch new Object() { String : 'literal' }");
	}
	
	public void testSwitchExpression_06() {
		assertEvaluatesTo("literal", "switch new Object() { Object : 'literal' }");
	}
	
	public void testSwitchExpression_07() {
		assertEvaluatesTo("literal", "switch new Object() { case false: null default: 'literal' }");
	}
	
	public void testSwitchExpression_08() {
		assertEvaluatesTo("literal", "switch x : new Object() { case false: null case x!=null: 'literal' }");
	}
	
	public void testSwitchExpression_09() {
		assertEvaluatesTo(Integer.valueOf(4), "switch 'foo' { case 'foo': 4 String: 3 }");
	}
	
	public void testSwitchExpression_10() {
		assertEvaluatesTo(Integer.valueOf(3), "switch 'foo' { Boolean case 'foo': 4 String: 3 }");
	}
	
	public void testCastedExpression_01() {
		assertEvaluatesTo("literal", "'literal' as String");
	}
	
	public void testCastedExpression_02() {
		assertEvaluatesWithException(ClassCastException.class, "'literal' as Object as Integer");
	}
	
	public void testCastedExpression_03() {
		assertEvaluatesTo(null, "null as Integer");
	}
	
	public void testTryCatch_01() {
		assertEvaluatesTo("caught", "try { 'literal' as Object as Boolean } catch(ClassCastException e) {'caught'}");
	}
	
	public void testTryCatch_02() {
		assertEvaluatesTo("literal", "try { 'literal' as String } catch(ClassCastException e) {'caught'}");
	}
	
	public void testTryCatch_03() {
		assertEvaluatesTo("ClassCastException", "try { 'literal' as Object as Boolean } catch(ClassCastException e) {e.getClass().getSimpleName()}");
	}
	
	public void testTryCatch_04() {
		assertEvaluatesWithException(NullPointerException.class, "try { 'literal' as Object as Boolean } catch(ClassCastException e) throw new NullPointerException()");
	}
	
	public void testTryCatch_05() {
		assertEvaluatesWithException(NullPointerException.class, 
				"try 'literal' as Object as Boolean" +
				"  catch(ClassCastException e) throw new NullPointerException()" +
				"  catch(NullPointerException e) 'dont catch subsequent exceptions'");
	}
	
	public void testTryCatch_WithThrows() {
		assertEvaluatesWithException(NullPointerException.class, 
				"try throw new NullPointerException()" +
				"  catch(ClassCastException e) throw new NullPointerException()" +
		"  catch(NullPointerException e) throw new NullPointerException()");
	}
	
	public void testTryCatch_WithThrows_01() {
		assertEvaluatesTo("x", 
				"try throw new NullPointerException()" +
				"  catch(ClassCastException e) throw new NullPointerException()" +
		"  catch(NullPointerException e) 'x'");
	}
	
	public void testTryFinally_01() {
		assertEvaluatesTo("literal", "try 'literal' finally 'finally'.toString");
	}
	
	public void testTryFinally_02() {
		assertEvaluatesTo("finally", "{ var x = 'foo' try 'literal' finally x = 'finally' x }");
	}
	
	public void testConstructor_01() {
		assertEvaluatesTo(Boolean.TRUE, "new java.io.StringReader('literal').markSupported");
	}
	
	public void testConstructor_02() {
		assertEvaluatesTo(new Character('a'), "new Character('abc'.charAt(0))");
	}
	
	public void testConstructor_03() {
		assertEvaluatesTo("source", "new java.util.EventObject('source').getSource");
	}
	
	public void testConstructor_04() {
		assertEvaluatesTo(new Stack<Object>(), "new java.util.Stack()");
	}
	
	public void testThrowExpression_01() {
		assertEvaluatesWithException(ExceptionSubclass.class, "throw new testdata.ExceptionSubclass()");
	}
	
	public void testThrowExpression_02() {
		assertEvaluatesWithException(ClassCastException.class, "throw 'literal' as Object as RuntimeException");
	}
	
	public void testThrowExpression_03() {
		assertEvaluatesWithException(NullPointerException.class, "throw null");
	}
	
	public void testThrowExpression_04() {
		assertEvaluatesWithException(NullPointerException.class, "throw new NullPointerException()");
	}
	
	public void testThrowExpression_05() {
		assertEvaluatesWithException(RuntimeException.class, "if ('foo'=='bar') 'foobar' else throw new RuntimeException()");
	}
	
	public void testInstanceOf_01() {
		assertEvaluatesTo(Boolean.TRUE, "'literal' instanceof String");
	}
	
	public void testInstanceOf_02() {
		assertEvaluatesTo(Boolean.TRUE, "'literal' instanceof CharSequence");
	}
	
	public void testInstanceOf_03() {
		assertEvaluatesTo(Boolean.FALSE, "'literal' as Object instanceof Boolean");
	}
	
	public void testInstanceOf_04() {
		assertEvaluatesTo(Boolean.FALSE, "null instanceof Boolean");
	}
	
//	public void testClosure_01() {
//		assertEvaluatesTo("literal", "new testdata.ClosureClient().invoke0(|'literal')");
//	}
//	
//	public void testClosure_02() {
//		assertEvaluatesTo("literal", 
//				"{" +
//				"  var result = 'literal'" +
//				"  new testdata.ClosureClient().invoke0(|result)" +
//				"}");
//	}
//	
//	public void testClosure_03() {
//		assertEvaluatesTo("literal", 
//				"{" +
//				"  var closure = |'literal'" +
//				"  new testdata.ClosureClient().invoke0(closure)" +
//				"}");
//	}
//	
//	public void testClosure_05() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().invoke1(String s|s.toUpperCase, 'literal')");
//	}
//	
//	public void testClosure_06() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().invoke2(" +
//				"  p1, String s|s.toUpperCase, null, 'literal')");
//	}
//	
//	public void testClosure_07() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().invoke3(" +
//				"  p1, p2, String s|s.toUpperCase, null, null, 'literal')");
//	}
//	
//	public void testClosure_08() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().invoke4(" +
//				"  p1, p2, p3, String s|s.toUpperCase, null, null, null, 'literal')");
//	}
//	
//	public void testClosure_09() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().invoke5(" +
//				"  p1, p2, p3, p4, String s|s.toUpperCase, null, null, null, null, 'literal')");
//	}
//	
//	public void testClosure_10() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().invoke6(" +
//				"  p1, p2, p3, p4, p5, String s|s.toUpperCase, null, null, null, null, null, 'literal')");
//	}
//	
//	public void testClosure_11() {
//		assertEvaluatesTo(new Character('a'), 
//				"new testdata.ClosureClient().invokeX(" +
//				"  Character c1, c2, c3, c4, c5, c6, c7|c1, 'abcdefg'.toCharArray)");
//	}
//	
//	public void testClosure_12() {
//		assertEvaluatesTo("literal", 
//				"new testdata.ClosureClient().useProvider(|'literal')");
//	}
//	
//	public void testClosure_13() {
//		assertEvaluatesTo("literal", 
//				"new testdata.ClosureClient().useSupplier(|'literal')");
//	}
//	
//	public void testClosure_14() {
//		assertEvaluatesTo("LITERAL", 
//				"new testdata.ClosureClient().useGoogleCollectFunction(String s|s.toUpperCase, 'literal')");
//	}
//	
//	public void testClosure_15() {
//		assertEvaluatesTo(Boolean.TRUE, 
//				"new testdata.ClosureClient().useGoogleCollectPredicate(String s|'literal'.equals(s), 'literal')");
//	}
//	
//	public void testClosure_16() {
//		assertEvaluatesTo(Boolean.FALSE, 
//				"new testdata.ClosureClient().useGoogleCollectPredicate(String s|'literal'.equals(s), 'false')");
//	}
//	
//	public void testClosure_17() {
//		assertEvaluatesTo("literal", 
//				"{" +
//				"  var client = new testdata.ClosureClient()" +
//				"  var fun = client.getIdentityFunction" +
//				"  client.useGoogleCollectFunction(fun, 'literal')" +
//				"}");
//	}
	
	public void testArrayConversion_01() {
		assertEvaluatesTo("LITERAL", 
				"{" +
				"  var client = new testdata.ArrayClient()" +
				"  var stringArray = client.toStringArray('literal', 'other')" +
				"  client.inplaceToUpperCase(stringArray)" +
				"  stringArray.get(0)" +
				"}");
	}
	
	public void testArrayConversion_02() {
		assertEvaluatesTo("OTHER", 
				"{" +
				"  var client = new testdata.ArrayClient()" +
				"  var stringArray = client.toStringArray('literal', 'other')" +
				"  client.inplaceToUpperCase(stringArray)" +
				"  stringArray.get(1)" +
				"}");
	}
	
	public void testArrayConversion_03() {
		assertEvaluatesTo(new Integer(42), 
				"{" +
				"  var client = new testdata.ArrayClient()" +
				"  var intArray = client.toIntArray(32, 7)" +
				"  client.inplaceAdd10(intArray)" +
				"  intArray.get(0)" +
				"}");
	}
	
	public void testArrayConversion_04() {
		assertEvaluatesTo(new Integer(17), 
				"{" +
				"  var client = new testdata.ArrayClient()" +
				"  var intArray = client.toIntArray(32, 7)" +
				"  client.inplaceAdd10(intArray)" +
				"  intArray.get(1)" +
				"}");
	}
	
	public void testArrayConversion_05() {
		assertEvaluatesTo("literal", 
				"{" +
				"  var client = new testdata.ArrayClient()" +
				"  var stringArray = client.toStringArray('literal', 'other')" +
				"  client.inplaceSwap(stringArray)" +
				"  stringArray.get(1)" +
				"}");
	}
}
