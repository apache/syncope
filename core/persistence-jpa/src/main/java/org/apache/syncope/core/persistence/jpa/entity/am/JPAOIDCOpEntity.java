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

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.persistence.jpa.converters.String2SetOfStringMapConverter;
import org.apache.syncope.core.persistence.jpa.entity.AbstractGeneratedKeyEntity;

@Entity
@Table(name = JPAOIDCOpEntity.TABLE)
public class JPAOIDCOpEntity extends AbstractGeneratedKeyEntity implements OIDCOpEntity {

    private static final long serialVersionUID = 47352617217394093L;

    public static final String TABLE = "OIDCOpEntity";

    @Column(nullable = false)
    @Lob
    private String jwks;

    @Convert(converter = String2SetOfStringMapConverter.class)
    @Lob
    private Map<String, Set<String>> customScopes = new HashMap<>();

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
        return customScopes;
    }
}
