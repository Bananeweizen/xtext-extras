/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.build

import com.google.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.build.AbstractIncrementalBuilderTest
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*
import org.eclipse.xtext.index.IndexTestLanguageInjectorProvider

/**
 * @author Sven Efftinge - Initial contribution and API
 */
@RunWith(XtextRunner)
@InjectWith(IndexTestLanguageInjectorProvider)
class IncrementalBuilderTest extends AbstractIncrementalBuilderTest {
	
	@Inject IResourceServiceProvider.Registry resourceServiceProviderFactory
	
	override getLanguages() {
		resourceServiceProviderFactory
	}

	@Test def void testSimpleFullBuild() {
		val buildRequest = newBuildRequest [
			dirtyFiles = #[
				'src/MyFile.indextestlanguage' - '''
					foo {
						entity B {}
						entity A { foo.B myReference }
					}
				'''
			]
		]
		build(buildRequest)
		assertTrue(issues.toString, issues.isEmpty)
		assertEquals(2, generated.size)
		assertTrue(generated.values.containsSuffix('src-gen/B.txt'))
		assertTrue(generated.values.containsSuffix('src-gen/A.txt'))
	}
	
	@Test def void testDelete_01() {
		build(newBuildRequest [
			dirtyFiles = #[
				'src/A.indextestlanguage' - '''
					foo {
						entity A {foo.B references}
					}
				''',
				'src/B.indextestlanguage' - '''
					foo {
						entity B
					}
				'''
			]
		])
		
		val validateCalled = new AtomicBoolean(false)
		
		build(newBuildRequest [
			deletedFiles = #[uri('src/B.indextestlanguage')]
			afterValidate = [ uri, issues |
				assertEquals(uri('src/A.indextestlanguage'), uri)
				assertTrue(issues.toString, issues.head.message.contains("Couldn't resolve reference to Type 'foo.B'"))
				validateCalled.set(true)
				return false
			]
		])
		assertTrue(validateCalled.get)
	}

	@Test def void testIncrementalBuild() {
		build(newBuildRequest [
			dirtyFiles = #[
				'src/A.indextestlanguage' - '''
					foo {
						entity A {foo.B reference}
					}
				''',
				'src/B.indextestlanguage' - '''
					foo {
						entity B {foo.A reference}
					}
				'''
			]
		])
		assertTrue(issues.toString, issues.isEmpty)
		assertEquals(2, generated.size)
		assertTrue(generated.values.containsSuffix('src-gen/B.txt'))
		assertTrue(generated.values.containsSuffix('src-gen/A.txt'))

		// non semantic change
		build(newBuildRequest [
			dirtyFiles = #[
				'src/A.indextestlanguage' - '''
					foo {
						entity A {foo.B reference}
					}
				'''
			]
		])
		assertTrue(issues.toString, issues.isEmpty)
		assertEquals(1, generated.size)
		assertFalse(generated.values.containsSuffix('src-gen/B.txt'))
		assertTrue(generated.values.containsSuffix('src-gen/A.txt'))

		// break foreign reference
		build(newBuildRequest [
			dirtyFiles = #[
				'src/A.indextestlanguage' - '''
					foo {
						entity X { foo.B reference }
					}
				'''
			]
		])
		assertEquals(issues.toString, 1, issues.size)
		assertEquals(1, generated.size)
		assertFalse(generated.values.containsSuffix('src-gen/B.txt'))
		assertTrue(generated.values.containsSuffix('src-gen/X.txt'))
		assertEquals(1, indexState.fileMappings.getGenerated(uri('src/A.indextestlanguage')).size)
		assertEquals(1, deleted.size)
		assertTrue(deleted.containsSuffix('src-gen/A.txt'))

		// fix foreign reference
		build(newBuildRequest [
			dirtyFiles = #[
				'src/A.indextestlanguage' - '''
					foo {
						entity A { foo.B reference }
					}
				'''
			]
		])
		assertTrue(issues.toString, issues.isEmpty)
		assertEquals(2, generated.size)
		assertTrue(generated.values.containsSuffix('src-gen/B.txt'))
		assertTrue(generated.values.containsSuffix('src-gen/A.txt'))
		assertTrue(deleted.containsSuffix('src-gen/X.txt'))
		
		// delete file
		build(newBuildRequest [
			deletedFiles = #[
				uri('src/A.indextestlanguage').delete
			]
		])
		assertEquals(issues.toString, 1, issues.size)
		assertEquals(0, generated.size)
		assertEquals(1, deleted.size)
		assertTrue(deleted.containsSuffix('src-gen/A.txt'))
	}

	
}
