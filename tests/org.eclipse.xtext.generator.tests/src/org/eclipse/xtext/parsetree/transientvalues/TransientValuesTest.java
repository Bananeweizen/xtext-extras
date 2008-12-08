package org.eclipse.xtext.parsetree.transientvalues;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.tests.AbstractGeneratorTest;

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
public class TransientValuesTest extends AbstractGeneratorTest {

	protected void setUp() throws Exception {
		with(TransientValuesTestStandaloneSetup.class);
		super.setUp();
	}

	public void testRequired1() throws Exception {
		final String model = "test required 1 1";
		EObject m = getModel(model);
		// System.out.println(EmfFormater.objToStr(m, ""));
		String s = serialize(m);
		assertEquals(model, s);
	}

	public void testOptional1() throws Exception {
		final String in = "test optional 12";
		final String out = "test optional ";
		String s = serialize(getModel(in));
		assertEquals(out, s);
	}

	public void testOptional2() throws Exception {
		final String in = "test optional 12:13";
		final String out = "test optional ";
		String s = serialize(getModel(in));
		assertEquals(out, s);
	}

	public void testList() throws Exception {
		final String in = "test list 1 2 3 4 5 6";
		final String out = "test list 1 3 5";
		String s = serialize(getModel(in)).trim();
		assertEquals(out, s);
	}
}
