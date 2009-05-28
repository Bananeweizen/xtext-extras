/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
package org.eclipse.xtext.parsetree;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.dummy.DummyTestLanguageStandaloneSetup;
import org.eclipse.xtext.tests.AbstractGeneratorTest;

public class ASTChangeTest extends AbstractGeneratorTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(DummyTestLanguageStandaloneSetup.class);
	}

	public void testWhitespaceIsIncluded() throws Exception {
		String model = "element foo;\nelement bar;";
		CompositeNode node = getRootNode(model);
		EObject me = node.getElement();
		assertWithXtend("'foo'", "elements.first().name", me);
		invokeWithXtend("elements.first().setName('stuff')", me);
		assertWithXtend("'stuff'", "elements.first().name", me);
		
//		assertEquals("element stuff;\nelement bar;", node.serialize());
	}

}
