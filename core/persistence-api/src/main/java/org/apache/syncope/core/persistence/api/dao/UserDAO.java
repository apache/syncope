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
package org.apache.syncope.core.persistence.api.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface UserDAO extends AnyDAO<User> {

    int count();

    Map<String, Integer> countByRealm();

    Map<String, Integer> countByStatus();

    User authFindByUsername(String username);

    User findByUsername(String username);

    User findByToken(String token);

    List<User> findBySecurityQuestion(SecurityQuestion securityQuestion);

    List<Role> findDynRoleMemberships(User user);

    List<Group> findDynGroupMemberships(User user);

    Collection<Role> findAllRoles(User user);

    Collection<Group> findAllGroups(User user);

    Collection<String> findAllGroupKeys(User user);

    Collection<String> findAllGroupNames(User user);

    Collection<ExternalResource> findAllResources(User user);

    Collection<String> findAllResourceNames(String key);

    Pair<Boolean, Boolean> enforcePolicies(User user);
}
