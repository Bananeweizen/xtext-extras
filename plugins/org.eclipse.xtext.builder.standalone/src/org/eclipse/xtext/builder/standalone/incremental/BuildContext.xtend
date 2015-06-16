/*******************************************************************************
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.builder.standalone.incremental

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.resource.IResourceServiceProvider
import org.eclipse.xtext.resource.XtextResourceSet
import org.eclipse.xtext.resource.clustering.IResourceClusteringPolicy

/**
 * @author Jan Koehnlein - Initial contribution and API
 * @since 2.9
 */
@FinalFieldsConstructor
class BuildContext {
	val IResourceServiceProvider.Registry resourceServiceProviderRegistry
	@Accessors val XtextResourceSet resourceSet
	@Accessors val IResourceClusteringPolicy clusteringPolicy
	
	ClusteringStorageAwareResourceLoader loader
	
	def <T> executeClustered(Iterable<URI> uri, (Resource)=>T operation) {
		if(loader == null) 
			loader = new ClusteringStorageAwareResourceLoader(this)
		loader.executeClustered(uri.filter[resourceServiceProvider!=null], operation)
	}
	
	def getResourceServiceProvider(URI uri) {
		val resourceServiceProvider = resourceServiceProviderRegistry.getResourceServiceProvider(uri)
		return resourceServiceProvider
	}
	
}