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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ReportTest extends AbstractTest {

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportTemplateDAO reportTemplateDAO;

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
        int beforeCount = reportDAO.findAll().size();

        Report report = entityFactory.newEntity(Report.class);
        report.setName("new report");
        report.setActive(true);
        report.add(new UserReportletConf("first"));
        report.add(new UserReportletConf("second"));
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
