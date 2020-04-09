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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RegisteredClientAppTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.service.RegisteredClientAppService;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RegisteredClientAppITCase extends AbstractITCase {

    protected static RegisteredClientAppService registeredClientAppService;

    @BeforeAll
    public static void setup() {
        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        registeredClientAppService = anonymous.getService(RegisteredClientAppService.class);
    }

    @Test
    public void list() {
        createClientApp(ClientAppType.OIDCRP, buildOIDCRP());

        List<RegisteredClientAppTO> list = registeredClientAppService.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void read() {
        OIDCRPTO oidcrpto = createClientApp(ClientAppType.OIDCRP, buildOIDCRP());
        RegisteredClientAppTO registeredOidcClientApp = registeredClientAppService.read(oidcrpto.getClientAppId());
        assertNotNull(registeredOidcClientApp);

        registeredOidcClientApp = registeredClientAppService.read(oidcrpto.getClientAppId(),
                ClientAppType.OIDCRP);
        assertNotNull(registeredOidcClientApp);

        registeredOidcClientApp = registeredClientAppService.read(oidcrpto.getName());
        assertNotNull(registeredOidcClientApp);

        registeredOidcClientApp = registeredClientAppService.read(oidcrpto.getName(), ClientAppType.OIDCRP);
        assertNotNull(registeredOidcClientApp);
        
        
        SAML2SPTO samlspto = createClientApp(ClientAppType.SAML2SP, buildSAML2SP());
        RegisteredClientAppTO registeredSamlClientApp=  registeredClientAppService.read(samlspto.getClientAppId());
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = registeredClientAppService.read(samlspto.getClientAppId(),
                ClientAppType.SAML2SP);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = registeredClientAppService.read(samlspto.getName());
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = registeredClientAppService.read(samlspto.getName(), ClientAppType.SAML2SP);
        assertNotNull(registeredSamlClientApp);
    }


    @Test
    public void delete() {
        SAML2SPTO samlspto = createClientApp(ClientAppType.SAML2SP, buildSAML2SP());

        assertTrue(registeredClientAppService.delete(samlspto.getName()));
        try {
            clientAppService.read(ClientAppType.SAML2SP, samlspto.getKey());
            fail("This should not happen");
        } catch (SyncopeClientException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void create() {
        OIDCRPTO oidcrpto = buildOIDCRP();
        RegisteredClientAppTO appTO = new RegisteredClientAppTO();
        appTO.setClientAppTO(oidcrpto);

        registeredClientAppService.create(appTO);
        assertNotNull(registeredClientAppService.read(oidcrpto.getClientAppId()));
    }

}
