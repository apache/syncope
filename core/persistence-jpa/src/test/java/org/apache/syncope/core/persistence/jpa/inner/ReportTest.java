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
package org.apache.syncope.core.persistence.jpa.inner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ReportTest extends AbstractTest {

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportTemplateDAO reportTemplateDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void find() {
        Report report = reportDAO.find("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        assertNotNull(report);

        report = reportDAO.find(UUID.randomUUID().toString());
        assertNull(report);
    }

    @Test
    public void findAll() {
        List<Report> reports = reportDAO.findAll();
        assertNotNull(reports);
        assertEquals(2, reports.size());
    }

    @Test
    public void save() {
        Implementation reportlet1 = entityFactory.newEntity(Implementation.class);
        reportlet1.setKey("UserReportlet" + UUID.randomUUID().toString());
        reportlet1.setEngine(ImplementationEngine.JAVA);
        reportlet1.setType(IdRepoImplementationType.REPORTLET);
        reportlet1.setBody(POJOHelper.serialize(new UserReportletConf("first")));
        reportlet1 = implementationDAO.save(reportlet1);

        Implementation reportlet2 = entityFactory.newEntity(Implementation.class);
        reportlet2.setKey("UserReportlet" + UUID.randomUUID().toString());
        reportlet2.setEngine(ImplementationEngine.JAVA);
        reportlet2.setType(IdRepoImplementationType.REPORTLET);
        reportlet2.setBody(POJOHelper.serialize(new UserReportletConf("second")));
        reportlet2 = implementationDAO.save(reportlet2);

        int beforeCount = reportDAO.findAll().size();

        Report report = entityFactory.newEntity(Report.class);
        report.setName("new report");
        report.setActive(true);
        report.add(reportlet1);
        report.add(reportlet2);
        report.setTemplate(reportTemplateDAO.find("sample"));

        report = reportDAO.save(report);
        assertNotNull(report);
        assertNotNull(report.getKey());

        int afterCount = reportDAO.findAll().size();
        assertEquals(afterCount, beforeCount + 1);
    }

    @Test
    public void delete() {
        Report report = reportDAO.find("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        assertNotNull(report);

        reportDAO.delete("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");

        report = reportDAO.find("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        assertNull(report);
    }
}
