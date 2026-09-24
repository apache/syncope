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
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.GroupTypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.springframework.data.domain.Pageable;

public interface GroupDAO extends AnyDAO<Group> {

    Optional<String> findKey(String name);

    Optional<? extends Group> findByName(String name);

    List<String> findKeysByNamePattern(String pattern);

    @Override
    Collection<String> findAllResourceKeys(String key);

    Map<String, Long> countByRealm();

    /**
     * Checks if the calling user is authorized to access the Group matching the provided key, under the given
     * realm.
     *
     * @param authRealms realms for which the calling user owns entitlement(s) to check
     * @param key Group key
     * @param realm Group's realm full path
     */
    void securityChecks(Set<String> authRealms, String key, String realm);

    long countUMembers(String groupKey);

    List<String> findUMembers(String groupKey);

    boolean existsUMembership(String userKey, String groupKey);

    List<UMembership> findUMemberships(Group group, Pageable pageable);

    long countAMembers(String groupKey);

    List<String> findAMembers(String groupKey);

    boolean existsAMembership(String anyObjectKey, String groupKey);

    List<AMembership> findAMemberships(Group group);

    List<GroupTypeExtension> findTypeExtensions(AnyTypeClass anyTypeClass);

    boolean isManager(String key);

    /**
     * Returns all users managed by the group for the given key.
     *
     * Given:
     *   * group G1 for the provided key
     *   * group G2, with user member U
     *
     * then we have 2 cases where U is managed by G1:
     *
     * (a) U has gManager set to G1
     * (b) G2 has gManager set to G1
     *
     * @param key manager key
     * @return users managed by the group for the given key
     */
    List<User> findManagedUsers(String key);

    /**
     * Returns all groups managed by the group for the given key.
     *
     * Given:
     *   * group G1 for the provided key
     *   * group G2
     *
     * then we have 1 case where G2 is managed by G1:
     *
     * (a) G2 has gManager set to G1
     *
     * @param key manager key
     * @return groups managed by the group for the given key
     */
    List<Group> findManagedGroups(String key);

    /**
     * Returns all any objects managed by the group for the given key.
     *
     * Given:
     *   * group G1 for the provided key
     *   * group G2, with any object member O
     *
     * then we have 2 cases where O is managed by G1:
     *
     * (a) O has gManager set to G1
     * (b) G2 has gManager set to G1
     *
     * @param key manager key
     * @return any objects managed by the group for the given key
     */
    List<AnyObject> findManagedAnyObjects(String key);
}
