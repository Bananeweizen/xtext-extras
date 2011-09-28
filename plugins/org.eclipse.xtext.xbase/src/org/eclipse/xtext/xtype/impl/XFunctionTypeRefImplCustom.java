/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtype.impl;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmDelegateTypeReference;
import org.eclipse.xtext.common.types.JvmLowerBound;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmPrimitiveType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmUpperBound;
import org.eclipse.xtext.common.types.JvmVoid;
import org.eclipse.xtext.common.types.JvmWildcardTypeReference;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.xbase.lib.Functions;

import com.google.common.collect.Lists;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class XFunctionTypeRefImplCustom extends XFunctionTypeRefImpl {
	
	// TODO should we update the type as soon as the number of argument types changes?  
	@Override
	public JvmType getType() {
		if (this.type == null) {
//			// make sure scoping has taken place and installed an IJvmTypeProvider
//			if (returnType != null)
//				returnType.getType();
			JvmType newType = TypesFactory.eINSTANCE.createJvmVoid();
			((InternalEObject)newType).eSetProxyURI(computeTypeUri());
			type = (JvmType) eResolveProxy((InternalEObject) newType);
		}
		return super.getType();
	}
	
	@Override
	public JvmTypeReference getEquivalent() {
		if (equivalent == null) {
			TypesFactory typesFactory = TypesFactory.eINSTANCE;
			JvmType rawType = getType();
			if (rawType != null && rawType instanceof JvmDeclaredType) {
//				EList<JvmTypeReference> superTypesWithObject = ((JvmDeclaredType) rawType).getSuperTypes();
//				JvmTypeReference objectReference = superTypesWithObject.get(0);
				JvmParameterizedTypeReference result = typesFactory.createJvmParameterizedTypeReference();
				result.setType(rawType);
				for(JvmTypeReference paramType: Lists.newArrayList(getParamTypes())) {
//					JvmWildcardTypeReference paramWildcard = typesFactory.createJvmWildcardTypeReference();
//					JvmLowerBound lowerBound = typesFactory.createJvmLowerBound();
					JvmTypeReference wrapped = wrapIfNecessary(paramType);
					if (wrapped == null || wrapped.eContainer() != null) {
						JvmDelegateTypeReference delegate = typesFactory.createJvmDelegateTypeReference();
						delegate.setDelegate(wrapped);
//					lowerBound.setTypeReference(delegate);
//					JvmUpperBound upperBound = typesFactory.createJvmUpperBound();
//					JvmDelegateTypeReference objectDelegate = typesFactory.createJvmDelegateTypeReference();
//					objectDelegate.setDelegate(objectReference);
//					upperBound.setTypeReference(objectDelegate);
					
//					paramWildcard.getConstraints().add(upperBound);
//					paramWildcard.getConstraints().add(lowerBound);
//					result.getArguments().add(paramWildcard);
					
						result.getArguments().add(delegate);
					} else {
						result.getArguments().add(wrapped);
					}
				}
				{
//					JvmWildcardTypeReference returnType = typesFactory.createJvmWildcardTypeReference();
//					JvmUpperBound returnTypeBound = typesFactory.createJvmUpperBound();
					JvmTypeReference wrapped = wrapIfNecessary(getReturnType());
					if (wrapped == null || wrapped.eContainer() != null) {
						JvmDelegateTypeReference delegate = typesFactory.createJvmDelegateTypeReference();
						delegate.setDelegate(wrapped);
//					returnTypeBound.setTypeReference(delegate);
//					returnType.getConstraints().add(returnTypeBound);
						result.getArguments().add(delegate);
					} else {
						result.getArguments().add(wrapped);
					}
				}
				equivalent = result;
			} else {
				equivalent = null;
			}
		}
		return equivalent;
	}
	
	public JvmTypeReference wrapIfNecessary(JvmTypeReference reference) {
		if (reference == null)
			return null;
		JvmType type = reference.getType();
		if (type instanceof JvmPrimitiveType) {
			JvmType wrappedType = null;
			String name = type.getIdentifier();
			if ("int".equals(name)) {
				wrappedType = getType(Integer.class, type);
			} else if ("boolean".equals(name)) {
				wrappedType = getType(Boolean.class, type);
			} else if ("char".equals(name)) {
				wrappedType = getType(Character.class, type);
			} else if ("long".equals(name)) {
				wrappedType = getType(Long.class, type);
			} else if ("double".equals(name)) {
				wrappedType = getType(Double.class, type);
			} else if ("byte".equals(name)) {
				wrappedType = getType(Byte.class, type);
			} else if ("float".equals(name)) {
				wrappedType = getType(Float.class, type);
			} else if ("short".equals(name)) {
				wrappedType = getType(Short.class, type);
			}
			if (wrappedType == null) {
				return reference;
			}
			JvmParameterizedTypeReference result = TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
			result.setType(wrappedType);
			return result;
		} else if (type instanceof JvmVoid && !type.eIsProxy()) {
			JvmParameterizedTypeReference result = TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
			JvmType wrappedType = getType(Void.class, type);
			result.setType(wrappedType);
			return result;
		}
		return reference;
	}
	
	protected JvmType getType(Class<?> clazz, EObject context) {
		InternalEObject proxy = (InternalEObject) TypesFactory.eINSTANCE.createJvmVoid();
		proxy.eSetProxyURI(computeTypeUri(clazz));
		return (JvmType) EcoreUtil.resolve(proxy, context);
	}
	
	protected URI computeTypeUri() {
		return URI.createURI("java:/Objects/"+Functions.class.getCanonicalName()+"#"+Functions.class.getCanonicalName()+"$Function"+getParamTypes().size());
	}
	
	protected URI computeTypeUri(Class<?> topLevelClass) {
		return URI.createURI("java:/Objects/"+topLevelClass.getCanonicalName()+"#"+topLevelClass.getCanonicalName());
	}
	
	@Override
	public String getIdentifier() {
		StringBuilder result = new StringBuilder("(");
		for (int i = 0;i< getParamTypes().size();i++) {
			JvmTypeReference reference = getParamTypes().get(i);
			result.append(reference.getIdentifier());
			if (i<getParamTypes().size()-1)
				result.append(", ");
		}
		result.append(")=>");
		if (getReturnType()!=null)
			result.append(getReturnType().getIdentifier());
		return result.toString();
	}
	
	@Override
	public String getQualifiedName(char innerClassDelimiter) {
		StringBuilder result = new StringBuilder("(");
		for (int i = 0;i< getParamTypes().size();i++) {
			JvmTypeReference reference = getParamTypes().get(i);
			result.append(reference.getQualifiedName(innerClassDelimiter));
			if (i<getParamTypes().size()-1)
				result.append(", ");
		}
		result.append(")=>");
		if (getReturnType()!=null)
			result.append(getReturnType().getQualifiedName(innerClassDelimiter));
		return result.toString();
	}
	
	@Override
	public String getSimpleName() {
		StringBuilder result = new StringBuilder("(");
		for (int i = 0;i< getParamTypes().size();i++) {
			JvmTypeReference reference = getParamTypes().get(i);
			result.append(reference.getSimpleName());
			if (i<getParamTypes().size()-1)
				result.append(", ");
		}
		result.append(")=>");
		if (getReturnType()!=null)
			result.append(getReturnType().getSimpleName());
		return result.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder(eClass().getName());
		result.append(": ");
		result.append(getIdentifier());
		return result.toString();
	}
}
