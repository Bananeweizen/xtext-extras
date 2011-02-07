/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.util;

import static com.google.common.collect.Iterables.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;
import org.eclipse.xtext.common.types.JvmArrayType;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.TypesFactory;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.tests.ClasspathBasedModule;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class TypeArgumentContextTest extends TestCase {

	private IJvmTypeProvider typeProvider;
	private JvmTypeReferences typeRefs;
	@Inject
	private TypeArgumentContextProvider typeArgCtxProvider;
	
	@Inject
	private ResourceSetImpl resourceSet;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		Injector injector = Guice.createInjector(new ClasspathBasedModule());
		injector.injectMembers(this);
		Resource resource = new XMLResourceImpl(URI.createURI("http://synthetic.resource"));
		resourceSet.getResources().add(resource);
		typeProvider = injector.getInstance(IJvmTypeProvider.Factory.class).findOrCreateTypeProvider(resourceSet);
		typeRefs = new JvmTypeReferences(TypesFactory.eINSTANCE, typeProvider);
	}
	
	@Override
	protected void tearDown() throws Exception {
		typeProvider = null;
		typeRefs = null;
		resourceSet = null;
		typeArgCtxProvider = null;
		super.tearDown();
	}
	
	/**
	 * test case: "Iterable<? extends String> = newArrayList();"
	 */
	public void testInferredMethodContext_00() throws Exception {
		JvmParameterizedTypeReference lists = typeRefs.typeReference(Lists.class.getCanonicalName()).create();
		JvmOperation operation = find(((JvmDeclaredType)lists.getType()).getDeclaredOperations(), new Predicate<JvmOperation>(){
			public boolean apply(JvmOperation input) {
				return input.getSimpleName().equals("newArrayList") && input.getParameters().isEmpty();
			}
		});
		JvmTypeReference expected = typeRefs.typeReference("java.lang.Iterable").wildCardExtends("java.lang.String").create();
		
		Map<JvmTypeParameter, JvmTypeReference> map = typeArgCtxProvider.resolveInferredMethodTypeArgContext(operation, expected);
		assertEquals(1,map.size());
		assertEquals("? extends java.lang.String",map.values().iterator().next().getCanonicalName());
	}
	
	/**
	 * test case: "val x = singleton("foo");"
	 */
	public void testInferredMethodContext_01() throws Exception {
		JvmParameterizedTypeReference lists = typeRefs.typeReference(Collections.class.getCanonicalName()).create();
		JvmOperation operation = find(((JvmDeclaredType)lists.getType()).getDeclaredOperations(), new Predicate<JvmOperation>(){
			public boolean apply(JvmOperation input) {
				return input.getSimpleName().equals("singleton");
			}
		});
		JvmTypeReference actualArg = typeRefs.typeReference("java.lang.String").create();
		
		Map<JvmTypeParameter, JvmTypeReference> map = typeArgCtxProvider.resolveInferredMethodTypeArgContext(operation, null, actualArg);
		assertEquals(1,map.size());
		assertEquals("java.lang.String",map.values().iterator().next().getCanonicalName());
	}
	
	/**
	 * test case: "val Object x = getLast(newArrayList("foo"));"
	 */
	public void testInferredMethodContext_02() throws Exception {
		
		JvmParameterizedTypeReference lists = typeRefs.typeReference(Iterables.class.getCanonicalName()).create();
		JvmOperation operation = find(((JvmDeclaredType)lists.getType()).getDeclaredOperations(), new Predicate<JvmOperation>(){
			public boolean apply(JvmOperation input) {
				return input.getSimpleName().equals("getLast");
			}
		});
		JvmTypeReference actualArg = typeRefs.typeReference("java.util.List").wildCardExtends("java.lang.String").create();
		JvmTypeReference expectation = typeRefs.typeReference("java.lang.Object").create();
		
		Map<JvmTypeParameter, JvmTypeReference> map = typeArgCtxProvider.resolveInferredMethodTypeArgContext(operation, expectation, actualArg);
		assertEquals(1,map.size());
		assertEquals("? extends java.lang.String",map.values().iterator().next().getCanonicalName());
	}
	
	public void testSimple() throws Exception {
		JvmTypeReference reference = typeRefs.typeReference("java.util.List").wildCardExtends("java.lang.CharSequence").create();
		TypeArgumentContext typeArgumentContext = typeArgCtxProvider.getReceiverContext(reference);
		JvmTypeReference argument = typeArgumentContext.getBoundArgument(((JvmGenericType)reference.getType()).getTypeParameters().get(0));
		assertTrue(EcoreUtil.equals(((JvmParameterizedTypeReference)reference).getArguments().get(0), argument));
	}
	
	public void testPrimitive() throws Exception {
		JvmTypeReference primitiveRef = typeRefs.typeReference("int").create();
		TypeArgumentContext typeArgumentContext = typeArgCtxProvider.getReceiverContext(primitiveRef);
		JvmTypeReference reference = typeRefs.typeReference("java.util.List").wildCardExtends("java.lang.CharSequence").create();
		JvmTypeReference argument = typeArgumentContext.getBoundArgument(((JvmGenericType)reference.getType()).getTypeParameters().get(0));
		assertNull(argument);
	}
	
	public void testNotRecursive() throws Exception {
		JvmTypeReference reference = typeRefs.typeReference("java.util.List").wildCardExtends("java.lang.CharSequence").create();
		
		JvmGenericType collType = (JvmGenericType) typeProvider.findTypeByName(Collection.class.getCanonicalName());
		JvmTypeReference collArgument = typeArgCtxProvider.getReceiverContext(reference).getBoundArgument(collType.getTypeParameters().get(0));
		
		JvmGenericType listType = (JvmGenericType) typeProvider.findTypeByName(List.class.getCanonicalName());
		JvmTypeReference listArgument = ((JvmParameterizedTypeReference)listType.getSuperTypes().get(0)).getArguments().get(0);
		
		assertTrue(EcoreUtil.equals(listArgument, collArgument));
	}
	
	public void testResolve_0() throws Exception {
		JvmTypeReference reference = typeRefs.typeReference("java.util.ArrayList").wildCardExtends("java.lang.CharSequence").create();
		TypeArgumentContext context = typeArgCtxProvider.getReceiverContext(reference);
		JvmOperation jvmOperation = findOperation("java.util.List", "add(E)");
		
		assertEquals(null, context.getLowerBound(jvmOperation.getParameters().get(0).getParameterType()));
		JvmOperation get = findOperation("java.util.List", "get(int)");
		assertEquals("java.lang.CharSequence",context.getUpperBound(get.getReturnType(), resourceSet).getCanonicalName());
	}
	
	public void testResolve_1() throws Exception {
		JvmTypeReference reference = typeRefs.typeReference("java.util.ArrayList").wildCardSuper("java.lang.CharSequence").create();
		TypeArgumentContext context = typeArgCtxProvider.getReceiverContext(reference);
		JvmOperation jvmOperation = findOperation("java.util.List", "add(E)");
		
		JvmTypeReference resolvedParameter = context.getLowerBound(jvmOperation.getParameters().get(0).getParameterType());
		assertEquals("java.lang.CharSequence", resolvedParameter.getCanonicalName());
		JvmOperation get = findOperation("java.util.List", "get(int)");
		assertEquals("java.lang.Object",context.getUpperBound(get.getReturnType(),resourceSet).getCanonicalName());
	}
	
	public void testResolve_WithUnResolved() throws Exception {
		JvmTypeReference reference = typeRefs.typeReference("java.util.ArrayList").create();
		TypeArgumentContext context = typeArgCtxProvider.getReceiverContext(reference);
		JvmOperation jvmOperation = findOperation("java.util.List", "add(E)");
		
		JvmTypeReference resolvedParameter = context.getLowerBound(jvmOperation.getParameters().get(0).getParameterType());
		assertEquals("E", resolvedParameter.getCanonicalName());
		JvmOperation get = findOperation("java.util.List", "get(int)");
		assertEquals("E", context.getUpperBound(get.getReturnType(),resourceSet).getCanonicalName());
	}
	
	public void testResolveDeeplyNested() throws Exception {
		JvmTypeReference reference = typeRefs
			.typeReference("java.util.ArrayList")
				.wildCardExtends("java.util.Map")
					.wildCardSuper("java.lang.String").x()
					.wildCardExtends("java.lang.Number").x()
				.create();
		TypeArgumentContext context = typeArgCtxProvider.getReceiverContext(reference);
		
		JvmOperation get = findOperation("java.util.List", "get(int)");
		assertEquals("java.util.Map<? super java.lang.String,? extends java.lang.Number>",context.getUpperBound(get.getReturnType(),resourceSet).getCanonicalName());
	}
	
	public void testResolveArray() throws Exception {
		JvmArrayType arrayType = TypesFactory.eINSTANCE.createJvmArrayType();
		arrayType.setComponentType(typeRefs.typeReference("java.lang.String").create());
		//TODO
	}
	
	protected JvmOperation findOperation(String typeName, String methodSignature) {
		JvmType type = typeProvider.findTypeByName(typeName);
		return (JvmOperation) type.eResource().getEObject(typeName+"."+methodSignature);
	}

}
