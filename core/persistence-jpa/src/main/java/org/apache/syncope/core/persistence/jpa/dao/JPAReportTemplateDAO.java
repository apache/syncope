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
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.apache.syncope.core.persistence.jpa.entity.JPAReportTemplate;

public class JPAReportTemplateDAO extends AbstractDAO<ReportTemplate> implements ReportTemplateDAO {

    @Override
    public ReportTemplate find(final String key) {
        return entityManager().find(JPAReportTemplate.class, key);
    }

    @Override
    public List<ReportTemplate> findAll() {
        TypedQuery<ReportTemplate> query = entityManager().createQuery(
                "SELECT e FROM " + JPAReportTemplate.class.getSimpleName() + " e", ReportTemplate.class);
        return query.getResultList();
    }

    @Override
    public ReportTemplate save(final ReportTemplate notification) {
        return entityManager().merge(notification);
    }

    @Override
    public void delete(final String key) {
        ReportTemplate template = find(key);
        if (template == null) {
            return;
        }

        entityManager().remove(template);
    }
}
