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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.rest.api.beans.ExecuteQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class ReportIssuesITCase extends AbstractITCase {

    @Test
    public void issueSYNCOPE43() {
        ReportTO reportTO = new ReportTO();
        reportTO.setName("issueSYNCOPE43" + getUUIDString());
        reportTO.setActive(true);
        reportTO.setTemplate("sample");
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        ExecTO execution = reportService.execute(new ExecuteQuery.Builder().key(reportTO.getKey()).build());
        assertNotNull(execution);

        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getKey());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);

        assertEquals(1, reportTO.getExecutions().size());
    }

    @Test
    public void issueSYNCOPE102() throws IOException {
        // Create
        ReportTO reportTO = reportService.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        reportTO.setKey(null);
        reportTO.setName("issueSYNCOPE102" + getUUIDString());
        reportTO = createReport(reportTO);
        assertNotNull(reportTO);

        // Execute (multiple requests)
        for (int i = 0; i < 10; i++) {
            ExecTO execution = reportService.execute(new ExecuteQuery.Builder().key(reportTO.getKey()).build());
            assertNotNull(execution);
        }

        // Wait for one execution
        int maxit = 50;
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            reportTO = reportService.read(reportTO.getKey());

            maxit--;
        } while (reportTO.getExecutions().isEmpty() && maxit > 0);
        assertFalse(reportTO.getExecutions().isEmpty());
    }
}
