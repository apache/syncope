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

import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.OIDCJWKSRestClient;
import org.apache.syncope.client.console.rest.WAConfigRestClient;
import org.apache.syncope.client.console.wicket.ajax.form.IndicatorAjaxEventBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

public class OIDCJWKSGenerationPanel extends AbstractModalPanel<OIDCJWKSTO> {

    private static final long serialVersionUID = -3372006007594607067L;

    protected final OIDCJWKSRestClient oidcJWKSRestClient;

    protected final Model<String> jwksKeyIdM;

    protected final Model<String> jwksTypeM;

    protected final Model<Integer> jwksKeySizeM;

    public OIDCJWKSGenerationPanel(
            final OIDCJWKSRestClient oidcJWKSRestClient,
            final WAConfigRestClient waConfigRestClient,
            final BaseModal<OIDCJWKSTO> modal,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.oidcJWKSRestClient = oidcJWKSRestClient;

        jwksKeyIdM = Model.of("syncope");
        try {
            jwksKeyIdM.setObject(waConfigRestClient.get("cas.authn.oidc.jwks.core.jwks-key-id").getValues().get(0));
        } catch (SyncopeClientException e) {
            LOG.error("While reading cas.authn.oidc.jwks.core.jwks-key-id", e);
        }
        add(new AjaxTextFieldPanel("jwksKeyId", "jwksKeyId", jwksKeyIdM).setRequired(true));

        jwksTypeM = Model.of("rsa");
        try {
            jwksTypeM.setObject(waConfigRestClient.get("cas.authn.oidc.jwks.core.jwks-type").getValues().get(0));
        } catch (SyncopeClientException e) {
            LOG.error("While reading cas.authn.oidc.jwks.core.jwks-type", e);
        }
        AjaxDropDownChoicePanel<String> jwksType = new AjaxDropDownChoicePanel<>("jwksType", "jwksType", jwksTypeM).
                setChoices(List.of("rsa", "ec"));
        add(jwksType.setRequired(true));

        jwksKeySizeM = Model.of(2048);
        try {
            jwksKeySizeM.setObject(Integer.valueOf(
                    waConfigRestClient.get("cas.authn.oidc.jwks.core.jwks-key-size").getValues().get(0)));
        } catch (SyncopeClientException e) {
            LOG.error("While reading cas.authn.oidc.jwks.core.jwks-key-size", e);
        }
        AjaxNumberFieldPanel<Integer> jwksKeySize = new AjaxNumberFieldPanel.Builder<Integer>().step(128).
                build("jwksKeySize", "jwksKeySize", Integer.class, jwksKeySizeM);
        add(jwksKeySize.setRequired(true));

        jwksType.add(new IndicatorAjaxEventBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -4255753643957306394L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                if ("ec".equals(jwksTypeM.getObject())) {
                    jwksKeySizeM.setObject(256);
                } else {
                    jwksKeySizeM.setObject(2048);
                }
                target.add(jwksKeySize);
            }
        });
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            oidcJWKSRestClient.generate(jwksKeyIdM.getObject(), jwksTypeM.getObject(), jwksKeySizeM.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While generating OIDC JWKS", e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
