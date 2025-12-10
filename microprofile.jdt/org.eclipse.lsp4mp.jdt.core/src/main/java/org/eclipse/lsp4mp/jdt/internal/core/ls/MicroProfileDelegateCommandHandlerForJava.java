/*******************************************************************************
* Copyright (c) 2019-2020 Red Hat Inc. and others.
* All rights reserved. This program and the accompanying materials
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v20.html
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.jdt.internal.core.ls;

import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getBoolean;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getCodeActionContext;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getFirst;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getInt;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getObject;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getPosition;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getRange;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getString;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getStringList;
import static org.eclipse.lsp4mp.jdt.internal.core.ls.ArgumentUtils.getTextDocumentIdentifier;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InlayHint;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.JavaCursorContextResult;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;
import org.eclipse.lsp4mp.commons.MicroProfileInlayHintTypeSettings;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionResult;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDefinitionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaInlayHintSettings;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.runtime.ExecutionMode;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;
import org.eclipse.lsp4mp.jdt.core.PropertiesManagerForJava;

/**
 * JDT LS delegate command handler for Java file.
 *
 * @author Angelo ZERR
 *
 */
public class MicroProfileDelegateCommandHandlerForJava extends AbstractMicroProfileDelegateCommandHandler {

	private static final String FILE_INFO_COMMAND_ID = "microprofile/java/fileInfo";
	private static final String JAVA_CODEACTION_COMMAND_ID = "microprofile/java/codeAction";
	private static final String JAVA_CODEACTION_RESOLVE_COMMAND_ID = "microprofile/java/codeActionResolve";
	private static final String JAVA_CODELENS_COMMAND_ID = "microprofile/java/codeLens";
	private static final String JAVA_INLAY_HINT_COMMAND_ID = "microprofile/java/inlayHint";

	private static final String JAVA_COMPLETION_COMMAND_ID = "microprofile/java/completion";
	private static final String JAVA_DEFINITION_COMMAND_ID = "microprofile/java/definition";
	private static final String JAVA_DIAGNOSTICS_COMMAND_ID = "microprofile/java/diagnostics";
	private static final String JAVA_HOVER_COMMAND_ID = "microprofile/java/hover";
	private static final String JAVA_WORKSPACE_SYMBOLS_ID = "microprofile/java/workspaceSymbols";

	public MicroProfileDelegateCommandHandlerForJava() {
	}

	@Override
	public Object executeCommand(String commandId, List<Object> arguments, IProgressMonitor progress) throws Exception {
		switch (commandId) {
		case FILE_INFO_COMMAND_ID:
			return getFileInfo(arguments, commandId, progress);
		case JAVA_CODEACTION_COMMAND_ID:
			return getCodeActionForJava(arguments, commandId, progress);
		case JAVA_CODEACTION_RESOLVE_COMMAND_ID:
			return resolveCodeActionForJava(arguments, commandId, progress);
		case JAVA_CODELENS_COMMAND_ID:
			return getCodeLensForJava(arguments, commandId, progress);
		case JAVA_INLAY_HINT_COMMAND_ID:
			return getInlayHintForJava(arguments, commandId, progress);
		case JAVA_COMPLETION_COMMAND_ID:
			return getCompletionForJava(arguments, commandId, progress);
		case JAVA_DEFINITION_COMMAND_ID:
			return getDefinitionForJava(arguments, commandId, progress);
		case JAVA_DIAGNOSTICS_COMMAND_ID:
			return getDiagnosticsForJava(arguments, commandId, progress);
		case JAVA_HOVER_COMMAND_ID:
			return getHoverForJava(arguments, commandId, progress);
		case JAVA_WORKSPACE_SYMBOLS_ID:
			return getWorkspaceSymbolsForJava(arguments, commandId, progress);
		default:
			throw new UnsupportedOperationException(String.format("Unsupported command '%s'!", commandId));
		}
	}

	/**
	 * Returns the file information (package name, etc) for the given Java file.
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return the file information (package name, etc) for the given Java file.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private static JavaFileInfo getFileInfo(List<Object> arguments, String commandId, IProgressMonitor monitor)
			throws JavaModelException, CoreException {
		// Create java file information parameter
		MicroProfileJavaFileInfoParams params = createJavaFileInfoParams(arguments, commandId);
		// Return file information from the parameter
		return PropertiesManagerForJava.getInstance().fileInfo(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Create the Java file information parameter from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return the Java file information parameter.
	 */
	private static MicroProfileJavaFileInfoParams createJavaFileInfoParams(List<Object> arguments, String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaFileInfoParams argument!", commandId));
		}
		// Get project name from the java file URI
		String javaFileUri = getString(obj, "uri");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaFileInfoParams.uri (java file URI)!",
					commandId));
		}
		MicroProfileJavaFileInfoParams params = new MicroProfileJavaFileInfoParams();
		params.setUri(javaFileUri);
		return params;
	}

	/**
	 * Returns the code action for the given Java file.
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return the code action for the given Java file.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private static List<? extends CodeAction> getCodeActionForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		// Create java code action parameter
		MicroProfileJavaCodeActionParams params = createMicroProfileJavaCodeActionParams(arguments, commandId);
		// Return code action from the code action parameter
		return PropertiesManagerForJava.getInstance().codeAction(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Create java code action parameter from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return java code action parameter
	 */
	private static MicroProfileJavaCodeActionParams createMicroProfileJavaCodeActionParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaCodeActionParams argument!", commandId));
		}
		TextDocumentIdentifier texdDocumentIdentifier = getTextDocumentIdentifier(obj, "textDocument");
		if (texdDocumentIdentifier == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaCodeActionParams.texdDocumentIdentifier",
					commandId));
		}
		Range range = getRange(obj, "range");
		CodeActionContext context = getCodeActionContext(obj, "context");
		boolean resourceOperationSupported = getBoolean(obj, "resourceOperationSupported");
		boolean commandConfigurationUpdateSupported = getBoolean(obj, "commandConfigurationUpdateSupported");
		boolean resolveSupported = getBoolean(obj, "resolveSupported");
		MicroProfileJavaCodeActionParams params = new MicroProfileJavaCodeActionParams();
		params.setTextDocument(texdDocumentIdentifier);
		params.setRange(range);
		params.setContext(context);
		params.setResourceOperationSupported(resourceOperationSupported);
		params.setCommandConfigurationUpdateSupported(commandConfigurationUpdateSupported);
		params.setResolveSupported(resolveSupported);
		return params;
	}

	private static CodeAction resolveCodeActionForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		// Create java code action parameter
		CodeAction unresolved = createMicroProfileJavaCodeActionResolveParams(arguments, commandId);
		// Return code action from the code action parameter
		return PropertiesManagerForJava.getInstance().resolveCodeAction(unresolved, JDTUtilsLSImpl.getInstance(),
				monitor);
	}

	private static CodeAction createMicroProfileJavaCodeActionResolveParams(List<Object> arguments, String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(
					String.format("Command '%s' must be called with one CodeAction argument!", commandId));
		}
		CodeAction codeAction = JSONUtility.toModel(obj, CodeAction.class);
		if (codeAction == null) {
			throw new UnsupportedOperationException(
					String.format("Command '%s' must be called with one CodeAction argument!", commandId));
		}
		CodeActionResolveData resolveData = JSONUtility.toModel(codeAction.getData(), CodeActionResolveData.class);
		if (resolveData == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with a CodeAction that has CodeActionResolveData!", commandId));
		}
		codeAction.setData(resolveData);
		return codeAction;
	}

	/**
	 * Returns the code lenses for the given Java file.
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return the code lenses for the given Java file.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private static List<? extends CodeLens> getCodeLensForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		// Create java code lens parameter
		MicroProfileJavaCodeLensParams params = createMicroProfileJavaCodeLensParams(arguments, commandId);
		// Return code lenses from the lens parameter
		return PropertiesManagerForJava.getInstance().codeLens(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Create java code lens parameter from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return java code lens parameter
	 */
	private static MicroProfileJavaCodeLensParams createMicroProfileJavaCodeLensParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaCodeLensParams argument!", commandId));
		}
		String javaFileUri = getString(obj, "uri");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaCodeLensParams.uri (java URI)!",
					commandId));
		}
		MicroProfileJavaCodeLensParams params = new MicroProfileJavaCodeLensParams(javaFileUri);
		params.setUrlCodeLensEnabled(getBoolean(obj, "urlCodeLensEnabled"));
		params.setCheckServerAvailable(getBoolean(obj, "checkServerAvailable"));
		params.setOpenURICommand(getString(obj, "openURICommand"));
		params.setLocalServerPort(getInt(obj, "localServerPort"));
		return params;
	}

	/**
	 * Returns the inlay hints for the given Java file.
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return the inlay hints for the given Java file.
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	private static List<InlayHint> getInlayHintForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		// Create java inlay hints parameter
		MicroProfileJavaInlayHintParams params = createMicroProfileJavaInlayHintParams(arguments, commandId);
		// Return inlay hints from the given parameter
		return PropertiesManagerForJava.getInstance().inlayHint(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Create java inlay hint parameter from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return java inlay hint parameter
	 */
	private static MicroProfileJavaInlayHintParams createMicroProfileJavaInlayHintParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaInlayHintParams argument!", commandId));
		}
		String javaFileUri = getString(obj, "uri");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaInlayHintParams.uri (java URI)!",
					commandId));
		}
		MicroProfileJavaInlayHintSettings settings = null;
		Map<String, Object> settingsObj = getObject(obj, "settings");
		if (settingsObj != null) {
			int executionMode = getInt(settingsObj, "mode");
			settings = new MicroProfileJavaInlayHintSettings(ExecutionMode.forValue(executionMode));
			settings.setConverters(createInlayHintTypeSettings("converters", settingsObj));
			settings.setDefaultValues(createInlayHintTypeSettings("defaultValues", settingsObj));
		}
		return new MicroProfileJavaInlayHintParams(javaFileUri, settings);
	}

	private static MicroProfileInlayHintTypeSettings createInlayHintTypeSettings(String fieldName,
			Map<String, Object> settingsObj) {
		Map<String, Object> obj = (Map<String, Object>) settingsObj.get(fieldName);
		if (obj == null) {
			return null;
		}
		boolean enabled = getBoolean(obj, "enabled");
		MicroProfileInlayHintTypeSettings settings = new MicroProfileInlayHintTypeSettings();
		settings.setEnabled(enabled);
		return settings;
	}

	/**
	 * Return the completion result for the given arguments
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return the completion result for the given arguments
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	private static MicroProfileJavaCompletionResult getCompletionForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		MicroProfileJavaCompletionParams params = createMicroProfileJavaCompletionParams(arguments, commandId);
		CompletionList completionList = PropertiesManagerForJava.getInstance().completion(params,
				JDTUtilsLSImpl.getInstance(), monitor);
		JavaCursorContextResult cursorContext = PropertiesManagerForJava.getInstance().javaCursorContext(params,
				JDTUtilsLSImpl.getInstance(), monitor);
		return new MicroProfileJavaCompletionResult(completionList, cursorContext);
	}

	/**
	 * Create the completion parameters from the given argument map
	 *
	 * @param arguments
	 * @param commandId
	 * @return the completion parameters from the given argument map
	 */
	private static MicroProfileJavaCompletionParams createMicroProfileJavaCompletionParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaCompletionParams argument!", commandId));
		}
		String javaFileUri = getString(obj, "uri");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaCompletionParams.uri (java URI)!",
					commandId));
		}
		Position position = getPosition(obj, "position");
		if (position == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaCompletionParams.position (completion trigger location)!",
					commandId));
		}
		MicroProfileJavaCompletionParams params = new MicroProfileJavaCompletionParams(javaFileUri, position);
		return params;
	}

	/**
	 * Returns the list o <code>MicroProfileLocationLink</code> for the definition
	 * described in <code>arguments</code>
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	private static List<MicroProfileDefinition> getDefinitionForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException, CoreException {
		// Create java definition parameter
		MicroProfileJavaDefinitionParams params = createMicroProfileJavaDefinitionParams(arguments, commandId);
		// Return hover info from hover parameter
		return PropertiesManagerForJava.getInstance().definition(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Returns the java definition parameters from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return the definition hover parameters
	 */
	private static MicroProfileJavaDefinitionParams createMicroProfileJavaDefinitionParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaDefinitionParams argument!", commandId));
		}
		String javaFileUri = getString(obj, "uri");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaDefinitionParams.uri (java URI)!",
					commandId));
		}

		Position hoverPosition = getPosition(obj, "position");
		return new MicroProfileJavaDefinitionParams(javaFileUri, hoverPosition);
	}

	/**
	 * Returns the publish diagnostics list for a given java file URIs.
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return the publish diagnostics list for a given java file URIs.
	 * @throws JavaModelException
	 */
	private static List<PublishDiagnosticsParams> getDiagnosticsForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) throws JavaModelException {
		// Create java diagnostics parameter
		MicroProfileJavaDiagnosticsParams params = createMicroProfileJavaDiagnosticsParams(arguments, commandId);
		// Return diagnostics from parameter
		return PropertiesManagerForJava.getInstance().diagnostics(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Returns the java diagnostics parameters from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return the java diagnostics parameters
	 */
	private static MicroProfileJavaDiagnosticsParams createMicroProfileJavaDiagnosticsParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with one MicroProfileJavaDiagnosticsParams argument!", commandId));
		}
		List<String> javaFileUri = getStringList(obj, "uris");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaDiagnosticsParams.uri (java URIs)!",
					commandId));
		}
		MicroProfileJavaDiagnosticsSettings settings = null;
		Map<String, Object> settingsObj = getObject(obj, "settings");
		if (settingsObj != null) {
			List<String> patterns = getStringList(settingsObj, "patterns");
			int severity = getInt(settingsObj, "validationValueSeverity");
			int executionMode = getInt(settingsObj, "mode");
			settings = new MicroProfileJavaDiagnosticsSettings(patterns,
					severity == 0 ? null : DiagnosticSeverity.forValue(severity),
					ExecutionMode.forValue(executionMode));
		}
		return new MicroProfileJavaDiagnosticsParams(javaFileUri, settings);
	}

	/**
	 * Returns the <code>Hover</code> for the hover described in
	 * <code>arguments</code>
	 *
	 * @param arguments
	 * @param commandId
	 * @param monitor
	 * @return
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	private static Hover getHoverForJava(List<Object> arguments, String commandId, IProgressMonitor monitor)
			throws JavaModelException, CoreException {
		// Create java hover parameter
		MicroProfileJavaHoverParams params = createMicroProfileJavaHoverParams(arguments, commandId);
		// Return hover info from hover parameter
		return PropertiesManagerForJava.getInstance().hover(params, JDTUtilsLSImpl.getInstance(), monitor);
	}

	/**
	 * Returns the java hover parameters from the given arguments map.
	 *
	 * @param arguments
	 * @param commandId
	 *
	 * @return the java hover parameters
	 */
	private static MicroProfileJavaHoverParams createMicroProfileJavaHoverParams(List<Object> arguments,
			String commandId) {
		Map<String, Object> obj = getFirst(arguments);
		if (obj == null) {
			throw new UnsupportedOperationException(String
					.format("Command '%s' must be called with one MicroProfileJavaHoverParams argument!", commandId));
		}
		String javaFileUri = getString(obj, "uri");
		if (javaFileUri == null) {
			throw new UnsupportedOperationException(String.format(
					"Command '%s' must be called with required MicroProfileJavaHoverParams.uri (java URI)!",
					commandId));
		}

		Position hoverPosition = getPosition(obj, "position");
		DocumentFormat documentFormat = DocumentFormat.PlainText;
		Number documentFormatIndex = (Number) obj.get("documentFormat");
		if (documentFormatIndex != null) {
			documentFormat = DocumentFormat.forValue(documentFormatIndex.intValue());
		}
		boolean surroundEqualsWithSpaces = ((Boolean) obj.get("surroundEqualsWithSpaces")).booleanValue();
		return new MicroProfileJavaHoverParams(javaFileUri, hoverPosition, documentFormat, surroundEqualsWithSpaces);
	}

	private List<SymbolInformation> getWorkspaceSymbolsForJava(List<Object> arguments, String commandId,
			IProgressMonitor monitor) {
		String projectUri = createMicroProfileJavaWorkspaceSymbolParams(arguments, commandId);
		return PropertiesManagerForJava.getInstance().workspaceSymbols(projectUri, JDTUtilsLSImpl.getInstance(),
				monitor);
	}

	private static String createMicroProfileJavaWorkspaceSymbolParams(List<Object> arguments, String commandId) {
		Object projectUriObj = (String) arguments.get(0);
		if (projectUriObj == null || !(projectUriObj instanceof String)) {
			throw new UnsupportedOperationException(
					String.format("Command '%s' must be called with one projectUri: String argument!", commandId));
		}
		return (String) projectUriObj;
	}

}
