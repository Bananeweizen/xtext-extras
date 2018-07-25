/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.util

import org.eclipse.xtext.common.types.JvmFeature
import org.eclipse.xtext.common.types.JvmOperation

import static extension java.beans.Introspector.*
import static extension java.lang.Character.*
import java.util.Locale

/**
 * @author kosyakov - Initial contribution and API
 */
class PropertyUtil {

	def static String getPropertyName(JvmFeature feature) {
		if (feature.static) {
			feature.getPropertyName(feature.simpleName, 1, 2)
		} else {
			feature.getPropertyName(feature.simpleName, 0, 1)
		}
	}

	def static String getPropertyName(JvmFeature feature, String methodName, int getterParams, int setterParams) {
		if (feature instanceof JvmOperation)
			feature.getPropertyName(methodName, 'get', getterParams) ?:
			feature.getPropertyName(methodName, 'set', setterParams) ?:
			feature.getPropertyName(methodName, 'is', getterParams)
	}

	protected static def getPropertyName(
		JvmOperation operation,
		String methodName,
		String prefix,
		int params
	) {
		val prefixLength = prefix.length
		if (methodName.startsWithPrefix(prefix, prefixLength) && operation.parameters.size === params)
			methodName.substring(prefixLength).decapitalize
	}

	protected def static startsWithPrefix(String methodName, String prefix, int prefixLength) {
		methodName.length > prefixLength && methodName.startsWith(prefix) && methodName.charAt(prefixLength).upperCase
	}

	/**
	 * Returns the name as a property name, e.g. a prefix {@code get}, {@code is} or {@code set}
	 * can be used with the result of this method.
	 * If the given name is invalid, the result is <code>null</code>.
	 * 
	 * @since 2.15
	 */
	/* @Nullable */
	def static String tryGetAsPropertyName(String name) {
		if (name.length() == 1) { // e.g. Point.getX()
			if (Character.isUpperCase(name.charAt(0))) {
				// X is not a valid sugar for getX()
				return null;
			}
			// x is a valid sugar for getX
			return name.toUpperCase(Locale.ENGLISH);
		} else if (name.length() > 1) {
			if (Character.isUpperCase(name.charAt(1))) { // e.g. Resource.getURI
				// if second char is uppercase, the name itself is the sugar variant
				// URI is the property name for getURI
				if (Character.isUpperCase(name.charAt(0))) {
					return name;
				}
				// if the first character is not upper case, it's not a valid sugar variant
				// e.g. uRI is no sugar access for getURI
				return null;
			} else if (Character.isUpperCase(name.charAt(0))) {
				// the first character is upper case, it is not valid property sugar, e.g.
				// Class.CanonicalName does not map to Class.getCanonicalName
				return null;
			} else {
				// code from java.beans.NameGenerator.capitalize()
				return name.substring(0, 1).toUpperCase(Locale.ENGLISH) + name.substring(1);
			}
		}
		// length 0 is invalid
		return null;
	}
}	