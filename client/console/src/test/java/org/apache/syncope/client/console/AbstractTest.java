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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractTest {

    protected static Properties PROPS;

    protected static SyncopeServiceClient service;

    protected static AccessTokenServiceClient accessTokenService;

    protected static TaskServiceClient taskService;

    protected static ReportServiceClient reportService;

    protected static NotificationServiceClient notificationService;

    public interface SyncopeServiceClient extends SyncopeService, Client {
    }

    public interface AccessTokenServiceClient extends AccessTokenService, Client {
    }

    public interface NotificationServiceClient extends NotificationService, Client {
    }

    public interface TaskServiceClient extends TaskService, Client {
    }

    public interface ReportServiceClient extends ReportService, Client {
    }

    @BeforeAll
    public static void loadProps() throws IOException {
        PROPS = new Properties();
        try (InputStream is = AbstractTest.class.getResourceAsStream("/console.properties")) {
            PROPS.load(is);
        }
        login();
    }

    private static void login() throws IOException {
        TESTER.startPage(Login.class);
        TESTER.assertRenderedPage(Login.class);

        FormTester formTester = TESTER.newFormTester("login");
        formTester.setValue("username", "admin");
        formTester.setValue("password", "password");
        formTester.submit("submit");

        TESTER.assertRenderedPage(Dashboard.class);
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
            userTO.setUsername("admin");
            return userTO;
        }

        private DomainService getDomainService() {
            DomainService domainService = mock(DomainService.class);
            DomainTO domainTO = new DomainTO();
            domainTO.setKey(SyncopeConstants.MASTER_DOMAIN);
            when(domainService.list()).thenReturn(Collections.singletonList(domainTO));
            return domainService;
        }

        private AccessTokenService getAccessTokenService() {
            accessTokenService = mock(AccessTokenServiceClient.class);
            when(accessTokenService.type(anyString())).thenReturn(accessTokenService);
            when(accessTokenService.accept(anyString())).thenReturn(accessTokenService);
            AccessTokenTO accessTokenTO = new AccessTokenTO();
            accessTokenTO.setKey("da7383c7-1981-401d-b7d6-a4aac6825f30");
            accessTokenTO.setOwner("admin");
            accessTokenTO.setBody(
                    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJqdGkiOiI2ZmIxZWE5OS0zN2Y0LTRmZWItYjFlYS05OTM3ZjQ4ZmViNGQiLCJzdWIiOiJhZG1pbiIsImlhdCI6MTU"
                    + "3Njc1MzEwNiwiaXNzIjoiQXBhY2hlU3luY29wZSIsImV4cCI6MTU3Njc2MDMwNiwibmJmIjoxNTc2NzUzMTA2fQ.aUIHLI9Ec8pZsFLOctFCdnUfQC7wt1HLUC5OVPWQ"
                    + "GZdePNnFGBvjEqf3Zui807GadbkO5RSl4-wJT5cLaqgOgA");
            PagedResult<AccessTokenTO> pagedResult = new PagedResult<>();
            pagedResult.setPage(1);
            pagedResult.setSize(1);
            pagedResult.setTotalCount(1);
            pagedResult.getResult().add(accessTokenTO);
            when(accessTokenService.list(any(AccessTokenQuery.class))).
                    thenReturn(pagedResult);
            pagedResult.setSize(10);
            when(accessTokenService.list(any(AccessTokenQuery.class))).
                    thenReturn(pagedResult);
            return accessTokenService;
        }

        private TaskService getControlTaskService() {
            taskService = mock(TaskServiceClient.class);
            when(taskService.type(anyString())).thenReturn(taskService);
            when(taskService.accept(anyString())).thenReturn(taskService);
            List<JobTO> taskJobTOs = new ArrayList<>();
            List<ExecTO> taskExecTOs = new ArrayList<>();
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
            reportService = mock(ReportServiceClient.class);
            when(reportService.type(anyString())).thenReturn(reportService);
            when(reportService.accept(anyString())).thenReturn(reportService);
            List<JobTO> repoJobTOs = new ArrayList<>();
            List<ExecTO> repoExecTOs = new ArrayList<>();
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
            notificationService = mock(NotificationServiceClient.class);
            when(notificationService.type(anyString())).thenReturn(notificationService);
            when(notificationService.accept(anyString())).thenReturn(notificationService);
            JobTO notificationJobTO = new JobTO();

            notificationJobTO.setType(JobType.NOTIFICATION);
            notificationJobTO.setRefKey(null);
            notificationJobTO.setRefDesc("NotificationJob");
            notificationJobTO.setRunning(false);
            notificationJobTO.setScheduled(true);
            notificationJobTO.setStart(new Date());
            notificationJobTO.setStatus("UNKNOWN");

            when(notificationService.getJob()).thenReturn(notificationJobTO);
            return notificationService;
        }

        @SuppressWarnings("unchecked")
        @Override
        public SyncopeClientFactoryBean newClientFactory() {
            SyncopeClient client = mock(SyncopeClient.class);

            Map<String, Set<String>> entitlements = new HashMap<>();
            Set<String> strings = new HashSet<>();
            strings.add("/");

            entitlements.put(StandardEntitlement.NOTIFICATION_LIST, strings);
            entitlements.put(StandardEntitlement.TASK_LIST, strings);
            entitlements.put(StandardEntitlement.REPORT_LIST, strings);
            entitlements.put(StandardEntitlement.ACCESS_TOKEN_LIST, strings);
            entitlements.put(StandardEntitlement.REPORT_EXECUTE, strings);
            entitlements.put(StandardEntitlement.REPORT_READ, strings);
            entitlements.put(StandardEntitlement.ACCESS_TOKEN_LIST, strings);

            when(client.self()).thenReturn(Pair.of(entitlements, getUserTO()));

            SyncopeService syncopeService = getSyncopeService();
            when(client.getService(SyncopeService.class)).thenReturn(syncopeService);

            DomainService domainService = getDomainService();
            when(client.getService(DomainService.class)).thenReturn(domainService);

            AccessTokenService accessTokenService = getAccessTokenService();
            when(client.getService(AccessTokenService.class)).thenReturn(accessTokenService);

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
