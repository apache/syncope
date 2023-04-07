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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;

public class SCIMSearchRequest extends SCIMBean {

    private static final long serialVersionUID = 5759362928661983543L;

    private final List<String> schemas = List.of(Resource.SearchRequest.schema());

    private final List<String> attributes = new ArrayList<>();

    private final List<String> excludedAttributes = new ArrayList<>();

    private final String filter;

    private final String sortBy;

    private final SortOrder sortOrder;

    private final Integer startIndex;

    private final Integer count;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SCIMSearchRequest(
            @JsonProperty("filter") final String filter,
            @JsonProperty("sortBy") final String sortBy,
            @JsonProperty("sortOrder") final SortOrder sortOrder,
            @JsonProperty("startIndex") final Integer startIndex,
            @JsonProperty("count") final Integer count) {

        this.filter = filter;
        this.sortBy = sortBy;
        this.sortOrder = sortOrder;
        this.startIndex = startIndex;
        this.count = count;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public List<String> getExcludedAttributes() {
        return excludedAttributes;
    }

    public String getFilter() {
        return filter;
    }

    public String getSortBy() {
        return sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public int getStartIndex() {
        return Optional.ofNullable(startIndex).orElse(1);
    }

    public int getCount() {
        return Optional.ofNullable(count).orElse(25);
    }
}
