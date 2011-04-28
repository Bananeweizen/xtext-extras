/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.access.jdt;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmVoid;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.access.IMirror;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.access.TypeNotFoundException;
import org.eclipse.xtext.common.types.access.TypeResource;
import org.eclipse.xtext.common.types.access.impl.AbstractTypeProviderTest;
import org.eclipse.xtext.common.types.access.impl.PrimitiveMirror;
import org.eclipse.xtext.common.types.access.impl.URIHelperConstants;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class JdtTypeProviderTest extends AbstractTypeProviderTest {

	private ResourceSet resourceSet;
	private JdtTypeProvider typeProvider;
	private MockJavaProjectProvider projectProvider;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		resourceSet = new ResourceSetImpl();
		projectProvider = new MockJavaProjectProvider();
		typeProvider = new JdtTypeProvider(projectProvider.getJavaProject(resourceSet), resourceSet);
	}

	@Override
	protected void tearDown() throws Exception {
		resourceSet = null;
		typeProvider = null;
		super.tearDown();
	}

	@Override
	protected IJvmTypeProvider getTypeProvider() {
		return typeProvider;
	}

	public void testSetup_01() {
		Map<String, Object> map = resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap();
		assertSame(typeProvider, map.get(URIHelperConstants.PROTOCOL));
	}

	public void testCreateResource_01() {
		URI primitivesURI = URI.createURI("java:/Primitives");
		TypeResource resource = typeProvider.createResource(primitivesURI);
		assertNotNull(resource);
		assertFalse(resource.isLoaded());
		assertTrue(resource.getContents().isEmpty());
	}

	public void testCreateResource_02() {
		URI primitivesURI = URI.createURI("java:/Primitives");
		TypeResource resource = (TypeResource) resourceSet.createResource(primitivesURI);
		assertNotNull(resource);
		assertFalse(resource.isLoaded());
		assertTrue(resource.getContents().isEmpty());
	}

	public void testGetResource_01() {
		URI primitivesURI = URI.createURI("java:/Primitives");
		TypeResource resource = (TypeResource) resourceSet.getResource(primitivesURI, true);
		assertNotNull(resource);
		assertTrue(resource.isLoaded());
		assertEquals(9, resource.getContents().size());
	}

	public void testGetResource_02() {
		URI primitivesURI = URI.createURI("java:/Primitives");
		TypeResource resource = (TypeResource) resourceSet.getResource(primitivesURI, false);
		assertNull(resource);
	}

	public void testGetResource_03() {
		URI primitivesURI = URI.createURI("java:/Primitives");
		TypeResource createdResource = (TypeResource) resourceSet.createResource(primitivesURI);
		TypeResource resource = (TypeResource) resourceSet.getResource(primitivesURI, false);
		assertSame(createdResource, resource);
		assertFalse(resource.isLoaded());
		assertTrue(resource.getContents().isEmpty());
	}

	public void testGetResource_04() {
		URI primitivesURI = URI.createURI("java:/Primitives");
		TypeResource createdResource = (TypeResource) resourceSet.createResource(primitivesURI);
		TypeResource resource = (TypeResource) resourceSet.getResource(primitivesURI, true);
		assertSame(createdResource, resource);
		assertTrue(resource.isLoaded());
		assertEquals(9, resource.getContents().size());
	}

	public void testCreateMirror_01() {
		URI uri = URI.createURI("java:/Objects/java.util.Map");
		IMirror mirror = typeProvider.createMirror(uri);
		assertNotNull(mirror);
		assertTrue(mirror instanceof JdtTypeMirror);
		assertEquals("java.util.Map", ((JdtTypeMirror) mirror).getMirroredType().getFullyQualifiedName());
	}

	public void testCreateMirror_02() {
		URI uri = URI.createURI("java:/Primitives");
		IMirror mirror = typeProvider.createMirror(uri);
		assertNotNull(mirror);
		assertTrue(mirror instanceof PrimitiveMirror);
	}

	public void testCreateMirror_03() {
		URI uri = URI.createURI("java:/Something");
		try {
			typeProvider.createMirror(uri);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			// ok
		}
	}

	public void testCreateMirror_04() {
		URI uri = URI.createURI("java:/Primitives").appendFragment("int");
		try {
			typeProvider.createMirror(uri);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException ex) {
			// ok
		}
	}

	public void testCreateMirror_05() {
		URI uri = URI.createURI("java:/Objects/java.lang.does.not.exist");
		try {
			typeProvider.createMirror(uri);
			fail("Expected TypeNotFoundException");
		} catch (TypeNotFoundException ex) {
			// OK
		}
	}

	public void testCreateMirror_06() {
		URI uri = URI.createURI("java:/Objects/java.lang.does.not.exist");
		try {
			typeProvider.createMirror(uri);
			fail("Expected TypeNotFoundException");
		} catch (TypeNotFoundException ex) {
			// OK
		}
	}

	public void testBug300216() {
		JvmDeclaredType type = (JvmDeclaredType) getTypeProvider().findTypeByName("java.lang.Object");
		assertTrue(type.getSuperTypes().isEmpty());
		URI unresolveableType = URI.createURI("java:/Objects/Something#Something");
		JvmVoid proxy = TypesFactory.eINSTANCE.createJvmVoid();
		JvmParameterizedTypeReference typeReference = TypesFactory.eINSTANCE.createJvmParameterizedTypeReference();
		typeReference.setType(proxy);
		((InternalEObject) proxy).eSetProxyURI(unresolveableType);
		type.getSuperTypes().add(typeReference);
		int resources = type.eResource().getResourceSet().getResources().size();
		assertTrue(type.getSuperTypes().get(0).getType().eIsProxy());
		assertEquals(resources, type.eResource().getResourceSet().getResources().size());
	}

	/**
	 * see https://bugs.eclipse.org/bugs/show_bug.cgi?id=319993
	 */
	public void testBug319993() {
		try {
			getTypeProvider().findTypeByName("interface myAggregation");
		} catch (IllegalArgumentException e) {
			fail();
		} catch (TypeNotFoundException e) {
			//expected
		} 
	}

	@Override
	protected String getCollectionParamName() {
		return "c";
	}

}
