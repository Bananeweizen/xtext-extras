/*******************************************************************************
 * Copyright (c) 2010 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.junit.AbstractXtextTests;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.IResourceDescription;
import org.eclipse.xtext.resource.IResourceDescription.Manager;
import org.eclipse.xtext.util.StringInputStream;

import com.google.common.collect.Lists;

/**
 * @author Sven Efftinge - Initial contribution and API
 */
public class XtextResourceDescriptionTest extends AbstractXtextTests {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(new XtextStandaloneSetup());
	}
	
	public void testComputeEObjectDescriptionsForEmptyFile() throws Exception {
		Resource res = getResourceAndExpect(new StringInputStream(""),URI.createURI("foo.xtext"),1);
		Manager manager = get(IResourceDescription.Manager.class);
		IResourceDescription description = manager.getResourceDescription(res);
		Iterable<IEObjectDescription> iterable = description.getExportedObjects();
		assertTrue(Lists.newArrayList(iterable).isEmpty());
	}
	
	public void testGetExportedEObjectsErroneousResource() throws Exception {
		Resource res = getResourceAndExpect(new StringInputStream("grammar foo Start : 'main';"),URI.createURI("foo.xtext"),1);
		Manager manager = get(IResourceDescription.Manager.class);
		IResourceDescription description = manager.getResourceDescription(res);
		Iterable<IEObjectDescription> iterable = description.getExportedObjects();
		assertTrue(Lists.newArrayList(iterable).size()==2);
	}

	public void testGetExportedEObjects() throws Exception {
		Resource res = getResource(new StringInputStream("grammar foo generate x \"someURI\" Start : 'main';"),URI.createURI("foo.xtext"));
		Manager manager = get(IResourceDescription.Manager.class);
		IResourceDescription description = manager.getResourceDescription(res);
		Iterable<IEObjectDescription> iterable = description.getExportedObjects();
		assertTrue(Lists.newArrayList(iterable).size()==3);
	}
}
