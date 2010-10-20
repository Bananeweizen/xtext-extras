package org.eclipse.xtext.resource.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.junit.AbstractXtextTests;
import org.eclipse.xtext.linking.LangATestLanguageStandaloneSetup;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.StringInputStream;

public class DefaultResourceDescription2Test extends AbstractXtextTests {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(new LangATestLanguageStandaloneSetup());
	}
	
	public void testNotYetLinked() throws Exception {
		XtextResourceSet rs = get(XtextResourceSet.class);
		Resource res1 = rs.createResource(URI.createURI("foo.langatestlanguage"));
		res1.load(new StringInputStream("type Foo"), null);
		
		XtextResource res2 = (XtextResource) rs.createResource(URI.createURI("bar.langatestlanguage"));
		res2.load(new StringInputStream("import 'foo.langatestlanguage'" +
				"type Bar extends Foo"), null);
		
		Iterable<QualifiedName> names = res2.getResourceServiceProvider().getResourceDescriptionManager().getResourceDescription(res2).getImportedNames();
		assertTrue(names.iterator().hasNext());
	}
	
	
	public void testValidExternalLink() throws Exception {
		XtextResourceSet rs = get(XtextResourceSet.class);
		Resource res1 = rs.createResource(URI.createURI("foo.langatestlanguage"));
		res1.load(new StringInputStream("type Foo"), null);
		
		XtextResource res2 = (XtextResource) rs.createResource(URI.createURI("bar.langatestlanguage"));
		res2.load(new StringInputStream("import 'foo.langatestlanguage'" +
		"type Bar extends Foo"), null);
		
		EcoreUtil.resolveAll(res2);
		Iterable<QualifiedName> names = res2.getResourceServiceProvider().getResourceDescriptionManager().getResourceDescription(res2).getImportedNames();
		assertEquals(QualifiedName.create("Foo"),names.iterator().next());
	}
	
	public void testValidLocalLink() throws Exception {
		XtextResourceSet rs = get(XtextResourceSet.class);
		Resource res1 = rs.createResource(URI.createURI("foo.langatestlanguage"));
		res1.load(new StringInputStream("type Foo"), null);
		
		XtextResource res2 = (XtextResource) rs.createResource(URI.createURI("bar.langatestlanguage"));
		res2.load(new StringInputStream("import 'foo.langatestlanguage'" +
		"type Foo type Bar extends Foo"), null);
		
		EcoreUtil.resolveAll(res2);
		Iterable<QualifiedName> names = res2.getResourceServiceProvider().getResourceDescriptionManager().getResourceDescription(res2).getImportedNames();
		assertFalse(names.iterator().hasNext());
	}
	
	public void testBrokenLink() throws Exception {
		XtextResourceSet rs = get(XtextResourceSet.class);
		Resource res1 = rs.createResource(URI.createURI("foo.langatestlanguage"));
		res1.load(new StringInputStream("type Foo"), null);
		
		XtextResource res2 = (XtextResource) rs.createResource(URI.createURI("bar.langatestlanguage"));
		res2.load(new StringInputStream("import 'foo.langatestlanguage'" +
		"type Bar extends Baz"), null);
		
		EcoreUtil.resolveAll(res2);
		Iterable<QualifiedName> names = res2.getResourceServiceProvider().getResourceDescriptionManager().getResourceDescription(res2).getImportedNames();
		assertEquals(QualifiedName.create("Baz"),names.iterator().next());
	}
}
