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
package org.apache.syncope.core.persistence.dao;

import java.util.List;

import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.core.persistence.beans.Entitlement;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.Policy;
import org.apache.syncope.core.persistence.beans.membership.Membership;
import org.apache.syncope.core.persistence.beans.role.RAttrValue;
import org.apache.syncope.core.persistence.beans.role.SyncopeRole;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;

public interface RoleDAO extends SubjectDAO {

    SyncopeRole find(Long id);

    List<SyncopeRole> find(String name);

    SyncopeRole find(String name, Long parent);

    List<SyncopeRole> findOwnedByUser(Long userId);

    List<SyncopeRole> findOwnedByRole(Long roleId);

    List<SyncopeRole> findByEntitlement(Entitlement entitlement);

    List<SyncopeRole> findByPolicy(Policy policy);

    List<SyncopeRole> findWithoutPolicy(PolicyType type);

    List<SyncopeRole> findAncestors(SyncopeRole role);

    boolean hasChildren(SyncopeRole role);

    List<SyncopeRole> findChildren(SyncopeRole role);

    List<SyncopeRole> findDescendants(SyncopeRole role);

    List<SyncopeRole> findByDerAttrValue(String schemaName, String value);

    List<SyncopeRole> findByAttrValue(String schemaName, RAttrValue attrValue);

    SyncopeRole findByAttrUniqueValue(String schemaName, RAttrValue attrUniqueValue);

    List<SyncopeRole> findByResource(ExternalResource resource);

    List<SyncopeRole> findAll();

    List<SyncopeRole> findAll(int page, int itemsPerPage, List<OrderByClause> orderBy);

    List<Membership> findMemberships(SyncopeRole role);

    int count();

    SyncopeRole save(SyncopeRole syncopeRole) throws InvalidEntityException;

    void delete(SyncopeRole role);

    void delete(Long id);
}
