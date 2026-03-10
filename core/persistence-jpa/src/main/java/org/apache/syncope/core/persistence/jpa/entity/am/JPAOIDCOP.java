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
package org.apache.syncope.core.persistence.jpa.entity.am;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOP;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAOIDCOP.TABLE)
public class JPAOIDCOP extends AbstractGeneratedKeyEntity implements OIDCOP {

    private static final long serialVersionUID = 47352617217394093L;

    public static final String TABLE = "OIDCOP";

    protected static final TypeReference<HashMap<String, Set<String>>> CUSTOMSCOPES_TYPEREF =
            new TypeReference<HashMap<String, Set<String>>>() {
    };

    @Column(nullable = false)
    @Lob
    private String jwks;

    @Lob
    private String customScopes;

    @Transient
    private Map<String, Set<String>> customScopesMap = new HashMap<>();

    @Override
    public String getJWKS() {
        return jwks;
    }

    @Override
    public void setJWKS(final String jwks) {
        this.jwks = jwks;
    }

    @Override
    public Map<String, Set<String>> getCustomScopes() {
        return customScopesMap;
    }

    protected void json2map(final boolean clearFirst) {
        if (clearFirst) {
            getCustomScopes().clear();
        }
        if (customScopes != null) {
            getCustomScopes().putAll(POJOHelper.deserialize(customScopes, CUSTOMSCOPES_TYPEREF));
        }
    }

    @PostLoad
    public void postLoad() {
        json2map(false);
    }

    @PostPersist
    @PostUpdate
    public void postSave() {
        json2map(true);
    }

    @PrePersist
    @PreUpdate
    public void map2json() {
        customScopes = POJOHelper.serialize(getCustomScopes());
    }
}
