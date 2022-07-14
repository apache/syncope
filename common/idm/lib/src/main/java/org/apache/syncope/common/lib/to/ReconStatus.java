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
package org.apache.syncope.common.lib.to;

import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MatchType;

/**
 * Reconciliation status.
 */
public class ReconStatus implements BaseBean {

    private static final long serialVersionUID = -8516345256596521490L;

    private AnyTypeKind anyTypeKind;

    private String anyKey;

    private String realm;

    private MatchType matchType;

    private ConnObject onSyncope;

    private ConnObject onResource;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public AnyTypeKind getAnyTypeKind() {
        return anyTypeKind;
    }

    public void setAnyTypeKind(final AnyTypeKind anyTypeKind) {
        this.anyTypeKind = anyTypeKind;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getAnyKey() {
        return anyKey;
    }

    public void setAnyKey(final String anyKey) {
        this.anyKey = anyKey;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public String getRealm() {
        return realm;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(final MatchType matchType) {
        this.matchType = matchType;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public ConnObject getOnSyncope() {
        return onSyncope;
    }

    public void setOnSyncope(final ConnObject onSyncope) {
        this.onSyncope = onSyncope;
    }

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    public ConnObject getOnResource() {
        return onResource;
    }

    public void setOnResource(final ConnObject onResource) {
        this.onResource = onResource;
    }
}
