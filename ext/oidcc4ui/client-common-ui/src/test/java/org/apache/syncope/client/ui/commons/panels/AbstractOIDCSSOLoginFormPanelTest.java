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
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.util.tester.WicketTester;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AbstractOIDCSSOLoginFormPanelTest {

    private WicketTester tester;

    private BaseSession session;

    private OIDCC4UIProviderService service;

    @BeforeEach
    void setUp() {
        service = mock(OIDCC4UIProviderService.class);
        session = mock(BaseSession.class);
        when(session.getAnonymousService(OIDCC4UIProviderService.class)).thenReturn(service);

        tester = new WicketTester();
    }

    @AfterEach
    void tearDown() {
        tester.destroy();
    }

    private static OIDCC4UIProviderTO provider(final String name) {
        OIDCC4UIProviderTO provider = new OIDCC4UIProviderTO();
        provider.setName(name);
        return provider;
    }

    private OIDCSSOLoginFormPanelStub panel(final OIDCC4UIProviderTO... providers) {
        when(service.list()).thenReturn(List.of(providers));
        OIDCSSOLoginFormPanelStub panel =
                tester.startComponentInPage(new OIDCSSOLoginFormPanelStub("panel", session));
        // do not follow the redirects scheduled by the panel itself
        tester.setFollowRedirects(false);
        return panel;
    }

    private static AjaxDropDownChoicePanel<?> ops(final OIDCSSOLoginFormPanelStub panel) {
        return (AjaxDropDownChoicePanel<?>) panel.get("ops");
    }

    private static AbstractAjaxBehavior onChangeBehavior(final AjaxDropDownChoicePanel<?> ops) {
        DropDownChoice<?> choice = (DropDownChoice<?>) ops.getField();
        List<AjaxFormComponentUpdatingBehavior> behaviors =
                choice.getBehaviors(AjaxFormComponentUpdatingBehavior.class);
        assertEquals(1, behaviors.size());
        return (AbstractAjaxBehavior) behaviors.getFirst();
    }

    @Test
    void rendersAvailableProviders() {
        OIDCSSOLoginFormPanelStub panel = panel(provider("op1"), provider("op2"));

        AjaxDropDownChoicePanel<?> ops = ops(panel);
        assertTrue(ops.isVisible());
        assertEquals(2, ((DropDownChoice<?>) ops.getField()).getChoices().size());

        tester.assertContains("value=\"op1\"");
        tester.assertContains("value=\"op2\"");
    }

    @Test
    void hidesDropDownWhenNoProviderAvailable() {
        assertFalse(ops(panel()).isVisible());
    }

    @Test
    void selectingProviderRedirectsToLogin() {
        OIDCSSOLoginFormPanelStub panel = panel(provider("op1"));
        panel.setReauth(true);

        AjaxDropDownChoicePanel<?> ops = ops(panel);
        FormComponent<?> field = ops.getField();
        tester.getRequest().getPostParameters().setParameterValue(field.getInputName(), "op1");

        tester.executeBehavior(onChangeBehavior(ops));

        String location = tester.getLastResponse().getRedirectLocation();
        assertNotNull(location);
        assertTrue(location.contains("oidcc4ui/login?op=op1&reauth=true"), location);
    }

    @Test
    void noSelectionDoesNotRedirect() {
        AjaxDropDownChoicePanel<?> ops = ops(panel(provider("op1")));

        tester.executeBehavior(onChangeBehavior(ops));

        assertNull(tester.getLastResponse().getRedirectLocation());
    }
}
