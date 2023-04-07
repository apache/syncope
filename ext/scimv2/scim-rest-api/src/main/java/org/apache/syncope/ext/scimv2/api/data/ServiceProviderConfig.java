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
package org.apache.syncope.ext.scimv2.api.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.ext.scimv2.api.type.Resource;

@JsonPropertyOrder(
        { "schemas", "patch", "bulk", "filter", "changePassword", "sort", "etag", "authenticationSchemes", "meta" })
public class ServiceProviderConfig extends SCIMBean {

    private static final long serialVersionUID = 1027738509789460252L;

    private final List<String> schemas = List.of(Resource.ServiceProviderConfig.schema());

    private final Meta meta;

    private final ConfigurationOption patch;

    private final BulkConfigurationOption bulk;

    private final FilterConfigurationOption filter;

    private final ConfigurationOption changePassword;

    private final ConfigurationOption sort;

    private final ConfigurationOption etag;

    private final List<AuthenticationScheme> authenticationSchemes = new ArrayList<>();

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ServiceProviderConfig(
            @JsonProperty("meta") final Meta meta,
            @JsonProperty("patch") final ConfigurationOption patch,
            @JsonProperty("bulk") final BulkConfigurationOption bulk,
            @JsonProperty("filter") final FilterConfigurationOption filter,
            @JsonProperty("changePassword") final ConfigurationOption changePassword,
            @JsonProperty("sort") final ConfigurationOption sort,
            @JsonProperty("etag") final ConfigurationOption etag) {

        this.meta = meta;
        this.patch = patch;
        this.bulk = bulk;
        this.filter = filter;
        this.changePassword = changePassword;
        this.sort = sort;
        this.etag = etag;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public Meta getMeta() {
        return meta;
    }

    public ConfigurationOption getPatch() {
        return patch;
    }

    public BulkConfigurationOption getBulk() {
        return bulk;
    }

    public FilterConfigurationOption getFilter() {
        return filter;
    }

    public ConfigurationOption getChangePassword() {
        return changePassword;
    }

    public ConfigurationOption getSort() {
        return sort;
    }

    public ConfigurationOption getEtag() {
        return etag;
    }

    public List<AuthenticationScheme> getAuthenticationSchemes() {
        return authenticationSchemes;
    }
}
