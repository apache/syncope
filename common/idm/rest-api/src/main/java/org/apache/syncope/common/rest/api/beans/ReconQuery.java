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
package org.apache.syncope.common.rest.api.beans;

import javax.validation.constraints.NotNull;
import javax.ws.rs.QueryParam;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class ReconQuery {

    public static class Builder {

        private final ReconQuery instance;

        public Builder(final String anyTypeKey, final String resourceKey) {
            instance = new ReconQuery();
            instance.setAnyTypeKey(anyTypeKey);
            instance.setResourceKey(resourceKey);
        }

        public Builder anyKey(final String anyKey) {
            instance.setAnyKey(anyKey);
            return this;
        }

        public Builder connObjectKeyValue(final String connObjectKeyValue) {
            instance.setConnObjectKeyValue(connObjectKeyValue);
            return this;
        }

        public ReconQuery build() {
            return instance;
        }
    }

    private String anyTypeKey;

    private String anyKey;

    private String resourceKey;

    private String connObjectKeyValue;

    public String getAnyTypeKey() {
        return anyTypeKey;
    }

    @NotNull
    @QueryParam(JAXRSService.PARAM_ANYTYPEKEY)
    public void setAnyTypeKey(final String anyTypeKey) {
        this.anyTypeKey = anyTypeKey;
    }

    public String getAnyKey() {
        return anyKey;
    }

    @QueryParam("anyKey")
    public void setAnyKey(final String anyKey) {
        this.anyKey = anyKey;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    @NotNull
    @QueryParam("resourceKey")
    public void setResourceKey(final String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String getConnObjectKeyValue() {
        return connObjectKeyValue;
    }

    @QueryParam("connObjectKeyValue")
    public void setConnObjectKeyValue(final String connObjectKeyValue) {
        this.connObjectKeyValue = connObjectKeyValue;
    }
}
