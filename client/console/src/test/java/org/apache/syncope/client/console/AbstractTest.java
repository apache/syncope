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
package org.apache.syncope.client.console;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import javax.servlet.ServletContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.client.console.pages.Dashboard;
import org.apache.syncope.client.console.pages.Login;
import org.apache.syncope.client.lib.AuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public abstract class AbstractTest {

    protected static Properties PROPS;

    protected static SyncopeServiceClient service;

    protected static TaskService taskService;

    protected static ReportService reportService;

    protected static NotificationService notificationService;

    public interface SyncopeServiceClient extends SyncopeService, Client {
    }

    @BeforeAll
    public static void loadProps() throws IOException {
        PROPS = new Properties();
        try (InputStream is = AbstractTest.class.getResourceAsStream("/console.properties")) {
            PROPS.load(is);
        }
    }

    @Test
    public void securityHeaders() throws IOException {
        Map<String, String> securityHeaders = getConfiguredSecurityHeaders();
        assertEquals(4, securityHeaders.size());

        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);
        securityHeaders.forEach((key, value) -> assertEquals(value, TESTER.getLastResponse().getHeader(key)));

        FormTester formTester = TESTER.newFormTester("login");
        formTester.setValue("username", "username");
        formTester.setValue("password", "password");
        formTester.submit("submit");

        TESTER.assertRenderedPage(Dashboard.class);
        securityHeaders.forEach((key, value) -> assertEquals(value, TESTER.getLastResponse().getHeader(key)));
    }

    protected Map<String, String> getConfiguredSecurityHeaders() throws IOException {
        Map<String, String> securityHeaders = new HashMap<>();

        @SuppressWarnings("unchecked")
        Enumeration<String> propNames = (Enumeration<String>) PROPS.propertyNames();
        while (propNames.hasMoreElements()) {
            String name = propNames.nextElement();
            if (name.startsWith("security.headers.")) {
                securityHeaders.put(StringUtils.substringAfter(name, "security.headers."), PROPS.getProperty(name));
            }
        }

        return securityHeaders;
    }

    protected static final WicketTester TESTER = new WicketTester(new SyncopeConsoleApplication() {

        @Override
        protected void init() {
            ServletContext ctx = getServletContext();
            ClassPathScanImplementationLookup lookup = new ClassPathScanImplementationLookup();
            lookup.load();
            ctx.setAttribute(ConsoleInitializer.CLASSPATH_LOOKUP, lookup);

            MIMETypesLoader mimeTypes = new MIMETypesLoader();
            mimeTypes.load();
            ctx.setAttribute(ConsoleInitializer.MIMETYPES_LOADER, mimeTypes);

            super.init();
        }

        @Override
        public List<String> getDomains() {
            return super.getDomains();
        }

        private SyncopeService getSyncopeService() {
            service = mock(SyncopeServiceClient.class);
            when(service.type(anyString())).thenReturn(service);
            when(service.accept(anyString())).thenReturn(service);
            when(service.platform()).thenReturn(new PlatformInfo());
            when(service.system()).thenReturn(new SystemInfo());

            NumbersInfo numbersInfo = new NumbersInfo();
            Stream.of(NumbersInfo.ConfItem.values()).
                    forEach(item -> numbersInfo.getConfCompleteness().put(item.name(), true));
            numbersInfo.setTotalUsers(4);
            numbersInfo.setTotalGroups(16);
            numbersInfo.setTotalRoles(6);
            numbersInfo.setTotalResources(21);
            when(service.numbers()).thenReturn(numbersInfo);

            return service;
        }

        private UserTO getUserTO() {
            UserTO userTO = new UserTO();
            userTO.setUsername("username");
            return userTO;
        }

        private DomainService getDomainService() {
            DomainService domainService = mock(DomainService.class);
            DomainTO domainTO = new DomainTO();
            domainTO.setKey(SyncopeConstants.MASTER_DOMAIN);
            when(domainService.list()).thenReturn(Collections.singletonList(domainTO));
            return domainService;
        }

        private TaskService getControlTaskService() {
            TaskService taskService = mock(TaskService.class);
            ArrayList<JobTO> taskJobTOs = new ArrayList<>();
            ArrayList<ExecTO> taskExecTOs = new ArrayList<>();
            JobTO taskJobTO = new JobTO();
            ExecTO taskExecTO = new ExecTO();

            taskJobTO.setType(JobType.TASK);
            taskJobTO.setRefKey("e074dde4-1652-4846-970a-6ba9878ce985");
            taskJobTO.setRefDesc("PUSH Task e074dde4-1652-4846-970a-6ba9878ce985 Export on resource-testdb2");
            taskJobTO.setRunning(false);
            taskJobTO.setScheduled(false);
            taskJobTO.setStart(null);
            taskJobTO.setStatus("UNKNOWN");
            taskJobTOs.add(taskJobTO);

            taskExecTO.setStart(new Date());
            taskExecTO.setEnd(new Date());
            taskExecTO.setKey("18342501-3345-4125-92a5-a4241aca22b6");
            taskExecTO.setJobType(JobType.TASK);
            taskExecTO.setRefKey("fa6eb8a4-7609-49ba-b60c-e8b06af8810c");
            taskExecTO.setRefDesc("PROPAGATION Task fa6eb8a4-7609-49ba-b60c-e8b06af8810c null");
            taskExecTO.setStatus("SUCCESS");
            taskExecTO.setMessage(null);
            taskExecTOs.add(taskExecTO);

            when(taskService.listJobs()).thenReturn(taskJobTOs);
            when(taskService.listRecentExecutions(10)).thenReturn(taskExecTOs);
            return taskService;
        }

        private ReportService getControlRepoService() {
            ReportService reportService = mock(ReportService.class);
            ArrayList<JobTO> repoJobTOs = new ArrayList<>();
            ArrayList<ExecTO> repoExecTOs = new ArrayList<>();
            JobTO repoJobTO = new JobTO();
            ExecTO repoExecTO = new ExecTO();

            repoJobTO.setType(JobType.TASK);
            repoJobTO.setRefKey("0b1511e2-73cb-42c5-86ce-8d6b1e7917d2");
            repoJobTO.setRefDesc("Report 0b1511e2-73cb-42c5-86ce-8d6b1e7917d2 reconciliation");
            repoJobTO.setRunning(false);
            repoJobTO.setScheduled(false);
            repoJobTO.setStart(null);
            repoJobTO.setStatus("UNKNOWN");
            repoJobTOs.add(repoJobTO);

            repoExecTO.setStart(new Date());
            repoExecTO.setEnd(new Date());
            repoExecTO.setKey("29cad031-74b9-4588-8ba7-2ae2034cbd1c");
            repoExecTO.setJobType(JobType.TASK);
            repoExecTO.setRefKey("989e0b81-78ae-4b8c-b321-9313e4e57255");
            repoExecTO.setRefDesc("PROPAGATION Task 989e0b81-78ae-4b8c-b321-9313e4e57255 null");
            repoExecTO.setStatus("SUCCESS");
            repoExecTO.setMessage(null);
            repoExecTOs.add(repoExecTO);

            when(reportService.listJobs()).thenReturn(repoJobTOs);
            when(reportService.listRecentExecutions(10)).thenReturn(repoExecTOs);
            return reportService;
        }

        private NotificationService getControlNotificationService() {
            NotificationService notificationService = mock(NotificationService.class);
            JobTO notificationJobTO = new JobTO();

            notificationJobTO.setType(JobType.TASK);
            notificationJobTO.setRefKey("0b1511e2-73cb-42c5-86ce-8d6b1e7917d2");
            notificationJobTO.setRefDesc("Report 0b1511e2-73cb-42c5-86ce-8d6b1e7917d2 reconciliation");
            notificationJobTO.setRunning(false);
            notificationJobTO.setScheduled(false);
            notificationJobTO.setStart(null);
            notificationJobTO.setStatus("UNKNOWN");

            when(notificationService.getJob()).thenReturn(notificationJobTO);
            return notificationService;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SyncopeClientFactoryBean newClientFactory() {
            SyncopeClient client = mock(SyncopeClient.class);

            when(client.self()).thenReturn(Pair.of(new HashMap<>(), getUserTO()));

            SyncopeService syncopeService = getSyncopeService();
            when(client.getService(SyncopeService.class)).thenReturn(syncopeService);

            DomainService domainService = getDomainService();
            when(client.getService(DomainService.class)).thenReturn(domainService);

            TaskService taskService = getControlTaskService();
            when(client.getService(TaskService.class)).thenReturn(taskService);

            ReportService repoService = getControlRepoService();
            when(client.getService(ReportService.class)).thenReturn(repoService);

            NotificationService notificationService = getControlNotificationService();
            when(client.getService(NotificationService.class)).thenReturn(notificationService);

            SyncopeClientFactoryBean clientFactory = mock(SyncopeClientFactoryBean.class);
            when(clientFactory.setDomain(any())).thenReturn(clientFactory);
            when(clientFactory.create(any(AuthenticationHandler.class))).thenReturn(client);
            when(clientFactory.create(anyString(), anyString())).thenReturn(client);

            return clientFactory;
        }
    });

}
