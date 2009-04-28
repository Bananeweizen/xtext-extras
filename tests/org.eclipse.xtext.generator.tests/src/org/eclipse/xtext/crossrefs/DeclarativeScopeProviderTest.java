/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.crossrefs;

import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.xtext.crossref.IScope;
import org.eclipse.xtext.crossref.IScopedElement;
import org.eclipse.xtext.crossref.impl.AbstractDeclarativeScopeProvider;
import org.eclipse.xtext.crossref.impl.DefaultScopeProvider;
import org.eclipse.xtext.crossref.impl.ImportUriResolver;
import org.eclipse.xtext.crossref.impl.SimpleScope;

import junit.framework.TestCase;

/**
 * @author Sven Efftinge - Initial contribution and API
 *
 */
public class DeclarativeScopeProviderTest extends TestCase {
	@SuppressWarnings("unused")
	public void testSimple() throws Exception {
		final IScope a =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		AbstractDeclarativeScopeProvider provider = new AbstractDeclarativeScopeProvider() {
			private IScope scope_EClass(EClass clazz, EReference ref) {
				return a;
			}
		};
		
		EReference details = EcorePackage.eINSTANCE.getEClass_ESuperTypes();
		assertEquals(a,provider.getScope(details, details));
	}
	
	@SuppressWarnings("unused")
	public void testNested() throws Exception {
		final IScope a =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		final IScope b =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		
		AbstractDeclarativeScopeProvider provider = new AbstractDeclarativeScopeProvider() {
			private IScope scope_EClass(EClass clazz, EReference ref) {
				return a;
			}
			private IScope scope_EClass(EReference ctx, EReference ref) {
				return b;
			}
		};
		
		EReference details = EcorePackage.eINSTANCE.getEClass_ESuperTypes();
		assertEquals(b,provider.getScope(details, details));
		assertEquals(a,provider.getScope(details.getEContainingClass(), details));
	}
	
	@SuppressWarnings("unused")
	public void testPolymorphic() throws Exception {
		final IScope a =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		final IScope b =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		final IScope c =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		
		AbstractDeclarativeScopeProvider provider = new AbstractDeclarativeScopeProvider() {
			private IScope scope_EClass(EClassifier clazz, EReference ref) {
				return a;
			}
			
			private IScope scope_EClass(EClass clazz, EReference ref) {
				return c;
			}
			
			private IScope scope_EClass(EReference ctx, EReference ref) {
				return b;
			}
		};
		
		EReference details = EcorePackage.eINSTANCE.getEClass_ESuperTypes();
		assertEquals(b,provider.getScope(details, details));
		assertEquals(c,provider.getScope(details.getEContainingClass(), details));
	}
	
	@SuppressWarnings("unused")
	public void testScopeByType() throws Exception {
		final IScope a =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		final IScope b =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		final IScope c =  new SimpleScope(IScope.NULLSCOPE, Collections.<IScopedElement>emptySet());
		AbstractDeclarativeScopeProvider provider = new AbstractDeclarativeScopeProvider() {
			private IScope scope_EClass(EClassifier clazz, EReference ref) {
				return a;
			}
			
			private IScope scope_EClass(EClass clazz, EReference ref) {
				return c;
			}
			
			private IScope scope_EClass(EReference ctx, EReference ref) {
				return b;
			}
		};
		DefaultScopeProvider defaultScopeProvider = new DefaultScopeProvider();
		defaultScopeProvider.setImportUriResolver(new ImportUriResolver());
		provider.setGenericFallBack(defaultScopeProvider);
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource res = new XMIResourceFactoryImpl().createResource(URI.createURI("test.uri"));
		resourceSet.getResources().add(res);
		EReference details = (EReference) EcoreUtil.copy(EcorePackage.eINSTANCE.getEClass_ESuperTypes());
		res.getContents().add(details);
		assertNotNull(provider.getScope(details, details.eClass()));
	}
}
