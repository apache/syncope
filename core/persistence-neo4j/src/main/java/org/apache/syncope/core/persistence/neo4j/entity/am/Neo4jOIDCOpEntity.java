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
package org.apache.syncope.core.persistence.neo4j.entity.am;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.core.persistence.api.entity.am.OIDCOpEntity;
import org.apache.syncope.core.persistence.neo4j.converters.String2SetOfStringMapConverter;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractGeneratedKeyNode;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jOIDCOpEntity.NODE)
public class Neo4jOIDCOpEntity extends AbstractGeneratedKeyNode implements OIDCOpEntity {

    private static final long serialVersionUID = 47352617217394093L;

    public static final String NODE = "OIDCOpEntity";

    @NotNull
    private byte[] jwks;

    @ConvertWith(converter = String2SetOfStringMapConverter.class)
    private Map<String, Set<String>> customScopes = new HashMap<>();

    @Override
    public byte[] getJWKS() {
        return jwks;
    }

    @Override
    public void setJWKS(final byte[] jwks) {
        this.jwks = ArrayUtils.clone(jwks);
    }

    @Override
    public Map<String, Set<String>> getCustomScopes() {
        return customScopes;
    }
}
