/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.imports;

import static com.google.common.collect.Iterables.*;
import static com.google.common.collect.Lists.*;
import static com.google.common.collect.Maps.*;
import static com.google.common.collect.Sets.*;
import static java.util.Collections.*;
import static org.eclipse.xtext.util.Strings.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.formatting.IWhitespaceInformationProvider;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.IAcceptor;
import org.eclipse.xtext.util.ITextRegion;
import org.eclipse.xtext.util.ReplaceRegion;
import org.eclipse.xtext.util.TextRegion;
import org.eclipse.xtext.xtype.XImportDeclaration;
import org.eclipse.xtext.xtype.XImportSection;
import org.eclipse.xtext.xtype.XtypeFactory;

import com.google.common.base.Predicate;
import com.google.inject.Inject;

/**
 * Model of an import section that can be changed. 
 * 
 * @author Jan Koehnlein - Initial contribution and API
 */
public class RewritableImportSection {

	public static class Factory {
		@Inject
		private IImportsConfiguration importsConfiguration;

		@Inject
		private IWhitespaceInformationProvider whitespaceInformationProvider;

		@Inject
		private ImportSectionRegionUtil regionUtil;
		
		public RewritableImportSection parse(XtextResource resource) {
			RewritableImportSection rewritableImportSection = new RewritableImportSection(
					resource,
					importsConfiguration,
					importsConfiguration.getImportSection(resource), 
					whitespaceInformationProvider.getLineSeparatorInformation(resource.getURI()).getLineSeparator(), 
					regionUtil);
			return rewritableImportSection;
		}

		public RewritableImportSection createNewEmpty(XtextResource resource) {
			RewritableImportSection rewritableImportSection = new RewritableImportSection(
					resource,
					importsConfiguration,
					null,
					whitespaceInformationProvider.getLineSeparatorInformation(resource.getURI()).getLineSeparator(), 
					regionUtil);
			rewritableImportSection.setSort(true);
			return rewritableImportSection;
		}
	}

	private List<XImportDeclaration> originalImportDeclarations = newArrayList();

	private List<XImportDeclaration> addedImportDeclarations = newArrayList();

	private Set<XImportDeclaration> removedImportDeclarations = newLinkedHashSet();

	private Map<String, JvmDeclaredType> plainImports = newHashMap();

	private Set<JvmDeclaredType> staticImports = newHashSet();

	private Set<JvmDeclaredType> staticExtensionImports = newHashSet();

	private String lineSeparator;

	private XtextResource resource;
	
	private IImportsConfiguration importsConfiguration;

	private ImportSectionRegionUtil regionUtil;
	
	private boolean isSort;

	private Set<JvmDeclaredType> locallyDeclaredTypes;

	private Set<String> implicitlyImportedPackages;

	public RewritableImportSection(XtextResource resource, IImportsConfiguration importsConfiguration, 
			XImportSection originalImportSection, String lineSeparator, ImportSectionRegionUtil regionUtil) {
		this.resource = resource;
		this.importsConfiguration = importsConfiguration;
		this.lineSeparator = lineSeparator;
		this.regionUtil = regionUtil;
		this.locallyDeclaredTypes = newHashSet(importsConfiguration.getLocallyDefinedTypes(resource).values());
		this.implicitlyImportedPackages = importsConfiguration.getImplicitlyImportedPackages(resource);
		if (originalImportSection != null) {
			for (XImportDeclaration originalImportDeclaration : originalImportSection.getImportDeclarations()) {
				this.originalImportDeclarations.add(originalImportDeclaration);
				if (originalImportDeclaration.isStatic()) {
					if (originalImportDeclaration.isExtension())
						staticExtensionImports.add(originalImportDeclaration.getImportedType());
					else
						staticImports.add(originalImportDeclaration.getImportedType());
				} else if(originalImportDeclaration.getImportedType() != null) {
					plainImports.put(originalImportDeclaration.getImportedType().getSimpleName(),
							originalImportDeclaration.getImportedType());
				}
			}
		}
	}

	public void setSort(boolean isSort) {
		this.isSort = isSort;
	}
	
	public boolean isSort() {
		return isSort;
	}
	
	public boolean addImport(JvmDeclaredType type) {
		if (plainImports.containsKey(type.getSimpleName()) || !needsImport(type))
			return false;
		plainImports.put(type.getSimpleName(), type);
		XImportDeclaration importDeclaration = XtypeFactory.eINSTANCE.createXImportDeclaration();
		importDeclaration.setImportedType(type);
		addedImportDeclarations.add(importDeclaration);
		return true;
	}
	
	protected boolean needsImport(JvmDeclaredType type)  {
		return !(implicitlyImportedPackages.contains(type.getPackageName())
			|| locallyDeclaredTypes.contains(type));
	}
	
	public boolean removeImport(JvmDeclaredType type) {
		XImportDeclaration importDeclaration = findOriginalImport(type, originalImportDeclarations, false, false);
		if(importDeclaration != null) 
			removedImportDeclarations.add(importDeclaration);
		else {
			importDeclaration = findOriginalImport(type, addedImportDeclarations, false, false);
			if(importDeclaration != null)
				addedImportDeclarations.remove(importDeclaration);
		}
		if(importDeclaration != null) {
			for(Map.Entry<String, JvmDeclaredType> entry: plainImports.entrySet()) {
				if(entry.getValue() == type) {
					plainImports.remove(entry.getKey());
					return true;
				}
			}
		}
		return false;
	}
	
	protected XImportDeclaration findOriginalImport(JvmDeclaredType type, Collection<XImportDeclaration> list,
			boolean isStatic, boolean isExtension) {
		for(XImportDeclaration importDeclaration: list) {
			if(!(isStatic ^ importDeclaration.isStatic())
					&& !(isExtension ^ importDeclaration.isExtension())
					&& importDeclaration.getImportedType()==type) 
				return importDeclaration;
		}
		return null;
	}

	public JvmDeclaredType getImportedType(String simpleName) {
		return plainImports.get(simpleName);
	}

	public boolean addStaticImport(JvmDeclaredType type) {
		if (staticImports.contains(type))
			return false;
		staticImports.add(type);
		XImportDeclaration importDeclaration = XtypeFactory.eINSTANCE.createXImportDeclaration();
		importDeclaration.setImportedType(type);
		importDeclaration.setStatic(true);
		addedImportDeclarations.add(importDeclaration);
		return true;
	}

	public boolean removeStaticImport(JvmDeclaredType type) {
		XImportDeclaration importDeclaration = findOriginalImport(type, originalImportDeclarations, true, false);
		if(importDeclaration != null) 
			removedImportDeclarations.add(importDeclaration);
		else {
			importDeclaration = findOriginalImport(type, addedImportDeclarations, true, false);
			if(importDeclaration != null)
				addedImportDeclarations.remove(importDeclaration);
		}
		staticImports.remove(type);
		return importDeclaration != null;
	}
	

	public boolean addStaticExtensionImport(JvmDeclaredType type) {
		if (staticExtensionImports.contains(type))
			return false;
		staticExtensionImports.add(type);
		XImportDeclaration importDeclaration = XtypeFactory.eINSTANCE.createXImportDeclaration();
		importDeclaration.setImportedType(type);
		importDeclaration.setStatic(true);
		importDeclaration.setExtension(true);
		addedImportDeclarations.add(importDeclaration);
		return true;
	}

	public boolean removeStaticExtensionImport(JvmDeclaredType type) {
		XImportDeclaration importDeclaration = findOriginalImport(type, originalImportDeclarations, true, true);
		if(importDeclaration != null) 
			removedImportDeclarations.add(importDeclaration);
		else {
			importDeclaration = findOriginalImport(type, addedImportDeclarations, true, true);
			if(importDeclaration != null)
				addedImportDeclarations.remove(importDeclaration);
		}
		staticExtensionImports.remove(type);
		return importDeclaration != null;
	}

	public List<ReplaceRegion> rewrite() {
		final List<ReplaceRegion> replaceRegions = newArrayList();
		if(isSort) {
			List<XImportDeclaration> allImportDeclarations = newArrayList();
			allImportDeclarations.addAll(originalImportDeclarations);
			allImportDeclarations.addAll(addedImportDeclarations);
			allImportDeclarations.removeAll(removedImportDeclarations);
			String newImportSection = serializeImports(allImportDeclarations);
			ITextRegion region = regionUtil.computeRegion(resource);
			region = regionUtil.addLeadingWhitespace(region, resource);
			region = regionUtil.addTrailingWhitespace(region, resource);
			return singletonList(new ReplaceRegion(region, newImportSection));
		} else {
			for(XImportDeclaration removedImportDeclaration: removedImportDeclarations) {
				ICompositeNode node = NodeModelUtils.findActualNodeFor(removedImportDeclaration);
				if(node != null) {
					ITextRegion textRegion = new TextRegion(node.getOffset(), node.getLength());
					if(removedImportDeclaration!=originalImportDeclarations.get(originalImportDeclarations.size()-1) 
							|| addedImportDeclarations.isEmpty()) {
						textRegion = regionUtil.addTrailingSingleWhitespace(textRegion, lineSeparator, resource);
					}
					replaceRegions.add(new ReplaceRegion(textRegion, ""));
				}
			}
			addSectionToAppend(new IAcceptor<ReplaceRegion>() {
				public void accept(ReplaceRegion t) {
					replaceRegions.add(t);
				}
			});
		}
		return replaceRegions;
	}

	protected void addSectionToAppend(IAcceptor<ReplaceRegion> acceptor) {
		StringBuilder importDeclarationsToAppend = getImportDeclarationsToAppend();
		if(importDeclarationsToAppend.length() ==0) 
			return;
		ITextRegion region = regionUtil.computeRegion(resource);
		region = regionUtil.addLeadingWhitespace(region, resource);
		int insertOffset = region.getOffset() + region.getLength();
		if(insertOffset != 0) {
			importDeclarationsToAppend.insert(0, lineSeparator);
			if(originalImportDeclarations.isEmpty()) 
				importDeclarationsToAppend.insert(0, lineSeparator);
		}
		importDeclarationsToAppend.append(lineSeparator);
		int insertLength = -region.getLength();
		insertLength += regionUtil.addTrailingWhitespace(region, resource).getLength();
		ReplaceRegion appendDeclarations = new ReplaceRegion(new TextRegion(insertOffset, insertLength), importDeclarationsToAppend.toString());
		acceptor.accept(appendDeclarations);
	}

	protected StringBuilder getImportDeclarationsToAppend() {
		StringBuilder builder = new StringBuilder();
		for (XImportDeclaration newImportDeclaration : addedImportDeclarations) {
			appendImport(builder, newImportDeclaration);
		}
		return builder;
	}

	protected void appendImport(StringBuilder builder, XImportDeclaration newImportDeclaration) {
		builder.append("import ");
		if (newImportDeclaration.isStatic()) {
			builder.append("static ");
			if (newImportDeclaration.isExtension()) {
				builder.append("extension ");
			}
		}
		builder.append(newImportDeclaration.getImportedTypeName());
		if (newImportDeclaration.isStatic())
			builder.append(".*");
		builder.append(lineSeparator);
	}
	
	protected String serializeImports(List<XImportDeclaration> allDeclarations) {
		StringBuilder builder = new StringBuilder();
		if(!isEmpty(importsConfiguration.getCommonPackageName(resource)))
			builder.append(lineSeparator).append(lineSeparator);
		boolean needNewline = appendSubsection(builder, filter(allDeclarations, new Predicate<XImportDeclaration>() {
			public boolean apply(XImportDeclaration input) {
				return !input.isStatic();
			}
		}), false);
		needNewline = appendSubsection(builder, filter(allDeclarations, new Predicate<XImportDeclaration>() {
			public boolean apply(XImportDeclaration input) {
				return input.isStatic() && !input.isExtension();
			}
		}), needNewline);
		appendSubsection(builder, filter(allDeclarations, new Predicate<XImportDeclaration>() {
			public boolean apply(XImportDeclaration input) {
				return input.isStatic() && input.isExtension();
			}
		}), needNewline);
		if(!isEmpty(allDeclarations)) 
			builder.append(lineSeparator);
		return builder.toString();
	}

	protected boolean appendSubsection(StringBuilder builder, Iterable<XImportDeclaration> subSection, boolean needsNewline) {
		if (!isEmpty(subSection)) {
			if(needsNewline)
				builder.append(lineSeparator);
			for (XImportDeclaration declaration: isSort() ? sort(subSection) : subSection) {
				appendImport(builder, declaration);
			}
			return true;
		}
		return needsNewline;
	}
	
	protected List<XImportDeclaration> sort(Iterable<XImportDeclaration> declarations) {
		List<XImportDeclaration> sortMe = newArrayList(declarations);
		Collections.sort(sortMe, new Comparator<XImportDeclaration>() {
			public int compare(XImportDeclaration o1, XImportDeclaration o2) {
				// TODO handle NPEs
				return o1.getImportedTypeName().compareTo(o2.getImportedTypeName());
			}
		});
		return sortMe;
	}
}
