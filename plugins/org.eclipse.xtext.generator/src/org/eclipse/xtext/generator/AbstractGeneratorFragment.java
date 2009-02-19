/*******************************************************************************
 * Copyright (c) 2008, 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.xtext.generator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.xpand2.XpandExecutionContext;
import org.eclipse.xpand2.XpandFacade;
import org.eclipse.xtext.Grammar;

/**
 * @author Sven Efftinge - Initial contribution and API
 *
 * base class redirecting callbacks to respective Xpand definitions.
 * The template needs to have the same qualified name the extending class has. 
 */
public abstract class AbstractGeneratorFragment implements IGeneratorFragment {
	
	private Logger log = Logger.getLogger(getClass());
	
	protected String getTemplate() {
		return getClass().getName().replaceAll("\\.", "::");
	}
	
	public void generate(Grammar grammar, XpandExecutionContext ctx) {
		if (log.isInfoEnabled())
			log.info("executing generate for "+getClass().getName());
		XpandFacade.create(ctx).evaluate2(getTemplate()+"::generate", grammar, getParameters(grammar));
	}

	public void addToPluginXmlRt(Grammar grammar, XpandExecutionContext ctx) {
		XpandFacade.create(ctx).evaluate2(getTemplate()+"::addToPluginXmlRt", grammar, getParameters(grammar));
	}
	
	public void addToPluginXmlUi(Grammar grammar, XpandExecutionContext ctx) {
		XpandFacade.create(ctx).evaluate2(getTemplate()+"::addToPluginXmlUi", grammar, getParameters(grammar));
	}

	public void addToStandaloneSetup(Grammar grammar, XpandExecutionContext ctx) {
		XpandFacade.create(ctx).evaluate2(getTemplate()+"::addToStandaloneSetup", grammar, getParameters(grammar));
	}
	
	public String[] getExportedPackagesRt(Grammar grammar) {
		return null;
	}

	public String[] getExportedPackagesUi(Grammar grammar) {
		return null;
	}

	public Map<String, String> getGuiceBindingsRt(Grammar grammar) {
		return null;
	}

	public Map<String, String> getGuiceBindingsUi(Grammar grammar) {
		return null;
	}

	public String[] getRequiredBundlesRt(Grammar grammar) {
		return null;
	}

	public String[] getRequiredBundlesUi(Grammar grammar) {
		return null;
	}

	protected List<Object> getParameters(Grammar grammar) {
		return Collections.emptyList();
	}

}
