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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.core.logic.init.AMEntitlementLoader;
import org.apache.syncope.core.logic.wa.ConsentDecisionLogic;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthAccountLogic;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthTokenLogic;
import org.apache.syncope.core.logic.wa.ImpersonationLogic;
import org.apache.syncope.core.logic.wa.MfaTrusStorageLogic;
import org.apache.syncope.core.logic.wa.WAClientAppLogic;
import org.apache.syncope.core.logic.wa.WAConfigLogic;
import org.apache.syncope.core.logic.wa.WebAuthnRegistrationLogic;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCOpEntityDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.PasswordManagementDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuthProfileDataBinder;
import org.apache.syncope.core.provisioning.api.data.ClientAppDataBinder;
import org.apache.syncope.core.provisioning.api.data.OIDCOpEntityDataBinder;
import org.apache.syncope.core.provisioning.api.data.PasswordManagementDataBinder;
import org.apache.syncope.core.provisioning.api.data.SAML2IdPEntityDataBinder;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.junit.jupiter.api.Test;

public class AMLogicContextTest {

    private final AMLogicContext context = new AMLogicContext();

    @Test
    void amEntitlementLoader() {
        assertInstanceOf(AMEntitlementLoader.class, context.amEntitlementLoader());
    }

    @Test
    void authModuleLogic() {
        assertInstanceOf(AuthModuleLogic.class, context.authModuleLogic(
                mock(AuthModuleDataBinder.class), mock(AuthModuleDAO.class)));
    }

    @Test
    void attrRepoLogic() {
        assertInstanceOf(AttrRepoLogic.class, context.attrRepoLogic(
                mock(AttrRepoDataBinder.class), mock(AttrRepoDAO.class)));
    }

    @Test
    void passwordManagementLogic() {
        assertInstanceOf(PasswordManagementLogic.class, context.passwordManagementLogic(
                mock(PasswordManagementDataBinder.class), mock(PasswordManagementDAO.class)));
    }

    @Test
    void authProfileLogic() {
        assertInstanceOf(AuthProfileLogic.class, context.authProfileLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void clientAppLogic() {
        assertInstanceOf(ClientAppLogic.class, context.clientAppLogic(
                mock(ServiceOps.class),
                mock(org.apache.syncope.core.persistence.api.entity.am.ClientAppUtilsFactory.class),
                mock(ClientAppDataBinder.class),
                mock(CASSPClientAppDAO.class),
                mock(OIDCRPClientAppDAO.class),
                mock(SAML2SPClientAppDAO.class)));
    }

    @Test
    void oidcOpEntityLogic() {
        assertInstanceOf(OIDCOpEntityLogic.class, context.oidcOpEntityLogic(
                mock(OIDCOpEntityDataBinder.class), mock(OIDCOpEntityDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void saml2IdPEntityLogic() {
        assertInstanceOf(SAML2IdPEntityLogic.class, context.saml2IdPEntityLogic(
                mock(SAML2IdPEntityDataBinder.class), mock(SAML2IdPEntityDAO.class)));
    }

    @Test
    void googleMfaAuthAccountLogic() {
        assertInstanceOf(GoogleMfaAuthAccountLogic.class, context.googleMfaAuthAccountLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void googleMfaAuthTokenLogic() {
        assertInstanceOf(GoogleMfaAuthTokenLogic.class, context.googleMfaAuthTokenLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void impersonationLogic() {
        assertInstanceOf(ImpersonationLogic.class, context.impersonationLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void consentDecisionLogic() {
        assertInstanceOf(ConsentDecisionLogic.class, context.consentDecisionLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void mfaTrusStorageLogic() {
        assertInstanceOf(MfaTrusStorageLogic.class, context.mfaTrusStorageLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }

    @Test
    void waClientAppLogic() {
        assertInstanceOf(WAClientAppLogic.class, context.waClientAppLogic(
                mock(WAClientAppDataBinder.class),
                mock(CASSPClientAppDAO.class),
                mock(OIDCRPClientAppDAO.class),
                mock(SAML2SPClientAppDAO.class)));
    }

    @Test
    void waConfigLogic() {
        assertInstanceOf(WAConfigLogic.class, context.waConfigLogic(
                mock(ServiceOps.class),
                mock(SecurityProperties.class),
                mock(WAConfigDataBinder.class),
                mock(WAConfigDAO.class)));
    }

    @Test
    void webAuthnRegistrationLogic() {
        assertInstanceOf(WebAuthnRegistrationLogic.class, context.webAuthnRegistrationLogic(
                mock(AuthProfileDataBinder.class), mock(AuthProfileDAO.class), mock(EntityFactory.class)));
    }
}
