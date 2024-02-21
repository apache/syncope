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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface GroupRepoExt extends AnyRepoExt<Group> {

    String ADYNMEMB_TABLE = "ADynGroupMembers";

    String UDYNMEMB_TABLE = "UDynGroupMembers";

    void securityChecks(Set<String> authRealms, String key, String realm);

    Map<String, Long> countByRealm();

    List<Group> findOwnedByUser(String userKey);

    boolean existsAMembership(String anyObjectKey, String groupKey);

    boolean existsUMembership(String userKey, String groupKey);

    List<AMembership> findAMemberships(Group group);

    List<UMembership> findUMemberships(Group group);

    Group saveAndRefreshDynMemberships(Group group);

    List<TypeExtension> findTypeExtensions(AnyTypeClass anyTypeClass);

    long countADynMembers(Group group);

    long countUDynMembers(Group group);

    List<String> findADynMembers(Group group);

    List<String> findUDynMembers(Group group);

    void clearADynMembers(Group group);

    void clearUDynMembers(Group group);

    Pair<Set<String>, Set<String>> refreshDynMemberships(AnyObject anyObject);

    Set<String> removeDynMemberships(AnyObject anyObject);

    Pair<Set<String>, Set<String>> refreshDynMemberships(User user);

    Set<String> removeDynMemberships(User user);

    @Override
    <S extends Group> S save(S group);

    @Override
    void delete(Group group);
}
