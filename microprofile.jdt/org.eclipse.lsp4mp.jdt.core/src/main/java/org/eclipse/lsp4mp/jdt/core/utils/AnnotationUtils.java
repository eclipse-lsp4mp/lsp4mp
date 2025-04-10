/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.jdt.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.internal.core.ImportContainerInfo;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.util.Ranges;

/**
 * Java annotations utilities.
 *
 * @author Angelo ZERR
 *
 */
public class AnnotationUtils {

	private static final String ATTRIBUTE_VALUE = "value";

	/**
	 * Returns checks if the <code>annotatable</code> parameter is annotated with
	 * the given annotation.
	 *
	 * @param annotatable    the class, field which can be annotated
	 * @param annotationName a non-null FQCN annotation to check against
	 * @return <code>true</code> if the <code>annotatable</code> parameter is
	 *         annotated with the given annotation, <code>false</code> otherwise.
	 */
	public static boolean hasAnnotation(IAnnotatable annotatable, String annotationName) throws JavaModelException {
		return hasAnyAnnotation(annotatable, annotationName);
	}

	/**
	 * Returns checks if the <code>annotatable</code> parameter is annotated with
	 * ANY of the given annotations.
	 *
	 * @param annotatable     the class, field which can be annotated
	 * @param annotationNames a non-null, non-empty array of FQCN annotations to
	 *                        check against
	 * @return <code>true</code> if the <code>annotatable</code> parameter is
	 *         annotated with ANY of the given annotations, <code>false</code>
	 *         otherwise.
	 * @throws JavaModelException
	 */
	public static boolean hasAnyAnnotation(IAnnotatable annotatable, String... annotationNames)
			throws JavaModelException {
		return getFirstAnnotation(annotatable, annotationNames) != null;
	}

	/**
	 * Returns an {@link IAnnotation} of the first annotation in
	 * <code>annotationNames</code> that appears on the given annotatable.
	 *
	 * It returns the first in the <code>annotationNames</code> list, <b>not</b> the
	 * first in the order that the annotations appear on the annotatable. <br />
	 * <br />
	 * e.g.
	 *
	 * <pre>
	 * &commat;Singleton &commat;Deprecated String myString;
	 * </pre>
	 *
	 * when given the <code>annotationNames</code> list
	 * <code>{"Potato", "Deprecated",
	 * "Singleton"}</code> will return the IAnnotation for
	 * <code>&commat;Deprecated</code>.
	 *
	 * @param annotatable     the annotatable to check for the annotations
	 * @param annotationNames the FQNs of the annotations to check for
	 * @return an {@link PsiAnnotation} of the first annotation in
	 *         <code>annotationNames</code> that appears on the given annotatable
	 */
	public static IAnnotation getFirstAnnotation(IAnnotatable annotatable, String... annotationNames)
			throws JavaModelException {
		if (annotatable == null) {
			return null;
		}
		IAnnotation[] annotations = annotatable.getAnnotations();
		for (IAnnotation annotation : annotations) {
			for (String annotationName : annotationNames) {
				if (isMatchAnnotation(annotation, annotationName)) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Returns an IAnnotation of the first annotation in
	 * <code>annotationNames</code> that appears on the given annotatable.
	 *
	 * It returns the first in the <code>annotationNames</code> list, <b>not</b> the
	 * first in the order that the annotations appear on the annotatable. <br />
	 * <br />
	 * eg.
	 *
	 * <pre>
	 * &commat;Singleton &commat;Deprecated String myString;
	 * </pre>
	 *
	 * when given the <code>annotationNames</code> list
	 * <code>{"Potato", "Deprecated",
	 * "Singleton"}</code> will return the IAnnotation for
	 * <code>&commat;Deprecated</code>.
	 *
	 * @param annotatable     the annotatable to check for the annotations
	 * @param annotationNames the FQNs of the annotations to check for
	 * @return an IAnnotation of the first annotation in
	 *         <code>annotationNames</code> that appears on the given annotatable
	 * @throws JavaModelException
	 */
	public static IAnnotation getAnnotation(IAnnotatable annotatable, String... annotationNames)
			throws JavaModelException {
		for (String annotationName : annotationNames) {
			IAnnotation annotation = getFirstAnnotation(annotatable, annotationName);
			if (annotation != null) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Returns true if the given annotation match the given annotation name and
	 * false otherwise.
	 *
	 * @param annotation     the annotation.
	 * @param annotationName the annotation name.
	 * @return true if the given annotation match the given annotation name and
	 *         false otherwise.
	 */
	public static boolean isMatchAnnotation(IAnnotation annotation, String annotationName) {
		// Annotation name is the fully qualified name of the annotation class (ex :
		// org.eclipse.microprofile.config.inject.ConfigProperties)
		// - when IAnnotation comes from binary, IAnnotation#getElementName() =
		// 'org.eclipse.microprofile.config.inject.ConfigProperties'
		// - when IAnnotation comes from source, IAnnotation#getElementName() =
		// 'ConfigProperties'
		if (!annotationName.endsWith(annotation.getElementName())) {
			return false;
		}
		if (annotationName.equals(annotation.getElementName())) {
			return true;
		}
		// Here IAnnotation comes from source and match only 'ConfigProperties', we must
		// check if the CU declares the proper import (ex : import
		// org.eclipse.microprofile.config.inject.ConfigProperties;)
		return isMatchAnnotationFullyQualifiedName(annotation, annotationName);
	}

	private static boolean isMatchAnnotationFullyQualifiedName(IAnnotation annotation, String annotationName) {

		// The clean code should use resolveType:

		// IJavaElement parent = annotation.getParent();
		// if (parent instanceof IMember) {
		// IType declaringType = parent instanceof IType ? (IType) parent : ((IMember)
		// parent).getDeclaringType();
		// String elementName = annotation.getElementName();
		// try {
		// String[][] fullyQualifiedName = declaringType.resolveType(elementName);
		// return annotationName.equals(fullyQualifiedName[0][0] + "." +
		// fullyQualifiedName[0][1]);
		// } catch (JavaModelException e) {
		// }
		// }

		// But for performance reason, we check if the import of annotation name is
		// declared

		ICompilationUnit unit = (ICompilationUnit) annotation.getAncestor(IJavaElement.COMPILATION_UNIT);
		if (unit == null) {
			return false;
		}
		IImportContainer container = unit.getImportContainer();
		if (container == null) {
			return false;
		}

		// The following code uses JDT internal class and looks like
		// ICompilationUnit#getImports()
		// To avoid creating an array of IImportDeclaration, we do the following code:

		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		Object info = manager.getInfo(container);
		if (info == null) {
			if (manager.getInfo(unit) != null) {
				// CU was opened, but no import container, then no imports
				// return NO_IMPORTS;
				return false;
			} else {
				try {
					unit.open(null);
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // force opening of CU
				info = manager.getInfo(container);
				if (info == null)
					// after opening, if no import container, then no imports
					// return NO_IMPORTS;
					return false;
			}
		}
		IJavaElement[] elements = ((ImportContainerInfo) info).getChildren();
		for (IJavaElement child : elements) {
			IImportDeclaration importDeclaration = (IImportDeclaration) child;
			if (importDeclaration.isOnDemand()) {
				String fqn = importDeclaration.getElementName();
				String qualifier = fqn.substring(0, fqn.lastIndexOf('.'));
				if (qualifier.equals(annotationName.substring(0, annotationName.lastIndexOf('.')))) {
					return true;
				}
			} else if (importDeclaration.getElementName().equals(annotationName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the given annotation match the given annotation name and
	 * false otherwise.
	 *
	 * @param annotation     the annotation.
	 * @param annotationName the annotation name.
	 * @return true if the given annotation match the given annotation name and
	 *         false otherwise.
	 */
	public static boolean isMatchAnnotation(Annotation annotation, String annotationName) {
		IAnnotationBinding binding = annotation.resolveAnnotationBinding();
		if (binding == null) {
			return false;
		}
		ITypeBinding annotationType = binding.getAnnotationType();
		if (annotationType == null) {
			return false;
		}
		return annotationName.endsWith(annotationType.getQualifiedName());
	}

	/**
	 * Returns the value of the given member name of the given annotation.
	 *
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the value of the given member name of the given annotation.
	 * @throws JavaModelException
	 */
	public static String getAnnotationMemberValue(IAnnotation annotation, String memberName) throws JavaModelException {
		for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
			if (memberName.equals(pair.getMemberName())) {
				String memberValue = pair.getValue() != null ? pair.getValue().toString() : null;
				if (memberValue != null) {
					switch (pair.getValueKind()) {
					case IMemberValuePair.K_STRING:
						// ex : @Path("foo") --> returns foo
						return memberValue;
					case IMemberValuePair.K_SIMPLE_NAME: {
						// ex : private static final String PATH = "foo";
						// @Path(PATH) --> returns foo
						IType type = ((IType) annotation.getAncestor(IJavaElement.TYPE));
						if (type != null) {
							String fieldValue = getFieldValue(type, memberValue);
							if (fieldValue != null) {
								return fieldValue;
							}
						}
						return memberValue;
					}
					case IMemberValuePair.K_QUALIFIED_NAME: {
						// ex : class SomePage {
						// private static final String PATH = "foo";
						// @Path(SomePage.PATH) --> returns foo
						IType type = ((IType) annotation.getAncestor(IJavaElement.TYPE));
						if (type != null) {
							int index = memberValue.lastIndexOf(".");
							String fieldName = memberValue.substring(index + 1);
							String valueClass = memberValue.substring(0, index);
							IType valueType = getResolvedType(valueClass, type);
							if (valueType != null) {
								String fieldValue = getFieldValue(valueType, fieldName);
								if (fieldValue != null) {
									return fieldValue;
								}
							}
						}
						return memberValue;
					}
					default:
						return memberValue;
					}
				}
				return null;
			}
		}
		return null;
	}

	private static IType getResolvedType(String valueClass, IType type) throws JavaModelException {
		String[][] resolvedTypes = type.resolveType(valueClass);
		if (resolvedTypes != null && resolvedTypes.length > 0) {
			// It is a Class and not an enum which returns resolvedTypes as null 
			valueClass = resolvedTypes[0][0] + "." + resolvedTypes[0][1];
		}
		return type.getJavaProject().findType(valueClass);
	}

	private static String getFieldValue(IType valueType, String fieldName) throws JavaModelException {
		IField valueField = valueType.getField(fieldName);
		if (valueField != null && valueField.exists()) {
			if (valueType.isEnum()) {
				return valueField.getElementName(); 
			}
			Object constantField = valueField.getConstant();
			if (constantField != null) {
				String constantValue = constantField.toString();
				// Remove double quote if needed.
				if (constantValue.length() > 1 && constantValue.charAt(0) == '"'
						&& constantValue.charAt(constantValue.length() - 1) == '"') {
					constantValue = constantValue.substring(1, constantValue.length() - 1);
				}
				return constantValue;
			}
		}
		return null;
	}

	/**
	 * Returns the expression for the value of the given member name of the given
	 * annotation.
	 *
	 * @param annotation the annotation.
	 * @param memberName the member name.
	 * @return the expression for the value of the given member name of the given
	 *         annotation.
	 * @throws JavaModelException
	 */
	public static Expression getAnnotationMemberValueExpression(Annotation annotation, String memberName)
			throws JavaModelException {
		if (annotation instanceof NormalAnnotation) {
			NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
			for (Object pair : normalAnnotation.values()) {
				MemberValuePair castPair = (MemberValuePair) pair;
				if (memberName.equals(castPair.getName().toString())) {
					return castPair.getValue();
				}
			}
			return null;
		} else if (annotation instanceof SingleMemberAnnotation) {
			SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) annotation;
			return ATTRIBUTE_VALUE.equals(memberName) || singleMemberAnnotation.getProperty(memberName) != null
					? singleMemberAnnotation.getValue()
					: null;
		}
		// MarkerAnnotation has no members
		return null;
	}

	/**
	 * Retrieve the value and range of an annotation member given a supported list
	 * of annotation members
	 *
	 * @param annotation            the annotation of the retrieved members
	 * @param annotationSource      the qualified name of the annotation
	 * @param annotationMemberNames the supported members of the annotation
	 * @param position              the hover position
	 * @param typeRoot              the java type root
	 * @param utils                 the utility to retrieve the member range
	 *
	 * @return an AnnotationMemberInfo object if the member exists, null otherwise
	 * @throws JavaModelException
	 */
	public static AnnotationMemberInfo getAnnotationMemberAt(IAnnotation annotation, String[] annotationMemberNames,
			Position position, ITypeRoot typeRoot, IJDTUtils utils) throws JavaModelException {
		String annotationSource = ((ISourceReference) annotation).getSource();
		ISourceRange r = ((ISourceReference) annotation).getSourceRange();
		String annotationMemberValue = null;
		for (String annotationMemberName : annotationMemberNames) {
			annotationMemberValue = getAnnotationMemberValue(annotation, annotationMemberName);
			if (annotationMemberValue != null) {
				// A regex is used to match the member and member value to find the position
				Pattern memberPattern = Pattern.compile(".*[^\"]\\s*(" + annotationMemberName + ")\\s*=.*",
						Pattern.DOTALL);
				Matcher match = memberPattern.matcher(annotationSource);
				if (match.matches()) {
					int offset = annotationSource.indexOf(annotationMemberValue, match.end(1));
					Range range = utils.toRange(typeRoot, r.getOffset() + offset, annotationMemberValue.length());

					if (!position.equals(range.getEnd()) && Ranges.containsPosition(range, position)) {
						return new AnnotationMemberInfo(annotationMemberValue, range);
					}
				}
			}
		}

		return null;

	}

}
