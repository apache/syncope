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
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.validation.MultiUniqueValueException;

public interface SchemaDAO extends DAO {

    <T extends AbstractSchema> T find(String name, Class<T> reference);

    <T extends AbstractSchema> List<T> findAll(Class<T> reference);

    <T extends AbstractSchema> T save(
            T schema)
            throws MultiUniqueValueException;

    <T extends AbstractSchema> void delete(String name, Class<T> reference);
}
