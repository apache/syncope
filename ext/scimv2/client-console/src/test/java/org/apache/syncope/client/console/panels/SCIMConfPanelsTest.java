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
package org.apache.syncope.client.console.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.common.lib.scim.SCIMComplexConf;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMExtensionAnyObjectConf;
import org.apache.syncope.common.lib.scim.SCIMUserAddressConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.types.AddressCanonicalType;
import org.apache.syncope.common.lib.scim.types.EmailCanonicalType;
import org.apache.syncope.common.lib.scim.types.IMCanonicalType;
import org.apache.syncope.common.lib.scim.types.PhoneNumberCanonicalType;
import org.apache.syncope.common.lib.scim.types.PhotoCanonicalType;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.wicket.resource.loader.ClassStringResourceLoader;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

class SCIMConfPanelsTest {

    private static WicketTester TESTER;

    @BeforeAll
    static void setup() {
        SchemaRestClient schemaRestClient = mock(SchemaRestClient.class);
        when(schemaRestClient.getSchemas(any(), any())).thenReturn(List.of());

        AnyTypeRestClient anyTypeRestClient = mock(AnyTypeRestClient.class);
        AnyTypeTO anyTypeTO = new AnyTypeTO();
        anyTypeTO.setKey("other");
        when(anyTypeRestClient.read(anyString())).thenReturn(anyTypeTO);

        AnyTypeClassRestClient anyTypeClassRestClient = mock(AnyTypeClassRestClient.class);
        when(anyTypeClassRestClient.list(any())).thenReturn(List.of());

        StaticApplicationContext ctx = new StaticApplicationContext();
        ctx.getBeanFactory().registerSingleton("schemaRestClient", schemaRestClient);
        ctx.getBeanFactory().registerSingleton("anyTypeRestClient", anyTypeRestClient);
        ctx.getBeanFactory().registerSingleton("anyTypeClassRestClient", anyTypeClassRestClient);

        TESTER = new WicketTester();
        de.agilecoders.wicket.core.Bootstrap.install(TESTER.getApplication());
        TESTER.getApplication().getResourceSettings().getStringResourceLoaders().
                add(new ClassStringResourceLoader(SyncopeWebApplication.class));
        SpringComponentInjector injector = new SpringComponentInjector(TESTER.getApplication(), ctx, true);
        TESTER.getApplication().getComponentInstantiationListeners().add(injector);
    }

    @AfterAll
    static void destroy() {
        TESTER.destroy();
    }

    @Test
    void groupPanelCreatesConfigurationAndBindsExternalId() {
        SCIMConf conf = new SCIMConf();

        SCIMConfGroupPanel panel = TESTER.startComponentInPage(new SCIMConfGroupPanel("panel", conf));

        assertNotNull(conf.getGroupConf());

        panel.get("externalId").setDefaultModelObject("568a44fc-c3bb-4feb-98c4-06ad29aff754");

        assertEquals("568a44fc-c3bb-4feb-98c4-06ad29aff754", conf.getGroupConf().getExternalId());
    }

    @Test
    void enterpriseUserPanelCreatesConfigurationAndManager() {
        SCIMConf conf = new SCIMConf();

        TESTER.startComponentInPage(new SCIMConfEnterpriseUserPanel("panel", conf));

        assertNotNull(conf.getEnterpriseUserConf());
        assertNotNull(conf.getEnterpriseUserConf().getManager());
    }

    @Test
    void userPanelPopulatesAllCanonicalTypes() {
        SCIMConf conf = new SCIMConf();
        conf.setUserConf(new SCIMUserConf());

        SCIMComplexConf<EmailCanonicalType> email = new SCIMComplexConf<>();
        email.setType(EmailCanonicalType.work);
        email.setValue("workEmail");
        conf.getUserConf().getEmails().add(email);

        TESTER.startComponentInPage(new SCIMConfUserPanel("panel", conf));

        SCIMUserConf userConf = conf.getUserConf();
        assertEquals(EmailCanonicalType.values().length, userConf.getEmails().size());
        assertEquals(PhoneNumberCanonicalType.values().length, userConf.getPhoneNumbers().size());
        assertEquals(IMCanonicalType.values().length, userConf.getIms().size());
        assertEquals(PhotoCanonicalType.values().length, userConf.getPhotos().size());
        assertEquals(AddressCanonicalType.values().length, userConf.getAddresses().size());

        assertEquals("workEmail", userConf.getEmails().stream().
                filter(complex -> EmailCanonicalType.work == complex.getType()).findFirst().
                orElseThrow().getValue());
        assertTrue(userConf.getAddresses().stream().
                anyMatch(address -> AddressCanonicalType.home == address.getType()));
        assertTrue(userConf.getAddresses().stream().
                map(SCIMUserAddressConf::getType).distinct().count()
                == AddressCanonicalType.values().length);
    }

    @Test
    void extensionAnyObjectPanelCreatesConfigurationForType() {
        SCIMConf conf = new SCIMConf();

        TESTER.startComponentInPage(new SCIMConfExtensionAnyObjectPanel("panel", conf, "other"));

        assertEquals(1, conf.getExtensionAnyObjectsConf().size());
        SCIMExtensionAnyObjectConf extensionConf = conf.getExtensionAnyObjectsConf().getFirst();
        assertEquals("other", extensionConf.getType());

        TESTER.startComponentInPage(new SCIMConfExtensionAnyObjectPanel("panel", conf, "other"));

        assertEquals(1, conf.getExtensionAnyObjectsConf().size());
    }
}
