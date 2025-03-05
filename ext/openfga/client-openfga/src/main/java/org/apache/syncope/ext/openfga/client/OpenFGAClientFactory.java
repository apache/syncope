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
package org.apache.syncope.ext.openfga.client;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.ext.openfga.client.model.AuthorizationModel;
import org.apache.syncope.ext.openfga.client.model.CreateStoreRequest;
import org.apache.syncope.ext.openfga.client.model.Metadata;
import org.apache.syncope.ext.openfga.client.model.RelationMetadata;
import org.apache.syncope.ext.openfga.client.model.RelationReference;
import org.apache.syncope.ext.openfga.client.model.Store;
import org.apache.syncope.ext.openfga.client.model.TypeDefinition;
import org.apache.syncope.ext.openfga.client.model.Userset;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelRequest;

public class OpenFGAClientFactory {

    public static final String MEMBERSHIP_RELATION = "member";

    public static final String SCHEMA_VERSION = "1.1";

    protected static String storeId(final String domain) {
        return StringUtils.rightPad(domain.toUpperCase(), 26, '0');
    }

    protected final ApiClient apiClient;

    protected final OpenFGAProperties properties;

    public OpenFGAClientFactory(final ApiClient openFgaApiClient, final OpenFGAProperties properties) {
        this.apiClient = openFgaApiClient;
        this.properties = properties;
    }

    public String getBaseUri() {
        return apiClient.getBaseUri();
    }

    protected final Map<String, OpenFGAClient> clients = new ConcurrentHashMap<>();

    public void initAuthorizationModel(
            final OpenFGAClient client,
            final String domain,
            final boolean force) throws Exception {

        String storeId = client.listStores().getStores().stream().
                filter(store -> domain.equals(store.getName())).findFirst().
                map(Store::getId).orElse(null);
        if (storeId == null) {
            storeId = client.createStore(new CreateStoreRequest().name(domain)).getId();
        }
        client.setStoreId(storeId);

        WriteAuthorizationModelRequest request = new WriteAuthorizationModelRequest().
                schemaVersion(SCHEMA_VERSION).
                addTypeDefinitionsItem(
                        new TypeDefinition().type(AnyTypeKind.USER.name()).relations(Map.of())).
                addTypeDefinitionsItem(new TypeDefinition().type(AnyTypeKind.GROUP.name()).
                        putRelationsItem(MEMBERSHIP_RELATION, new Userset()._this(Map.of())).
                        metadata(new Metadata().putRelationsItem(
                                MEMBERSHIP_RELATION,
                                new RelationMetadata().addDirectlyRelatedUserTypesItem(
                                        new RelationReference().type(AnyTypeKind.USER.name())))));
        if (force) {
            client.setAuthorizationModelId(
                    client.writeAuthorizationModel(request).getAuthorizationModelId());
        } else {
            List<AuthorizationModel> models = client.readAuthorizationModels().getAuthorizationModels();
            if (models.isEmpty()) {
                client.setAuthorizationModelId(client.writeAuthorizationModel(request).getAuthorizationModelId());
            } else {
                client.setAuthorizationModelId(models.get(0).getId());
            }
        }
    }

    public OpenFGAClient get(final String domain) {
        return clients.computeIfAbsent(domain, d -> {
            try {
                OpenFGAClient client = new OpenFGAClient(apiClient);

                initAuthorizationModel(client, domain, false);

                return client;
            } catch (Exception e) {
                throw new IllegalStateException("Could not init OpenFga client for domain " + d, e);
            }
        });
    }

    public void remove(final String domain) {
        clients.remove(domain);
    }
}
