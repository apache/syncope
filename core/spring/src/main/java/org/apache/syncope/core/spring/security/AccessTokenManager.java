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

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.cache.Cache;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.springframework.transaction.annotation.Transactional;

public class AccessTokenManager {

    public static final String CACHE = "AuthoritiesCache";

    protected static String key(final String domain, final String username, final String delegationKey) {
        return domain + '|' + username + (delegationKey == null ? "" : "|" + delegationKey);
    }

    protected final AccessTokenDAO accessTokenDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final Cache<String, Set<SyncopeGrantedAuthority>> authoritiesCache;

    public AccessTokenManager(
            final AccessTokenDAO accessTokenDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final Cache<String, Set<SyncopeGrantedAuthority>> authoritiesCache) {

        this.accessTokenDAO = accessTokenDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.authoritiesCache = authoritiesCache;
    }

    public Set<SyncopeGrantedAuthority> getAuthorities(
            final String domain,
            final String username,
            final String delegationKey,
            final Supplier<Set<SyncopeGrantedAuthority>> loader) {

        String key = key(domain, username, delegationKey);
        Set<SyncopeGrantedAuthority> authorities = authoritiesCache.get(key);
        if (authorities == null) {
            authorities = loader.get();
            authoritiesCache.putIfAbsent(key, authorities);
        }
        return authorities;
    }

    @Transactional
    public void invalidate(final String username) {
        accessTokenDAO.deleteByOwner(username);

        evict(username);
    }

    public void evict(final String username) {
        authoritiesCache.remove(key(AuthContextUtils.getDomain(), username, null));
    }

    @Transactional
    public void evictForMembers(final String group) {
        groupDAO.findUMembers(group).stream().
                map(userDAO::findById).flatMap(Optional::stream).
                forEach(m -> evict(m.getUsername()));
    }
}
