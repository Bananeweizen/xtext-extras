/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.scoping.batch;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class XbaseBatchScopeProvider extends XtypeScopeProvider implements IBatchScopeProvider {

	@Inject
	private XbaseScopeSessionProvider scopeSession;
	
	@Inject
	private TypeScopeProvider typeScopeProvider;
	
	@Inject
	private FeatureScopeProvider featureScopeProvider;
	
	@Inject
	@Named(AbstractDeclarativeScopeProvider.NAMED_DELEGATE)
	private IScopeProvider delegate;
	
	@Override
	public IScope getScope(EObject context, EReference reference) {
		if (isConstructorCallScope(reference)) {
			return delegateGetScope(context, reference);
		}
		if (isTypeScope(reference)) {
			IScope parent = delegateGetScope(context, reference);
			return typeScopeProvider.createTypeScope(parent, context, reference);
		}
		if (isFeatureCallScope(reference)) {
			// TODO funnel through scope session
			IFeatureScopeSession session = newSession(context.eResource());
			session = session.recursiveInitialize(context);
			return session.createFeatureCallScope(context, reference, IResolvedTypes.NULL);
		}
		return delegateGetScope(context, reference);
	}
	
	public IFeatureScopeSession newSession(Resource context) {
		return scopeSession.withContext(context);
	}
	
	@Override
	protected IScope delegateGetScope(EObject context, EReference reference) {
		return getDelegate().getScope(context, reference);
	}

	@Override
	public void setDelegate(IScopeProvider delegate) {
		this.delegate = delegate;
	}

	@Override
	public IScopeProvider getDelegate() {
		return delegate;
	}

	protected boolean isTypeScope(EReference reference) {
		return TypesPackage.Literals.JVM_TYPE.isSuperTypeOf(reference.getEReferenceType());
	}

	protected boolean isConstructorCallScope(EReference reference) {
		return reference.getEReferenceType() == TypesPackage.Literals.JVM_CONSTRUCTOR;
	}

	public boolean isFeatureCallScope(EReference reference) {
		return featureScopeProvider.isFeatureCallScope(reference);
	}

}
