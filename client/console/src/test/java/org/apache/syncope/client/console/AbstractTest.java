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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import javax.servlet.ServletContext;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.syncope.client.console.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.console.init.ConsoleInitializer;
import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.client.lib.AuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.info.SystemInfo;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractTest {

    protected static Properties PROPS;

    public interface SyncopeServiceClient extends SyncopeService, Client {
    }

    @BeforeAll
    public static void loadProps() throws IOException {
        PROPS = new Properties();
        try (InputStream is = AbstractTest.class.getResourceAsStream("/console.properties")) {
            PROPS.load(is);
        }
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
            SyncopeServiceClient service = mock(SyncopeServiceClient.class);
            when(service.type(anyString())).thenReturn(service);
            when(service.accept(anyString())).thenReturn(service);

            when(service.platform()).thenReturn(new PlatformInfo());
            when(service.system()).thenReturn(new SystemInfo());

            NumbersInfo numbersInfo = new NumbersInfo();
            Stream.of(NumbersInfo.ConfItem.values()).
                    forEach(item -> numbersInfo.getConfCompleteness().put(item.name(), true));
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

        @SuppressWarnings("unchecked")
        @Override
        public SyncopeClientFactoryBean newClientFactory() {
            SyncopeClient client = mock(SyncopeClient.class);

            when(client.self()).thenReturn(Pair.of(new HashMap<>(), getUserTO()));

            SyncopeService syncopeService = getSyncopeService();
            when(client.getService(SyncopeService.class)).thenReturn(syncopeService);

            DomainService domainService = getDomainService();
            when(client.getService(DomainService.class)).thenReturn(domainService);

            SyncopeClientFactoryBean clientFactory = mock(SyncopeClientFactoryBean.class);
            when(clientFactory.setDomain(any())).thenReturn(clientFactory);
            when(clientFactory.create(any(AuthenticationHandler.class))).thenReturn(client);
            when(clientFactory.create(anyString(), anyString())).thenReturn(client);

            return clientFactory;
        }
    });

}
