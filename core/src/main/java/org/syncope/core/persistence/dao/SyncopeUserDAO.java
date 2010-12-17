/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.dao;

import java.util.List;
import org.syncope.client.search.NodeCond;
import org.syncope.client.search.PaginatedResult;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;

public interface SyncopeUserDAO extends DAO {

    SyncopeUser find(Long id);

    SyncopeUser findByWorkflowId(Long workflowId);

    List<SyncopeUser> findByAttributeValue(UAttrValue attributeValue);

    List<SyncopeUser> findAll();

    List<SyncopeUser> findAll(int page, int itemsPerPage);

    Long count();

    List<SyncopeUser> search(NodeCond searchCondition);

    List<SyncopeUser> search(NodeCond searchCondition,
            int page, int itemsPerPage, PaginatedResult paginatedResult);

    SyncopeUser save(SyncopeUser syncopeUser)
            throws InvalidEntityException;

    void delete(Long id);
}
