/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.api.jexl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

public class JexlUtilsTest extends AbstractTest {

    @Mock
    private JexlContext context;

    @Test
    void isExpressionValid() {
        String expression = "6 * 12 + 5 / 2.6";
        assertTrue(jexlTools().isExpressionValid(expression));

        expression = "@inv4lid expression!";
        assertFalse(jexlTools().isExpressionValid(expression));
    }

    @Test
    void evaluateExpr() {
        String expression = null;
        assertEquals(StringUtils.EMPTY, jexlTools().evaluateExpression(expression, context));

        expression = "6 * 12 + 5 / 2.6";
        double result = 73.92307692307692;
        assertEquals(result, jexlTools().evaluateExpression(expression, context));
    }

    @Test
    void builderFields(
            final @Mock Any any,
            final @Mock AnyTO anyTO,
            final @Mock Realm realm,
            final @Mock RealmTO realmTO) {

        JexlContextBuilder builder = new JexlContextBuilder();
        ReflectionTestUtils.setField(builder, "jexlContext", context);

        builder.fields(new Exception());
        verify(context, times(2)).set(eq("cause"), any());

        String testFullPath = "testFullPath";
        when(any.getRealm()).thenReturn(realm);
        when(realm.getFullPath()).thenReturn(testFullPath);
        builder.fields(any);
        verify(context).set("realm", testFullPath);

        String testRealm = "testRealm";
        when(anyTO.getRealm()).thenReturn(testRealm);
        builder.fields(anyTO);
        verify(context, times(3)).set("realm", testRealm);

        String fullPath = "test/full/path";
        when(realm.getFullPath()).thenReturn(fullPath);
        builder.fields(realm);
        verify(context, times(2)).set("fullPath", fullPath);

        fullPath = "test/full/path2";
        when(realmTO.getFullPath()).thenReturn(fullPath);
        builder.fields(realmTO);
        verify(context, times(2)).set("fullPath", fullPath);
    }

    @Test
    void builderAttrs() {
        String schema = "testSchema";
        String value = "testValue";
        Collection<Attr> attrs = new ArrayList<>();
        Attr attr = new Attr.Builder(schema).build();
        attrs.add(attr);

        JexlContextBuilder builder = new JexlContextBuilder();
        ReflectionTestUtils.setField(builder, "jexlContext", context);

        builder.attrs(attrs);
        verify(context).set(schema, StringUtils.EMPTY);

        attr = new Attr.Builder(schema).value(value).build();
        attrs.clear();
        attrs.add(attr);

        builder.attrs(attrs);
        verify(context).set(schema, value);
    }

    @Test
    void builderPlainAttrs(final @Mock Collection<PlainAttr> attrs) {
        JexlContextBuilder builder = new JexlContextBuilder();
        ReflectionTestUtils.setField(builder, "jexlContext", context);

        builder.plainAttrs(attrs);
        verify(context, times(0)).set(anyString(), any());
    }

    @Test
    public void addDerAttrsToContext(
            final @Mock DerAttrHandler derAttrHandler,
            final @Mock Any any) {

        String expression = null;

        Map<String, String> derAttrs = new HashMap<>();
        derAttrs.put("derSchema", expression);

        when(derAttrHandler.getValues(any(Any.class))).thenReturn(derAttrs);

        JexlContextBuilder builder = new JexlContextBuilder();
        ReflectionTestUtils.setField(builder, "jexlContext", context);

        builder.derAttrs(derAttrHandler.getValues(any));
        verify(context).set("derSchema", expression);
    }

    @Test
    void evaluateMandatoryCondition(
            final @Mock DerAttrHandler derAttrHandler,
            final @Mock Any any,
            final @Mock Collection<PlainAttr> plainAttrs) {

        String expression = null;

        Map<String, String> derAttrs = new HashMap<>();
        derAttrs.put("derSchema", expression);

        when(any.getPlainAttrs()).thenReturn(new ArrayList<>());
        when(derAttrHandler.getValues(any(Any.class))).thenReturn(derAttrs);

        assertTrue(jexlTools().evaluateMandatoryCondition("true", any, derAttrHandler));
        assertFalse(jexlTools().evaluateMandatoryCondition("false", any, derAttrHandler));
    }

    @Test
    void evaluateTemplate() {
        byte[] byteArray = "a value".getBytes();
        String result = jexlTools().evaluateTemplate(
                "${syncope:base64Encode(value)}", new MapContext(Map.of("value", byteArray)));
        assertEquals(Base64.getEncoder().encodeToString(byteArray), result);

        result = jexlTools().evaluateTemplate(
                "${syncope:fullPath2Dn(value, 'ou')}", new MapContext(Map.of("value", "/a/b/c")));
        assertEquals("ou=c,ou=b,ou=a", result);
    }
}
