/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.resource.impl;

import static org.eclipse.xtext.scoping.Selectors.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ContentHandler;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IContainer;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription.Manager;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.IResourceScopeCache;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ResourceSetBasedResourceDescriptionsTest extends TestCase implements IResourceServiceProvider.Registry, Function<IEObjectDescription, EObject> {

	private ResourceSet resourceSet;
	private DefaultResourceDescriptionManager resourceDescriptionManager;
	private IContainer container;
	private int nameCount;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		resourceSet = new ResourceSetImpl();
		IQualifiedNameProvider qualifiedNameProvider = new IQualifiedNameProvider.AbstractImpl() {
			
			public QualifiedName getFullyQualifiedName(EObject obj) {
				return QualifiedName.create(((ENamedElement) obj).getName());
			}

			@Override
			public QualifiedName apply(EObject from) {
				return QualifiedName.create(((ENamedElement) from).getName());
			}
			
		};
		resourceDescriptionManager = new DefaultResourceDescriptionManager();
		resourceDescriptionManager.setCache(IResourceScopeCache.NullImpl.INSTANCE);
		resourceDescriptionManager.setNameProvider(qualifiedNameProvider);
		ResourceSetBasedResourceDescriptions resDescs = new ResourceSetBasedResourceDescriptions();
		resDescs.setContext(resourceSet);
		resDescs.setRegistry(this);
		container = new ResourceDescriptionsBasedContainer(resDescs);
	}

	public IResourceServiceProvider getResourceServiceProvider(URI uri, String contentType) {
		return new DefaultResourceServiceProvider() {
			@Override
			public Manager getResourceDescriptionManager() {
				return resourceDescriptionManager;
			}
		};
	}
	
	public void testEmptyResourceSet() {
		Iterable<IEObjectDescription> iterable = container.getElements(selectByType(EcorePackage.Literals.EOBJECT));
		assertTrue(Iterables.isEmpty(iterable));
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EOBJECT, QualifiedName.create("Zonk")));
		assertTrue(Iterables.isEmpty(iterable));
	}
	
	public void testOneElement_Mismatch() {
		QualifiedName qualifiedName = QualifiedName.create("SomeName");
		EClass type = EcorePackage.Literals.EPACKAGE;
		Resource resource = createResource();
		createNamedElement(qualifiedName, type, resource);
		Iterable<IEObjectDescription> iterable = container.getElements(selectByType(EcorePackage.Literals.ECLASS));
		assertTrue(Iterables.isEmpty(iterable));
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.ECLASS, qualifiedName));
		assertTrue(Iterables.isEmpty(iterable));
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EPACKAGE, QualifiedName.create("AnotherName")));
		assertTrue(Iterables.isEmpty(iterable));
	}
	
	public void testOneElement_Match() {
		QualifiedName qualifiedName = QualifiedName.create("SomeName");
		EClass type = EcorePackage.Literals.EPACKAGE;
		Resource resource = createResource();
		ENamedElement element = createNamedElement(qualifiedName, type, resource);
		Iterable<IEObjectDescription> iterable = container.getElements(selectByType(EcorePackage.Literals.EPACKAGE));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByType(EcorePackage.Literals.EOBJECT));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EPACKAGE, qualifiedName));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.ENAMED_ELEMENT, qualifiedName));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EOBJECT, qualifiedName));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
	}
	
	public void testTwoElements_OneMatch() {
		QualifiedName qualifiedName = QualifiedName.create("SomeName");
		EClass type = EcorePackage.Literals.EPACKAGE;
		Resource resource = createResource();
		ENamedElement element = createNamedElement(qualifiedName, type, resource);
		ENamedElement other = createNamedElement(null, EcorePackage.Literals.ECLASS, resource);
		Iterable<IEObjectDescription> iterable = container.getElements(selectByType(EcorePackage.Literals.EPACKAGE));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByType(EcorePackage.Literals.ECLASSIFIER));
		assertSame(other, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EPACKAGE, qualifiedName));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.ENAMED_ELEMENT, qualifiedName));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EOBJECT, qualifiedName));
		assertSame(element, Iterables.getOnlyElement(iterable).getEObjectOrProxy());
	}
	
	public void testTwoResources_TwoMatches() {
		QualifiedName qualifiedName = QualifiedName.create("SomeName");
		EClass type = EcorePackage.Literals.EPACKAGE;
		Resource resource = createResource();
		ENamedElement first = createNamedElement(qualifiedName, type, resource);
		resource = createResource();
		ENamedElement second = createNamedElement(qualifiedName, type, resource);
		List<ENamedElement> expected = Lists.newArrayList(first, second);
		Iterable<IEObjectDescription> iterable = container.getElements(selectByType(EcorePackage.Literals.EPACKAGE));
		checkFindAllEObjectsResult(expected, iterable);
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EPACKAGE, qualifiedName));
		checkFindAllEObjectsResult(expected, iterable);
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.ENAMED_ELEMENT, qualifiedName));
		checkFindAllEObjectsResult(expected, iterable);
		iterable = container.getElements(selectByTypeAndName(EcorePackage.Literals.EOBJECT, qualifiedName));
		checkFindAllEObjectsResult(expected, iterable);
	}

	private void checkFindAllEObjectsResult(List<ENamedElement> expected, Iterable<IEObjectDescription> iterable) {
		Iterable<EObject> transformed = Iterables.transform(iterable, this);
		Set<EObject> transformedSet = Sets.newHashSet(transformed);
		Set<ENamedElement> expectedSet = Sets.newHashSet(expected);
		assertEquals(expected.size(), expectedSet.size());
		assertEquals(expectedSet, transformedSet);
	}
	
	public void testPerformance10Resources100EClassesEach() {
		int resourceCount = 10;
		int eClassCount = 100;
		for(int i = 0; i < resourceCount; i++) {
			Resource resource = createResource();
			for(int j = 0; j < eClassCount; j++) {
				createNamedElement(null, EcorePackage.Literals.ECLASS, resource);
			}
		}
		Iterable<IEObjectDescription> iterable = container.getElements(selectByType(EcorePackage.Literals.EDATA_TYPE));
		assertTrue(Iterables.isEmpty(iterable));
		iterable = container.getElements(selectByType(EcorePackage.Literals.ECLASS));
		assertEquals(resourceCount*eClassCount, Iterables.size(iterable));
	}

	private ENamedElement createNamedElement(QualifiedName qualifiedName, EClass type, Resource resource) {
		ENamedElement result = (ENamedElement) EcoreUtil.create(type);
		if (qualifiedName != null)
			result.setName(qualifiedName.getFirstSegment());
		else
			result.setName("" + nameCount++);
		if (resource != null)
			resource.getContents().add(result);
		return result;
	}

	private Resource createResource() {
		Resource resource = new EcoreResourceFactoryImpl().createResource(URI.createURI("test://uri" + nameCount++ + ".ecore"));
		resourceSet.getResources().add(resource);
		return resource;
	}

	public EObject apply(IEObjectDescription from) {
		return from.getEObjectOrProxy();
	}

	public Map<String, Object> getContentTypeToFactoryMap() {
		return null;
	}

	public Map<String, Object> getExtensionToFactoryMap() {
		return null;
	}

	public Map<String, Object> getProtocolToFactoryMap() {
		return null;
	}

	public IResourceServiceProvider getResourceServiceProvider(URI uri) {
		return getResourceServiceProvider(uri, ContentHandler.UNSPECIFIED_CONTENT_TYPE);
	}
	
}
