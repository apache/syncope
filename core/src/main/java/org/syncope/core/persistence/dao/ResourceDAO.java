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
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.types.IntMappingType;

public interface ResourceDAO extends DAO {

    ExternalResource find(String name);

    List<ExternalResource> findAll();

    List<ExternalResource> findAllByPriority();

    ExternalResource save(ExternalResource resource)
            throws InvalidEntityException;

    List<SchemaMapping> findAllMappings();

    SchemaMapping getMappingForAccountId(String resourceName);

    void deleteMappings(String schemaName, IntMappingType intMappingType);

    void deleteAllMappings(ExternalResource resource);

    void delete(String name);
}
