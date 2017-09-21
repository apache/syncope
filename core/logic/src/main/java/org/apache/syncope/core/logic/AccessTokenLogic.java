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

import static org.apache.syncope.core.logic.AbstractLogic.LOG;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.Encryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenLogic extends AbstractTransactionalLogic<AccessTokenTO> {

    private static final Encryptor ENCRYPTOR = Encryptor.getInstance();

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Autowired
    private AccessTokenDataBinder binder;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    private byte[] getAuthorities() {
        byte[] authorities = null;
        try {
            authorities = ENCRYPTOR.encode(POJOHelper.serialize(
                    AuthContextUtils.getAuthorities()), CipherAlgorithm.AES).
                    getBytes();
        } catch (Exception e) {
            LOG.error("Could not fetch authorities", e);
        }

        return authorities;
    }

    @PreAuthorize("isAuthenticated()")
    public Pair<String, Date> login() {
        if (anonymousUser.equals(AuthContextUtils.getUsername())) {
            throw new IllegalArgumentException(anonymousUser + " cannot be granted an access token");
        }

        return binder.create(
                AuthContextUtils.getUsername(),
                Collections.<String, Object>emptyMap(),
                getAuthorities(),
                false);
    }

    @PreAuthorize("isAuthenticated()")
    public Pair<String, Date> refresh() {
        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
        if (accessToken == null) {
            throw new NotFoundException("AccessToken for " + AuthContextUtils.getUsername());
        }

        return binder.update(accessToken, getAuthorities());
    }

    @PreAuthorize("isAuthenticated()")
    public void logout() {
        AccessToken accessToken = accessTokenDAO.findByOwner(AuthContextUtils.getUsername());
        if (accessToken == null) {
            throw new NotFoundException("AccessToken for " + AuthContextUtils.getUsername());
        }

        delete(accessToken.getKey());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ACCESS_TOKEN_LIST + "')")
    public Pair<Integer, List<AccessTokenTO>> list(
            final int page,
            final int size,
            final List<OrderByClause> orderByClauses) {

        Integer count = accessTokenDAO.count();

        List<AccessTokenTO> result = accessTokenDAO.findAll(page, size, orderByClauses).stream().
                map(accessToken -> binder.getAccessTokenTO(accessToken)).collect(Collectors.toList());

        return Pair.of(count, result);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.ACCESS_TOKEN_DELETE + "')")
    public void delete(final String key) {
        accessTokenDAO.delete(key);
    }

    @Override
    protected AccessTokenTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }

}
