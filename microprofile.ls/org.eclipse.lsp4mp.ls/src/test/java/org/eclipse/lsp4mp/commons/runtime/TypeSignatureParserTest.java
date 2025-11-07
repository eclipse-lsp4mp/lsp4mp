/*******************************************************************************
* Copyright (c) 2025 Red Hat Inc. and others.
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
package org.eclipse.lsp4mp.commons.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

public class TypeSignatureParserTest {

    @Test
    public void testSimpleClass() {
        Type t = TypeSignatureParser.parse("java.math.BigDecimal");
        assertEquals(BigDecimal.class, t);
    }

    @Test
    public void testOptionalSimple() {
        Type t = TypeSignatureParser.parse("java.util.Optional<java.math.BigDecimal>");

        assertTrue(t instanceof ParameterizedType);
        ParameterizedType p = (ParameterizedType) t;

        assertEquals(Optional.class, p.getRawType());
        assertEquals(BigDecimal.class, p.getActualTypeArguments()[0]);
    }

    @Test
    public void testListSimple() {
        Type t = TypeSignatureParser.parse("java.util.List<java.lang.String>");

        assertTrue(t instanceof ParameterizedType);
        ParameterizedType p = (ParameterizedType) t;

        assertEquals(List.class, p.getRawType());
        assertEquals(String.class, p.getActualTypeArguments()[0]);
    }

    @Test
    public void testMapSimple() {
        Type t = TypeSignatureParser.parse("java.util.Map<java.lang.String, java.math.BigDecimal>");

        assertTrue(t instanceof ParameterizedType);
        ParameterizedType p = (ParameterizedType) t;

        assertEquals(Map.class, p.getRawType());
        assertEquals(String.class, p.getActualTypeArguments()[0]);
        assertEquals(BigDecimal.class, p.getActualTypeArguments()[1]);
    }

    @Test
    public void testOptionalNested() {
        Type t = TypeSignatureParser.parse("java.util.Optional<java.util.Optional<java.math.BigDecimal>>");

        ParameterizedType outer = (ParameterizedType) t;
        assertEquals(Optional.class, outer.getRawType());

        ParameterizedType inner = (ParameterizedType) outer.getActualTypeArguments()[0];
        assertEquals(Optional.class, inner.getRawType());

        assertEquals(BigDecimal.class, ((ParameterizedType) inner).getActualTypeArguments()[0]);
    }

    @Test
    public void testMapWithOptionalValue() {
        Type t = TypeSignatureParser.parse(
                "java.util.Map<java.lang.String, java.util.Optional<java.math.BigDecimal>>"
        );

        ParameterizedType map = (ParameterizedType) t;
        assertEquals(Map.class, map.getRawType());
        assertEquals(String.class, map.getActualTypeArguments()[0]);

        ParameterizedType opt = (ParameterizedType) map.getActualTypeArguments()[1];
        assertEquals(Optional.class, opt.getRawType());
        assertEquals(BigDecimal.class, opt.getActualTypeArguments()[0]);
    }

    @Test
    public void testDeepGenericTree() {
        Type t = TypeSignatureParser.parse(
                "java.util.Map<java.lang.String, java.util.List<java.util.Optional<java.math.BigDecimal>>>"
        );

        ParameterizedType map = (ParameterizedType) t;
        assertEquals(Map.class, map.getRawType());

        ParameterizedType list = (ParameterizedType) map.getActualTypeArguments()[1];
        assertEquals(List.class, list.getRawType());

        ParameterizedType opt = (ParameterizedType) list.getActualTypeArguments()[0];
        assertEquals(Optional.class, opt.getRawType());
        assertEquals(BigDecimal.class, opt.getActualTypeArguments()[0]);
    }

    @Test
    public void testInvalidMissingAngle() {
        assertThrows(IllegalArgumentException.class, () ->
                TypeSignatureParser.parse("java.util.Optional<java.math.BigDecimal")
        );
    }

    @Test
    public void testInvalidClassName() {
        assertThrows(IllegalArgumentException.class, () ->
                TypeSignatureParser.parse("com.does.not.Exist<Something>")
        );
    }
}
