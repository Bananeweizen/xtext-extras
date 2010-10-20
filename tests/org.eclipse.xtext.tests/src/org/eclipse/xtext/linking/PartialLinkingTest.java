/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.linking;

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.junit.AbstractXtextTests;
import org.eclipse.xtext.linking.impl.DefaultLinkingService;
import org.eclipse.xtext.linking.impl.Linker;
import org.eclipse.xtext.linking.impl.LinkingDiagnosticMessageProvider;
import org.eclipse.xtext.naming.SimpleNameProvider;
import org.eclipse.xtext.resource.IResourceDescriptions;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.IScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;
import org.eclipse.xtext.scoping.impl.ImportUriResolver;
import org.eclipse.xtext.scoping.impl.ImportedNamespaceAwareLocalScopeProvider;
import org.eclipse.xtext.scoping.impl.LoadOnDemandResourceDescriptions;
import org.eclipse.xtext.testlanguages.ReferenceGrammarTestLanguageStandaloneSetup;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.inject.Provider;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class PartialLinkingTest extends AbstractXtextTests implements IScopeProvider {

	private boolean doFakeScope;

	private EObject context;

	private EObject model;

	private XtextResource resource;

	private String modelAsText;

	private ImportedNamespaceAwareLocalScopeProvider scopeProvider;

	private EReference reference;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(ReferenceGrammarTestLanguageStandaloneSetup.class);
		SimpleNameProvider nameProvider = new SimpleNameProvider();
		ImportUriGlobalScopeProvider globalScopeProvider = new ImportUriGlobalScopeProvider();
		globalScopeProvider.setImportResolver(new ImportUriResolver());
		globalScopeProvider.setCache(IResourceScopeCache.NullImpl.INSTANCE);
		globalScopeProvider.setResourceServiceProviderRegistry(get(IResourceServiceProvider.Registry.class));
		globalScopeProvider.setResourceDescriptions(new Provider<IResourceDescriptions>() {
			public IResourceDescriptions get() {
				return new IResourceDescriptions.NullImpl();
			}
		});
		globalScopeProvider.setLoadOnDemandDescriptions(new Provider<LoadOnDemandResourceDescriptions>() {
			public LoadOnDemandResourceDescriptions get() {
				return PartialLinkingTest.this.get(LoadOnDemandResourceDescriptions.class);
			}
		});
		scopeProvider = new ImportedNamespaceAwareLocalScopeProvider(globalScopeProvider, nameProvider);
		modelAsText =
			"spielplatz 1 {\n" +
			"  kind( Bommel1 1)\n" +
			"  kind( Bommel2 2)\n" +
			"  kind( Bommel3 3)\n" +
			"  erwachsener( Bob 4)\n" +
			"  erwachsener( Joe 5)\n" +
			"  familie( Familienname Bob Joe Bommel2 )\n" +
			"}";
		resource = getResourceFromString(modelAsText);
		assertTrue(resource.getErrors().toString(), resource.getErrors().isEmpty());
		model = resource.getParseResult().getRootASTElement();
		final Iterator<EObject> iter = model.eAllContents();
		while (iter.hasNext() && context == null) {
			final EObject candidate = iter.next();
			final String className = candidate.eClass().getName();
			if (className.endsWith("Familie"))
				context = candidate;
		}
		reference = (EReference) context.eClass().getEStructuralFeature("kinder");
	}

	/**
	 * We try to emulate a changed scope after a partial parsing so we mock the ScopeService.
	 */
	public IScope getScope(EObject context, EReference reference) {
		if (doFakeScope && context == this.context && reference == this.reference)
			return IScope.NULLSCOPE;
		return scopeProvider.getScope(context, reference);
	}

	public IScope getScope(EObject context, EClass type) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public void testPartialLinking() {
		int idx = modelAsText.indexOf("Bommel2 2");
		resource.update(idx + 1, 1, "o");
		EList<EObject> content = (EList<EObject>) context.eGet(reference);
		assertEquals(1, content.size());
	}

	@SuppressWarnings("unchecked")
	public void testPartialLinkingTwice() {
		int idx = modelAsText.indexOf("Bommel2 2");
		resource.update(idx + 1, 1, "o");
		resource.update(idx + 1, 1, "o");
		EList<EObject> content = (EList<EObject>) context.eGet(reference);
		assertEquals(1, content.size());
	}

	@SuppressWarnings("unchecked")
	public void testPartialLinkingFakedScope() {
		replaceLinker(resource);
		assertTrue(resource.getErrors().isEmpty());
		int idx = modelAsText.indexOf("Bommel2 2");
		resource.update(idx + 1, 1, "o");
		doFakeScope = true;
		resource.update(idx + 1, 1, "o");
		EList<EObject> content = (EList<EObject>) context.eGet(reference);
		assertEquals(0, content.size());
		assertEquals(1, resource.getErrors().size());
	}

	@SuppressWarnings("unchecked")
	public void testLinking() {
		EList<EObject> content = (EList<EObject>) context.eGet(reference);
		assertEquals(1, content.size());
	}

	protected void replaceLinker(XtextResource resource) {
		Linker linker = new Linker();
		DefaultLinkingService linkingService = new DefaultLinkingService();
		getInjector().injectMembers(linkingService);
		linkingService.setScopeProvider(this);
		linker.setLinkingService(linkingService);
		linker.setDiagnosticMessageProvider(new LinkingDiagnosticMessageProvider());
		resource.setLinker(linker);
	}

}
