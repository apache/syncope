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
package org.apache.syncope.client.ui.commons.panels;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractSAMLSSOLoginFormPanelTest {

    private WicketTester tester;

    private BaseSession session;

    private SAML2SP4UIIdPService service;

    @BeforeEach
    void setUp() {
        service = mock(SAML2SP4UIIdPService.class);
        session = mock(BaseSession.class);
        when(session.getAnonymousService(SAML2SP4UIIdPService.class)).thenReturn(service);

        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private static SAML2SP4UIIdPTO idp(final String name, final String entityID) {
        SAML2SP4UIIdPTO idp = new SAML2SP4UIIdPTO();
        idp.setName(name);
        idp.setEntityID(entityID);
        return idp;
    }

    private SAMLSSOLoginFormPanelStub panel(final SAML2SP4UIIdPTO... idps) {
        when(service.list()).thenReturn(List.of(idps));
        SAMLSSOLoginFormPanelStub panel =
                tester.startComponentInPage(new SAMLSSOLoginFormPanelStub("panel", session));
        // do not follow the redirects scheduled by the panel itself
        tester.setFollowRedirects(false);
        return panel;
    }

    private static AjaxDropDownChoicePanel<?> idps(final SAMLSSOLoginFormPanelStub panel) {
        return (AjaxDropDownChoicePanel<?>) panel.get("idps");
    }

    private static AbstractAjaxBehavior onChangeBehavior(final AjaxDropDownChoicePanel<?> idps) {
        DropDownChoice<?> choice = (DropDownChoice<?>) idps.getField();
        List<AjaxFormComponentUpdatingBehavior> behaviors =
                choice.getBehaviors(AjaxFormComponentUpdatingBehavior.class);
        assertEquals(1, behaviors.size());
        return (AbstractAjaxBehavior) behaviors.getFirst();
    }

    @Test
    void rendersAvailableIdPs() {
        SAMLSSOLoginFormPanelStub panel = panel(
                idp("idp1", "https://idp1.example.com"),
                idp("idp2", "https://idp2.example.com"));

        AjaxDropDownChoicePanel<?> idps = idps(panel);
        assertTrue(idps.isVisible());
        assertEquals(2, ((DropDownChoice<?>) idps.getField()).getChoices().size());

        tester.assertContains("value=\"https://idp1.example.com\"");
        tester.assertContains("value=\"https://idp2.example.com\"");
    }

    @Test
    void hidesDropDownWhenNoIdPAvailable() {
        assertFalse(idps(panel()).isVisible());
    }

    @Test
    void selectingIdPRedirectsToLogin() {
        SAMLSSOLoginFormPanelStub panel = panel(idp("idp1", "idp1"));
        panel.setReauth(true);

        AjaxDropDownChoicePanel<?> idps = idps(panel);
        FormComponent<?> field = idps.getField();
        tester.getRequest().getPostParameters().setParameterValue(field.getInputName(), "idp1");

        tester.executeBehavior(onChangeBehavior(idps));

        String location = tester.getLastResponse().getRedirectLocation();
        assertNotNull(location);
        assertTrue(location.contains("saml2sp4ui/login?idp=idp1&reauth=true"), location);
    }

    @Test
    void noSelectionDoesNotRedirect() {
        AjaxDropDownChoicePanel<?> idps = idps(panel(idp("idp1", "idp1")));

        tester.executeBehavior(onChangeBehavior(idps));

        assertNull(tester.getLastResponse().getRedirectLocation());
    }
}
