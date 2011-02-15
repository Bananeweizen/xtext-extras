/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types;

import org.eclipse.xtext.common.types.impl.JvmFieldImpl;

import junit.framework.TestCase;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class JvmFieldTest extends TestCase {

	private JvmField field;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		field = TypesFactory.eINSTANCE.createJvmField();
	}
	
	public void testCanonicalName_01() {
		assertNull(field.getIdentifier(), field.getIdentifier());
	}
	
	public void testCanonicalName_02() {
		field.internalSetIdentifier("java.lang.String.name");
		assertEquals("java.lang.String.name", field.getIdentifier());
	}
	
//	public void testGetSimpleName_01() {
//		assertNull(field.getSimpleName());
//	}
//	
//	public void testGetSimpleName_02() {
//		field.setCanonicalName("java.lang.String.name");
//		assertEquals("name", field.getSimpleName());
//	}
}
