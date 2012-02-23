/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.xbase.XBooleanLiteral;
import org.eclipse.xtext.xbase.XClosure;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.XNullLiteral;
import org.eclipse.xtext.xbase.XNumberLiteral;
import org.eclipse.xtext.xbase.XStringLiteral;
import org.eclipse.xtext.xbase.XTypeLiteral;
import org.eclipse.xtext.xbase.compiler.output.ITreeAppendable;
import org.eclipse.xtext.xbase.typing.NumberLiterals;

import com.google.inject.Inject;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class LiteralsCompiler extends TypeConvertingCompiler {

	@Inject
	private TypeReferenceSerializer referenceSerializer;	

	@Inject
	private NumberLiterals numberLiterals;
	
	@Override
	protected void internalToConvertedExpression(XExpression obj, ITreeAppendable appendable) {
		if (obj instanceof XStringLiteral) {
			_toJavaExpression((XStringLiteral) obj, appendable);
		} else if (obj instanceof XNumberLiteral) {
			_toJavaExpression((XNumberLiteral) obj, appendable);
		} else if (obj instanceof XNullLiteral) {
			_toJavaExpression((XNullLiteral) obj, appendable);
		} else if (obj instanceof XBooleanLiteral) {
			_toJavaExpression((XBooleanLiteral) obj, appendable);
		} else if (obj instanceof XTypeLiteral) {
			_toJavaExpression((XTypeLiteral) obj, appendable);
		} else {
			super.internalToConvertedExpression(obj, appendable);
		}
	}
	
	@Override
	protected void doInternalToJavaStatement(XExpression obj, ITreeAppendable appendable, boolean isReferenced) {
		if (obj instanceof XStringLiteral) {
			_toJavaStatement((XStringLiteral) obj, appendable, isReferenced);
		} else if (obj instanceof XNumberLiteral) {
			_toJavaStatement((XNumberLiteral) obj, appendable, isReferenced);
		} else if (obj instanceof XNullLiteral) {
			_toJavaStatement((XNullLiteral) obj, appendable, isReferenced);
		} else if (obj instanceof XBooleanLiteral) {
			_toJavaStatement((XBooleanLiteral) obj, appendable, isReferenced);
		} else if (obj instanceof XTypeLiteral) {
			_toJavaStatement((XTypeLiteral) obj, appendable, isReferenced);
		} else {
			super.doInternalToJavaStatement(obj, appendable, isReferenced);
		}
	}
	
	public void _toJavaExpression(XStringLiteral expr, ITreeAppendable b) {
		String javaString = Strings.convertToJavaString(expr.getValue());
		b.append("\"").append(javaString).append("\"");
	}
	
	public void _toJavaStatement(XStringLiteral expr, ITreeAppendable b, boolean isReferenced) {
		generateComment(expr, b, isReferenced);
	}

	protected void generateComment(XExpression expr, ITreeAppendable b, boolean isReferenced) {
		if (!isReferenced) {
			b.append("/*");
			internalToJavaExpression(expr, b);
			b.append("*/;");
		}
	}

	public void _toJavaExpression(XNumberLiteral expr, ITreeAppendable b) {
		JvmTypeReference type = getTypeProvider().getType(expr);
		if(getTypeReferences().is(type, BigInteger.class)) {
			b.append("new ").append(type.getType()).append("(\"")
				.append(numberLiterals.getDigits(expr)).append("\", ")
				.append("" + numberLiterals.getBase(expr))
				.append(")");
		} else if(getTypeReferences().is(type, BigDecimal.class)) {			
			b.append("new ").append(type.getType()).append("(\"")
				.append(numberLiterals.getDigits(expr)).append("\")");
		} else {
			b.append(numberLiterals.toJavaLiteral(expr));
		}
	}
	
	public void _toJavaStatement(XNumberLiteral expr, ITreeAppendable b, boolean isReferenced) {
		generateComment(expr, b, isReferenced);
	}

	/**
	 * @param expr the expression. Used by the dispatch strategy.
	 */
	public void _toJavaExpression(XNullLiteral expr, ITreeAppendable b) {
		b.append("null");
	}
	
	public void _toJavaStatement(XNullLiteral expr, ITreeAppendable b, boolean isReferenced) {
		generateComment(expr, b, isReferenced);
	}

	public void _toJavaExpression(XBooleanLiteral expr, ITreeAppendable b) {
		b.append(Boolean.toString(expr.isIsTrue()));
	}
	
	public void _toJavaStatement(XBooleanLiteral expr, ITreeAppendable b, boolean isReferenced) {
		generateComment(expr, b, isReferenced);
	}

	public void _toJavaExpression(XTypeLiteral expr, ITreeAppendable b) {
		b.append(expr.getType().getQualifiedName('.')).append(".class");
	}
	
	public void _toJavaStatement(XTypeLiteral expr, ITreeAppendable b, boolean isReferenced) {
		generateComment(expr, b, isReferenced);
	}
	
	@Override
	protected boolean isVariableDeclarationRequired(XExpression expr, ITreeAppendable b) {
		if (expr instanceof XBooleanLiteral
			|| expr instanceof XStringLiteral
			|| expr instanceof XTypeLiteral
			|| expr instanceof XClosure
			|| expr instanceof XNullLiteral)
			return false;
		if(expr instanceof XNumberLiteral) {
			JvmTypeReference expectedType = getTypeProvider().getExpectedType(expr);
			return getTypeReferences().is(expectedType, BigInteger.class) ||
					getTypeReferences().is(expectedType, BigDecimal.class);
		}
		return super.isVariableDeclarationRequired(expr,b);
	}
}
