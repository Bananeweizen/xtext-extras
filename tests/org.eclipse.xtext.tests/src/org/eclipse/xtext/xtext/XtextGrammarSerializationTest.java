/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xtext;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.services.XtextGrammarAccess;
import org.eclipse.xtext.tests.AbstractGeneratorTest;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class XtextGrammarSerializationTest extends AbstractGeneratorTest {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		with(XtextStandaloneSetup.class);
	}

	public void testSimpleSerialization() throws Exception {
		final String model = "language foo\n"
			+ "generate mm \"http://bar\" as fooMM\n"
			+ "StartRule returns T: name=ID;";
		final String expectedModel = "language foo\n"
			+ "generate mm 'http://bar' as fooMM\n"
			+ "StartRule returns T: name=ID;";
		doTestSerialization(model, expectedModel);
	}

	private void doTestSerialization(String model, String expectedModel) throws Exception {
		final XtextResource resource = getResourceFromString(model);
		assertTrue(resource.getErrors().isEmpty());
		final Grammar g = (Grammar) resource.getParseResult().getRootASTElement();
		assertNotNull(g);
		final OutputStream outputStream = new ByteArrayOutputStream();
		resource.save(outputStream, Collections.emptyMap());
		final String serializedModel = outputStream.toString();
		assertEquals(expectedModel, serializedModel);
	}
	
	public void testMetamodelRefSerialization() throws Exception {
		final String model = "language foo\n" 
			+ "import \"http://www.eclipse.org/2008/Xtext\" as xtext\n"
			+ "generate mm \"http://bar\" as fooMM\n"
			+ "Foo : name=ID (nameRefs+=NameRef)*;\n"
			+ "NameRef returns xtext::RuleCall : rule=[ParserRule];\n"
			+ "MyRule returns xtext::ParserRule : name=ID;";
		final String expectedModel = "language foo\n" 
			+ "import 'http://www.eclipse.org/2008/Xtext' as xtext\n"
			+ "generate mm 'http://bar' as fooMM\n"
			+ "Foo : name=ID (nameRefs+=NameRef)*;\n"
			+ "NameRef returns RuleCall : rule=[ParserRule];\n"
			+ "MyRule returns ParserRule : name=ID;";
		doTestSerialization(model, expectedModel);
	}

	public void _testXtestSerializationSelfTest() throws Exception {
		Resource res = new XtextResourceSet().createResource(URI
				.createURI("myfile.xtext"));
		res.getContents().add(XtextGrammarAccess.INSTANCE.getGrammar());
		OutputStream outputStream = new ByteArrayOutputStream();
		res.save(outputStream, Collections.emptyMap());
	}

}
