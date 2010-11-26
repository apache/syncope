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
import org.syncope.core.persistence.beans.role.RAttr;
import org.syncope.core.persistence.beans.role.RDerAttr;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;

public interface SyncopeRoleDAO extends DAO {

    SyncopeRole find(String name, Long parent);

    SyncopeRole find(Long id);

    List<SyncopeRole> findChildren(Long roleId);

    List<RAttr> findInheritedAttributes(SyncopeRole role);

    List<RDerAttr> findInheritedDerivedAttributes(SyncopeRole role);

    List<SyncopeRole> findAll();

    SyncopeRole save(SyncopeRole syncopeRole)
            throws InvalidEntityException;

    void delete(Long id);
}
