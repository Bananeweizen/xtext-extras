/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.resource.metamodel;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.same;
import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.xtext.GeneratedMetamodel;
import org.eclipse.xtext.XtextFactory;
import org.eclipse.xtext.resource.metamodel.EClassifierInfo.EClassInfo;
import org.eclipse.xtext.resource.metamodel.ErrorAcceptor.ErrorCode;

/**
 * @author Heiko Behrens - Initial contribution and API
 * 
 */
public class TypeHierarchyHelperTests extends TestCase {

	private TypeHierarchyHelper helper;
	private EClassifierInfos infos = new EClassifierInfos();
	private EDataType INT = EcoreFactory.eINSTANCE.createEDataType();
	private EDataType STRING = EcoreFactory.eINSTANCE.createEDataType();
	private ErrorAcceptor errorAcceptorMock;
	private GeneratedMetamodel metamodel;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		errorAcceptorMock = createMock(ErrorAcceptor.class);
		metamodel = XtextFactory.eINSTANCE.createGeneratedMetamodel();
		metamodel.setNsURI("myURI");
	}
	
	private void liftUpFeatures() throws Exception {
		initializeHelper();
		helper.liftUpFeaturesRecursively();
		EasyMock.verify(errorAcceptorMock);
	}

	private void initializeHelper() {
		EasyMock.replay(errorAcceptorMock);
		helper = new TypeHierarchyHelper(infos, errorAcceptorMock);
	}

	private EClassInfo addClass(String name, boolean isGenerated) {
		EClass eClass = EcoreFactory.eINSTANCE.createEClass();
		eClass.setName(name);
		EClassInfo info = (EClassInfo) EClassifierInfo.createEClassInfo(eClass, isGenerated);
		infos.addInfo(metamodel, name, info);
		return info;
	}

	private EClassInfo addClass(String name) {
		return addClass(name, true);
	}

	private void addAttribute(EClassInfo eClass, EDataType eType, String name) {
		EAttribute feature = EcoreFactory.eINSTANCE.createEAttribute();
		feature.setName(name);
		feature.setEType(eType);
		eClass.getEClass().getEStructuralFeatures().add(feature);
	}

	private EReference addReference(EClassInfo eClass, EClassInfo ref, String name) {
		EReference feature = EcoreFactory.eINSTANCE.createEReference();
		feature.setName(name);
		feature.setEType(ref.getEClassifier());
		eClass.getEClass().getEStructuralFeatures().add(feature);
		return feature;
	}

	public void testSimpeCase01() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		b.addSupertype(a);
		c.addSupertype(a);
		addAttribute(b, INT, "f1");
		addAttribute(c, INT, "f1");

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());

		liftUpFeatures();

		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
	}

	public void testSimpeCase02() throws Exception {
		// no uplift for less than two children
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		b.addSupertype(a);
		addAttribute(b, INT, "f1");

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());

		liftUpFeatures();

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
	}

	public void testRecursiveUplift01() throws Exception {
		// no uplift for less than two children
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		EClassInfo d = addClass("d");
		EClassInfo e = addClass("e");
		b.addSupertype(a);
		c.addSupertype(a);
		d.addSupertype(c);
		e.addSupertype(c);

		addAttribute(b, INT, "f1");
		addAttribute(d, INT, "f1");
		addAttribute(e, INT, "f1");

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
		assertEquals(1, d.getEClass().getEStructuralFeatures().size());
		assertEquals(1, e.getEClass().getEStructuralFeatures().size());

		liftUpFeatures();

		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
		assertEquals(0, d.getEClass().getEStructuralFeatures().size());
		assertEquals(0, e.getEClass().getEStructuralFeatures().size());
	}

	public void testNikolaus() throws Exception {
		// no uplift for less than two children
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		EClassInfo d = addClass("d");
		EClassInfo e = addClass("e");
		b.addSupertype(a);
		c.addSupertype(a);
		d.addSupertype(b);
		d.addSupertype(c);
		e.addSupertype(b);
		e.addSupertype(c);

		addAttribute(b, STRING, "f2");
		addAttribute(c, STRING, "f2");
		addAttribute(d, INT, "f1");
		addAttribute(e, INT, "f1");

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());
		assertEquals(1, d.getEClass().getEStructuralFeatures().size());
		assertEquals(1, e.getEClass().getEStructuralFeatures().size());

		liftUpFeatures();

		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
		assertEquals(1, d.getEClass().getEStructuralFeatures().size());
		assertEquals(1, e.getEClass().getEStructuralFeatures().size());
	}

	public void testImcompatipleFeatures() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		b.addSupertype(a);
		c.addSupertype(a);
		addAttribute(b, INT, "f1");
		addAttribute(c, STRING, "f1");

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());

		liftUpFeatures();

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());
	}

	public void testReferences() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		EClassInfo d = addClass("d");
		b.addSupertype(a);
		c.addSupertype(a);
		addReference(b, d, "r1");
		addReference(c, d, "r1");

		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());

		liftUpFeatures();

		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
	}
	
	public void testConfigurationOfLiftedReference() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		
		b.addSupertype(a);
		c.addSupertype(a);
		EReference refB = addReference(b, a, "ref");
		refB.setContainment(true);
		EReference refC = addReference(c, a, "ref");
		refC.setContainment(true);
		
		assertEquals(0, a.getEClass().getEStructuralFeatures().size());
		assertEquals(1, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());
		
		liftUpFeatures();
		
		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
		
		EReference refA = (EReference) a.getEClass().getEStructuralFeatures().get(0);
		assertTrue(refA.isContainment());
	}

	public void testDublicateDerivedFeature() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		b.addSupertype(a);
		c.addSupertype(b);
		addAttribute(a, INT, "f");
		addAttribute(c, INT, "f");
		
		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(1, c.getEClass().getEStructuralFeatures().size());
		
		initializeHelper();
		helper.removeDuplicateDerivedFeatures();

		assertEquals(1, a.getEClass().getEStructuralFeatures().size());
		assertEquals(0, b.getEClass().getEStructuralFeatures().size());
		assertEquals(0, c.getEClass().getEStructuralFeatures().size());
	}
	
	public void testCylceInTypeHierarchy() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		EClassInfo c = addClass("c");
		EClassInfo d = addClass("d");
		a.addSupertype(c);
		b.addSupertype(a);
		c.addSupertype(b);
		d.addSupertype(a);
		
		errorAcceptorMock.acceptError(same(ErrorCode.TypeWithCycleInHierarchy), (String) anyObject(),
				(EObject) anyObject());
		EasyMock.expectLastCall().times(3);
		
		initializeHelper();
		helper.detectEClassesWithCyclesInTypeHierachy();
		EasyMock.verify(errorAcceptorMock);
	}
	
	public void testDuplicateFeatures01() throws Exception {
		EClassInfo a = addClass("a");
		EClassInfo b = addClass("b");
		
		b.addSupertype(a);
		addAttribute(a, INT, "f1");
		addAttribute(a, STRING, "f2");
		addAttribute(b, INT, "f2");
		
		errorAcceptorMock.acceptError(same(ErrorCode.MoreThanOneFeatureWithSameName), (String) anyObject(),
				(EObject) anyObject());
		
		
		initializeHelper();
		helper.detectDuplicatedFeatures();
		EasyMock.verify(errorAcceptorMock);
	}

}
