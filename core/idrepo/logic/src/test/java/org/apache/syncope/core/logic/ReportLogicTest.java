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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.job.report.ReportJobDelegate;
import org.apache.syncope.core.provisioning.java.job.report.AbstractReportJobDelegate;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ReportLogicTest extends AbstractTest {

    public static class TestReportJobDelegate extends AbstractReportJobDelegate {

        @Override
        protected String doExecute(
                final boolean dryRun,
                final OutputStream os,
                final String executor,
                final JobExecutionContext context) throws JobExecutionException {

            try {
                os.write("test".getBytes());
            } catch (IOException e) {
                throw new JobExecutionException(e);
            }

            return "";
        }
    }

    @BeforeAll
    public static void setAuthContext() {
        List<GrantedAuthority> authorities = IdMEntitlement.values().stream().
                map(entitlement -> new SyncopeGrantedAuthority(entitlement, SyncopeConstants.ROOT_REALM)).
                collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        "admin", "FAKE_PASSWORD", authorities), "FAKE_PASSWORD", authorities);
        auth.setDetails(new SyncopeAuthenticationDetails(SyncopeConstants.MASTER_DOMAIN, null));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterAll
    public static void unsetAuthContext() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Autowired
    private ReportLogic logic;

    private void checkExport(final String execKey) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        logic.exportExecutionResult(os, execKey);

        os.close();
        byte[] entity = os.toByteArray();
        assertTrue(entity.length > 0);
    }

    @Test
    public void executeAndExport() throws Exception {
        ReportTO report = logic.read("0062ea9c-924d-4ecf-9961-4492a8cc6d1b");
        assertNotNull(report);
        assertTrue(report.isActive());

        report.getExecutions().forEach(exec -> logic.deleteExecution(exec.getKey()));

        report = logic.read(report.getKey());
        assertTrue(report.getExecutions().isEmpty());

        JobExecutionContext ctx = mock(JobExecutionContext.class);
        when(ctx.getExecutor()).thenReturn("test");

        ReportJobDelegate delegate =
                ApplicationContextProvider.getBeanFactory().createBean(TestReportJobDelegate.class);
        delegate.execute(report.getKey(), false, ctx);

        report = logic.read(report.getKey());
        assertFalse(report.getExecutions().isEmpty());

        String execKey = report.getExecutions().getFirst().getKey();

        checkExport(execKey);
    }
}
