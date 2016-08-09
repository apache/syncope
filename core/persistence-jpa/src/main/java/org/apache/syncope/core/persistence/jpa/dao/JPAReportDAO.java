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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.List;
import javax.persistence.TypedQuery;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.apache.syncope.core.persistence.jpa.entity.JPAReport;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JPAReportDAO extends AbstractDAO<Report> implements ReportDAO {

    @Transactional(readOnly = true)
    @Override
    public Report find(final String key) {
        return entityManager().find(JPAReport.class, key);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Report> findByTemplate(final ReportTemplate template) {
        TypedQuery<Report> query = entityManager().createQuery(
                "SELECT e FROM " + JPAReport.class.getSimpleName() + " e "
                + "WHERE e.template=:template", Report.class);
        query.setParameter("template", template);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<Report> findAll() {
        TypedQuery<Report> query = entityManager().createQuery(
                "SELECT e FROM " + JPAReport.class.getSimpleName() + " e", Report.class);

        return query.getResultList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Report save(final Report report) {
        return entityManager().merge(report);
    }

    @Override
    public void delete(final String key) {
        Report report = find(key);
        if (report == null) {
            return;
        }

        delete(report);
    }

    @Override
    public void delete(final Report report) {
        entityManager().remove(report);
    }
}
