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
import java.util.Set;
import org.apache.syncope.client.search.NodeCond;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.util.AttributableUtil;

public interface AttributableSearchDAO extends DAO {

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param attrUtil AttributeUtil
     * @return size of search result
     */
    int count(Set<Long> adminRoles, NodeCond searchCondition, AttributableUtil attrUtil);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param attrUtil AttributeUtil
     * @param <T> user/role
     * @return the list of users/roles matching the given search condition
     */
    <T extends AbstractAttributable> List<T> search(Set<Long> adminRoles, NodeCond searchCondition,
            AttributableUtil attrUtil);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param page position of the first result, start from 1
     * @param itemsPerPage number of results per page
     * @param attrUtil AttributeUtil
     * @param <T> user/role
     * @return the list of users/roles matching the given search condition (in the given page)
     */
    <T extends AbstractAttributable> List<T> search(Set<Long> adminRoles, NodeCond searchCondition,
            int page, int itemsPerPage, AttributableUtil attrUtil);

    /**
     * Verify if user/role matches the given search condition.
     *
     * @param subject to be checked
     * @param searchCondition to be verified
     * @param attrUtil AttributeUtil
     * @param <T> user/role
     * @return true if user/role matches searchCondition
     */
    <T extends AbstractAttributable> boolean matches(T subject, NodeCond searchCondition, AttributableUtil attrUtil);
}
