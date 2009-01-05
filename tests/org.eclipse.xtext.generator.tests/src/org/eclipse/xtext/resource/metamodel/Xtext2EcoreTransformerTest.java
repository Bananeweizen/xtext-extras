/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.resource.metamodel;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;

import java.io.InputStream;
import java.util.List;

import org.apache.log4j.Logger;
import org.easymock.EasyMock;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.ReferencedMetamodel;
import org.eclipse.xtext.TypeRef;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.crossref.internal.Linker;
import org.eclipse.xtext.diagnostics.ExceptionDiagnostic;
import org.eclipse.xtext.diagnostics.IDiagnosticConsumer;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.tests.AbstractGeneratorTest;
import org.eclipse.xtext.util.EmfFormater;
import org.eclipse.xtext.xtext.XtextLinker;

/**
 * @author Jan K�hnlein - Initial contribution and API
 * @author Heiko Behrens
 * @see http://wiki.eclipse.org/Xtext/Documentation#Meta-Model_Inference
 */
public class Xtext2EcoreTransformerTest extends AbstractGeneratorTest {
	private static final Logger logger = Logger.getLogger(Xtext2EcoreTransformerTest.class);
	private ErrorAcceptor errorAcceptorMock;

	public static class MyErrorAcceptor implements ErrorAcceptor {

		private final ErrorAcceptor first;
		private final ErrorAcceptor second;

		public MyErrorAcceptor(ErrorAcceptor first, ErrorAcceptor second) {
			this.first = first;
			this.second = second;
		}
		
		public void acceptError(TransformationErrorCode errorCode, String message, EObject element) {
			first.acceptError(errorCode, message, element);
			second.acceptError(errorCode, message, element);
		}
		
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		errorAcceptorMock = createMock(ErrorAcceptor.class);
		with(XtextStandaloneSetup.class);
	}

	private EPackage getEPackageFromGrammar(String xtextGrammar) throws Exception {
		List<EPackage> metamodels = getEPackagesFromGrammar(xtextGrammar);
		assertEquals(1, metamodels.size());

		EPackage result = metamodels.get(0);
		assertNotNull(result);
		return result;
	}
	
	protected XtextResource getResource(InputStream in) throws Exception {
		ResourceSet rs = new XtextResourceSet();
		XtextResource resource = (XtextResource) rs.createResource(URI.createURI("mytestmodel."+getResourceFactory().getModelFileExtensions()[0]));
		XtextLinker linker = new XtextLinker() {
			@Override
			protected Xtext2EcoreTransformer createTransformer(Grammar grammar, IDiagnosticConsumer consumer) {
				Xtext2EcoreTransformer result = super.createTransformer(grammar, consumer);
				result.setErrorAcceptor(new MyErrorAcceptor(result.getErrorAcceptor(), errorAcceptorMock));
				return result;
			}
		};
		linker.setScopeProvider(((XtextLinker) resource.getLinker()).getScopeProvider());
		linker.setLinkingService(((Linker) resource.getLinker()).getLinkingService());
		resource.setLinker(linker);
		resource.load(in, null);
		
		for(Diagnostic d: resource.getErrors())
			System.out.println("Resource Error: "+d);
		
		for(Diagnostic d: resource.getWarnings())
			System.out.println("Resource Warning: "+d);
		
		return resource;
	}

	private List<EPackage> getEPackagesFromGrammar(String xtextGrammar) throws Exception {
		replay(errorAcceptorMock);
		Grammar grammar = (Grammar) getModel(xtextGrammar);
		verify(errorAcceptorMock);
//		xtext2EcoreTransformer.setErrorAcceptor(errorAcceptorMock);
//		xtext2EcoreTransformer.transform(grammar);
		List<EPackage> metamodels = Xtext2EcoreTransformer.doGetGeneratedPackages(grammar);
		assertNotNull(metamodels);
		return metamodels;
	}

	private EAttribute assertAttributeConfiguration(EClass eClass, int attributeIndex, String featureName,
			String featureTypeName) {
		EAttribute feature = eClass.getEAttributes().get(attributeIndex);
		assertEquals(featureName, feature.getName());
		assertNotNull(feature.getEType());
		assertEquals(featureTypeName, feature.getEType().getName());

		return feature;
	}

	private EAttribute assertAttributeConfiguration(EClass eClass, int attributeIndex, String featureName,
			String featureTypeName, int lowerBound, int upperBound) {
		EAttribute feature = assertAttributeConfiguration(eClass, attributeIndex, featureName, featureTypeName);
		assertEquals(lowerBound, feature.getLowerBound());
		assertEquals(upperBound, feature.getUpperBound());

		return feature;
	}

	private EReference assertReferenceConfiguration(EClass eClass, int referenceIndex, String featureName,
			String featureTypeName, boolean isContainment, int lowerBound, int upperBound) {
		EReference reference = eClass.getEReferences().get(referenceIndex);
		assertEquals(featureName, reference.getName());
		assertNotNull(reference.getEType());
		assertEquals(featureTypeName, reference.getEType().getName());
		assertEquals(isContainment, reference.isContainment());
		assertEquals(lowerBound, reference.getLowerBound());
		assertEquals(upperBound, reference.getUpperBound());
		return reference;
	}

	public void testTypesOfImplicitSuperGrammar() throws Exception {
		final String xtextGrammar = "language test generate test 'http://test' MyRule: myFeature=INT;";
		Grammar grammar = (Grammar) getModel(xtextGrammar);
		Xtext2EcoreTransformer transformer = new Xtext2EcoreTransformer(grammar);
		transformer.removeGeneratedPackages();
		transformer.transform();
		// directly from grammar
		AbstractRule rule = grammar.getRules().get(0);
		TypeRef type = rule.getType();
		assertNotNull(type);
		assertNotNull(transformer.getEClassifierInfos().getInfo(type));
		// ecore data types
		ReferencedMetamodel referenced = (ReferencedMetamodel) GrammarUtil.allMetamodelDeclarations(grammar).get(1);
		assertNotNull(referenced);
		assertEquals("ecore", referenced.getAlias());
		assertNull(transformer.getEClassifierInfos().getInfo(referenced, "EString"));
		assertNull(transformer.getEClassifierInfos().getInfo(referenced, "EInt"));
		EClassifierInfos parentInfos = transformer.getEClassifierInfos().getParent();
		assertNotNull(parentInfos.getInfo(referenced, "EString"));
		assertNotNull(parentInfos.getInfo(referenced, "EInt"));
	}

	public void testRuleWithoutExplicitReturnType() throws Exception {
		final String grammar = "language test generate test 'http://test' MyRule: myFeature=INT;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		EList<EClassifier> classifiers = ePackage.getEClassifiers();
		assertEquals(1, classifiers.size());
		EClassifier implicitlyDefinedMetatype = classifiers.get(0);
		assertEquals("MyRule", implicitlyDefinedMetatype.getName());
	}

	public void testRulesWithExplicitReturnType() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA returns TypeA: featureA=INT; RuleB returns TypeB: featureB= INT;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(2, ePackage.getEClassifiers().size());
		assertNotNull(ePackage.getEClassifier("TypeA"));
		assertNotNull(ePackage.getEClassifier("TypeB"));
	}

	public void testSimpleHierarchy() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: RuleB; RuleB: featureB= INT;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(2, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);
		assertTrue(ruleA.getESuperTypes().isEmpty());
		assertEquals(1, ruleB.getESuperTypes().size());
		EClass superClass = ruleB.getESuperTypes().get(0);
		assertEquals(ruleA, superClass);
	}

	public void testSingleFeatures() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: featureA=INT;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);

		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EInt");
	}

	public void testBuiltInFeatureTypes() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: featureA=ID featureB=INT featureC=STRING;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);

		assertEquals(3, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EString");
		assertAttributeConfiguration(ruleA, 1, "featureB", "EInt");
		assertAttributeConfiguration(ruleA, 2, "featureC", "EString");
	}

	public void testCardinalityOfFeatures() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: featureA?=ID featureB=INT featureC+=STRING;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);

		assertEquals(3, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EBoolean", 0, 1);
		assertAttributeConfiguration(ruleA, 1, "featureB", "EInt", 0, 1);
		assertAttributeConfiguration(ruleA, 2, "featureC", "EString", 0, -1);
	}

	public void testOptionalAssignmentsInGroup() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: (featureA?='abstract' featureB+=INT)?;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(1, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		assertEquals(2, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EBoolean", 0, 1);
		assertAttributeConfiguration(ruleA, 1, "featureB", "EInt", 0, -1);
	}

	public void testFeaturesAndInheritanceOptionalRuleCall() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: RuleB? featureA=INT; RuleB: featureB=STRING;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(2, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);

		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EInt");

		assertEquals(1, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureB", "EString");
	}

	public void testFeaturesAndInheritanceMandatoryRuleCall() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: RuleB featureA=INT; RuleB: featureB=STRING;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(2, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);

		assertEquals(0, ruleA.getEAttributes().size());

		assertEquals(2, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureA", "EInt");
		assertAttributeConfiguration(ruleB, 1, "featureB", "EString");
	}

	public void testFeaturesAndInheritanceOfMandatoryAlternativeRuleCalls() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: (RuleB|RuleC featureC1=ID) featureA=ID; RuleB: featureB=ID; RuleC: featureC2=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(3, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);

		// test inheritance
		assertTrue(ruleA.getESuperTypes().isEmpty());
		assertEquals(1, ruleB.getESuperTypes().size());
		assertEquals(ruleA, ruleB.getESuperTypes().get(0));
		assertEquals(1, ruleC.getESuperTypes().size());
		assertEquals(ruleA, ruleC.getESuperTypes().get(0));

		// test all features are separated
		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EString");
		assertEquals(1, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureB", "EString");
		assertEquals(2, ruleC.getEAttributes().size());
		assertAttributeConfiguration(ruleC, 0, "featureC1", "EString");
		assertAttributeConfiguration(ruleC, 1, "featureC2", "EString");
	}

	public void testFeaturesAndInheritanceOfOptionalOptionalRuleCalls() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: (RuleB|RuleC featureC1=ID)? featureA=ID; RuleB: featureB=ID; RuleC: featureC2=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(3, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);

		// test inheritance
		assertTrue(ruleA.getESuperTypes().isEmpty());
		assertEquals(1, ruleB.getESuperTypes().size());
		assertEquals(ruleA, ruleB.getESuperTypes().get(0));
		assertEquals(1, ruleC.getESuperTypes().size());
		assertEquals(ruleA, ruleC.getESuperTypes().get(0));

		// test all features are separated
		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EString");
		assertEquals(1, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureB", "EString");
		assertEquals(2, ruleC.getEAttributes().size());
		assertAttributeConfiguration(ruleC, 0, "featureC1", "EString");
		assertAttributeConfiguration(ruleC, 1, "featureC2", "EString");
	}

	public void testFeaturesAndInheritanceOfNestedRuleCalls() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += " RuleA: ((RuleB|RuleC featureC1=ID)? featureBC=ID | (RuleC|RuleD featureD1=ID) featureCD=ID) featureA=ID;";
		grammar += " RuleB: featureB2=ID;";
		grammar += " RuleC: featureC2=ID;";
		grammar += " RuleD: featureD2=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(4, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);
		EClass ruleD = (EClass) ePackage.getEClassifier("RuleD");
		assertNotNull(ruleD);

		// test inheritance
		assertTrue(ruleA.getESuperTypes().isEmpty());
		assertEquals(1, ruleB.getESuperTypes().size());
		assertEquals(ruleA, ruleB.getESuperTypes().get(0));
		assertEquals(1, ruleC.getESuperTypes().size());
		assertEquals(ruleA, ruleC.getESuperTypes().get(0));
		assertEquals(1, ruleD.getESuperTypes().size());
		assertEquals(ruleA, ruleD.getESuperTypes().get(0));

		// test all features are separated
		assertEquals(2, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureBC", "EString");
		assertAttributeConfiguration(ruleA, 1, "featureA", "EString");
		assertEquals(1, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureB2", "EString");
		assertEquals(3, ruleC.getEAttributes().size());
		assertAttributeConfiguration(ruleC, 0, "featureC1", "EString");
		assertAttributeConfiguration(ruleC, 1, "featureCD", "EString");
		assertAttributeConfiguration(ruleC, 2, "featureC2", "EString");
		assertEquals(3, ruleD.getEAttributes().size());
		assertAttributeConfiguration(ruleD, 0, "featureD1", "EString");
		assertAttributeConfiguration(ruleD, 1, "featureCD", "EString");
		assertAttributeConfiguration(ruleD, 2, "featureD2", "EString");
	}

	public void testFeaturesAndInheritanceOfActions01() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: ({Add.a=current} '+'|{Sub.a=current} '-') featureAS=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(3, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass add = (EClass) ePackage.getEClassifier("Add");
		assertNotNull(add);
		EClass sub = (EClass) ePackage.getEClassifier("Sub");
		assertNotNull(sub);

		// test inheritance
		assertTrue(ruleA.getESuperTypes().isEmpty());
		assertEquals(1, add.getESuperTypes().size());
		assertEquals(ruleA, add.getESuperTypes().get(0));
		assertEquals(1, sub.getESuperTypes().size());
		assertEquals(ruleA, sub.getESuperTypes().get(0));

		// test features
		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureAS", "EString");
		assertEquals(1, ruleA.getEReferences().size());
		assertReferenceConfiguration(ruleA, 0, "a", "RuleA", true, 0, 1);
		
		assertEquals(0, add.getEAttributes().size());
		assertEquals(0, add.getEReferences().size());

		assertEquals(0, sub.getEAttributes().size());
		assertEquals(0, sub.getEReferences().size());
	}

	public void testFeaturesAndInheritanceOfActions02() throws Exception {
		String grammar = "";
		grammar += "language org.eclipse.xtext.testlanguages.ActionTestLanguage ";
		grammar += "generate ActionLang";
		grammar += " 'http://www.eclipse.org/2008/tmf/xtext/ActionLang'";
		grammar += "";
		grammar += " Model:";
		grammar += " (children+=Element)*;";
		grammar += "";
		grammar += " Element returns Type:";
		grammar += " Item ( { Item.items+=current } items+=Item );";
		grammar += "";
		grammar += " Item returns Type:";
		grammar += " { Thing.content=current } name=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(4, ePackage.getEClassifiers().size());

		EClass model = (EClass) ePackage.getEClassifier("Model");
		assertNotNull(model);
		EClass type = (EClass) ePackage.getEClassifier("Type");
		assertNotNull(type);
		EClass item = (EClass) ePackage.getEClassifier("Item");
		assertNotNull(item);
		EClass thing = (EClass) ePackage.getEClassifier("Thing");
		assertNotNull(thing);

		// type hierarchy
		assertEquals(0, model.getESuperTypes().size());
		assertEquals(0, type.getESuperTypes().size());
		assertEquals(1, item.getESuperTypes().size());
		assertSame(type, item.getESuperTypes().get(0));
		assertEquals(1, thing.getESuperTypes().size());
		assertSame(type, thing.getESuperTypes().get(0));
	}

	public void testAssignedRuleCall() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: callA1=RuleB callA2+=RuleB simpleFeature=ID; RuleB: featureB=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(2, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);

		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "simpleFeature", "EString");
		assertEquals(2, ruleA.getEReferences().size());
		assertReferenceConfiguration(ruleA, 0, "callA1", "RuleB", true, 0, 1);
		assertReferenceConfiguration(ruleA, 1, "callA2", "RuleB", true, 0, -1);
		assertEquals(1, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureB", "EString");
	}

	public void testAssignedCrossReference() throws Exception {
		final String grammar = "language test generate test 'http://test' " +
				"RuleA: refA1=[TypeB] refA2+=[TypeB|RuleB] simpleFeature=ID; " +
				"RuleB returns TypeB: featureB=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(2, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass typeB = (EClass) ePackage.getEClassifier("TypeB");
		assertNotNull(typeB);

		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "simpleFeature", "EString");
		assertEquals(2, ruleA.getEReferences().size());
		assertReferenceConfiguration(ruleA, 0, "refA1", "TypeB", false, 0, 1);
		assertReferenceConfiguration(ruleA, 1, "refA2", "TypeB", false, 0, -1);
		assertEquals(1, typeB.getEAttributes().size());
		assertAttributeConfiguration(typeB, 0, "featureB", "EString");
	}

	public void testAssignedParenthesizedElement() throws Exception {
		String grammar = " language test generate test 'http://test'";
		grammar += " RuleA: featureA1?=(RuleB) refA1=(RuleB) refA2=(RuleB|RuleC) refA3+=(RuleB|RuleC|RuleD) refA4=(RuleB|RuleD) featureA2+=('a'|'b');";
		grammar += " RuleB returns TypeB: RuleC? featureB=ID;";
		grammar += " RuleC: featureC=ID;";
		grammar += " RuleD returns TypeB: featureD=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);

		assertEquals(3, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		assertEquals(0, ruleA.getESuperTypes().size());
		EClass typeB = (EClass) ePackage.getEClassifier("TypeB");
		assertNotNull(typeB);
		assertEquals(0, typeB.getESuperTypes().size());
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);
		assertEquals(1, ruleC.getESuperTypes().size());
		assertEquals(typeB, ruleC.getESuperTypes().get(0));

		assertEquals(2, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA1", "EBoolean");
		assertAttributeConfiguration(ruleA, 1, "featureA2", "EString", 0, -1);

		assertEquals(4, ruleA.getEReferences().size());
		assertReferenceConfiguration(ruleA, 0, "refA1", "TypeB", true, 0, 1);
		assertReferenceConfiguration(ruleA, 1, "refA2", "TypeB", true, 0, 1);
		assertReferenceConfiguration(ruleA, 2, "refA3", "TypeB", true, 0, -1);
		assertReferenceConfiguration(ruleA, 3, "refA4", "TypeB", true, 0, 1);
	}

	public void testAssignedKeyword() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: featureA?=('+'|'-') featureB=('*'|'/');";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(1, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");

		assertEquals(2, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EBoolean", 0, 1);
		assertAttributeConfiguration(ruleA, 1, "featureB", "EString", 0, 1);
	}

	public void testImportWithoutAlias() throws Exception {
		final String grammar = "language test generate test 'http://test' import 'http://www.eclipse.org/emf/2002/Ecore' RuleA: feature=ID;";
		getEPackageFromGrammar(grammar);
	}

	public void testGenerateTwoModels() throws Exception {
		String grammar = "";
		grammar += " language test";
		grammar += " generate t1 'http://t1'";
		grammar += " generate t2 'http://t2' as t2";
		grammar += " RuleA: featureA=ID;";
		grammar += " RuleB returns t2::TypeB: featureB=ID;";
		List<EPackage> ePackages = getEPackagesFromGrammar(grammar);
		assertEquals(2, ePackages.size());

		EPackage t1 = ePackages.get(0);
		assertEquals("t1", t1.getName());

		assertEquals(1, t1.getEClassifiers().size());
		EClassifier ruleA = t1.getEClassifier("RuleA");
		assertNotNull(ruleA);

		EPackage t2 = ePackages.get(1);
		assertEquals(1, t2.getEClassifiers().size());
		assertEquals("t2", t2.getName());
		EClassifier typeB = t2.getEClassifier("TypeB");
		assertNotNull(typeB);
	}

	public void testUseSameModelAlias() throws Exception {
		String grammar = "";
		grammar += " language test";
		grammar += " generate t1 'http://t1' as target";
		grammar += " generate t2 'http://t2' as target";
		grammar += " RuleA: featureA=ID;"; // no alias => cannot be created
		grammar += " RuleB returns target::TypeB: featureB=ID;";

		errorAcceptorMock.acceptError(same(TransformationErrorCode.AliasForMetamodelAlreadyExists), (String) anyObject(),	(EObject) anyObject());
		errorAcceptorMock.acceptError(same(TransformationErrorCode.UnknownMetaModelAlias), (String) anyObject(), (EObject) anyObject());
		errorAcceptorMock.acceptError(same(TransformationErrorCode.UnknownMetaModelAlias), (String) anyObject(), (EObject) anyObject());

		List<EPackage> ePackages = getEPackagesFromGrammar(grammar);
		assertEquals(0, ePackages.size());
	}

	public void testModifyingSealedModel() throws Exception {
		final String grammar = "language test " +
				"generate test 'http://test' " +
				"import 'http://www.eclipse.org/emf/2002/Ecore' as ecore " +
				"RuleA returns ecore::SomeNewTypeA: feature=ID;";
		errorAcceptorMock.acceptError(same(TransformationErrorCode.CannotCreateTypeInSealedMetamodel), (String) anyObject(), (EObject) anyObject());
		EPackage result = getEPackageFromGrammar(grammar);
		assertTrue(result.getEClassifiers().isEmpty());
	}

	public void testImportingUnknownModel() throws Exception {
		final String grammar = "language test " +
				"generate test 'http://test' " +
				"import 'http://www.unknownModel' as unknownModel " +
				"RuleA: feature=ID;";
		getEPackageFromGrammar(grammar);
	}

	public void testMoreThanOneRuleCall() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: RuleB RuleC; RuleB: featureB=ID; RuleC: featureC=ID;";
		errorAcceptorMock.acceptError(same(TransformationErrorCode.MoreThanOneTypeChangeInOneRule), (String) anyObject(),
				(EObject) anyObject());
		getEPackageFromGrammar(grammar);
	}

	public void testRuleCallAndAction() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: RuleB {TypeC.B = current}; RuleB: featureB=ID;";
		getEPackageFromGrammar(grammar);
	}

	public void testRuleCallActionAndRuleCall() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA: RuleB {TypeC.B = current} RuleB; RuleB: featureB=ID;";
		errorAcceptorMock.acceptError(same(TransformationErrorCode.MoreThanOneTypeChangeInOneRule), (String) anyObject(),
				(EObject) anyObject());
		getEPackageFromGrammar(grammar);
	}

	public void testAddingFeatureTwice() throws Exception {
		final String grammar = "language test generate test 'http://test' RuleA returns TypeA: featureA=ID; RuleB returns TypeA: featureA=STRING;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(1, ePackage.getEClassifiers().size());
		EClass typeA = (EClass) ePackage.getEClassifier("TypeA");
		assertNotNull(typeA);

		assertEquals(1, typeA.getEAttributes().size());
		assertAttributeConfiguration(typeA, 0, "featureA", "EString");
	}

	public void testAddingDifferentFeaturesWithSameName01() throws Exception {
		// simple datatypes do not have a common compatible type
		final String grammar = "" + " language test generate test 'http://test'" + " RuleA returns TypeA: featureA=ID;"
				+ " RuleB returns TypeA: featureA=INT;";

		errorAcceptorMock.acceptError(same(TransformationErrorCode.NoCompatibleFeatureTypeAvailable), (String) anyObject(),
				(EObject) anyObject());
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(1, ePackage.getEClassifiers().size());
		EClass typeA = (EClass) ePackage.getEClassifier("TypeA");
		assertNotNull(typeA);

		assertEquals(1, typeA.getEAttributes().size());
		assertAttributeConfiguration(typeA, 0, "featureA", "EString");
	}

	public void testAddingDifferentFeaturesWithSameName02() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += " RuleA returns TypeA: featureA=RuleD;";
		grammar += " RuleB returns TypeA: featureA=RuleC;";
		grammar += " RuleC: RuleD;";
		grammar += " RuleD: featureD=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);

		assertEquals(3, ePackage.getEClassifiers().size());
		EClass typeA = (EClass) ePackage.getEClassifier("TypeA");
		assertNotNull(typeA);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);
		EClass ruleD = (EClass) ePackage.getEClassifier("RuleD");
		assertNotNull(ruleD);

		assertEquals(1, typeA.getEReferences().size());
		assertReferenceConfiguration(typeA, 0, "featureA", "RuleC", true, 0, 1);
	}

	public void testAddingDifferentFeaturesWithSameName03() throws Exception {
		// independent rules are combined as EObject
		String grammar = "language test generate test 'http://test'";
		grammar += " RuleA returns TypeA: featureA1=ID featureA2=RuleD featureA3=RuleC;";
		grammar += " RuleB returns TypeA: featureA2=RuleC featureA4=INT;";
		grammar += " RuleC: featureC=INT;";
		grammar += " RuleD: featureD=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);

		assertEquals(3, ePackage.getEClassifiers().size());
		EClass typeA = (EClass) ePackage.getEClassifier("TypeA");
		assertNotNull(typeA);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);
		EClass ruleD = (EClass) ePackage.getEClassifier("RuleD");
		assertNotNull(ruleD);

		assertEquals(2, typeA.getEAllAttributes().size());
		assertAttributeConfiguration(typeA, 0, "featureA1", "EString");
		assertAttributeConfiguration(typeA, 1, "featureA4", "EInt");

		assertEquals(2, typeA.getEReferences().size());
		assertReferenceConfiguration(typeA, 0, "featureA2", "EObject", true, 0, 1);
		assertReferenceConfiguration(typeA, 1, "featureA3", "RuleC", true, 0, 1);
	}
	
	public void testUplift01() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += " RuleA: (RuleB|RuleC) featureA=ID;";
		grammar += " RuleB: featureB=INT;";
		grammar += " RuleC: (featureA=ID)?;";
		EPackage ePackage = getEPackageFromGrammar(grammar);

		assertEquals(3, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);

		assertEquals(1, ruleA.getEAttributes().size());
		assertAttributeConfiguration(ruleA, 0, "featureA", "EString");
		
		assertEquals(1, ruleB.getEAttributes().size());
		assertAttributeConfiguration(ruleB, 0, "featureB", "EInt");

		assertEquals(0, ruleC.getEAttributes().size());
	}
	
	public void testCallOfUndeclaredRule() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += " RuleA: CallOfUndeclaredRule featureA=ID;";
		errorAcceptorMock.acceptError(same(TransformationErrorCode.NoSuchRuleAvailable), (String) anyObject(),
				(EObject) anyObject());
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(1, ePackage.getEClassifiers().size());
		assertEquals("RuleA", ePackage.getEClassifiers().get(0).getName());
	}
	
	public void testCycleInTypeHierarchy() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += " RuleA: RuleB;";
		grammar += " RuleB: RuleC;";
		grammar += " RuleC: RuleA;";
		grammar += " RuleD: RuleA;";
		
		errorAcceptorMock.acceptError(same(TransformationErrorCode.TypeWithCycleInHierarchy), (String) anyObject(),
				(EObject) anyObject());
		EasyMock.expectLastCall().times(3);
		
		EPackage ePackage = getEPackageFromGrammar(grammar);
		assertEquals(4, ePackage.getEClassifiers().size());
		EClass ruleA = (EClass) ePackage.getEClassifier("RuleA");
		assertNotNull(ruleA);
		EClass ruleB = (EClass) ePackage.getEClassifier("RuleB");
		assertNotNull(ruleB);
		EClass ruleC = (EClass) ePackage.getEClassifier("RuleC");
		assertNotNull(ruleC);	
		EClass ruleD = (EClass) ePackage.getEClassifier("RuleD");
		assertNotNull(ruleD);	
		
		assertEquals(2, ruleA.getESuperTypes().size());
		assertSame(ruleC, ruleA.getESuperTypes().get(0));
		assertSame(ruleD, ruleA.getESuperTypes().get(1));
		assertEquals(1, ruleB.getESuperTypes().size());
		assertSame(ruleA, ruleB.getESuperTypes().get(0));
		assertEquals(1, ruleC.getESuperTypes().size());
		assertSame(ruleB, ruleC.getESuperTypes().get(0));
		assertEquals(0, ruleD.getESuperTypes().size());
	}
	
	public void testExpressionLikeLangauge() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += " Ex :	Atom  ({ChainExpression.left+=current} operator=('+'|'-'|'*'|'/') right=Atom )*;" +
				"Atom returns Ex :   Number |  '(' Ex ')';" +
				"Number : value=INT;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		EClass classifier = (EClass) ePackage.getEClassifier("Ex");
		logger.debug(EmfFormater.objToStr(ePackage, ""));
		assertEquals(0,classifier.getEStructuralFeatures().size());
	}
	
	public void testClassNameEString() throws Exception {
		String grammar = "language test generate test 'http://test'";
		grammar += "Start returns EString: id=ID;";
		EPackage ePackage = getEPackageFromGrammar(grammar);
		EClass classifier = (EClass) ePackage.getEClassifier("EString");
		assertEquals("EString", classifier.getName());
		EStructuralFeature feature = classifier.getEStructuralFeature("id");
		assertNotNull(feature);
		assertEquals("EString", feature.getEType().getName());
		assertFalse(feature.getEType().equals(classifier));
		assertEquals(EcorePackage.Literals.ESTRING, feature.getEType());
	}
	
	public void testNoException_01() throws Exception {
		String grammar = "language test import 'http://www.eclipse.org/emf/2002/Ecore' as ecore " +
				"generate test 'http://test'\n" +
				"CompositeModel: (model+=Model)+;\n" +
				"Model: id=NestedModelId (':' value=Fraction)? ('#' vector=Vector)? ('+' dots=Dots)? ';'\n" +
				"ModelId returns ecore::EString: ID '.' ID;\n" +
				"NestedModelId : ModelId '.' ModelId;\n" +
				"Fraction returns EBigDecimal: INT ('/' INT)?;\n" +
				"Vector : '(' INT I";
		XtextResource resource = getResourceFromString(grammar);
		for(Diagnostic d: resource.getErrors()) {
			assertFalse(d instanceof ExceptionDiagnostic);
		}
	}
	
	public void testNoException_02() throws Exception {
		String grammar = "language test generate test 'http://test'\n" +
				"Model: (children+=Element)*;\n" +
				"Element returns Type: Item ( { Item.items+=current } items+=Item );\n" +
				"Item returns Type:	{ T";
		XtextResource resource = getResourceFromString(grammar);
		for(Diagnostic d: resource.getErrors()) {
			assertFalse(d instanceof ExceptionDiagnostic);
		}
	}
	
	public void testNoException_03() throws Exception {
		String grammar = "language test import 'http://www.eclipse.org/emf/2002/Ecore' as ecore " +
				"generate test 'http://test'\n" +
				"CompositeModel: (type+=EClassifier)+;\n" +
				"EClassifier returns ecore::EClassifier: EDataType | EClass;\n" +
				"EClass returns ecore::EClass: 'class' name=ID;\n" +
				"EDataType returns ecore::EDataType: 'dt' name=ID;";
		XtextResource resource = getResourceFromString(grammar);
		assertTrue(resource.getErrors().isEmpty());
	}
}
