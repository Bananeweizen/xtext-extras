/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.typesystem.conformance.ConformanceHint;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 * TODO JavaDoc, toString
 */
@NonNullByDefault
public class ChildExpressionTypeComputationState extends ExpressionTypeComputationState {

	protected ChildExpressionTypeComputationState(StackedResolvedTypes resolvedTypes,
			IFeatureScopeSession featureScopeSession,
			DefaultReentrantTypeResolver reentrantTypeResolver, 
			ExpressionTypeComputationState parent,
			XExpression expression) {
		super(resolvedTypes, featureScopeSession, reentrantTypeResolver, parent, expression);
	}
	
	@Override
	protected ExpressionTypeComputationState getParent() {
		return (ExpressionTypeComputationState) super.getParent();
	}
	
	@Override
	protected LightweightTypeReference acceptType(ResolvedTypes resolvedTypes, AbstractTypeExpectation expectation,
			LightweightTypeReference type, boolean returnType, ConformanceHint... hints) {
		if (getParent().getExpression() != getExpression()) {
			LightweightTypeReference actualType = super.acceptType(resolvedTypes, expectation, type, returnType, hints);
			getParent().acceptType(resolvedTypes, expectation, actualType, returnType, hints);
			return actualType;
		} else {
			LightweightTypeReference actualType = getParent().acceptType(resolvedTypes, expectation, type, returnType, hints);
			return actualType;
		}
	}
	
	@Override
	public TypeAssigner assignTypes() {
		final ExpressionTypeCheckpointComputationState state = new ChildExpressionTypeCheckpointComputationState(
				getResolvedTypes(), getFeatureScopeSession(), getResolver(), this, getExpression());
		return createTypeAssigner(state);
	}
	
	@Override
	public AbstractTypeComputationState withTypeCheckpoint() {
		return new ChildExpressionTypeCheckpointComputationState(getResolvedTypes(), getFeatureScopeSession(), getResolver(), this, getExpression());
	}
}
