/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.xtext.parsetree;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.testlanguages.OptionalEmptyLanguageStandaloneSetup;
import org.eclipse.xtext.tests.AbstractGeneratorTest;

/**
 * 
 * @author Jan K�hnlein - Initial contribution and API
 *
 */
public class EmptyModelTest extends AbstractGeneratorTest {
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		OptionalEmptyLanguageStandaloneSetup.doSetup();
		with(OptionalEmptyLanguageStandaloneSetup.class);
	}
	
	public void testParseEmpty() throws Exception {
		EObject model = getModel("");
		assertNull(model);
		model = getModel("hallo welt");
		assertWithXtend("'welt'", "child.name", model);
	}
	
	public void testParseCommentOnly() throws Exception {
	    String model = "// some single line comment \n /* and \n a \n \n multiline \n comment */";
        assertEmptyModel(model);
	}

    private void assertEmptyModel(String model) throws Exception {
        IParseResult parseResult = parse(model);
	    assertNull(parseResult.getRootASTElement());
	    CompositeNode rootNode = parseResult.getRootNode();
        assertNotNull(rootNode);
        assertEquals(model, rootNode.serialize());
    }
	
	public void testParseWhitespaceOnly() throws Exception {
        String model = "    \t\n\r  \t\n\n ";
	    assertEmptyModel(model);
	}
	
}
