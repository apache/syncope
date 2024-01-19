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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface GroupDAO extends AnyDAO<Group> {

    Optional<String> findKey(String name);

    Optional<? extends Group> findByName(String name);

    /**
     * Checks if the calling user is authorized to access the Group matching the provided key, under the given
     * realm.
     *
     * @param authRealms realms for which the calling user owns entitlement(s) to check
     * @param key Group key
     * @param realm Group's realm full path
     */
    void securityChecks(Set<String> authRealms, String key, String realm);

    Map<String, Long> countByRealm();

    List<String> findKeysByNamePattern(String pattern);

    List<Group> findOwnedByUser(String userKey);

    List<Group> findOwnedByGroup(String groupKey);

    List<AMembership> findAMemberships(Group group);

    List<UMembership> findUMemberships(Group group);

    List<String> findAMembers(String groupKey);

    List<String> findUMembers(String groupKey);

    boolean existsAMembership(String anyObjectKey, String groupKey);

    boolean existsUMembership(String userKey, String groupKey);

    List<String> findADynMembers(Group group);

    long countAMembers(String groupKey);

    long countUMembers(String groupKey);

    long countADynMembers(Group group);

    long countUDynMembers(Group group);

    void clearADynMembers(Group group);

    /**
     * Evaluates all the dynamic group membership conditions against the given anyObject (invoked during save).
     *
     * @param anyObject anyObject being saved
     * @return pair of groups dynamically assigned before and after refresh
     */
    Pair<Set<String>, Set<String>> refreshDynMemberships(AnyObject anyObject);

    /**
     * Removes the dynamic group memberships of the given anyObject (invoked during delete).
     *
     * @param anyObject anyObject being deleted
     * @return groups dynamically assigned before refresh
     */
    Set<String> removeDynMemberships(AnyObject anyObject);

    List<String> findUDynMembers(Group group);

    void clearUDynMembers(Group group);

    /**
     * Evaluates all the dynamic group membership conditions against the given user (invoked during save).
     *
     * @param user user being saved
     * @return pair of groups dynamically assigned before and after refresh
     */
    Pair<Set<String>, Set<String>> refreshDynMemberships(User user);

    /**
     * Removes the dynamic group memberships of the given anyObject (invoked during delete).
     *
     * @param user user being deleted
     * @return groups dynamically assigned before refresh
     */
    Set<String> removeDynMemberships(User user);

    /**
     * Saves the provided group and refreshes all User and AnyObject members.
     *
     * @param group group to save
     * @return merged group
     */
    Group saveAndRefreshDynMemberships(Group group);

    List<TypeExtension> findTypeExtensions(AnyTypeClass anyTypeClass);

    @Override
    Collection<String> findAllResourceKeys(String key);
}
