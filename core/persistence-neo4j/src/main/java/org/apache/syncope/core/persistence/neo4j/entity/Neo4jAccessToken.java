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
package org.apache.syncope.core.persistence.neo4j.entity;

import java.time.OffsetDateTime;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.springframework.data.neo4j.core.schema.Node;

@Node(Neo4jAccessToken.NODE)
public class Neo4jAccessToken extends AbstractProvidedKeyNode implements AccessToken {

    private static final long serialVersionUID = 6839211057212932936L;

    public static final String NODE = "AccessToken";

    private String body;

    private OffsetDateTime expirationTime;

    private String owner;

    private byte[] authorities;

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public void setBody(final String body) {
        this.body = body;
    }

    @Override
    public OffsetDateTime getExpirationTime() {
        return expirationTime;
    }

    @Override
    public void setExpirationTime(final OffsetDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(final String owner) {
        this.owner = owner;
    }

    @Override
    public byte[] getAuthorities() {
        return authorities;
    }

    @Override
    public void setAuthorities(final byte[] authorities) {
        this.authorities = ArrayUtils.clone(authorities);
    }
}
