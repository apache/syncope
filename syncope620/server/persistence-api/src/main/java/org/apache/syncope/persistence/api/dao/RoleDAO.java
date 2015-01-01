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
package org.apache.syncope.persistence.api.dao;

import java.util.List;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.persistence.api.entity.Entitlement;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.Policy;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.persistence.api.entity.role.Role;

public interface RoleDAO extends SubjectDAO<RPlainAttr, RDerAttr, RVirAttr> {

    Role find(Long key);

    List<Role> find(String name);

    Role find(String name, Long parent);

    List<Role> findOwnedByUser(Long userId);

    List<Role> findOwnedByRole(Long roleId);

    List<Role> findByEntitlement(Entitlement entitlement);

    List<Role> findByPolicy(Policy policy);

    List<Role> findWithoutPolicy(PolicyType type);

    List<Role> findAncestors(Role role);

    List<Role> findChildren(Role role);

    List<Role> findDescendants(Role role);

    List<Role> findByDerAttrValue(String schemaName, String value);

    List<Role> findByAttrValue(String schemaName, RPlainAttrValue attrValue);

    Role findByAttrUniqueValue(String schemaName, RPlainAttrValue attrUniqueValue);

    List<Role> findByResource(ExternalResource resource);

    List<Role> findAll();

    List<Role> findAll(int page, int itemsPerPage, List<OrderByClause> orderBy);

    List<Membership> findMemberships(Role role);

    int count();

    Role save(Role syncopeRole) throws InvalidEntityException;

    void delete(Role role);

    void delete(Long key);
}
