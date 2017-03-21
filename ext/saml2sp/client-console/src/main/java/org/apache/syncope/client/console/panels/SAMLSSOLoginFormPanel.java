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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.apache.syncope.common.rest.api.service.SAML2IdPService;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.UrlUtils;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.handler.RedirectRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SAMLSSOLoginFormPanel extends SSOLoginFormPanel {

    private static final long serialVersionUID = -5252094098970677128L;

    private static final Logger LOG = LoggerFactory.getLogger(SAMLSSOLoginFormPanel.class);

    public SAMLSSOLoginFormPanel(final String id) {
        super(id);

        List<SAML2IdPTO> available =
                SyncopeConsoleSession.get().getAnonymousClient().getService(SAML2IdPService.class).list();

        final Model<SAML2IdPTO> model = new Model<>();
        AjaxDropDownChoicePanel<SAML2IdPTO> idps =
                new AjaxDropDownChoicePanel<>("idps", "SAML 2.0", model, false);
        idps.setChoices(available);
        idps.setChoiceRenderer(new IChoiceRenderer<SAML2IdPTO>() {

            private static final long serialVersionUID = 1814750973898916102L;

            @Override
            public Object getDisplayValue(final SAML2IdPTO object) {
                return object.getName();
            }

            @Override
            public String getIdValue(final SAML2IdPTO object, final int index) {
                return object.getEntityID();
            }

            @Override
            public SAML2IdPTO getObject(final String id, final IModel<? extends List<? extends SAML2IdPTO>> choices) {
                return IterableUtils.find(choices.getObject(), new Predicate<SAML2IdPTO>() {

                    @Override
                    public boolean evaluate(final SAML2IdPTO object) {
                        return object.getEntityID().equals(id);
                    }
                });
            }
        });
        idps.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (model.getObject() != null) {
                    try {
                        RequestCycle.get().scheduleRequestHandlerAfterCurrent(new RedirectRequestHandler(
                                UrlUtils.rewriteToContextRelative("saml2sp/login?idp="
                                        + URLEncoder.encode(
                                                model.getObject().getEntityID(), StandardCharsets.UTF_8.name()),
                                        RequestCycle.get())));
                    } catch (Exception e) {
                        LOG.error("Could not redirect to the selected IdP {}", model.getObject().getEntityID(), e);
                    }
                }
            }
        });
        idps.setOutputMarkupPlaceholderTag(true);
        idps.setVisible(!available.isEmpty());
        add(idps);
    }

}
