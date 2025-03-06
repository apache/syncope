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

import java.util.List;
import java.util.Optional;

public abstract class SCIMResource extends SCIMBean {

    private static final long serialVersionUID = -8465880682458920021L;

    private final String id;

    private String externalId;

    private String displayName;

    private final List<String> schemas;

    private final Meta meta;

    protected SCIMResource(final String id, final List<String> schemas, final Meta meta) {
        this.id = id;
        this.schemas = Optional.ofNullable(schemas).orElseGet(List::of);
        this.meta = meta;
    }

    public String getId() {
        return id;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }
}
