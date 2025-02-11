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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class JexlUtilsTest extends AbstractTest {

    @Mock
    private JexlContext context;

    @Test
    public void isExpressionValid() {
        String expression = "6 * 12 + 5 / 2.6";
        assertTrue(JexlUtils.isExpressionValid(expression));

        expression = "@inv4lid expression!";
        assertFalse(JexlUtils.isExpressionValid(expression));
    }

    @Test
    public void evaluate() {
        String expression = null;
        assertEquals(StringUtils.EMPTY, JexlUtils.evaluateExpr(expression, context));

        expression = "6 * 12 + 5 / 2.6";
        double result = 73.92307692307692;
        assertEquals(result, JexlUtils.evaluateExpr(expression, context));
    }

    @Test
    public void addFieldsToContext(
            final @Mock Any any,
            final @Mock AnyTO anyTO,
            final @Mock Realm realm,
            final @Mock RealmTO realmTO) {

        JexlUtils.addFieldsToContext(new Exception(), context);
        verify(context, times(2)).set(eq("cause"), any());

        String testFullPath = "testFullPath";
        when(any.getRealm()).thenReturn(realm);
        when(realm.getFullPath()).thenReturn(testFullPath);
        JexlUtils.addFieldsToContext(any, context);
        verify(context).set("realm", testFullPath);

        String testRealm = "testRealm";
        when(anyTO.getRealm()).thenReturn(testRealm);
        JexlUtils.addFieldsToContext(anyTO, context);
        verify(context, times(3)).set("realm", testRealm);

        String fullPath = "test/full/path";
        when(realm.getFullPath()).thenReturn(fullPath);
        JexlUtils.addFieldsToContext(realm, context);
        verify(context, times(2)).set("fullPath", fullPath);

        fullPath = "test/full/path2";
        when(realmTO.getFullPath()).thenReturn(fullPath);
        JexlUtils.addFieldsToContext(realmTO, context);
        verify(context, times(2)).set("fullPath", fullPath);
    }

    @Test
    public void addAttrTOsToContext() {
        String schemaName = "testSchema";
        String value = "testValue";
        Collection<Attr> attrs = new ArrayList<>();
        Attr attr = new Attr.Builder(schemaName).build();
        attrs.add(attr);

        JexlUtils.addAttrsToContext(attrs, context);
        verify(context).set(schemaName, StringUtils.EMPTY);

        attr = new Attr.Builder(schemaName).value(value).build();
        attrs.clear();
        attrs.add(attr);

        JexlUtils.addAttrsToContext(attrs, context);
        verify(context).set(schemaName, value);
    }

    @Test
    public void addPlainAttrsToContext(final @Mock Collection<PlainAttr> attrs) {
        JexlUtils.addPlainAttrsToContext(attrs, context);
        verify(context, times(0)).set(anyString(), any());
    }

    @Test
    public void addDerAttrsToContext(
            final @Mock DerAttrHandler derAttrHandler,
            final @Mock Any any,
            final @Mock DerSchema derSchema) {

        String expression = null;

        Map<DerSchema, String> derAttrs = new HashMap<>();
        derAttrs.put(derSchema, expression);

        when(derAttrHandler.getValues(any())).thenReturn(derAttrs);
        JexlUtils.addDerAttrsToContext(any, derAttrHandler, context);
        verify(context).set(derAttrs.get(derSchema), expression);
    }

    @Test
    public void evaluateMandatoryCondition(
            final @Mock DerAttrHandler derAttrHandler,
            final @Mock Any any,
            final @Mock DerSchema derSchema,
            final @Mock Collection<PlainAttr> plainAttrs) {

        String expression = null;

        Map<DerSchema, String> derAttrs = new HashMap<>();
        derAttrs.put(derSchema, expression);

        when(any.getPlainAttrs()).thenReturn(new ArrayList<>());
        when(derAttrHandler.getValues(any())).thenReturn(derAttrs);

        assertTrue(JexlUtils.evaluateMandatoryCondition("true", any, derAttrHandler));
        assertFalse(JexlUtils.evaluateMandatoryCondition("false", any, derAttrHandler));
    }
}
