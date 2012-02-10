/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.persistence.dao.impl;

import java.util.List;
import javax.persistence.Query;
import org.springframework.stereotype.Repository;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.dao.ReportDAO;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;

@Repository
public class ReportDAOImpl extends AbstractDAOImpl implements ReportDAO {

    @Override
    public Report find(final Long id) {
        return entityManager.find(Report.class, id);
    }

    @Override
    public List<Report> findAll() {
        return findAll(-1, -1);
    }

    @Override
    public List<Report> findAll(final int page, final int itemsPerPage) {
        final Query query = entityManager.createQuery(
                "SELECT e FROM " + Report.class.getSimpleName() + " e");

        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

        if (itemsPerPage > 0) {
            query.setMaxResults(itemsPerPage);
        }

        return query.getResultList();
    }

    @Override
    public int count() {
        Query countQuery = entityManager.createNativeQuery(
                "SELECT COUNT(id) FROM " + Report.class.getSimpleName());

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    public Report save(final Report report)
            throws InvalidEntityException {

        return entityManager.merge(report);
    }

    @Override
    public void delete(final Long id) {
        Report report = find(id);
        if (report == null) {
            return;
        }

        delete(report);
    }

    @Override
    public void delete(final Report report) {
        entityManager.remove(report);
    }
}
