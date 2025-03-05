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

import org.apache.syncope.ext.openfga.client.api.AuthorizationModelsApi;
import org.apache.syncope.ext.openfga.client.api.RelationshipQueriesApi;
import org.apache.syncope.ext.openfga.client.api.RelationshipTuplesApi;
import org.apache.syncope.ext.openfga.client.api.StoresApi;
import org.apache.syncope.ext.openfga.client.model.AuthorizationModel;
import org.apache.syncope.ext.openfga.client.model.CheckRequest;
import org.apache.syncope.ext.openfga.client.model.CheckResponse;
import org.apache.syncope.ext.openfga.client.model.CreateStoreRequest;
import org.apache.syncope.ext.openfga.client.model.CreateStoreResponse;
import org.apache.syncope.ext.openfga.client.model.GetStoreResponse;
import org.apache.syncope.ext.openfga.client.model.ListStoresResponse;
import org.apache.syncope.ext.openfga.client.model.ReadAuthorizationModelsResponse;
import org.apache.syncope.ext.openfga.client.model.ReadRequest;
import org.apache.syncope.ext.openfga.client.model.ReadResponse;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelRequest;
import org.apache.syncope.ext.openfga.client.model.WriteAuthorizationModelResponse;
import org.apache.syncope.ext.openfga.client.model.WriteRequest;

public class OpenFGAClient {

    protected final StoresApi storesApi;

    protected final AuthorizationModelsApi authorizationModelsApi;

    protected final RelationshipTuplesApi relationshipTuplesApi;

    protected final RelationshipQueriesApi relationshipQueriesApi;

    protected String storeId;

    protected String authorizationModelId;

    public OpenFGAClient(final ApiClient apiClient) {
        this.storesApi = new StoresApi(apiClient);
        this.authorizationModelsApi = new AuthorizationModelsApi(apiClient);
        this.relationshipTuplesApi = new RelationshipTuplesApi(apiClient);
        this.relationshipQueriesApi = new RelationshipQueriesApi(apiClient);
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(final String storeId) {
        this.storeId = storeId;
    }

    public String getAuthorizationModelId() {
        return authorizationModelId;
    }

    public void setAuthorizationModelId(final String authorizationModelId) {
        this.authorizationModelId = authorizationModelId;
    }

    public GetStoreResponse getStore() throws ApiException {
        return storesApi.getStore(storeId);
    }

    public ListStoresResponse listStores() throws ApiException {
        return storesApi.listStores(null, null, null);
    }

    public ApiResponse<ListStoresResponse> listStoresWithHttpInfo() throws ApiException {
        return storesApi.listStoresWithHttpInfo(null, null, null);
    }

    public CreateStoreResponse createStore(final CreateStoreRequest request) throws ApiException {
        return storesApi.createStore(request);
    }

    public WriteAuthorizationModelResponse writeAuthorizationModel(
            final WriteAuthorizationModelRequest request) throws ApiException {

        return authorizationModelsApi.writeAuthorizationModel(storeId, request);
    }

    public ReadAuthorizationModelsResponse readAuthorizationModels() throws ApiException {
        return authorizationModelsApi.readAuthorizationModels(storeId, null, null);
    }

    public AuthorizationModel readLatestAuthorizationModel() throws ApiException {
        return readAuthorizationModels().getAuthorizationModels().get(0);
    }

    public ReadResponse read(final ReadRequest request) throws ApiException {
        return relationshipTuplesApi.read(storeId, request);
    }

    public void write(final WriteRequest request) throws ApiException {
        relationshipTuplesApi.write(storeId, request);
    }

    public CheckResponse check(final CheckRequest request) throws ApiException {
        return relationshipQueriesApi.check(storeId, request);
    }
}
