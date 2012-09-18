/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.internal;

import org.eclipse.xtext.common.types.JvmExecutable;
import org.eclipse.xtext.xbase.XAssignment;
import org.eclipse.xtext.xbase.XExpression;

/**
 * Externalized for testing purpose.
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ExpressionArgumentFactory {

	public IExpressionArguments createExpressionArguments(XExpression expression, AbstractLinkingCandidate<?> candidate) {
		if (expression instanceof XAssignment && !(candidate.getFeature() instanceof JvmExecutable))
			return new AssignmentArguments(candidate);
		else
			return new FeatureCallArguments(candidate);
	}

}
