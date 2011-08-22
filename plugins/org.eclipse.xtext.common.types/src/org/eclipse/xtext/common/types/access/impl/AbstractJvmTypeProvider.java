/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.access.impl;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.access.IMirror;
import org.eclipse.xtext.common.types.access.TypeResource;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public abstract class AbstractJvmTypeProvider implements IJvmTypeProvider, Resource.Factory {

	private final ResourceSet resourceSet;
	
	private final PrimitiveTypeFactory primitiveTypeFactory;
	
	protected AbstractJvmTypeProvider(ResourceSet resourceSet) {
		if (resourceSet == null)
			throw new IllegalArgumentException("resourceSet may not be null");
		this.resourceSet = resourceSet;
		registerProtocol(resourceSet);
		primitiveTypeFactory = new PrimitiveTypeFactory();
	}

	protected void registerProtocol(ResourceSet resourceSet) {
		resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put(URIHelperConstants.PROTOCOL, this);
	}

	public abstract JvmType findTypeByName(String name);

	public TypeResource createResource(URI uri) {
		TypeResource result = doCreateResource(uri);
		final IMirror createMirror = createMirror(uri);
		if (createMirror ==  null) 
			return null;
		result.setMirror(createMirror);
		return result;
	}

	protected TypeResource doCreateResource(URI uri) {
		return new TypeResource(uri);
	}

	public ResourceSet getResourceSet() {
		return resourceSet;
	}

	public IMirror createMirror(URI resourceURI) {
		if (resourceURI.hasFragment())
			throw new IllegalArgumentException("Cannot create mirror for uri '" + resourceURI.toString() + "'");
		String name = resourceURI.path();
		if (URIHelperConstants.PRIMITIVES.equals(name))
			return new PrimitiveMirror(primitiveTypeFactory);
		if (!name.startsWith(URIHelperConstants.OBJECTS))
			throw new IllegalArgumentException("Invalid resource uri '" + resourceURI.toString() + "'");
		name = name.substring(URIHelperConstants.OBJECTS.length());
		return createMirrorForFQN(name);
	}

	protected abstract IMirror createMirrorForFQN(String name);

}
