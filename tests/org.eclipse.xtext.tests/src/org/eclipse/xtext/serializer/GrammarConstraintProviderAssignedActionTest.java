/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.serializer;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarToDot;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.grammaranalysis.IGrammarNFAProvider;
import org.eclipse.xtext.junit.AbstractXtextTests;
import org.eclipse.xtext.serializer.IGrammarConstraintProvider.IConstraint;
import org.eclipse.xtext.serializer.IGrammarConstraintProvider.IConstraintContext;
import org.eclipse.xtext.serializer.impl.GrammarConstraintProvider;
import org.eclipse.xtext.serializer.impl.GrammarConstraintProvider.ActionFilterNFAProvider;
import org.eclipse.xtext.serializer.impl.GrammarConstraintProvider.ActionFilterState;
import org.eclipse.xtext.serializer.impl.GrammarConstraintProvider.ActionFilterTransition;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.internal.Join;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class GrammarConstraintProviderAssignedActionTest extends AbstractXtextTests {

	private static final Logger log = Logger.getLogger(GrammarConstraintProviderAssignedActionTest.class);

	private static class ActionFilter2Dot extends GrammarToDot {
		protected IGrammarNFAProvider<ActionFilterState, ActionFilterTransition> nfaProvider = new ActionFilterNFAProvider();

		@Override
		protected Node drawAbstractElementTree(AbstractElement ele, Digraph d) {
			Node n = super.drawAbstractElementTree(ele, d);
			ActionFilterState nfas = nfaProvider.getNFA(ele);

			for (ActionFilterTransition t : nfas.getOutgoing())
				d.add(drawFollowerEdge(ele, t, false));
			for (ActionFilterTransition t : nfas.getOutgoingAfterReturn())
				d.add(drawFollowerEdge(ele, t, true));

			if (nfas.getOutgoing().size() == 0 && nfas.getOutgoingAfterReturn().size() == 0 && !nfas.isEndState())
				n.setStyle("dotted");
			if (nfas.isEndState())
				n.put("peripheries", "2");
			return n;
		}

		protected Edge drawFollowerEdge(AbstractElement ele, ActionFilterTransition t, boolean isParent) {
			Edge e = new Edge(ele, t.getTarget().getGrammarElement());
			e.setLabel(String.valueOf(t.getPrecedence()));
			e.setStyle("dotted");
			if (isParent)
				e.put("arrowtail", "odot");
			if (t.isRuleCall())
				e.put("arrowhead", "onormalonormal");
			else
				e.put("arrowhead", "onormal");
			return e;
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(XtextStandaloneSetup.class);
	}

	final static String HEADER = "grammar org.eclipse.xtext.validation.GrammarConstraintTestLanguage"
			+ " with org.eclipse.xtext.common.Terminals "
			+ "generate grammarConstraintTest \"http://www.eclipse.org/2010/tmf/xtext/GCT\"  ";

	private String getParserRule(String body) throws Exception {
		Grammar grammar = (Grammar) getModel(HEADER + body);
		IGrammarConstraintProvider gcp = new GrammarConstraintProvider();

		try {
			new ActionFilter2Dot().draw(grammar, getName() + ".pdf", "-T pdf");
		} catch (IOException e) {
			if (log.isDebugEnabled())
				log.debug(e.getMessage(), e);
		}

		List<IConstraintContext> ctxts = gcp.getConstraints(grammar);
		List<String> result = Lists.newArrayList();
		Set<IConstraint> visited = Sets.newHashSet();
		for (IConstraintContext ctx : ctxts) {
			result.add(ctx.toString());
			for (IConstraint c : ctx.getConstraints())
				if (visited.add(c))
					result.add("  " + c.toString());
		}
		return Join.join("\n", result);
	}

		public void testXtext() {
			IGrammarConstraintProvider gcp = new GrammarConstraintProvider();
			List<IConstraintContext> ctxts = gcp.getConstraints(getGrammarAccess().getGrammar());
			try {
				new ActionFilter2Dot().draw(getGrammarAccess().getGrammar(), getName() + ".pdf", "-T pdf");
			} catch (IOException e) {
				if (log.isDebugEnabled())
					log.debug(e.getMessage(), e);
			}
			List<String> result = Lists.newArrayList();
			Set<IConstraint> visited = Sets.newHashSet();
			for (IConstraintContext ctx : ctxts) {
				result.add(ctx.toString());
				for (IConstraint c : ctx.getConstraints())
					if (visited.add(c))
						result.add("  " + c.toString());
			}
			System.out.println(Join.join("\n", result));
		}

	public void testAssignedActionMandatory1() throws Exception {
		String actual = getParserRule("Rule: Foo {Bar.left=current} '+' right=ID; Foo: val=ID;");
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_Bar;\n");
		expected.append("  Rule_Bar returns Bar: (left=Rule_Bar_1 right=ID);\n");
		expected.append("Rule_Bar_1: Foo_Foo;\n");
		expected.append("  Foo_Foo returns Foo: val=ID;\n");
		expected.append("Foo: Foo_Foo;");
		assertEquals(expected.toString(), actual);
	}

	public void testAssignedActionMandatory2() throws Exception {
		String actual = getParserRule("Rule: val=ID {Bar.left=current} '+' right=ID;");
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_Bar;\n");
		expected.append("  Rule_Bar returns Bar: (left=Rule_Bar_1 right=ID);\n");
		expected.append("Rule_Bar_1: Rule_Bar_1_Rule;\n");
		expected.append("  Rule_Bar_1_Rule returns Rule: val=ID;");
		assertEquals(expected.toString(), actual);
	}

	public void testAssignedActionOptional() throws Exception {
		String actual = getParserRule("Rule: Foo ({Bar.left=current} '+' right=ID)?; Foo: val=ID;");
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Foo_Foo | Rule_Bar;\n");
		expected.append("  Foo_Foo returns Foo: val=ID;\n");
		expected.append("  Rule_Bar returns Bar: (left=Rule_Bar_1_0 right=ID);\n");
		expected.append("Rule_Bar_1_0: Foo_Foo;\n");
		expected.append("Foo: Foo_Foo;");
		assertEquals(expected.toString(), actual);
	}

	public void testAssignedActionOptionalMany() throws Exception {
		String actual = getParserRule("Rule: Foo ({Bar.left=current} '+' right=ID)*; Foo: val=ID;");
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Foo_Foo | Rule_Bar;\n");
		expected.append("  Foo_Foo returns Foo: val=ID;\n");
		expected.append("  Rule_Bar returns Bar: (left=Rule_Bar_1_0 right=ID);\n");
		expected.append("Rule_Bar_1_0: Foo_Foo | Rule_Bar;\n");
		expected.append("Foo: Foo_Foo;");
		assertEquals(expected.toString(), actual);
	}

	public void testAssignedActionManadatoryMany() throws Exception {
		String actual = getParserRule("Rule: Foo ({Bar.left=current} '+' right=ID)+; Foo: val=ID;");
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_Bar;\n");
		expected.append("  Rule_Bar returns Bar: (left=Rule_Bar_1_0 right=ID);\n");
		expected.append("Rule_Bar_1_0: Foo_Foo | Rule_Bar;\n");
		expected.append("  Foo_Foo returns Foo: val=ID;\n");
		expected.append("Foo: Foo_Foo;");
		assertEquals(expected.toString(), actual);
	}

	public void testExpression1() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Addition returns Expr: Prim ({Add.left=current} '+' right=Prim)*;\n");
		grammar.append("Prim returns Expr: {Val} name=ID | '(' Addition ')';\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Addition: Addition_Add | Prim_Val;\n");
		expected.append("  Addition_Add returns Add: (left=Addition_Add_1_0 right=Prim);\n");
		expected.append("  Prim_Val returns Val: name=ID;\n");
		expected.append("Addition_Add_1_0: Addition_Add | Prim_Val;\n");
		expected.append("Prim: Addition_Add | Prim_Val;");
		assertEquals(expected.toString(), actual);
	}

	public void testExpression2() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Addition returns Expr: Multiplication ({Add.left=current} '+' right=Multiplication)*;\n");
		grammar.append("Multiplication returns Expr: Prim ({Mult.left=current} '*' right=Prim)*;\n");
		grammar.append("Prim returns Expr: {Val} name=ID | '(' Addition ')';\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Addition: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("  Addition_Add returns Add: (left=Addition_Add_1_0 right=Multiplication);\n");
		expected.append("  Multiplication_Mult returns Mult: (left=Multiplication_Mult_1_0 right=Prim);\n");
		expected.append("  Prim_Val returns Val: name=ID;\n");
		expected.append("Addition_Add_1_0: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("Multiplication: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("Multiplication_Mult_1_0: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("Prim: Addition_Add | Multiplication_Mult | Prim_Val;");
		assertEquals(expected.toString(), actual);
	}

	public void testExpression3() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Addition returns Expr: Prim ({Add.children+=current} ('+' children+=Prim)+)?;\n");
		grammar.append("Prim returns Expr: {Val} name=ID | '(' Addition ')';\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Addition: Addition_Add | Prim_Val;\n");
		expected.append("  Addition_Add returns Add: (children+=Addition_Add_1_0 children+=Prim+);\n");
		expected.append("  Prim_Val returns Val: name=ID;\n");
		expected.append("Addition_Add_1_0: Addition_Add | Prim_Val;\n");
		expected.append("Prim: Addition_Add | Prim_Val;");
		assertEquals(expected.toString(), actual);
	}

	public void testExpression4() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Addition returns Expr: Multiplication ({Add.addCh+=current} ('+' addCh+=Multiplication)+)?;\n");
		grammar.append("Multiplication returns Expr: Prim ({Mult.mulCh+=current} ('*' mulCh+=Prim)+)?;\n");
		grammar.append("Prim returns Expr: {Val} name=ID | '(' Addition ')';\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Addition: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("  Addition_Add returns Add: (addCh+=Addition_Add_1_0 addCh+=Multiplication+);\n");
		expected.append("  Multiplication_Mult returns Mult: (mulCh+=Multiplication_Mult_1_0 mulCh+=Prim+);\n");
		expected.append("  Prim_Val returns Val: name=ID;\n");
		expected.append("Addition_Add_1_0: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("Multiplication: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("Multiplication_Mult_1_0: Addition_Add | Multiplication_Mult | Prim_Val;\n");
		expected.append("Prim: Addition_Add | Multiplication_Mult | Prim_Val;");
		assertEquals(expected.toString(), actual);
	}

	public void testExpression5() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Addition returns Expr: Multiplication ({Bin.left+=current} op='+' right=Multiplication)*;\n");
		grammar.append("Multiplication returns Expr: Prim ({Bin.left+=current} op='*' right=Prim)*;\n");
		grammar.append("Prim returns Expr: {Val} name=ID | '(' Addition ')';\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Addition: Addition_Bin | Prim_Val;\n");
		expected.append("  Addition_Bin returns Bin: ((left+=Addition_Bin_1_0 op='+' right=Multiplication) | (left+=Multiplication_Bin_1_0 op='*' right=Prim));\n");
		expected.append("  Prim_Val returns Val: name=ID;\n");
		expected.append("Addition_Bin_1_0: Addition_Bin | Prim_Val;\n");
		expected.append("Multiplication: Addition_Bin | Prim_Val;\n");
		expected.append("Multiplication_Bin_1_0: Addition_Bin | Prim_Val;\n");
		expected.append("Prim: Addition_Bin | Prim_Val;");
		assertEquals(expected.toString(), actual);
	}

	public void testExpression6() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Assignment returns Expr: Addition ({Bin.left+=current} op='=' right=Addition)*;\n");
		grammar.append("Addition returns Expr: Multiplication ({Bin.left+=current} op='+' right=Multiplication)*;\n");
		grammar.append("Multiplication returns Expr: Prim ({Bin.left+=current} op='*' right=Prim)*;\n");
		grammar.append("Prim returns Expr: {Val} name=ID | '(' Assignment ')';\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Assignment: Addition_Bin | Prim_Val;\n");
		expected.append("  Addition_Bin returns Bin: ((left+=Addition_Bin_1_0 op='+' right=Multiplication) | (left+=Multiplication_Bin_1_0 op='*' right=Prim) | (left+=Assignment_Bin_1_0 op='=' right=Addition));\n");
		expected.append("  Prim_Val returns Val: name=ID;\n");
		expected.append("Assignment_Bin_1_0: Addition_Bin | Prim_Val;\n");
		expected.append("Addition: Addition_Bin | Prim_Val;\n");
		expected.append("Addition_Bin_1_0: Addition_Bin | Prim_Val;\n");
		expected.append("Multiplication: Addition_Bin | Prim_Val;\n");
		expected.append("Multiplication_Bin_1_0: Addition_Bin | Prim_Val;\n");
		expected.append("Prim: Addition_Bin | Prim_Val;");
		assertEquals(expected.toString(), actual);
	}

	public void testActionSequence1() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Rule: val1=ID {A.a1=current} a2=ID {B.b1=current} b2=ID {C.c1=current} c2=ID;\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_C;\n");
		expected.append("  Rule_C returns C: (c1=Rule_C_5 c2=ID);\n");
		expected.append("Rule_A_1: Rule_A_1_Rule;\n");
		expected.append("  Rule_A_1_Rule returns Rule: val1=ID;\n");
		expected.append("Rule_B_3: Rule_B_3_A;\n");
		expected.append("  Rule_B_3_A returns A: (a1=Rule_A_1 a2=ID);\n");
		expected.append("Rule_C_5: Rule_C_5_B;\n");
		expected.append("  Rule_C_5_B returns B: (b1=Rule_B_3 b2=ID);");
		assertEquals(expected.toString(), actual);
	}

	public void testActionSequence2() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Rule: val1=ID {A.a1=current} a2=ID {A.a1=current} a2=ID {A.a1=current} a2=ID;\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_A;\n");
		expected.append("  Rule_A returns A: (a1=Rule_A_5 a2=ID);\n");
		expected.append("Rule_A_1: Rule_A_1_Rule;\n");
		expected.append("  Rule_A_1_Rule returns Rule: val1=ID;\n");
		expected.append("Rule_A_3: Rule_A_3_A;\n");
		expected.append("  Rule_A_3_A returns A: (a1=Rule_A_1 a2=ID);\n");
		expected.append("Rule_A_5: Rule_A_5_A;\n");
		expected.append("  Rule_A_5_A returns A: (a1=Rule_A_3 a2=ID);");
		assertEquals(expected.toString(), actual);
	}

	public void testActionSequence3() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Rule: val1=ID ({A.a1=current} a2=ID ({A.a1=current} a2=ID ({A.a1=current} a2=ID)?)?)?;\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_A | Rule_Rule;\n");
		expected.append("  Rule_A returns A: ((a1=Rule_A_1_2_2_0 a2=ID) | (a1=Rule_A_1_2_0 a2=ID) | (a1=Rule_A_1_0 a2=ID));\n");
		expected.append("  Rule_Rule returns Rule: val1=ID;\n");
		expected.append("Rule_A_1_0: Rule_Rule;\n");
		expected.append("Rule_A_1_2_0: Rule_A_1_2_0_A;\n");
		expected.append("  Rule_A_1_2_0_A returns A: (a1=Rule_A_1_0 a2=ID);\n");
		expected.append("Rule_A_1_2_2_0: Rule_A_1_2_2_0_A;\n");
		expected.append("  Rule_A_1_2_2_0_A returns A: (a1=Rule_A_1_2_0 a2=ID);");
		assertEquals(expected.toString(), actual);
	}

	public void testActionAlternative1() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Rule: root=ID (val1=ID | {A.a1=current} a2=ID | {B.b1=current} b2=ID | {C.c1=current} c2=ID);\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Rule: Rule_A | Rule_B | Rule_C | Rule_Rule;\n");
		expected.append("  Rule_A returns A: (a1=Rule_A_1_1_0 a2=ID);\n");
		expected.append("  Rule_B returns B: (b1=Rule_B_1_2_0 b2=ID);\n");
		expected.append("  Rule_C returns C: (c1=Rule_C_1_3_0 c2=ID);\n");
		expected.append("  Rule_Rule returns Rule: (root=ID val1=ID);\n");
		expected.append("Rule_A_1_1_0: Rule_A_1_1_0_B_1_2_0_C_1_3_0_Rule;\n");
		expected.append("  Rule_A_1_1_0_B_1_2_0_C_1_3_0_Rule returns Rule: root=ID;\n");
		expected.append("Rule_B_1_2_0: Rule_A_1_1_0_B_1_2_0_C_1_3_0_Rule;\n");
		expected.append("Rule_C_1_3_0: Rule_A_1_1_0_B_1_2_0_C_1_3_0_Rule;");
		assertEquals(expected.toString(), actual);
	}

	/*
	XMemberFeatureCall returns XExpression:
	XPrimaryExpression
	(=>({XAssignment.assignable=current} '.' feature=[types::JvmIdentifiableElement] OpSingleAssign) value=XAssignment
	|=>({XMemberFeatureCall.memberCallTarget=current} ("."|nullSafe?="?."|spreading?="*.")) 
		('<' typeArguments+=JvmArgumentTypeReference (',' typeArguments+=JvmArgumentTypeReference)* '>')?  
		feature=[types::JvmIdentifiableElement] (
			=>explicitOperationCall?='(' 
				(
				    memberCallArguments+=XShortClosure
				  |	memberCallArguments+=XExpression (',' memberCallArguments+=XExpression)*
				)? 
			')')?
		)*;
	 */
	public void testActionAlternative2() throws Exception {
		StringBuilder grammar = new StringBuilder();
		grammar.append("Model: Bar ({Foo.f1=current} f2=ID f3=ID? f4=ID)*; Bar: bar=ID;\n");
		String actual = getParserRule(grammar.toString());
		StringBuilder expected = new StringBuilder();
		expected.append("Model: Bar_Bar | Model_Foo;\n");
		expected.append("  Bar_Bar returns Bar: bar=ID;\n");
		expected.append("  Model_Foo returns Foo: (f1=Model_Foo_1_0 f2=ID f3=ID? f4=ID);\n");
		expected.append("Model_Foo_1_0: Bar_Bar | Model_Foo;\n");
		expected.append("Bar: Bar_Bar;");
		assertEquals(expected.toString(), actual);
	}
}
