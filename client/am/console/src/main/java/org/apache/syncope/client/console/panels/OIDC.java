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

import com.fasterxml.jackson.databind.json.JsonMapper;
import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.IOException;
import java.util.Optional;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.OIDCJWKSRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OIDC extends Panel {

    private static final long serialVersionUID = 12898029694947L;

    private static final Logger LOG = LoggerFactory.getLogger(OIDC.class);

    protected static final JsonMapper MAPPER = JsonMapper.builder().findAndAddModules().build();

    @SpringBean
    protected OIDCJWKSRestClient oidcJWKSRestClient;

    protected final BaseModal<String> viewModal = new BaseModal<>("viewModal") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(true);
        }
    };

    protected final AjaxLink<Void> view;

    protected final AjaxLink<Void> generate;

    protected final AjaxLink<Void> delete;

    public OIDC(final String id, final String waPrefix, final PageReference pageRef) {
        super(id);
        setOutputMarkupId(true);

        add(viewModal);
        viewModal.size(Modal.Size.Extra_large);
        viewModal.setWindowClosedCallback(target -> viewModal.show(false));

        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container.setOutputMarkupId(true));

        Mutable<OIDCJWKSTO> oidcjwksto = oidcJWKSRestClient.get();

        view = new AjaxLink<>("view") {

            private static final long serialVersionUID = 6250423506463465679L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                String pretty;
                try {
                    pretty = MAPPER.writerWithDefaultPrettyPrinter().
                            writeValueAsString(MAPPER.readTree(oidcjwksto.getValue().getJson()));
                } catch (IOException e) {
                    LOG.error("Could not pretty-print", e);
                    pretty = Optional.ofNullable(oidcjwksto.getValue()).map(OIDCJWKSTO::getJson).orElse(null);
                }

                viewModal.header(Model.of("JSON Web Key Sets"));
                target.add(viewModal.setContent(new JsonEditorPanel(viewModal, Model.of(pretty), true, pageRef)));
                viewModal.show(true);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);

                if (oidcjwksto.getValue() == null) {
                    tag.put("class", "btn btn-app disabled");
                }
            }
        };
        view.setEnabled(oidcjwksto.getValue() != null);
        container.add(view.setOutputMarkupId(true));
        MetaDataRoleAuthorizationStrategy.authorize(view, ENABLE, AMEntitlement.OIDC_JWKS_READ);

        generate = new AjaxLink<>("generate") {

            private static final long serialVersionUID = 6250423506463465679L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    oidcjwksto.setValue(oidcJWKSRestClient.generate());
                    generate.setEnabled(false);
                    view.setEnabled(true);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While generating OIDC JWKS", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);

                if (oidcjwksto.getValue() != null) {
                    tag.put("class", "btn btn-app disabled");
                }
            }
        };
        generate.setEnabled(oidcjwksto.getValue() == null);
        container.add(generate.setOutputMarkupId(true));
        MetaDataRoleAuthorizationStrategy.authorize(generate, ENABLE, AMEntitlement.OIDC_JWKS_GENERATE);

        delete = new AjaxLink<>("delete") {

            private static final long serialVersionUID = 6250423506463465679L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    oidcJWKSRestClient.delete();
                    oidcjwksto.setValue(null);
                    generate.setEnabled(true);
                    view.setEnabled(false);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting OIDC JWKS", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);

                if (oidcjwksto.getValue() == null) {
                    tag.put("class", "btn btn-app disabled");
                }
            }
        };
        delete.setEnabled(oidcjwksto.getValue() != null);
        container.add(delete.setOutputMarkupId(true));
        MetaDataRoleAuthorizationStrategy.authorize(delete, ENABLE, AMEntitlement.OIDC_JWKS_DELETE);

        String wellKnownURI = waPrefix + "/oidc/.well-known/openid-configuration";
        container.add(new ExternalLink("wellKnownURI", wellKnownURI, wellKnownURI));
    }
}
