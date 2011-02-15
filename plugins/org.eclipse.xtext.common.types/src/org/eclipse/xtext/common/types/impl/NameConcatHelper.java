/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.impl;

import java.util.List;

import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeConstraint;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmWildcardTypeReference;
import org.eclipse.xtext.common.types.TypesPackage;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
class NameConcatHelper {

	enum NameType {
		ID, QUALIFIED, SIMPLE
	}
	
	static void appendConstraintsName(StringBuilder result, List<JvmTypeConstraint> constraints, char innerClassDelimiter, NameType nameType) {
		if (constraints == null || constraints.isEmpty())
			return;
		int wasLength = result.length();
		for (JvmTypeConstraint constraint : constraints) {
			if (result.length() != wasLength)
				result.append(" & ");
			switch(nameType) {
				case ID: result.append(constraint.getIdentifier()); break;
				case QUALIFIED: result.append(constraint.getQualifiedName(innerClassDelimiter)); break;
				case SIMPLE: result.append(constraint.getSimpleName()); break;
			}
		}
	}

	static String computeFor(JvmWildcardTypeReference typeReference, char innerClassDelimiter, NameType nameType) {
		if (typeReference.eIsSet(TypesPackage.Literals.JVM_CONSTRAINT_OWNER__CONSTRAINTS)) {
			StringBuilder mutableResult = new StringBuilder(64);
			mutableResult.append("? ");
			appendConstraintsName(mutableResult, typeReference.getConstraints(), innerClassDelimiter, nameType);
			return mutableResult.toString();
		}
		return "?";
	}
	
	static String computeFor(JvmParameterizedTypeReference typeReference, char innerClassDelimiter, NameType nameType) {
		if (typeReference.eIsSet(TypesPackage.Literals.JVM_PARAMETERIZED_TYPE_REFERENCE__TYPE)) {
			StringBuilder mutableResult = new StringBuilder(64);
			JvmType type = typeReference.getType();
			switch(nameType) {
				case ID: mutableResult.append(type.getIdentifier()); break;
				case QUALIFIED: mutableResult.append(type.getQualifiedName(innerClassDelimiter)); break;
				case SIMPLE: mutableResult.append(type.getSimpleName()); break;
			}
			if (typeReference.eIsSet(TypesPackage.Literals.JVM_PARAMETERIZED_TYPE_REFERENCE__ARGUMENTS)) {
				mutableResult.append("<");
				appendArguments(mutableResult, typeReference.getArguments(), innerClassDelimiter, nameType);
				mutableResult.append(">");
				return mutableResult.toString();
			}
			return mutableResult.toString();
		}
		return null;
	}

	static void appendArguments(StringBuilder result, List<JvmTypeReference> arguments, char innerClassDelimiter, NameType nameType) {
		if (arguments == null || arguments.isEmpty())
			return;
		int wasLength = result.length();
		for (JvmTypeReference argument : arguments) {
			if (result.length() != wasLength)
				result.append(",");
			switch(nameType) {
				case ID: result.append(argument.getIdentifier()); break;
				case QUALIFIED: result.append(argument.getQualifiedName(innerClassDelimiter)); break;
				case SIMPLE: result.append(argument.getSimpleName()); break;
			}
		}
	}
	
}
