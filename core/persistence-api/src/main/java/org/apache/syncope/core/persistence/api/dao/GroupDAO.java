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

import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;

public interface GroupDAO extends AnyDAO<Group> {

    Group find(String name);

    List<Group> findOwnedByUser(Long userKey);

    List<Group> findOwnedByGroup(Long groupKey);

    List<AMembership> findAMemberships(Group group);

    List<UMembership> findUMemberships(Group group);

    /**
     * Finds any objects having resources assigned exclusively because of memberships of the given group.
     *
     * @param groupKey group key
     * @return map containing pairs with any object key and operations to be performed on those resources (DELETE,
     * typically).
     */
    Map<Long, PropagationByResource> findAnyObjectsWithTransitiveResources(Long groupKey);

    /**
     * Finds users having resources assigned exclusively because of memberships of the given group.
     *
     * @param groupKey group key
     * @return map containing pairs with user key and operations to be performed on those resources (DELETE,
     * typically).
     */
    Map<Long, PropagationByResource> findUsersWithTransitiveResources(Long groupKey);

    List<TypeExtension> findTypeExtensionByAnyTypeClass(AnyTypeClass anyTypeClass);

    void refreshDynMemberships(AnyObject anyObject);

    void refreshDynMemberships(User user);

}
