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

import java.util.List;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.to.client.SAML2SPTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.apache.syncope.common.rest.api.service.wa.WAClientAppService;

public class WAClientAppITCase extends AbstractITCase {

    private static WAClientAppService waClientAppService;

    @BeforeAll
    public static void setup() {
        SyncopeClient anonymous = clientFactory.create(
                new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));
        waClientAppService = anonymous.getService(WAClientAppService.class);
    }

    @Test
    public void list() {
        createClientApp(ClientAppType.OIDCRP, buildOIDCRP());

        List<WAClientApp> list = waClientAppService.list();
        assertFalse(list.isEmpty());
    }

    @Test
    public void read() {
        OIDCRPTO oidcrpto = createClientApp(ClientAppType.OIDCRP, buildOIDCRP());
        WAClientApp waClientApp = waClientAppService.read(oidcrpto.getClientAppId(), null);
        assertNotNull(waClientApp);

        waClientApp = waClientAppService.read(oidcrpto.getClientAppId(), ClientAppType.OIDCRP);
        assertNotNull(waClientApp);

        waClientApp = waClientAppService.read(oidcrpto.getName(), null);
        assertNotNull(waClientApp);

        waClientApp = waClientAppService.read(oidcrpto.getName(), ClientAppType.OIDCRP);
        assertNotNull(waClientApp);

        SAML2SPTO samlspto = createClientApp(ClientAppType.SAML2SP, buildSAML2SP());
        WAClientApp registeredSamlClientApp = waClientAppService.read(samlspto.getClientAppId(), null);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = waClientAppService.read(samlspto.getClientAppId(), ClientAppType.SAML2SP);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = waClientAppService.read(samlspto.getName(), null);
        assertNotNull(registeredSamlClientApp);

        registeredSamlClientApp = waClientAppService.read(samlspto.getName(), ClientAppType.SAML2SP);
        assertNotNull(registeredSamlClientApp);
    }
}
