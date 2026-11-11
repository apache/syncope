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
package org.apache.syncope.ext.opensearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.core.Ordered;

@ExtendWith(MockitoExtension.class)
public class OpenSearchIndexLoaderTest {

    @Mock
    private OpenSearchIndexManager indexManager;

    private OpenSearchIndexLoader loader;

    @BeforeEach
    public void setupLoader() {
        loader = new OpenSearchIndexLoader(indexManager);
    }

    @Test
    public void order() {
        assertThat(loader.getOrder()).isEqualTo(Ordered.LOWEST_PRECEDENCE);
    }

    @Test
    public void loadMissingIndexes() throws Exception {
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.USER)).thenReturn(false);
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.GROUP)).thenReturn(false);
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.ANY_OBJECT)).thenReturn(false);
        when(indexManager.existsRealmIndex("Master")).thenReturn(false);
        when(indexManager.existsAuditIndex("Master")).thenReturn(false);

        IndexSettings settings = new IndexSettings.Builder().numberOfShards(1).numberOfReplicas(1).build();
        TypeMapping anyMapping = new TypeMapping.Builder().build();
        TypeMapping realmMapping = new TypeMapping.Builder().build();
        TypeMapping auditMapping = new TypeMapping.Builder().build();
        when(indexManager.defaultSettings()).thenReturn(settings);
        when(indexManager.defaultAnyMapping()).thenReturn(anyMapping);
        when(indexManager.defaultRealmMapping()).thenReturn(realmMapping);
        when(indexManager.defaultAuditMapping()).thenReturn(auditMapping);

        loader.load("Master");

        verify(indexManager).createAnyIndex("Master", AnyTypeKind.USER, settings, anyMapping);
        verify(indexManager).createAnyIndex("Master", AnyTypeKind.GROUP, settings, anyMapping);
        verify(indexManager).createAnyIndex("Master", AnyTypeKind.ANY_OBJECT, settings, anyMapping);
        verify(indexManager).createRealmIndex("Master", settings, realmMapping);
        verify(indexManager).createAuditIndex("Master", settings, auditMapping);
    }

    @Test
    public void loadExistingIndexes() throws Exception {
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.USER)).thenReturn(true);
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.GROUP)).thenReturn(true);
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.ANY_OBJECT)).thenReturn(true);
        when(indexManager.existsRealmIndex("Master")).thenReturn(true);
        when(indexManager.existsAuditIndex("Master")).thenReturn(true);

        loader.load("Master");

        verify(indexManager, never()).createAnyIndex(anyString(), any(), any(), any());
        verify(indexManager, never()).createRealmIndex(anyString(), any(), any());
        verify(indexManager, never()).createAuditIndex(anyString(), any(), any());
    }

    @Test
    public void loadFailureIsSwallowed() throws Exception {
        when(indexManager.existsAnyIndex("Master", AnyTypeKind.USER)).thenThrow(new IOException("boom"));

        assertDoesNotThrow(() -> loader.load("Master"));

        verify(indexManager, never()).createAnyIndex(anyString(), any(), any(), any());
        verify(indexManager, never()).createRealmIndex(anyString(), any(), any());
        verify(indexManager, never()).createAuditIndex(anyString(), any(), any());
    }
}
