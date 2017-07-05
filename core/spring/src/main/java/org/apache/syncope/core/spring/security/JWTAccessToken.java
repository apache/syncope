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
package org.apache.syncope.core.spring.security;

import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

/**
 * Convenience {@link AccessToken} implementation wrapping the received JWT, for usage with custom
 * {@link JWTSSOProvider#resolve} implementations.
 */
public class JWTAccessToken implements AccessToken {

    private static final long serialVersionUID = -3824671946137458487L;

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    private final String key = UUID.randomUUID().toString();

    private final JwtClaims claims;

    private byte[] authorities;

    public JWTAccessToken(final JwtClaims claims) throws Exception {
        this.claims = claims;
        this.authorities = ENCRYPTOR.encode(
                POJOHelper.serialize(Collections.emptySet()), CipherAlgorithm.AES).
                getBytes();
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getBody() {
        return null;
    }

    @Override
    public Date getExpiryTime() {
        return new Date(claims.getExpiryTime());
    }

    @Override
    public String getOwner() {
        return claims.getSubject();
    }

    @Override
    public byte[] getAuthorities() {
        return authorities;
    }

    @Override
    public void setKey(final String key) {
        // nothing to do
    }

    @Override
    public void setBody(final String body) {
        // nothing to do
    }

    @Override
    public void setExpiryTime(final Date expiryTime) {
        // nothing to do
    }

    @Override
    public void setOwner(final String owner) {
        // nothing to do
    }

    @Override
    public void setAuthorities(final byte[] authorities) {
        this.authorities = ArrayUtils.clone(authorities);
    }

}
