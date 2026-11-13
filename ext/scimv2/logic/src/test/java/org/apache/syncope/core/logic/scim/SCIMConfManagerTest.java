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
package org.apache.syncope.core.logic.scim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.MediaType;
import java.util.Base64;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyConf;
import org.apache.syncope.common.lib.scim.SCIMItem;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.core.logic.SchemaLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SCIMConfManagerTest {

    private ConfParamOps confParamOps;

    private SchemaLogic schemaLogic;

    private SCIMConfManager manager;

    @BeforeEach
    void setup() {
        confParamOps = mock(ConfParamOps.class);
        schemaLogic = mock(SchemaLogic.class);
        when(confParamOps.get(anyString(), eq(SCIMConf.KEY), isNull(), eq(String.class))).thenReturn(null);
        when(schemaLogic.read(SchemaType.PLAIN, SCIMConf.KEY)).thenThrow(new NotFoundException("not found"));

        manager = new SCIMConfManager(confParamOps, schemaLogic);
    }

    private static String serialize(final SCIMConf conf) {
        return Base64.getEncoder().encodeToString(POJOHelper.serialize(conf).getBytes());
    }

    @Test
    void getGeneratesAndStoresDefault() {
        SCIMConf conf = manager.get();

        assertNotNull(conf);
        assertNotNull(conf.getGeneralConf());
        assertNotNull(conf.getGeneralConf().getLastChangeDate());

        ArgumentCaptor<PlainSchemaTO> schemaCaptor = ArgumentCaptor.forClass(PlainSchemaTO.class);
        verify(schemaLogic).create(eq(SchemaType.PLAIN), schemaCaptor.capture());
        assertEquals(SCIMConf.KEY, schemaCaptor.getValue().getKey());
        assertEquals(AttrSchemaType.Binary, schemaCaptor.getValue().getType());
        assertEquals(MediaType.APPLICATION_JSON, schemaCaptor.getValue().getMimeType());

        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(confParamOps).set(anyString(), eq(SCIMConf.KEY), valueCaptor.capture());

        SCIMConf stored = POJOHelper.deserialize(
                new String(Base64.getDecoder().decode(valueCaptor.getValue().toString())), SCIMConf.class);
        assertNotNull(stored.getGeneralConf());
    }

    @Test
    void getReadsStoredConfiguration() {
        SCIMConf conf = new SCIMConf();
        conf.setUserConf(new SCIMUserConf());
        conf.getUserConf().setDisplayName("displayName");
        when(confParamOps.get(anyString(), eq(SCIMConf.KEY), isNull(), eq(String.class))).
                thenReturn(serialize(conf));

        SCIMConf actual = manager.get();

        assertNotNull(actual.getUserConf());
        assertEquals("displayName", actual.getUserConf().getDisplayName());
        verify(schemaLogic, never()).create(any(), any());
        verify(confParamOps, never()).set(anyString(), anyString(), any());
    }

    @Test
    void getRevertsToDefaultOnUnreadableConfiguration() {
        when(confParamOps.get(anyString(), eq(SCIMConf.KEY), isNull(), eq(String.class))).
                thenReturn("this-is-not-base64");

        SCIMConf conf = manager.get();

        assertNotNull(conf);
        assertNotNull(conf.getGeneralConf());
        verify(confParamOps).set(anyString(), eq(SCIMConf.KEY), any());
    }

    private static SCIMConf confWithExtensionUser() {
        SCIMConf conf = new SCIMConf();

        SCIMItem item = new SCIMItem();
        item.setIntAttrName("surname");
        item.setExtAttrName("surname");
        item.setMandatoryCondition("true");

        SCIMExtensionAnyConf extensionUserConf = new SCIMExtensionAnyConf();
        extensionUserConf.add(item);
        conf.setExtensionUserConf(extensionUserConf);

        return conf;
    }

    private static PlainSchemaTO schema(final String key) {
        PlainSchemaTO schema = new PlainSchemaTO();
        schema.setKey(key);
        schema.setMandatoryCondition("true");
        schema.setMultivalue(false);
        schema.setReadonly(false);
        schema.setUniqueConstraint(false);
        schema.setType(AttrSchemaType.String);
        return schema;
    }

    @Test
    void setValidatesAndStores() {
        when(schemaLogic.read(SchemaType.PLAIN, "surname")).thenReturn(schema("surname"));

        manager.set(confWithExtensionUser());

        verify(confParamOps).set(anyString(), eq(SCIMConf.KEY), any());
    }

    @Test
    void setRejectsInvalidMapping() {
        PlainSchemaTO schema = schema("surname");
        schema.setMandatoryCondition("false");
        when(schemaLogic.read(SchemaType.PLAIN, "surname")).thenReturn(schema);

        SyncopeClientException ex = assertThrows(
                SyncopeClientException.class, () -> manager.set(confWithExtensionUser()));
        assertEquals(ClientExceptionType.InvalidMapping, ex.getType());
        assertEquals(1, ex.getElements().size());

        verify(confParamOps, never()).set(anyString(), anyString(), any());
    }

    @Test
    void setRejectsMissingSchema() {
        when(schemaLogic.read(SchemaType.PLAIN, "surname")).thenThrow(new NotFoundException("not found"));

        SyncopeClientException ex = assertThrows(
                SyncopeClientException.class, () -> manager.set(confWithExtensionUser()));
        assertEquals(ClientExceptionType.InvalidMapping, ex.getType());
        assertEquals(1, ex.getElements().size());

        verify(confParamOps, never()).set(anyString(), anyString(), any());
    }
}
