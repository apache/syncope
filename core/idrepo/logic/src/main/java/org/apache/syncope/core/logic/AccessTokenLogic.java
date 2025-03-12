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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

public class AccessTokenLogic extends AbstractTransactionalLogic<AccessTokenTO> {

    protected final SecurityProperties securityProperties;

    protected final EncryptorManager encryptorManager;

    protected final AccessTokenDataBinder binder;

    protected final AccessTokenDAO accessTokenDAO;

    public AccessTokenLogic(
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager,
            final AccessTokenDataBinder binder,
            final AccessTokenDAO accessTokenDAO) {

        this.securityProperties = securityProperties;
        this.encryptorManager = encryptorManager;
        this.binder = binder;
        this.accessTokenDAO = accessTokenDAO;
    }

    protected byte[] getAuthorities() {
        byte[] authorities = null;
        try {
            authorities = encryptorManager.getInstance().encode(POJOHelper.serialize(
                    AuthContextUtils.getAuthorities()), CipherAlgorithm.AES).
                    getBytes();
        } catch (Exception e) {
            LOG.error("Could not fetch authorities", e);
        }

        return authorities;
    }

    @PreAuthorize("isAuthenticated()")
    public Pair<String, OffsetDateTime> login() {
        if (securityProperties.getAnonymousUser().equals(AuthContextUtils.getUsername())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add(securityProperties.getAnonymousUser() + " cannot be granted an access token");
            throw sce;
        }

        return binder.create(
                AuthContextUtils.getUsername(),
                Collections.emptyMap(),
                getAuthorities(),
                false);
    }

    @PreAuthorize("isAuthenticated()")
    public Pair<String, OffsetDateTime> refresh() {
        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername()).
                orElseThrow(() -> new NotFoundException("AccessToken for " + AuthContextUtils.getUsername()));

        return binder.update(accessToken, getAuthorities());
    }

    @PreAuthorize("isAuthenticated()")
    public void logout() {
        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername()).
                orElseThrow(() -> new NotFoundException("AccessToken for " + AuthContextUtils.getUsername()));

        delete(accessToken.getKey());
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ACCESS_TOKEN_LIST + "')")
    public Page<AccessTokenTO> list(final Pageable pageable) {
        return accessTokenDAO.findAll(pageable).map(binder::getAccessTokenTO);
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ACCESS_TOKEN_DELETE + "')")
    public void delete(final String key) {
        accessTokenDAO.deleteById(key);
    }

    @Override
    protected AccessTokenTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
