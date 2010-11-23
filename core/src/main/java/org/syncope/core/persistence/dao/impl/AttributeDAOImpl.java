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
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.AbstractAttribute;
import org.syncope.core.persistence.dao.AttributeDAO;

@Repository
public class AttributeDAOImpl extends AbstractDAOImpl
        implements AttributeDAO {

    @Override
    public <T extends AbstractAttribute> T find(final Long id,
            final Class<T> reference) {

        return entityManager.find(reference, id);
    }

    @Override
    public <T extends AbstractAttribute> List<T> findAll(
            final Class<T> reference) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + reference.getSimpleName() + " e");
        return query.getResultList();
    }

    @Override
    public <T extends AbstractAttribute> T save(final T attribute) {
        return entityManager.merge(attribute);
    }

    @Override
    public <T extends AbstractAttribute> void delete(
            final Long id, final Class<T> reference) {

        T attribute = find(id, reference);
        if (attribute == null) {
            return;
        }

        delete(attribute);
    }

    @Override
    public <T extends AbstractAttribute> void delete(final T attribute) {
        if (attribute.getOwner() != null) {
            attribute.getOwner().removeAttribute(attribute);
        }
        attribute.getSchema().removeAttribute(attribute);

        entityManager.remove(attribute);
    }
}
