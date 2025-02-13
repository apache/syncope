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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface UserRepoExt extends AnyRepoExt<User> {

    String CACHE = "userCache";

    Optional<? extends User> findByToken(String token);

    List<User> findBySecurityQuestion(SecurityQuestion securityQuestion);

    void securityChecks(Set<String> authRealms, String key, String realm, Collection<String> groups);

    Map<String, Long> countByRealm();

    Map<String, Long> countByStatus();

    UMembership findMembership(String key);

    void deleteMembership(UMembership membership);

    List<Role> findDynRoles(String key);

    Collection<Role> findAllRoles(User user);

    List<Group> findDynGroups(String key);

    Collection<Group> findAllGroups(User user);

    Collection<String> findAllGroupKeys(User user);

    Collection<String> findAllGroupNames(User user);

    Collection<ExternalResource> findAllResources(User user);

    @Override
    <S extends User> S save(S user);

    Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(User user);

    @Override
    void delete(User user);

    boolean linkedAccountExists(String userKey, String connObjectKeyValue);

    Optional<? extends LinkedAccount> findLinkedAccount(ExternalResource resource, String connObjectKeyValue);

    List<LinkedAccount> findLinkedAccounts(String userKey);

    List<LinkedAccount> findLinkedAccountsByResource(ExternalResource resource);
}
