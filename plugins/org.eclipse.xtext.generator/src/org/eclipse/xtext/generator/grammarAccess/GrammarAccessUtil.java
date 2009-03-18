/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.grammarAccess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EnumLiteralDeclaration;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.XtextRuntimeModule;
import org.eclipse.xtext.parsetree.reconstr.SerializerUtil;
import org.eclipse.xtext.parsetree.reconstr.XtextSerializationException;

import com.google.inject.Guice;

/**
 * @author Moritz Eysholdt
 */
public class GrammarAccessUtil {

	public static String getClassName(EObject obj) {
		return obj.eClass().getName();
	}

	private static List<String> getElementDescription(AbstractElement element) {
		final ArrayList<String> result = new ArrayList<String>();
		AbstractElement container = element;
		while (container != null) {
			String s = getSingleElementDescription(container);
			if (s != null)
				result.add(0, s);
			container = container.eContainer() instanceof AbstractElement ?
					(AbstractElement) container.eContainer() : null;
		}
		return result;
	}

	private static String getSingleElementDescription(AbstractElement ele) {
		if (ele instanceof Keyword)
			return ((Keyword) ele).getValue();
		else if (ele instanceof Assignment)
			return ((Assignment) ele).getFeature();
		else if (ele instanceof RuleCall)
			return ((RuleCall) ele).getRule().getName();
		else if (ele instanceof Action) {
			Action a = (Action) ele;
			return (a.getType() != null && a.getType().getClassifier() != null ? a
					.getType().getClassifier().getName()
					: "")
					+ a.getFeature();
		} else if (ele instanceof CrossReference) {
			CrossReference cr = (CrossReference) ele;
			if (cr.getType() != null && cr.getType().getClassifier() != null)
				return cr.getType().getClassifier().getName();
		} else if (ele instanceof EnumLiteralDeclaration) {
			EnumLiteralDeclaration decl = (EnumLiteralDeclaration) ele;
			return decl.getEnumLiteral().getName();
		}
		return null;
	}

	private static SerializerUtil xtextSerializer = Guice.createInjector(
			new XtextRuntimeModule()).getInstance(SerializerUtil.class);

	public static String serialize(EObject obj, String prefix) {
		String s;
		try {
			s = xtextSerializer.serialize(obj);
		} catch (XtextSerializationException e) {
			s = e.toString();
			// e.printStackTrace();
		}
		s = prefix + s.replaceAll("[\\r\\n]", "\n" + prefix);
		return s;
	}

	private static String getElementPath(AbstractElement ele) {
		EObject obj = ele;
		StringBuffer buf = new StringBuffer();
		while ((!(obj.eContainer() instanceof AbstractRule))
				&& obj.eContainer() != null) {
			EObject tmp = obj.eContainer();
			buf.insert(0, tmp.eContents().indexOf(obj));
			buf.insert(0, "_");
			obj = tmp;
		}
		return buf.toString();
	}

	private static String getElementTypeDescription(AbstractElement ele) {
		if (ele instanceof RuleCall) {
			AbstractRule r = ((RuleCall) ele).getRule();
			return r.eClass().getName() + "Call";
		}
		return ele.eClass().getName();
	}

	public static String getGrammarAccessFQName(Grammar grammar) {
		return GrammarUtil.getNamespace(grammar) + ".services."
				+ getGrammarAccessSimpleName(grammar);
	}

	public static String getGrammarAccessSimpleName(Grammar grammar) {
		return GrammarUtil.getName(grammar) + "GrammarAccess";
	}

	public static String getUniqueElementName(AbstractElement ele) {
		try {
			if (ele == null)
				return "null";
			ArrayList<String> r = new ArrayList<String>();
			r.addAll(getElementDescription(ele));
			r.add(getElementTypeDescription(ele));
			r.add(getElementPath(ele));
			return toJavaIdentifier(r, true);
		} catch (Throwable t) {
			t.printStackTrace();
			return "failure";
		}
	}

	private static String toJavaIdentifier(List<String> text,
			boolean uppercaseFirst) {
		Iterator<String> i = text.iterator();
		StringBuffer b = new StringBuffer(toJavaIdentifierSegment(i.next(),
				true, uppercaseFirst));
		while (i.hasNext())
			b.append(toJavaIdentifierSegment(i.next(), false, true));
		return b.toString();
	}

	private static String toJavaIdentifierSegmentInt(String text,
			boolean isFirst, boolean uppercaseFirst) {
		boolean start = isFirst, up = true;
		StringBuffer r = new StringBuffer();
		for (char c : text.toCharArray()) {
			boolean valid = c!='$' /* special case: don't use dollar sign, since antlr uses them as variable prefix */
				&& (start ? Character.isJavaIdentifierStart(c) : Character.isJavaIdentifierPart(c));
			if (valid) {
				if (start)
					r.append(uppercaseFirst ? Character.toUpperCase(c)
							: Character.toLowerCase(c));
				else
					r.append(up ? Character.toUpperCase(c) : c);
				up = false;
				start = false;
			} else
				up = true;
		}
		return r.toString();
	}

	private static String toJavaIdentifierSegment(String text, boolean isFirst,
			boolean uppercaseFirst) {
		String r = toJavaIdentifierSegmentInt(text, isFirst, uppercaseFirst);
		if (r.length() > 0)
			return r;
		StringBuffer b = new StringBuffer();
		for (char c : text.toCharArray()) {
			String n = UnicodeCharacterDatabaseNames.getCharacterName(c);
			if (n != null)
				b.append(n + " ");
		}
		return toJavaIdentifierSegmentInt(b.toString().toLowerCase().trim(),
				isFirst, true);
	}

	public static String toJavaIdentifier(String text, Boolean uppercaseFirst) {
		try {
			return toJavaIdentifierSegment(text, true, uppercaseFirst);
		} catch (Throwable t) {
			t.printStackTrace();
			return "%_FAILURE_(" + text + ")%";
		}
	}
}
