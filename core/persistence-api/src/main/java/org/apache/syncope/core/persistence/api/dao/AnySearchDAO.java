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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;

public interface AnySearchDAO extends DAO<Any<?>> {

    /**
     * @param adminRealms realms for which the caller owns the proper entitlement(s)
     * @param searchCondition the search condition
     * @param kind any object
     * @return size of search result
     */
    int count(Set<String> adminRealms, SearchCond searchCondition, AnyTypeKind kind);

    /**
     * @param searchCondition the search condition
     * @param kind any object
     * @param <T> any
     * @return the list of any objects matching the given search condition
     */
    <T extends Any<?>> List<T> search(SearchCond searchCondition, AnyTypeKind kind);

    /**
     * @param searchCondition the search condition
     * @param orderBy list of ordering clauses
     * @param kind any object
     * @param <T> any
     * @return the list of any objects matching the given search condition
     */
    <T extends Any<?>> List<T> search(SearchCond searchCondition, List<OrderByClause> orderBy, AnyTypeKind kind);

    /**
     * @param adminRealms realms for which the caller owns the proper entitlement(s)
     * @param searchCondition the search condition
     * @param page position of the first result, start from 1
     * @param itemsPerPage number of results per page
     * @param orderBy list of ordering clauses
     * @param kind any object
     * @param <T> any
     * @return the list of any objects matching the given search condition (in the given page)
     */
    <T extends Any<?>> List<T> search(
            Set<String> adminRealms, SearchCond searchCondition, int page, int itemsPerPage,
            List<OrderByClause> orderBy, AnyTypeKind kind);
}
