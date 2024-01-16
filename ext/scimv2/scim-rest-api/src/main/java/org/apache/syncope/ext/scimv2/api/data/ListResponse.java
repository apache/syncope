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
import org.apache.syncope.ext.scimv2.api.type.Resource;

public class ListResponse<R extends SCIMResource> extends SCIMBean {

    private static final long serialVersionUID = -776611610457583160L;

    private final List<String> schemas = List.of(Resource.ListResponse.schema());

    private final long totalResults;

    @JsonProperty("Resources")
    private final List<R> resources = new ArrayList<>();

    private final long startIndex;

    private final long itemsPerPage;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ListResponse(
            @JsonProperty("totalResults") final long totalResults,
            @JsonProperty("startIndex") final long startIndex,
            @JsonProperty("itemsPerPage") final long itemsPerPage) {

        this.totalResults = totalResults;
        this.startIndex = startIndex;
        this.itemsPerPage = itemsPerPage;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public long getTotalResults() {
        return totalResults;
    }

    public List<R> getResources() {
        return resources;
    }

    public long getStartIndex() {
        return startIndex;
    }

    public long getItemsPerPage() {
        return itemsPerPage;
    }
}
