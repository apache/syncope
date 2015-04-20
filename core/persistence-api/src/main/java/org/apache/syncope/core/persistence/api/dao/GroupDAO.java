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
import java.util.Set;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.Group;

public interface GroupDAO extends SubjectDAO<GPlainAttr, GDerAttr, GVirAttr> {

    Group find(Long key);

    Group find(String name);

    List<Group> findOwnedByUser(Long userKey);

    List<Group> findOwnedByGroup(Long groupKey);

    List<Group> findByAttrValue(String schemaName, GPlainAttrValue attrValue);

    List<Group> findByDerAttrValue(String schemaName, String value);

    Group findByAttrUniqueValue(String schemaName, GPlainAttrValue attrUniqueValue);

    List<Group> findByResource(ExternalResource resource);

    List<Group> findAll(Set<String> adminRealms, int page, int itemsPerPage);

    List<Group> findAll(Set<String> adminRealms, int page, int itemsPerPage, List<OrderByClause> orderBy);

    List<Membership> findMemberships(Group group);

    int count(Set<String> adminRealms);

    Group save(Group group);

    void delete(Group group);

    void delete(Long key);

    Group authFetch(Long key);

    /**
     * Finds users having resources assigned exclusively because of memberships of the given group.
     *
     * @param groupKey group key
     * @return map containing pairs with user key and operations to be performed on those resources (DELETE, typically).
     */
    Map<Long, PropagationByResource> findUsersWithIndirectResources(Long groupKey);
}
