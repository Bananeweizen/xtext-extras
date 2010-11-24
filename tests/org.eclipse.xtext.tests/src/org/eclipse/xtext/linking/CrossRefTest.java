package org.eclipse.xtext.linking;

import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.GrammarUtil;
import org.eclipse.xtext.IGrammarAccess;
import org.eclipse.xtext.junit.AbstractXtextTests;
import org.eclipse.xtext.linking.langATestLanguage.LangATestLanguageFactory;
import org.eclipse.xtext.linking.langATestLanguage.Main;
import org.eclipse.xtext.linking.langATestLanguage.Type;
import org.eclipse.xtext.linking.lazy.LazyLinkingTestLanguageStandaloneSetup;
import org.eclipse.xtext.linking.lazy.lazyLinking.Model;
import org.eclipse.xtext.linking.lazy.lazyLinking.Property;
import org.eclipse.xtext.linking.lazy.services.LazyLinkingTestLanguageGrammarAccess;
import org.eclipse.xtext.linking.services.LangATestLanguageGrammarAccess;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parsetree.reconstr.ITokenSerializer;
import org.eclipse.xtext.parsetree.reconstr.ITokenSerializer.ICrossReferenceSerializer;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

public class CrossRefTest extends AbstractXtextTests {
	private static final Logger logger = Logger.getLogger(CrossRefTest.class);
	private ICrossReferenceSerializer linkingService;
	private LangATestLanguageGrammarAccess grammar;
	
	protected INode getCrossReferenceNode(EObject context, EReference reference, EObject target) {
		List<INode> nodes = NodeModelUtils.findNodesForFeature(context, reference);
		if (!nodes.isEmpty()) {
			if (reference.isMany()) {
				int index = ((List<?>) context.eGet(reference, false)).indexOf(target);
				if (index >= 0 && index < nodes.size())
					return nodes.get(index);
			} else {
				return nodes.get(0);
			}
		}
		return null;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		with(LangATestLanguageStandaloneSetup.class);
		linkingService =  get(ICrossReferenceSerializer.class);
		grammar = (LangATestLanguageGrammarAccess) get(IGrammarAccess.class);
	}

	public void testSimple() throws Exception {
		EObject model = getModel("type A extends B type B extends A");
		logger.debug(invokeWithXtend("types.collect(e|e.name+' '+e.extends.name).toString(',')", model));
		assertWithXtend("'B'", "types.first().extends.name", model);
		assertWithXtend("types.first()", "types.first().extends.extends", model);
	}

	public void testGetLinkedObjects() throws Exception {
		XtextResource r = getResourceFromString("type TypeA extends TypeB type TypeB extends TypeA type AnotherType extends TypeA");
		EObject model = r.getParseResult().getRootASTElement();
		ILeafNode leaf = NodeModelUtils.findLeafNodeAtOffset(r.getParseResult().getRootNode2(), 6);

		assertWithXtend("3", "types.size", model);

		EObject context = (EObject) invokeWithXtend("types.first()", model);
		Assignment asExtends = get(LangATestLanguageGrammarAccess.class).getTypeAccess().getExtendsAssignment_2_1();
		CrossReference xref = (CrossReference) asExtends.getTerminal();
		EReference ref = GrammarUtil.getReference(xref, context.eClass());

		assertEquals(1, getLinkingService().getLinkedObjects(context, ref, leaf).size());
	}

	public void testGetSingleValuedLinkText() throws Exception {
		XtextResource r = getResourceFromStringAndExpect("type TypeA extends ^extends type ^extends extends ^type", 1);
		Main model = (Main) r.getContents().get(0);
		assertEquals(2, model.getTypes().size());

		Type type = model.getTypes().get(0);
		assertEquals("TypeA", type.getName());
		Type superType = type.getExtends();
		assertEquals("extends", superType.getName());
		String linkText = linkingService.serializeCrossRef(type, grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0(), superType, null);
		assertEquals("^extends", linkText);

		type = superType;
		superType = type.getExtends();
		assertTrue(superType.eIsProxy());
		INode node = getCrossReferenceNode(type, GrammarUtil.getReference(grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0()), superType);
		linkText = linkingService.serializeCrossRef(type, grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0(), superType, node);
		assertEquals("^type", linkText);

		type.eAdapters().remove(NodeModelUtils.getNode(type));
		linkText = linkingService.serializeCrossRef(type, grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0(), superType, null);
		assertNull(linkText);
	}

	public void testGetMultiValuedLinkText() throws Exception {
		with(LazyLinkingTestLanguageStandaloneSetup.class);
		linkingService =  get(ICrossReferenceSerializer.class);
		LazyLinkingTestLanguageGrammarAccess g =  (LazyLinkingTestLanguageGrammarAccess) get(IGrammarAccess.class);

		XtextResource r = getResourceFromStringAndExpect("type TypeA {} type TypeB { TypeA TypeC TypeB p1; }", 1);
		Model model = (Model) r.getContents().get(0);
		assertEquals(2, model.getTypes().size());
		
		org.eclipse.xtext.linking.lazy.lazyLinking.Type type = model.getTypes().get(1);
		assertEquals("TypeB", type.getName());
		assertEquals(1, type.getProperties().size());

		Property prop = type.getProperties().get(0);
		assertEquals("p1", prop.getName());
		assertEquals(3, prop.getType().size());

		org.eclipse.xtext.linking.lazy.lazyLinking.Type propType = prop.getType().get(0);
		assertFalse(propType.eIsProxy());
		String linkText = linkingService.serializeCrossRef(prop,g.getPropertyAccess().getTypeTypeCrossReference_0_0(), propType, null);
		assertEquals("TypeA", linkText);

		propType = prop.getType().get(1);
		assertTrue(propType.eIsProxy());
		INode node = getCrossReferenceNode(prop, GrammarUtil.getReference(g.getPropertyAccess().getTypeTypeCrossReference_0_0()), propType);
		linkText = linkingService.serializeCrossRef(prop,g.getPropertyAccess().getTypeTypeCrossReference_0_0(), propType, node);
		assertEquals("TypeC", linkText);

		propType = prop.getType().get(2);
		assertFalse(propType.eIsProxy());
		node = getCrossReferenceNode(prop, GrammarUtil.getReference(g.getPropertyAccess().getTypeTypeCrossReference_0_0()), propType);
		linkText = linkingService.serializeCrossRef(prop,g.getPropertyAccess().getTypeTypeCrossReference_0_0(), propType, null);
		assertEquals("TypeB", linkText);

		prop.eAdapters().remove(NodeModelUtils.getNode(prop));
		propType = prop.getType().get(1);
		assertTrue(propType.eIsProxy());
		linkText = linkingService.serializeCrossRef(prop,g.getPropertyAccess().getTypeTypeCrossReference_0_0(), propType, null);
		assertNull(linkText);
	}

	/* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=287813 */
	public void testNonDefaultLinkText() throws Exception {
		XtextResource r = getResourceFromString("type TypeA extends ^TypeB type TypeB");
		Main model = (Main) r.getContents().get(0);
		assertEquals(2, model.getTypes().size());

		Type type = model.getTypes().get(0);
		assertEquals("TypeA", type.getName());
		Type superType = type.getExtends();
		assertEquals("TypeB", superType.getName());
		INode node = getCrossReferenceNode(type, GrammarUtil.getReference(grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0()), superType);
		String linkText = linkingService.serializeCrossRef(type, grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0(), superType, node);
		assertTrue(ITokenSerializer.KEEP_VALUE_FROM_NODE_MODEL == linkText);
	}

	/* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=287813 */
	public void testOutOfSyncNodeModel() throws Exception {
		XtextResource r = getResourceFromString("type TypeA extends ^TypeB type TypeB ");
		Main model = (Main) r.getContents().get(0);

		Type type = model.getTypes().get(0);
		Type superType = type.getExtends();
		superType.setName("TypeC");

		String linkText = linkingService.serializeCrossRef(type, grammar.getTypeAccess().getExtendsTypeCrossReference_2_1_0(), superType, null);
		assertEquals("TypeC", linkText);
	}
	
	/* see https://bugs.eclipse.org/bugs/show_bug.cgi?id=298506 */
	public void testCrossReferenceValueConverter() throws Exception {
		Resource r = get(XtextResourceSet.class).createResource(URI.createURI("test." + getCurrentFileExtension()));
		Type ele = LangATestLanguageFactory.eINSTANCE.createType();
		r.getContents().add(ele);
		ele.setName("type");
		ele.setExtends(ele);
		assertEquals("type ^type extends ^type", getSerializer().serialize(ele));
	}

}
