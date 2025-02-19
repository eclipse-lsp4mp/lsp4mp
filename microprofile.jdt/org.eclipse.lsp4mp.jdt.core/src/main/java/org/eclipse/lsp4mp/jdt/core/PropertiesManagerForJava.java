/*******************************************************************************
* Copyright (c) 2019, 2025 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.jdt.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.core.manipulation.dom.ASTResolving;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.JavaCursorContextKind;
import org.eclipse.lsp4mp.commons.JavaCursorContextResult;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDefinitionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4mp.jdt.core.java.codelens.JavaCodeLensContext;
import org.eclipse.lsp4mp.jdt.core.java.completion.JavaCompletionContext;
import org.eclipse.lsp4mp.jdt.core.java.definition.JavaDefinitionContext;
import org.eclipse.lsp4mp.jdt.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4mp.jdt.core.java.hover.JavaHoverContext;
import org.eclipse.lsp4mp.jdt.core.utils.ASTNodeUtils;
import org.eclipse.lsp4mp.jdt.core.utils.IJDTUtils;
import org.eclipse.lsp4mp.jdt.core.utils.JDTMicroProfileUtils;
import org.eclipse.lsp4mp.jdt.internal.core.java.JavaFeaturesRegistry;
import org.eclipse.lsp4mp.jdt.internal.core.java.codeaction.CodeActionHandler;
import org.eclipse.lsp4mp.jdt.internal.core.java.codelens.JavaCodeLensDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.completion.JavaCompletionDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.definition.JavaDefinitionDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.diagnostics.JavaDiagnosticsDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.hover.JavaHoverDefinition;
import org.eclipse.lsp4mp.jdt.internal.core.java.symbols.JavaWorkspaceSymbolsDefinition;

/**
 * JDT MicroProfile manager for Java files.
 *
 * @author Angelo ZERR
 *
 */
public class PropertiesManagerForJava {

	private static final PropertiesManagerForJava INSTANCE = new PropertiesManagerForJava();

	public static PropertiesManagerForJava getInstance() {
		return INSTANCE;
	}

	private final CodeActionHandler codeActionHandler;

	private PropertiesManagerForJava() {
		this.codeActionHandler = new CodeActionHandler();
	}

	/**
	 * Returns the Java file information (ex : package name) from the given file URI
	 * and null otherwise.
	 *
	 * @param params  the file information parameters.
	 * @param utils   the utilities class
	 * @param monitor the monitor
	 * @return the Java file information (ex : package name) from the given file URI
	 *         and null otherwise.
	 */
	public JavaFileInfo fileInfo(MicroProfileJavaFileInfoParams params, IJDTUtils utils, IProgressMonitor monitor) {
		String uri = params.getUri();
		final ICompilationUnit unit = utils.resolveCompilationUnit(uri);
		if (unit != null && unit.exists()) {
			JavaFileInfo fileInfo = new JavaFileInfo();
			String packageName = unit.getParent() != null ? unit.getParent().getElementName() : "";
			fileInfo.setPackageName(packageName);
			return fileInfo;
		}
		return null;
	}

	/**
	 * Returns the codeAction list according the given codeAction parameters.
	 *
	 * @param params  the codeAction parameters
	 * @param utils   the utilities class
	 * @param monitor the monitor
	 * @return the codeAction list according the given codeAction parameters.
	 * @throws JavaModelException
	 */
	public List<? extends CodeAction> codeAction(MicroProfileJavaCodeActionParams params, IJDTUtils utils,
			IProgressMonitor monitor) throws JavaModelException {
		return codeActionHandler.codeAction(params, utils, monitor);
	}

	/**
	 * Returns the codeAction list according the given codeAction parameters.
	 *
	 * @param unresolved the CodeAction to resolve
	 * @param utils      the utilities class
	 * @param monitor    the monitor
	 * @return the codeAction list according the given codeAction parameters.
	 * @throws JavaModelException
	 */
	public CodeAction resolveCodeAction(CodeAction unresolved, IJDTUtils utils, IProgressMonitor monitor)
			throws JavaModelException {
		return codeActionHandler.resolveCodeAction(unresolved, utils, monitor);
	}

	/**
	 * Returns the codelens list according the given codelens parameters.
	 *
	 * @param params  the codelens parameters
	 * @param utils   the utilities class
	 * @param monitor the monitor
	 * @return the codelens list according the given codelens parameters.
	 * @throws JavaModelException
	 */
	public List<? extends CodeLens> codeLens(MicroProfileJavaCodeLensParams params, IJDTUtils utils,
			IProgressMonitor monitor) throws JavaModelException {
		String uri = params.getUri();
		ITypeRoot typeRoot = resolveTypeRoot(uri, utils, monitor);
		if (typeRoot == null) {
			return Collections.emptyList();
		}
		List<CodeLens> lenses = new ArrayList<>();
		collectCodeLens(uri, typeRoot, utils, params, lenses, monitor);
		if (monitor.isCanceled()) {
			return Collections.emptyList();
		}
		return lenses;
	}

	private void collectCodeLens(String uri, ITypeRoot typeRoot, IJDTUtils utils, MicroProfileJavaCodeLensParams params,
			List<CodeLens> lenses, IProgressMonitor monitor) {
		// Collect all adapted codeLens participant
		JavaCodeLensContext context = new JavaCodeLensContext(uri, typeRoot, utils, params);
		List<JavaCodeLensDefinition> definitions = JavaFeaturesRegistry.getInstance().getJavaCodeLensDefinitions()
				.stream().filter(definition -> definition.isAdaptedForCodeLens(context, monitor))
				.collect(Collectors.toList());
		if (definitions.isEmpty()) {
			return;
		}

		// Begin, collect, end participants
		definitions.forEach(definition -> definition.beginCodeLens(context, monitor));
		definitions.forEach(definition -> {
			List<CodeLens> collectedLenses = definition.collectCodeLens(context, monitor);
			if (collectedLenses != null && !collectedLenses.isEmpty()) {
				lenses.addAll(collectedLenses);
			}
		});
		definitions.forEach(definition -> definition.endCodeLens(context, monitor));
	}

	/**
	 * Returns the CompletionItems given the completion item params
	 *
	 * @param params  the completion item params
	 * @param utils   the IJDTUtils
	 * @param monitor the progress monitors
	 * @return the CompletionItems for the given the completion item params
	 * @throws JavaModelException
	 */
	public CompletionList completion(MicroProfileJavaCompletionParams params, IJDTUtils utils, IProgressMonitor monitor)
			throws JavaModelException {
		String uri = params.getUri();
		ITypeRoot typeRoot = resolveTypeRoot(uri, utils, monitor);
		if (typeRoot == null) {
			return null;
		}

		Position completionPosition = params.getPosition();
		int completionOffset = utils.toOffset(typeRoot.getBuffer(), completionPosition.getLine(),
				completionPosition.getCharacter());

		List<CompletionItem> completionItems = new ArrayList<>();
		JavaCompletionContext completionContext = new JavaCompletionContext(uri, typeRoot, utils, completionOffset);

		List<JavaCompletionDefinition> completions = JavaFeaturesRegistry.getInstance().getJavaCompletionDefinitions()
				.stream().filter(completion -> completion.isAdaptedForCompletion(completionContext, monitor))
				.collect(Collectors.toList());

		if (completions.isEmpty()) {
			return null;
		}

		completions.forEach(completion -> {
			List<? extends CompletionItem> collectedCompletionItems = completion
					.collectCompletionItems(completionContext, monitor);
			if (collectedCompletionItems != null) {
				completionItems.addAll(collectedCompletionItems);
			}
		});

		if (monitor.isCanceled()) {
			return null;
		}
		CompletionList completionList = new CompletionList();
		completionList.setItems(completionItems);
		return completionList;
	}

	/**
	 * Returns the definition list according the given definition parameters.
	 *
	 * @param params  the definition parameters
	 * @param utils   the utilities class
	 * @param monitor the monitor
	 * @return the definition list according the given definition parameters.
	 * @throws JavaModelException
	 */
	public List<MicroProfileDefinition> definition(MicroProfileJavaDefinitionParams params, IJDTUtils utils,
			IProgressMonitor monitor) throws JavaModelException {
		String uri = params.getUri();
		ITypeRoot typeRoot = resolveTypeRoot(uri, utils, monitor);
		if (typeRoot == null) {
			return Collections.emptyList();
		}

		Position hyperlinkedPosition = params.getPosition();
		int definitionOffset = utils.toOffset(typeRoot.getBuffer(), hyperlinkedPosition.getLine(),
				hyperlinkedPosition.getCharacter());
		IJavaElement hyperlinkedElement = getHoveredElement(typeRoot, definitionOffset);

		List<MicroProfileDefinition> locations = new ArrayList<>();
		collectDefinition(uri, typeRoot, hyperlinkedElement, utils, hyperlinkedPosition, locations, monitor);
		if (monitor.isCanceled()) {
			return Collections.emptyList();
		}
		return locations;
	}

	private void collectDefinition(String uri, ITypeRoot typeRoot, IJavaElement hyperlinkedElement, IJDTUtils utils,
			Position hyperlinkedPosition, List<MicroProfileDefinition> locations, IProgressMonitor monitor) {
		// Collect all adapted definition participant
		JavaDefinitionContext context = new JavaDefinitionContext(uri, typeRoot, utils, hyperlinkedElement,
				hyperlinkedPosition);
		List<JavaDefinitionDefinition> definitions = JavaFeaturesRegistry.getInstance().getJavaDefinitionDefinitions()
				.stream().filter(definition -> definition.isAdaptedForDefinition(context, monitor))
				.collect(Collectors.toList());
		if (definitions.isEmpty()) {
			return;
		}

		// Begin, collect, end participants
		definitions.forEach(definition -> definition.beginDefinition(context, monitor));
		definitions.forEach(definition -> {
			List<MicroProfileDefinition> collectedDefinitions = definition.collectDefinitions(context, monitor);
			if (collectedDefinitions != null && !collectedDefinitions.isEmpty()) {
				locations.addAll(collectedDefinitions);
			}
		});
		definitions.forEach(definition -> definition.endDefinition(context, monitor));
	}

	/**
	 * Returns diagnostics for the given uris list.
	 *
	 * @param params the diagnostics parameters
	 * @param utils  the utilities class
	 * @return diagnostics for the given uris list.
	 * @throws JavaModelException
	 */
	public List<PublishDiagnosticsParams> diagnostics(MicroProfileJavaDiagnosticsParams params, IJDTUtils utils,
			IProgressMonitor monitor) throws JavaModelException {
		List<String> uris = params.getUris();
		if (uris == null) {
			return Collections.emptyList();
		}
		DocumentFormat documentFormat = params.getDocumentFormat();
		List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<PublishDiagnosticsParams>();
		for (String uri : uris) {
			List<Diagnostic> diagnostics = new ArrayList<>();
			PublishDiagnosticsParams publishDiagnostic = new PublishDiagnosticsParams(uri, diagnostics);
			publishDiagnostics.add(publishDiagnostic);
			collectDiagnostics(uri, utils, documentFormat, params.getSettings(), diagnostics, monitor);
		}
		if (monitor.isCanceled()) {
			return Collections.emptyList();
		}
		return publishDiagnostics;
	}

	private void collectDiagnostics(String uri, IJDTUtils utils, DocumentFormat documentFormat,
			MicroProfileJavaDiagnosticsSettings settings, List<Diagnostic> diagnostics, IProgressMonitor monitor) {
		ITypeRoot typeRoot = resolveTypeRoot(uri, utils, monitor);
		if (typeRoot == null) {
			return;
		}

		// Collect all adapted diagnostics participant
		JavaDiagnosticsContext context = new JavaDiagnosticsContext(uri, typeRoot, utils, documentFormat, settings);
		List<JavaDiagnosticsDefinition> definitions = JavaFeaturesRegistry.getInstance().getJavaDiagnosticsDefinitions()
				.stream().filter(definition -> definition.isAdaptedForDiagnostics(context, monitor))
				.collect(Collectors.toList());
		if (definitions.isEmpty()) {
			return;
		}

		// Begin, collect, end participants
		definitions.forEach(definition -> definition.beginDiagnostics(context, monitor));
		definitions.forEach(definition -> {
			List<Diagnostic> collectedDiagnostics = definition.collectDiagnostics(context, monitor);
			if (collectedDiagnostics != null && !collectedDiagnostics.isEmpty()) {
				diagnostics.addAll(collectedDiagnostics);
			}
		});
		definitions.forEach(definition -> definition.endDiagnostics(context, monitor));
	}

	/**
	 * Returns the hover information according to the given <code>params</code>
	 *
	 * @param params  the hover parameters
	 * @param utils   the utilities class
	 * @param monitor the monitor
	 * @return the hover information according to the given <code>params</code>
	 * @throws JavaModelException
	 */
	public Hover hover(MicroProfileJavaHoverParams params, IJDTUtils utils, IProgressMonitor monitor)
			throws JavaModelException {
		String uri = params.getUri();
		ITypeRoot typeRoot = resolveTypeRoot(uri, utils, monitor);
		if (typeRoot == null) {
			return null;
		}

		Position hoverPosition = params.getPosition();
		int hoveredOffset = utils.toOffset(typeRoot.getBuffer(), hoverPosition.getLine(), hoverPosition.getCharacter());
		IJavaElement hoverElement = getHoveredElement(typeRoot, hoveredOffset);

		DocumentFormat documentFormat = params.getDocumentFormat();
		boolean surroundEqualsWithSpaces = params.isSurroundEqualsWithSpaces();
		List<Hover> hovers = new ArrayList<>();
		collectHover(uri, typeRoot, hoverElement, utils, hoverPosition, documentFormat, surroundEqualsWithSpaces,
				hovers, monitor);
		if (hovers.isEmpty()) {
			return null;
		}
		if (monitor.isCanceled()) {
			return null;
		}
		// TODO : aggregate the hover
		return hovers.get(0);
	}

	/**
	 * Returns the cursor context for the given file and cursor position.
	 *
	 * @param params  the completion params that provide the file and cursor
	 *                position to get the context for
	 * @param utils   the jdt utils
	 * @param monitor the progress monitor
	 * @return the cursor context for the given file and cursor position
	 * @throws JavaModelException when the buffer for the file cannot be accessed or
	 *                            the Java model cannot be accessed
	 */
	public JavaCursorContextResult javaCursorContext(MicroProfileJavaCompletionParams params, IJDTUtils utils,
			IProgressMonitor monitor) throws JavaModelException {
		String uri = params.getUri();
		ITypeRoot typeRoot = resolveTypeRoot(uri, utils, monitor);

		if (typeRoot == null) {
			return new JavaCursorContextResult(JavaCursorContextKind.IN_EMPTY_FILE, "");
		}
		CompilationUnit ast = ASTResolving.createQuickFixAST((ICompilationUnit) typeRoot, monitor);

		JavaCursorContextKind kind = getJavaCursorContextKind(params, typeRoot, ast, utils, monitor);
		String prefix = getJavaCursorPrefix(params, typeRoot, ast, utils, monitor);

		return new JavaCursorContextResult(kind, prefix);
	}

	private static JavaCursorContextKind getJavaCursorContextKind(MicroProfileJavaCompletionParams params,
			ITypeRoot typeRoot, CompilationUnit ast, IJDTUtils utils, IProgressMonitor monitor)
			throws JavaModelException {

		if (typeRoot.findPrimaryType() == null) {
			return JavaCursorContextKind.IN_EMPTY_FILE;
		}

		Position completionPosition = params.getPosition();
		int completionOffset = utils.toOffset(typeRoot.getBuffer(), completionPosition.getLine(),
				completionPosition.getCharacter());

		NodeFinder nodeFinder = new NodeFinder(ast, completionOffset, 0);
		ASTNode node = nodeFinder.getCoveringNode();
		ASTNode oldNode = node;
		while (node != null && (!(node instanceof AbstractTypeDeclaration)
				|| offsetOfFirstNonAnnotationModifier((BodyDeclaration) node) >= completionOffset)) {
			if (node.getParent() != null) {
				switch (node.getParent().getNodeType()) {
				case ASTNode.METHOD_DECLARATION:
				case ASTNode.FIELD_DECLARATION:
				case ASTNode.ENUM_CONSTANT_DECLARATION:
				case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
					if (!ASTNodeUtils.isAnnotation(node) && node.getStartPosition() < completionOffset) {
						return JavaCursorContextKind.NONE;
					}
					break;
				}
			}
			oldNode = node;
			node = node.getParent();
		}

		if (node == null) {
			// we are likely before or after the type root class declaration
			FindWhatsBeingAnnotatedASTVisitor visitor = new FindWhatsBeingAnnotatedASTVisitor(completionOffset, false);
			oldNode.accept(visitor);
			switch (visitor.getAnnotatedNodeType()) {
			case ASTNode.TYPE_DECLARATION:
			case ASTNode.ANNOTATION_TYPE_DECLARATION:
			case ASTNode.ENUM_DECLARATION:
			case ASTNode.RECORD_DECLARATION: {
				if (visitor.isInAnnotations()) {
					return JavaCursorContextKind.IN_CLASS_ANNOTATIONS;
				}
				return JavaCursorContextKind.BEFORE_TOP_LEVEL_CLASS;
			}
			default:
				return JavaCursorContextKind.NONE;
			}
		}

		AbstractTypeDeclaration typeDeclaration = (AbstractTypeDeclaration) node;
		FindWhatsBeingAnnotatedASTVisitor visitor = new FindWhatsBeingAnnotatedASTVisitor(completionOffset);
		typeDeclaration.accept(visitor);
		switch (visitor.getAnnotatedNodeType()) {
		case ASTNode.TYPE_DECLARATION:
		case ASTNode.ANNOTATION_TYPE_DECLARATION:
		case ASTNode.ENUM_DECLARATION:
		case ASTNode.RECORD_DECLARATION:
			return visitor.isInAnnotations() ? JavaCursorContextKind.IN_CLASS_ANNOTATIONS
					: JavaCursorContextKind.BEFORE_INNER_CLASS;
		case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION:
		case ASTNode.METHOD_DECLARATION:
			return visitor.isInAnnotations() ? JavaCursorContextKind.IN_METHOD_ANNOTATIONS
					: JavaCursorContextKind.BEFORE_METHOD;
		case ASTNode.FIELD_DECLARATION:
		case ASTNode.ENUM_CONSTANT_DECLARATION:
			return visitor.isInAnnotations() ? JavaCursorContextKind.IN_FIELD_ANNOTATIONS
					: JavaCursorContextKind.BEFORE_FIELD;
		default:
			return JavaCursorContextKind.IN_CLASS;
		}
	}

	private static @NonNull String getJavaCursorPrefix(MicroProfileJavaCompletionParams params, ITypeRoot typeRoot,
			CompilationUnit ast, IJDTUtils utils, IProgressMonitor monitor) throws JavaModelException {
		Position completionPosition = params.getPosition();
		int completionOffset = utils.toOffset(typeRoot.getBuffer(), completionPosition.getLine(),
				completionPosition.getCharacter());

		String fileContents = null;
		try {
			IBuffer buffer = typeRoot.getBuffer();
			if (buffer == null) {
				return null;
			}
			fileContents = buffer.getContents();
		} catch (JavaModelException e) {
			return "";
		}
		if (fileContents == null) {
			return "";
		}
		int i;
		for (i = completionOffset; i > 0 && !Character.isWhitespace(fileContents.charAt(i - 1)); i--) {
		}
		return fileContents.substring(i, completionOffset);
	}

	/**
	 * Returns the hovered element from the given <code>typeRoot</code> and
	 * <code>offset</code>. Returns null otherwise
	 *
	 * @param typeRoot the typeRoot
	 * @param offset   the offset representing the hover location
	 * @return the hovered element from the given <code>typeRoot</code> and
	 *         <code>offset</code>. Returns null otherwise
	 * @throws JavaModelException
	 */
	private IJavaElement getHoveredElement(ITypeRoot typeRoot, int offset) throws JavaModelException {
		IJavaElement hoverElement = typeRoot.getElementAt(offset);
		if (hoverElement == null) {
			return null;
		}
		if (hoverElement.getElementType() == IJavaElement.METHOD) {
			hoverElement = getHoveredMethodParameter((IMethod) hoverElement, offset);
		}
		return hoverElement;
	}

	/**
	 * Returns the parameter element from the given <code>method</code> that
	 * contains the given <code>offset</code>.
	 *
	 * Returns the given <code>method</code> if the correct parameter element cannot
	 * be found
	 *
	 * @param method the method
	 * @param offset the offset
	 * @return the parameter element from the given <code>method</code> that
	 *         contains the given <code>offset</code>
	 * @throws JavaModelException
	 */
	private IJavaElement getHoveredMethodParameter(IMethod method, int offset) throws JavaModelException {
		ILocalVariable[] parameters = method.getParameters();
		for (int i = 0; i < parameters.length; i++) {
			ISourceRange range = parameters[i].getSourceRange();
			int start = range.getOffset();
			int end = start + range.getLength();
			if (start <= offset && offset <= end) {
				return parameters[i];
			}
		}
		return method;
	}

	private void collectHover(String uri, ITypeRoot typeRoot, IJavaElement hoverElement, IJDTUtils utils,
			Position hoverPosition, DocumentFormat documentFormat, boolean surroundEqualsWithSpaces, List<Hover> hovers,
			IProgressMonitor monitor) {
		// Collect all adapted hover participant
		JavaHoverContext context = new JavaHoverContext(uri, typeRoot, utils, hoverElement, hoverPosition,
				documentFormat, surroundEqualsWithSpaces);
		List<JavaHoverDefinition> definitions = JavaFeaturesRegistry.getInstance().getJavaHoverDefinitions().stream()
				.filter(definition -> definition.isAdaptedForHover(context, monitor)).collect(Collectors.toList());
		if (definitions.isEmpty()) {
			return;
		}

		// Begin, collect, end participants
		definitions.forEach(definition -> definition.beginHover(context, monitor));
		definitions.forEach(definition -> {
			Hover hover = definition.collectHover(context, monitor);
			if (hover != null) {
				hovers.add(hover);
			}
		});
		definitions.forEach(definition -> definition.endHover(context, monitor));
	}

	/**
	 * Returns the workspace symbols for the given java project.
	 *
	 * @param projectUri the uri of the java project
	 * @param utils      the JDT utils
	 * @param monitor    the progress monitor
	 * @return the workspace symbols for the given java project
	 */
	public List<SymbolInformation> workspaceSymbols(String projectUri, IJDTUtils utils, IProgressMonitor monitor) {
		List<SymbolInformation> symbols = new ArrayList<>();
		Optional<IJavaProject> projectOpt = Stream.of(JDTMicroProfileUtils.getJavaProjects()) //
				.filter(project -> projectUri.equals(JDTMicroProfileUtils.getProjectURI(project))) //
				.findFirst();
		if (projectOpt.isEmpty()) {
			return symbols;
		}
		collectWorkspaceSymbols(projectOpt.get(), utils, symbols, monitor);
		return symbols;
	}

	private void collectWorkspaceSymbols(IJavaProject project, IJDTUtils utils, List<SymbolInformation> symbols,
			IProgressMonitor monitor) {
		if (monitor.isCanceled()) {
			return;
		}
		List<JavaWorkspaceSymbolsDefinition> definitions = JavaFeaturesRegistry.getInstance()
				.getJavaWorkspaceSymbolsDefinitions();
		if (definitions.isEmpty()) {
			return;
		}
		definitions.forEach(definition -> definition.collectSymbols(project, utils, symbols, monitor));
	}

	/**
	 * Given the uri returns a {@link ITypeRoot}. May return null if it can not
	 * associate the uri with a Java file or class file.
	 *
	 * @param uri
	 * @param utils   JDT LS utilities
	 * @param monitor the progress monitor
	 * @return compilation unit
	 */
	private static ITypeRoot resolveTypeRoot(String uri, IJDTUtils utils, IProgressMonitor monitor) {
		utils.waitForLifecycleJobs(monitor);
		final ICompilationUnit unit = utils.resolveCompilationUnit(uri);
		IClassFile classFile = null;
		if (unit == null) {
			classFile = utils.resolveClassFile(uri);
			if (classFile == null) {
				return null;
			}
		} else {
			if (!unit.getResource().exists() || monitor.isCanceled()) {
				return null;
			}
		}
		return unit != null ? unit : classFile;
	}

	/**
	 * Searches through the AST to figure out the following:
	 * <ul>
	 * <li>If an annotation were to be placed at the completionOffset, what type of
	 * node would it be annotating?</li>
	 * <li>Is the completionOffset within the list of annotations before a
	 * member?</li>
	 * </ul>
	 */
	private static class FindWhatsBeingAnnotatedASTVisitor extends ASTVisitor {

		private int completionOffset;
		private int closest = Integer.MAX_VALUE;
		private int annotatedNode = 0;
		private boolean visitedParentType;
		private boolean inAnnotations = false;

		public FindWhatsBeingAnnotatedASTVisitor(int completionOffset, boolean startingInParent) {
			this.completionOffset = completionOffset;
			this.visitedParentType = !startingInParent;
		}

		public FindWhatsBeingAnnotatedASTVisitor(int completionOffset) {
			this(completionOffset, true);
		}

		@Override
		public boolean visit(MethodDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(FieldDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(EnumConstantDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(AnnotationTypeMemberDeclaration node) {
			return visitNode(node);
		}

		@Override
		public boolean visit(TypeDeclaration node) {
			return visitAbstractType(node);
		}

		@Override
		public boolean visit(EnumDeclaration node) {
			return visitAbstractType(node);
		}

		@Override
		public boolean visit(AnnotationTypeDeclaration node) {
			return visitAbstractType(node);
		}

		@Override
		public boolean visit(RecordDeclaration node) {
			return visitAbstractType(node);
		}

		private boolean visitAbstractType(AbstractTypeDeclaration node) {
			// we need to visit the children of the first type declaration,
			// since the visitor start visiting from the supplied node.
			if (!visitedParentType) {
				visitedParentType = true;
				return true;
			}
			return visitNode(node);
		}

		private boolean visitNode(BodyDeclaration node) {
			// ignore generated nodes
			if (isGenerated(node)) {
				return false;
			}
			// consider the start of the declaration to be after the annotations
			int start = node.modifiers().isEmpty() ? node.getStartPosition() : offsetOfFirstNonAnnotationModifier(node);
			if (start < closest && completionOffset <= start) {
				closest = node.getStartPosition();
				annotatedNode = node.getNodeType();
				inAnnotations = node.getStartPosition() < completionOffset && completionOffset <= start;
			}
			// We don't want to enter nested classes
			return false;
		}

		/**
		 * Returns the type of the node that an annotation placed at the completion
		 * offset would be annotating.
		 *
		 * @see org.eclipse.jdt.core.dom.ASTNode#getNodeType()
		 * @return the type of the node that an annotation placed at the completion
		 *         offset would be annotating
		 */
		public int getAnnotatedNodeType() {
			return annotatedNode;
		}

		/**
		 * Returns true if the completion offset is within the list of annotations
		 * preceding a body declaration (field, method, class declaration) or false
		 * otherwise.
		 *
		 * @return true if the completion offset is within the list of annotations
		 *         preceding a body declaration (field, method, class declaration) or
		 *         false otherwise
		 */
		public boolean isInAnnotations() {
			return inAnnotations;
		}

	}

	private static int offsetOfFirstNonAnnotationModifier(BodyDeclaration node) {
		List modifiers = node.modifiers();
		for (int i = 0; i < modifiers.size(); i++) {
			ASTNode modifier = (ASTNode) modifiers.get(i);
			if (!ASTNodeUtils.isAnnotation(modifier)) {
				return modifier.getStartPosition();
			}
		}
		if (node instanceof MethodDeclaration method) {
			if (method.getReturnType2() != null) {
				return method.getReturnType2().getStartPosition();
			}
			// package protected constructor
			return method.getName().getStartPosition();
		} else if (node instanceof FieldDeclaration field) {
			return field.getType().getStartPosition();
		} else {
			var type = (AbstractTypeDeclaration) node;
			int nameOffset = type.getName().getStartPosition();
			int keywordLength = (switch (type.getNodeType()) {
			case ASTNode.TYPE_DECLARATION -> ((TypeDeclaration) type).isInterface() ? "interface" : "class";
			case ASTNode.ENUM_DECLARATION -> "enum";
			case ASTNode.ANNOTATION_TYPE_DECLARATION -> "@interface";
			case ASTNode.RECORD_DECLARATION -> "record";
			default -> "";
			}).length();

			// HACK: this assumes the code contains one space between the keyword and the
			// type name, which isn't always the case
			return nameOffset - (keywordLength + 1);
		}
	}

	/**
	 * Returns true if the given node was generated by Project Lombok and false
	 * otherwise.
	 * 
	 * @param node the node to check if it's generated
	 * @return true if the given node was generated by Project Lombok and false
	 *         otherwise
	 */
	private static boolean isGenerated(ASTNode node) {
		try {
			return ((Boolean) node.getClass().getField("$isGenerated").get(node)).booleanValue();
		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			return false;
		}
	}

}
