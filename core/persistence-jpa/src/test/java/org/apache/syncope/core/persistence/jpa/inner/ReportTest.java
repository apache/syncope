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

import jakarta.ws.rs.core.MediaType;
import java.util.List;
import java.util.UUID;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.jpa.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class ReportTest extends AbstractTest {

    @Autowired
    private ReportDAO reportDAO;

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
        assertEquals(1, reports.size());
    }

    @Test
    public void save() {
        int beforeCount = reportDAO.findAll().size();

        Report report = entityFactory.newEntity(Report.class);
        report.setName("new report");
        report.setJobDelegate(implementationDAO.find("SampleReportJobDelegate"));
        report.setMimeType(MediaType.TEXT_PLAIN);
        report.setFileExt("txt");
        report.setActive(true);

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
