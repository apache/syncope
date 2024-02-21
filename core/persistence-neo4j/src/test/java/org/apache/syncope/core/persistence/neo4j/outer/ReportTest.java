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
package org.apache.syncope.core.persistence.neo4j.outer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.neo4j.AbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ReportTest extends AbstractTest {

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private ReportExecDAO reportExecDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Test
    public void find() {
        Report report = reportDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").orElseThrow();

        assertNotNull(report.getExecs());
        assertFalse(report.getExecs().isEmpty());
        assertEquals(1, report.getExecs().size());
    }

    @Test
    public void saveWithExistingName() {
        assertThrows(DataIntegrityViolationException.class, () -> {
            Report report = reportDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").orElseThrow();

            String name = report.getName();

            report = entityFactory.newEntity(Report.class);
            report.setName(name);
            report.setJobDelegate(implementationDAO.findById("SampleReportJobDelegate").orElseThrow());
            report.setMimeType(MediaType.TEXT_PLAIN);
            report.setFileExt("txt");
            report.setActive(true);

            reportDAO.save(report);
        });
    }

    @Test
    public void save() {
        Report report = reportDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").orElseThrow();
        assertEquals(1, report.getExecs().size());

        ReportExec reportExec = entityFactory.newEntity(ReportExec.class);
        reportExec.setReport(report);
        reportExec.setStart(OffsetDateTime.now());
        reportExec.setEnd(OffsetDateTime.now());
        reportExec.setStatus("SUCCESS");
        reportExec.setExecutor("admin");

        report.add(reportExec);
        reportDAO.save(report);

        report = reportDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").orElseThrow();
        assertEquals(2, report.getExecs().size());
    }

    @Test
    public void deleteReport() {
        reportDAO.deleteById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");

        assertTrue(reportDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").isEmpty());
        assertTrue(reportExecDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").isEmpty());
    }

    @Test
    public void deleteReportExecution() {
        ReportExec execution = reportExecDAO.findById("c13f39c5-0d35-4bff-ba79-3cd5de940369").orElseThrow();
        int executionNumber = execution.getReport().getExecs().size();

        reportExecDAO.deleteById("c13f39c5-0d35-4bff-ba79-3cd5de940369");

        assertTrue(reportExecDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").isEmpty());

        Report report = reportDAO.findById("0062ea9c-924d-4ecf-9961-4492a8cc6d1b").orElseThrow();
        assertEquals(report.getExecs().size(), executionNumber - 1);
    }
}
