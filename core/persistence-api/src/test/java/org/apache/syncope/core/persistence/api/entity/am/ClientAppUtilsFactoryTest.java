/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the License.  You may not use this file except in compliance
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
package org.apache.syncope.core.persistence.api.entity.am;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.junit.jupiter.api.Test;

class ClientAppUtilsFactoryTest {

    private final ClientAppUtilsFactory factory = new ClientAppUtilsFactory();

    @Test
    void getInstanceByType() {
        assertEquals(ClientAppType.SAML2SP, factory.getInstance(ClientAppType.SAML2SP).getType());
    }

    @Test
    void getInstanceByClientApp() {
        final SAML2SPClientApp saml2SpClientApp = mock(SAML2SPClientApp.class);
        assertEquals(ClientAppType.SAML2SP, factory.getInstance(saml2SpClientApp).getType());
    }

    @Test
    void getInstanceByInvalidClientApp() {
        assertThrows(IllegalArgumentException.class, () -> factory.getInstance(mock(ClientApp.class)));
    }

    @Test
    void getInstanceByClientAppTOClass() {
        final SAML2SPClientAppTO saml2SpClientAppTO = mock(SAML2SPClientAppTO.class);
        final ClientAppUtils clientAppUtils = factory.getInstance(saml2SpClientAppTO);
        assertEquals(ClientAppType.SAML2SP, clientAppUtils.getType());
    }

    @Test
    void getInstanceByInvalidClientAppTOClass() {
        assertThrows(IllegalArgumentException.class, () -> factory.getInstance(ClientAppTO.class));
    }
}
