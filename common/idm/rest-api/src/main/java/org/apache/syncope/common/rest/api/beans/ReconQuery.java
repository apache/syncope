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

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.common.rest.api.service.JAXRSService;

public class ReconQuery implements Serializable {

    private static final long serialVersionUID = -3797021989909461591L;

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

        public Builder fiql(final String fiql) {
            instance.setFiql(fiql);
            return this;
        }

        public Builder moreAttrsToGet(final String... moreAttrsToGet) {
            if (moreAttrsToGet != null) {
                Set<String> matg = Optional.ofNullable(instance.getMoreAttrsToGet()).orElseGet(HashSet::new);
                matg.addAll(Stream.of(moreAttrsToGet).collect(Collectors.toSet()));
                instance.setMoreAttrsToGet(matg);
            }
            return this;
        }

        public Builder moreAttrsToGet(final Collection<String> moreAttrsToGet) {
            if (moreAttrsToGet != null) {
                Set<String> matg = Optional.ofNullable(instance.getMoreAttrsToGet()).orElseGet(HashSet::new);
                matg.addAll(moreAttrsToGet);
                instance.setMoreAttrsToGet(matg);
            }
            return this;
        }

        public ReconQuery build() {
            return instance;
        }
    }

    private String anyTypeKey;

    private String anyKey;

    private String resourceKey;

    private String fiql;

    private Set<String> moreAttrsToGet;

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

    public String getFiql() {
        return fiql;
    }

    @QueryParam(JAXRSService.PARAM_FIQL)
    public void setFiql(final String fiql) {
        this.fiql = fiql;
    }

    public Set<String> getMoreAttrsToGet() {
        return moreAttrsToGet;
    }

    @QueryParam("moreAttrsToGet")
    public void setMoreAttrsToGet(final Set<String> moreAttrsToGet) {
        this.moreAttrsToGet = moreAttrsToGet;
    }
}
