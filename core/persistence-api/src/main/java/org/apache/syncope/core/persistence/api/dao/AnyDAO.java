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
import java.util.Set;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Schema;

public interface AnyDAO<A extends Any<?>> extends DAO<A> {

    A authFind(String key);

    A find(String key);

    A findByWorkflowId(String workflowId);

    List<A> findByAttrValue(String schemaName, PlainAttrValue attrValue);

    A findByAttrUniqueValue(String schemaName, PlainAttrValue attrUniqueValue);

    /**
     * Find any objects by derived attribute value. This method could fail if one or more string literals contained
     * into the derived attribute value provided derive from identifier (schema name) replacement. When you are going to
     * specify a derived attribute expression you must be quite sure that string literals used to build the expression
     * cannot be found into the attribute values used to replace attribute schema names used as identifiers.
     *
     * @param schemaName derived schema name
     * @param value derived attribute value
     * @return list of any objects
     */
    List<A> findByDerAttrValue(String schemaName, String value);

    List<A> findByResource(ExternalResource resource);

    /**
     * Find any objects without any limitation.
     *
     * @return all any objects of type {@link A} available.
     */
    List<A> findAll();

    /**
     * Find any objects visible from the given admin realms, according to given page and items per page, sorted as
     * required.
     *
     * @param adminRealms admin realms
     * @param page search result page
     * @param itemsPerPage items per search result page
     * @param orderBy ordering clauses
     * @return any objects of type {@link A} matching the provided conditions
     */
    List<A> findAll(Set<String> adminRealms, int page, int itemsPerPage, List<OrderByClause> orderBy);

    <S extends Schema> AllowedSchemas<S> findAllowedSchemas(A any, Class<S> reference);

    int count(Set<String> adminRealms);

    A save(A any);

    void delete(String key);

    void delete(A any);

}
