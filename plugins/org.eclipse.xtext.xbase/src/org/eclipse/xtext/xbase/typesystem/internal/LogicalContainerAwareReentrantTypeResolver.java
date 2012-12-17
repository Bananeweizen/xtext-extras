/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.internal;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.common.types.JvmAnnotationAnnotationValue;
import org.eclipse.xtext.common.types.JvmAnnotationReference;
import org.eclipse.xtext.common.types.JvmAnnotationTarget;
import org.eclipse.xtext.common.types.JvmAnnotationValue;
import org.eclipse.xtext.common.types.JvmConstructor;
import org.eclipse.xtext.common.types.JvmCustomAnnotationValue;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmFeature;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.common.types.JvmTypeParameterDeclarator;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.ILogicalContainerProvider;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureNames;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.InferredTypeIndicator;
import org.eclipse.xtext.xbase.typesystem.computation.ITypeComputationResult;
import org.eclipse.xtext.xbase.typesystem.references.LightweightMergedBoundTypeArgument;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.OwnedConverter;
import org.eclipse.xtext.xbase.typesystem.util.AbstractReentrantTypeReferenceProvider;
import org.eclipse.xtext.xbase.typesystem.util.DeclaratorTypeArgumentCollector;
import org.eclipse.xtext.xbase.typesystem.util.StandardTypeParameterSubstitutor;
import org.eclipse.xtext.xtype.XComputedTypeReference;
import org.eclipse.xtext.xtype.impl.XComputedTypeReferenceImplCustom;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 * TODO JavaDoc, toString
 */
@NonNullByDefault
public class LogicalContainerAwareReentrantTypeResolver extends DefaultReentrantTypeResolver {

	public class DemandTypeReferenceProvider extends AbstractReentrantTypeReferenceProvider {
		private final JvmMember member;
		private final ResolvedTypes resolvedTypes;
		private final boolean returnType;
		private final IFeatureScopeSession session;
		private final XExpression expression;
		private final Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext;

		public DemandTypeReferenceProvider(JvmMember member, XExpression expression, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext, ResolvedTypes resolvedTypes, IFeatureScopeSession session, boolean returnType) {
			this.member = member;
			this.expression = expression;
			this.resolvedTypesByContext = resolvedTypesByContext;
			this.resolvedTypes = resolvedTypes;
			this.session = session;
			this.returnType = returnType;
		}

		@Override
		@Nullable
		protected JvmTypeReference doGetTypeReference(XComputedTypeReferenceImplCustom context) {
			LightweightTypeReference actualType = returnType ? resolvedTypes.getReturnType(expression) : resolvedTypes.getActualType(expression);
			if (actualType == null) {
				computeTypes(resolvedTypesByContext, resolvedTypes, session, member);
				actualType = returnType ? resolvedTypes.getReturnType(expression) : resolvedTypes.getActualType(expression);
			}
			if (actualType == null)
				return null;
			return actualType.toJavaCompliantTypeReference();
		}
	}

	@Inject
	private ILogicalContainerProvider logicalContainerProvider;
	
	@Override
	public void initializeFrom(EObject root) {
		if (!(root instanceof JvmType)) {
			throw new IllegalArgumentException("only JvmTypes are supported as root by this resolver");
		}
		super.initializeFrom(root);
	}
	
	@Override
	protected JvmType getRoot() {
		return (JvmType) super.getRoot();
	}
	
	/**
	 * Assign computed type references to the identifiable structural elements in the processed type.
	 * @return the stacked resolved types that shall be used in the computation.
	 */
	protected Map<JvmIdentifiableElement, ResolvedTypes> prepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession) {
		Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext = Maps.newHashMapWithExpectedSize(3); 
		doPrepare(resolvedTypes, featureScopeSession, getRoot(), resolvedTypesByContext);
		return resolvedTypesByContext;
	}
	
	protected void doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmIdentifiableElement element, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext) {
		if (element instanceof JvmDeclaredType) {
			_doPrepare(resolvedTypes, featureScopeSession, (JvmDeclaredType) element, resolvedTypesByContext);
		} else if (element instanceof JvmConstructor) {
			_doPrepare(resolvedTypes, featureScopeSession, (JvmConstructor) element, resolvedTypesByContext);
		} else if (element instanceof JvmField) {
			_doPrepare(resolvedTypes, featureScopeSession, (JvmField) element, resolvedTypesByContext);
		} else if (element instanceof JvmOperation) {
			_doPrepare(resolvedTypes, featureScopeSession, (JvmOperation) element, resolvedTypesByContext);
		}
	}
	
	protected void _doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmDeclaredType type, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByType) {
		IFeatureScopeSession childSession = addThisAndSuper(featureScopeSession, type);
		prepareMembers(resolvedTypes, childSession, type, resolvedTypesByType);
	}

	protected void prepareMembers(ResolvedTypes resolvedTypes, IFeatureScopeSession childSession, JvmDeclaredType type, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByType) {
		StackedResolvedTypes childResolvedTypes = declareTypeParameters(resolvedTypes, type, resolvedTypesByType);
		
		JvmTypeReference superType = getExtendedClass(type);
		if (superType != null) {
			LightweightTypeReference lightweightSuperType = resolvedTypes.getConverter().toLightweightReference(superType);
			childResolvedTypes.reassignType(superType.getType(), lightweightSuperType);
			/* 
			 * We use reassignType to make sure that the following works:
			 *
			 * StringList extends AbstractList<String> {
			 *   NestedIntList extends AbstractList<Integer> {
			 *   }
			 *   SubType extends StringList {}
			 * }
			 */
		}
		JvmParameterizedTypeReference thisType = getServices().getTypeReferences().createTypeRef(type);
		LightweightTypeReference lightweightThisType = resolvedTypes.getConverter().toLightweightReference(thisType);
		childResolvedTypes.reassignType(type, lightweightThisType);
		
		List<JvmMember> members = type.getMembers();
		for(int i = 0; i < members.size(); i++) {
			doPrepare(childResolvedTypes, childSession, members.get(i), resolvedTypesByType);
		}
	}

	protected StackedResolvedTypes declareTypeParameters(ResolvedTypes resolvedTypes, JvmIdentifiableElement declarator,
			Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext) {
		StackedResolvedTypes childResolvedTypes = resolvedTypes.pushTypes();
		if (declarator instanceof JvmTypeParameterDeclarator)
			childResolvedTypes.addDeclaredTypeParameters(((JvmTypeParameterDeclarator) declarator).getTypeParameters());
		resolvedTypesByContext.put(declarator, childResolvedTypes);
		return childResolvedTypes;
	}

	protected void _doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmField field, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext) {
		StackedResolvedTypes childResolvedTypes = declareTypeParameters(resolvedTypes, field, resolvedTypesByContext);
		
		JvmTypeReference knownType = field.getType();
		if (InferredTypeIndicator.isInferred(knownType)) {
			XComputedTypeReference casted = (XComputedTypeReference) knownType;
			JvmTypeReference reference = createComputedTypeReference(resolvedTypesByContext, childResolvedTypes, featureScopeSession, field, false);
			casted.setEquivalent(reference);
		} else if (knownType != null) {
			LightweightTypeReference lightweightReference = childResolvedTypes.getConverter().toLightweightReference(knownType);
			childResolvedTypes.setType(field, lightweightReference);
		} else {
			JvmTypeReference reference = createComputedTypeReference(resolvedTypesByContext, childResolvedTypes, featureScopeSession, field, false);
			field.setType(reference);
		}
	}
	
	protected void _doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmConstructor constructor, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext) {
		StackedResolvedTypes childResolvedTypes = declareTypeParameters(resolvedTypes, constructor, resolvedTypesByContext);
		
		JvmDeclaredType producedType = constructor.getDeclaringType();
		JvmParameterizedTypeReference asReference = getServices().getTypeReferences().createTypeRef(producedType);
		LightweightTypeReference lightweightReference = childResolvedTypes.getConverter().toLightweightReference(asReference);
		childResolvedTypes.setType(constructor, lightweightReference);
	}
	
	protected void _doPrepare(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmOperation operation, Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext) {
		StackedResolvedTypes childResolvedTypes = declareTypeParameters(resolvedTypes, operation, resolvedTypesByContext);
		
		JvmTypeReference knownType = operation.getReturnType();
		if (InferredTypeIndicator.isInferred(knownType)) {
			XComputedTypeReference casted = (XComputedTypeReference) knownType;
			JvmTypeReference reference = createComputedTypeReference(resolvedTypesByContext, childResolvedTypes, featureScopeSession, operation, true);
			casted.setEquivalent(reference);
		} else if (knownType != null) {
			LightweightTypeReference lightweightReference = childResolvedTypes.getConverter().toLightweightReference(knownType);
			childResolvedTypes.setType(operation, lightweightReference);
		} else {
			JvmTypeReference reference = createComputedTypeReference(resolvedTypesByContext, childResolvedTypes, featureScopeSession, operation, true);
			operation.setReturnType(reference);
		}
	}
	
	protected JvmTypeReference createComputedTypeReference(Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmMember member, boolean returnType) {
		XComputedTypeReference result = getServices().getXtypeFactory().createXComputedTypeReference();
		result.setTypeProvider(createTypeProvider(resolvedTypesByContext, resolvedTypes, featureScopeSession, member, returnType));
		// TODO do we need a lightweight computed type reference?
//		resolvedTypes.setType(member, result);
		return result;
	}
	
	protected AbstractReentrantTypeReferenceProvider createTypeProvider(Map<JvmIdentifiableElement, ResolvedTypes> resolvedTypesByContext, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmMember member, boolean returnType) {
		XExpression expression = logicalContainerProvider.getAssociatedExpression(member);
		resolvedTypes.markToBeInferred(expression);
		return new DemandTypeReferenceProvider(member, expression, resolvedTypesByContext, resolvedTypes, featureScopeSession, returnType);
	}
	
	@Override
	protected void computeTypes(ResolvedTypes resolvedTypes, IFeatureScopeSession session) {
		Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes = prepare(resolvedTypes, session);
		computeTypes(preparedResolvedTypes, resolvedTypes, session, getRoot());
		processResult(resolvedTypes);
	}
	
	protected void computeTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, EObject element) {
		if (element instanceof JvmDeclaredType) {
			_computeTypes(preparedResolvedTypes, resolvedTypes, featureScopeSession, (JvmDeclaredType) element);
		} else if (element instanceof JvmConstructor) {
			_computeTypes(preparedResolvedTypes, resolvedTypes, featureScopeSession, (JvmConstructor) element);
		} else if (element instanceof JvmField) {
			_computeTypes(preparedResolvedTypes, resolvedTypes, featureScopeSession, (JvmField) element);
		} else if (element instanceof JvmOperation) {
			_computeTypes(preparedResolvedTypes, resolvedTypes, featureScopeSession, (JvmOperation) element);
		} else {
			computeTypes(resolvedTypes, featureScopeSession, element);
		}
	}
	
	@Override
	protected void computeTypes(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, EObject element) {
		if (element instanceof JvmConstructor) {
			throw new IllegalStateException();
		} else if (element instanceof JvmField) {
			throw new IllegalStateException();
		} else if (element instanceof JvmOperation) {
			throw new IllegalStateException();
		} else if (element instanceof JvmDeclaredType) {
			throw new IllegalStateException();
		} else {
			super.computeTypes(resolvedTypes, featureScopeSession, element);
		}
	}

	protected void _computeTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmField field) {
		ResolvedTypes childResolvedTypes = preparedResolvedTypes.get(field);
		if (childResolvedTypes == null)
			throw new IllegalStateException("No resolved type found. Type was: " + field.getIdentifier());
		
		FieldTypeComputationState state = new FieldTypeComputationState(childResolvedTypes, featureScopeSession, field, this);
		ITypeComputationResult result = state.computeTypes();
		if (InferredTypeIndicator.isInferred(field.getType())) {
			LightweightTypeReference fieldType = result.getActualExpressionType();
			if (fieldType != null)
				InferredTypeIndicator.resolveTo(field.getType(), fieldType.toJavaCompliantTypeReference());
		}
		computeAnnotationTypes(childResolvedTypes, featureScopeSession, field);
		
		mergeChildTypes(preparedResolvedTypes, childResolvedTypes, field);
	}
	
	protected void _computeTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmConstructor constructor) {
		ResolvedTypes childResolvedTypes = preparedResolvedTypes.get(constructor);
		if (childResolvedTypes == null)
			throw new IllegalStateException("No resolved type found. Type was: " + constructor.getIdentifier());
		
		ConstructorBodyComputationState state = new ConstructorBodyComputationState(childResolvedTypes, featureScopeSession, constructor, this);
		state.computeTypes();
		computeAnnotationTypes(childResolvedTypes, featureScopeSession, constructor);
		
		mergeChildTypes(preparedResolvedTypes, childResolvedTypes, constructor);
	}
	
	protected void _computeTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmOperation operation) {
		ResolvedTypes childResolvedTypes = preparedResolvedTypes.get(operation);
		if (childResolvedTypes == null) {
			if (preparedResolvedTypes.containsKey(operation))
				return;
			throw new IllegalStateException("No resolved type found. Type was: " + operation.getIdentifier());
		}
		
		OperationBodyComputationState state = new OperationBodyComputationState(childResolvedTypes, featureScopeSession, operation, this);
		setReturnType(operation, state.computeTypes());
		computeAnnotationTypes(childResolvedTypes, featureScopeSession, operation);
		
		mergeChildTypes(preparedResolvedTypes, childResolvedTypes, operation);
	}

	protected void mergeChildTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes childResolvedTypes, JvmIdentifiableElement element) {
		if (childResolvedTypes instanceof StackedResolvedTypes)
			((StackedResolvedTypes) childResolvedTypes).mergeIntoParent();
		preparedResolvedTypes.put(element, null);
	}

	protected void setReturnType(JvmOperation operation, ITypeComputationResult computedType) {
		if (InferredTypeIndicator.isInferred(operation.getReturnType())) {
			LightweightTypeReference returnType = computedType.getReturnType();
			if (returnType != null) {
				InferredTypeIndicator.resolveTo(operation.getReturnType(), returnType.toJavaCompliantTypeReference());
			}
		}
	}
	
	protected void computeAnnotationTypes(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmAnnotationTarget annotable) {
		final EList<JvmAnnotationReference> annotations = annotable.getAnnotations();
		computeAnnotationTypes(resolvedTypes, featureScopeSession, annotations);
	}

	protected void computeAnnotationTypes(ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession,
			final EList<JvmAnnotationReference> annotations) {
		for(JvmAnnotationReference annotation: annotations) {
			for(JvmAnnotationValue value: annotation.getValues()) {
				if (value instanceof JvmCustomAnnotationValue) {
					JvmCustomAnnotationValue custom = (JvmCustomAnnotationValue) value;
					for(Object object: custom.getValues()) {
						if (object instanceof XExpression) {
							AnnotationValueTypeComputationState state = new AnnotationValueTypeComputationState(resolvedTypes, featureScopeSession, value, (XExpression) object, this);
							state.computeTypes();
						}
					}
				} else if (value instanceof JvmAnnotationAnnotationValue) {
					computeAnnotationTypes(resolvedTypes, featureScopeSession, ((JvmAnnotationAnnotationValue) value).getValues());
				}
			}
		}
	}
	
	protected void _computeTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession, JvmDeclaredType type) {
		ResolvedTypes childResolvedTypes = preparedResolvedTypes.get(type);
		if (childResolvedTypes == null)
			throw new IllegalStateException("No resolved type found. Type was: " + type.getIdentifier());
		IFeatureScopeSession childSession = addThisAndSuper(featureScopeSession, type);
		computeMemberTypes(preparedResolvedTypes, childResolvedTypes, childSession, type);
		computeAnnotationTypes(childResolvedTypes, featureScopeSession, type);
		
		mergeChildTypes(preparedResolvedTypes, childResolvedTypes, type);
	}

	protected void computeMemberTypes(Map<JvmIdentifiableElement, ResolvedTypes> preparedResolvedTypes, ResolvedTypes resolvedTypes, IFeatureScopeSession featureScopeSession,
			JvmDeclaredType type) {
		List<JvmMember> members = type.getMembers();
		for(int i = 0; i < members.size(); i++) {
			computeTypes(preparedResolvedTypes, resolvedTypes, featureScopeSession, members.get(i));
		}
	}
	
	protected IFeatureScopeSession addThisAndSuper(IFeatureScopeSession session, JvmDeclaredType type) {
		JvmTypeReference superType = getExtendedClass(type);
		return addThisAndSuper(session, type, superType);
	}

	protected IFeatureScopeSession addThisAndSuper(IFeatureScopeSession session, JvmDeclaredType thisType,
			@Nullable JvmTypeReference superType) {
		IFeatureScopeSession childSession;
		if (superType != null) {
			ImmutableMap.Builder<QualifiedName, JvmIdentifiableElement> builder = ImmutableMap.builder();
			builder.put(IFeatureNames.THIS, thisType);
			builder.put(IFeatureNames.SUPER, superType.getType());
			childSession = session.addLocalElements(builder.build());
		} else {
			childSession = session.addLocalElement(IFeatureNames.THIS, thisType);
		}
		return childSession;
	}
	
	@Nullable
	public JvmTypeReference getExtendedClass(JvmDeclaredType type) {
		for(JvmTypeReference candidate: type.getSuperTypes()) {
			if (candidate.getType() instanceof JvmGenericType && !((JvmGenericType) candidate.getType()).isInterface())
				return candidate;
		}
		return null;
	}

	protected void processResult(@SuppressWarnings("unused") ResolvedTypes resolvedTypes) {
		// TODO keep the result available for subsequent linking requests et al
	}
	
	protected ILogicalContainerProvider getLogicalContainerProvider() {
		return logicalContainerProvider;
	}
	
	/**
	 * Returns <code>null</code> if the given operation declares it's own return type or if it does not override
	 * another operation.
	 */
	@Nullable
	protected LightweightTypeReference getReturnTypeOfOverriddenOperation(JvmOperation operation, ResolvedTypes resolvedTypes, IFeatureScopeSession session) {
		if (operation.getVisibility() == JvmVisibility.PRIVATE)
			return null;
		int parameterSize = operation.getParameters().size();
		if (InferredTypeIndicator.isInferred(operation.getReturnType())) {
			LightweightTypeReference declaringType = resolvedTypes.getActualType(operation.getDeclaringType());
			if (declaringType == null) {
				throw new IllegalStateException("Cannot determine declaring type of operation: " + operation);
			}
			Map<JvmTypeParameter, LightweightMergedBoundTypeArgument> parameterMapping = new DeclaratorTypeArgumentCollector().getTypeParameterMapping(declaringType);
			StandardTypeParameterSubstitutor substitutor = new StandardTypeParameterSubstitutor(parameterMapping, resolvedTypes.getReferenceOwner());
			List<LightweightTypeReference> superTypes = declaringType.getSuperTypes();
			OwnedConverter converter = resolvedTypes.getConverter();
			for(LightweightTypeReference superType: superTypes) {
				JvmDeclaredType declaredSuperType = (JvmDeclaredType) superType.getType();
				if (declaredSuperType != null) {
					Iterable<JvmFeature> equallyNamedFeatures = declaredSuperType.findAllFeaturesByName(operation.getSimpleName());
					for(JvmFeature feature: equallyNamedFeatures) {
						if (session.isVisible(feature)) {
							if (feature instanceof JvmOperation) {
								JvmOperation candidate = (JvmOperation) feature;
								if (parameterSize == candidate.getParameters().size()) {
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
										return substitutor.substitute(converter.toLightweightReference(candidate.getReturnType()));
									}
								}
							}
						}
					}
				}
			}
		}
		return null;
	}
	
}
