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
import org.syncope.core.persistence.beans.Task;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.PropagationTask;
import org.syncope.core.persistence.beans.user.SyncopeUser;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;

public interface TaskDAO extends DAO {

     <T extends Task> T find(Long id);

     <T extends Task> List<T> findWithoutExecs(Class<T> reference);

     <T extends Task> List<T> findAll(ExternalResource resource,
            Class<T> reference);

     <T extends Task> List<T> findAll(Class<T> reference);

     <T extends Task> List<T> findAll(int page, int itemsPerPage,
            Class<T> reference);

    List<PropagationTask> findAll(ExternalResource resource, SyncopeUser user);

    List<PropagationTask> findAll(SyncopeUser user);

     <T extends Task> Integer count(Class<T> reference);

     <T extends Task> T save(T task)
            throws InvalidEntityException;

     <T extends Task> void delete(Long id);

     <T extends Task> void delete(T task);

     <T extends Task> void deleteAll(ExternalResource resource,
            Class<T> reference);
}
