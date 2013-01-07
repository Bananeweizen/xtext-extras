/*******************************************************************************
 * Copyright (c) 2013 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.override;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.xbase.typesystem.InferredTypeIndicator;
import org.eclipse.xtext.xbase.typesystem.legacy.StandardTypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.ITypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.LightweightMergedBoundTypeArgument;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.OwnedConverter;
import org.eclipse.xtext.xbase.typesystem.references.ParameterizedTypeReference;
import org.eclipse.xtext.xbase.typesystem.util.CommonTypeComputationServices;
import org.eclipse.xtext.xbase.typesystem.util.ContextualVisibilityHelper;
import org.eclipse.xtext.xbase.typesystem.util.DeclaratorTypeArgumentCollector;
import org.eclipse.xtext.xbase.typesystem.util.IVisibilityHelper;
import org.eclipse.xtext.xbase.typesystem.util.StandardTypeParameterSubstitutor;
import org.eclipse.xtext.xbase.typesystem.util.TypeParameterSubstitutor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Utilities to deal with overridden or implemented operations.
 * 
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@Singleton
@NonNullByDefault
public class OverrideHelper {

	@Inject
	private IVisibilityHelper visibilityHelper;
	
	@Inject
	private CommonTypeComputationServices services;
	
	/**
	 * Returns <code>null</code> if the given operation declares it's own return type or if it does not override
	 * another operation.
	 * 
	 * TODO support this case:
	 * 
	 * <pre>
	 * interface I {
	 *   String m()
	 *   String m2()
	 * }
	 * class A {
	 *   CharSequence m()
	 *   int m2()
	 * }
	 * class B extends A implements I {
	 *   m() will expect String since this is the best choice
	 *   m2() will expect int since this is actually overridden and not compatible to String from I#m2
	 * }
	 * </pre>
	 */
	@Nullable
	public LightweightTypeReference getReturnTypeOfOverriddenOperation(JvmOperation operation, ITypeReferenceOwner owner, IVisibilityHelper visibilityHelper) {
		if (operation.getVisibility() == JvmVisibility.PRIVATE || !InferredTypeIndicator.isInferred(operation.getReturnType())) {
			return null;
		}
		LightweightTypeReference declaringType = new ParameterizedTypeReference(owner, operation.getDeclaringType());
		TypeParameterSubstitutor<?> substitutor = createSubstitutor(owner, declaringType);
		OwnedConverter converter = new OwnedConverter(owner);
		JvmOperation overriddenOperation = findOverriddenOperation(operation, declaringType, substitutor, converter, visibilityHelper);
		if (overriddenOperation != null) {
			return substitutor.substitute(converter.toLightweightReference(overriddenOperation.getReturnType()));
		}
		return null;
	}

	@Nullable
	protected JvmOperation findOverriddenOperation(JvmOperation operation, LightweightTypeReference declaringType,
			TypeParameterSubstitutor<?> substitutor, OwnedConverter converter, IVisibilityHelper visibilityHelper) {
		int parameterSize = operation.getParameters().size();
		List<LightweightTypeReference> superTypes = declaringType.getSuperTypes();
		for(LightweightTypeReference superType: superTypes) {
			JvmDeclaredType declaredSuperType = (JvmDeclaredType) superType.getType();
			if (declaredSuperType != null) {
				Iterable<JvmFeature> equallyNamedFeatures = declaredSuperType.findAllFeaturesByName(operation.getSimpleName());
				for(JvmFeature feature: equallyNamedFeatures) {
					if (feature instanceof JvmOperation) {
						JvmOperation candidate = (JvmOperation) feature;
						if (parameterSize == candidate.getParameters().size()) {
							if (visibilityHelper.isVisible(feature)) {
								boolean matchesSignature = true;
								for(int i = 0; i < parameterSize && matchesSignature; i++) {
									JvmFormalParameter parameter = operation.getParameters().get(i);
									String identifier = parameter.getParameterType().getIdentifier();
									JvmFormalParameter candidateParameter = candidate.getParameters().get(i);
									LightweightTypeReference candidateParameterType =
											substitutor.substitute(converter.toLightweightReference(candidateParameter.getParameterType()));
									if (!identifier.equals(candidateParameterType.getIdentifier())) {
										matchesSignature = false;
									}
								}
								if (matchesSignature) {
									return candidate;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	protected TypeParameterSubstitutor<?> createSubstitutor(ITypeReferenceOwner owner,
			LightweightTypeReference declaringType) {
		Map<JvmTypeParameter, LightweightMergedBoundTypeArgument> parameterMapping = new DeclaratorTypeArgumentCollector().getTypeParameterMapping(declaringType);
		StandardTypeParameterSubstitutor substitutor = new StandardTypeParameterSubstitutor(parameterMapping, owner);
		return substitutor;
	}
	
	@Nullable
	public JvmOperation findOverriddenOperation(JvmOperation operation) {
		if (operation.getVisibility() == JvmVisibility.PRIVATE) {
			return null;
		}
		ITypeReferenceOwner owner = new StandardTypeReferenceOwner(services, operation.eResource().getResourceSet());
		LightweightTypeReference declaringType = new ParameterizedTypeReference(owner, operation.getDeclaringType());
		TypeParameterSubstitutor<?> substitutor = createSubstitutor(owner, declaringType);
		OwnedConverter converter = new OwnedConverter(owner);
		return findOverriddenOperation(operation, declaringType, substitutor, converter, new ContextualVisibilityHelper(visibilityHelper, declaringType));
	}

	/**
	 * Returns all operations that are defined in the given <code>type</code> and that are not overridden.
	 * Considers private methods of super types, too.
	 * @return all operations. May be empty. 
	 */
	public List<JvmOperation> getAllOperations(JvmDeclaredType type) {
		ITypeReferenceOwner owner = new StandardTypeReferenceOwner(services, type.eResource().getResourceSet()); 
		return getAllOperations(type, owner, IVisibilityHelper.ALL);
	}
	
	public List<JvmOperation> getAllOperations(JvmDeclaredType type, ITypeReferenceOwner owner, IVisibilityHelper visibilityHelper) {
		LightweightTypeReference declaringType = new ParameterizedTypeReference(owner, type);
		return getAllOperations(declaringType, visibilityHelper);
	}

	public List<JvmOperation> getAllOperations(LightweightTypeReference declaringType, IVisibilityHelper visibilityHelper) {
		if (!(declaringType.getType() instanceof JvmDeclaredType)) {
			return Collections.emptyList();
		}
		Iterable<JvmFeature> allFeatures = ((JvmDeclaredType) declaringType.getType()).getAllFeatures();
		TypeParameterSubstitutor<?> substitutor = createSubstitutor(declaringType.getOwner(), declaringType);
		OwnedConverter converter = new OwnedConverter(declaringType.getOwner());
		List<JvmOperation> result = Lists.newArrayList();
		Set<String> signatures = Sets.newHashSet();
		for(JvmFeature feature: allFeatures) {
			if (feature instanceof JvmOperation) {
				JvmOperation operation = (JvmOperation) feature;
				if (visibilityHelper.isVisible(operation)) {
					if (!operation.isStatic()) {
						List<JvmFormalParameter> parameters = operation.getParameters();
						StringBuilder signature = new StringBuilder(operation.getSimpleName());
						if (parameters.isEmpty()) {
							signature.append("()");
						} else {
							signature.append("(");
							for(JvmFormalParameter parameter: parameters) {
								LightweightTypeReference parameterType =
										substitutor.substitute(converter.toLightweightReference(parameter.getParameterType()));
								signature.append(parameterType.getIdentifier());
								signature.append(",");
							}
							signature.replace(signature.length() - 1, signature.length(), ")");
						}
						if (signatures.add(signature.toString())) {
							result.add(operation);
						}
					} else {
						result.add(operation);
					}
				}
			}
		}
		return result;
	}
	
}

