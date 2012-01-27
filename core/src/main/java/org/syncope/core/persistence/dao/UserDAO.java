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
import java.util.Set;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.beans.user.UAttrValue;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.core.rest.controller.InvalidSearchConditionException;

public interface UserDAO extends DAO {

    SyncopeUser find(Long id);

    SyncopeUser find(String username);

    SyncopeUser findByWorkflowId(String workflowId);

    List<SyncopeUser> findByDerAttrValue(String schemaName, String value)
            throws InvalidSearchConditionException;

    List<SyncopeUser> findByAttrValue(String schemaName, UAttrValue attrValue);

    SyncopeUser findByAttrUniqueValue(String schemaName,
            UAttrValue attrUniqueValue);

    List<SyncopeUser> findByResource(ExternalResource resource);

    List<SyncopeUser> findAll(Set<Long> adminRoles);

    List<SyncopeUser> findAll(Set<Long> adminRoles, int page, int itemsPerPage);

    Integer count(Set<Long> adminRoles);

    SyncopeUser save(SyncopeUser user)
            throws InvalidEntityException;

    void delete(Long id);

    void delete(SyncopeUser user);
}
