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
import java.util.Set;
import org.apache.syncope.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.persistence.api.dao.validation.InvalidEntityException;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.persistence.api.entity.user.UNormAttr;
import org.apache.syncope.persistence.api.entity.user.UNormAttrValue;
import org.apache.syncope.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.persistence.api.entity.user.User;

public interface UserDAO extends SubjectDAO<UNormAttr, UDerAttr, UVirAttr> {

    User find(Long key);

    User find(String username);

    User findByWorkflowId(String workflowId);

    User findByToken(String token);

    List<User> findBySecurityQuestion(SecurityQuestion securityQuestion);

    List<User> findByDerAttrValue(String schemaName, String value);

    List<User> findByAttrValue(String schemaName, UNormAttrValue attrValue);

    User findByAttrUniqueValue(String schemaName, UNormAttrValue attrUniqueValue);

    List<User> findByResource(ExternalResource resource);

    List<User> findAll(Set<Long> adminRoles, int page, int itemsPerPage);

    List<User> findAll(Set<Long> adminRoles, int page, int itemsPerPage, List<OrderByClause> orderBy);

    int count(Set<Long> adminRoles);

    User save(User user) throws InvalidEntityException;

    void delete(Long key);

    void delete(User user);

}
