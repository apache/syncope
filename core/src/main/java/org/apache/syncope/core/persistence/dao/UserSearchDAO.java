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

import org.apache.syncope.core.persistence.beans.user.SyncopeUser;
import org.apache.syncope.search.NodeCond;

public interface UserSearchDAO extends DAO {

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @return size of search result
     */
    int count(Set<Long> adminRoles, NodeCond searchCondition);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @return the list of users matching the given search condition
     */
    List<SyncopeUser> search(Set<Long> adminRoles, NodeCond searchCondition);

    /**
     * @param adminRoles the set of admin roles owned by the caller
     * @param searchCondition the search condition
     * @param page position of the first result, start from 1
     * @param itemsPerPage number of results per page
     * @return the list of users matching the given search condition
     */
    List<SyncopeUser> search(Set<Long> adminRoles, NodeCond searchCondition, int page, int itemsPerPage);

    /**
     * Verify if user matched the given search condition.
     *
     * @param user to be checked
     * @param searchCondition to be verified
     * @return true if user matched searchCondition
     */
    boolean matches(SyncopeUser user, NodeCond searchCondition);
}
