/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator;

import org.eclipse.xpand2.XpandExecutionContext;

/**
 * @since 2.0
 */
public class Xtend2ExecutionContext {
	XpandExecutionContext legacyContext;

	Xtend2ExecutionContext(XpandExecutionContext legacyContext) {
		super();
		this.legacyContext = legacyContext;
	}

	public void writeFile(String outletName, String filename, String contents) {
		legacyContext.getOutput().openFile(filename, outletName);
		legacyContext.getOutput().write(contents);
		legacyContext.getOutput().closeFile();
	}
}