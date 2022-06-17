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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.SAML2SP4UIConstants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.SAML2SP4UIIdPTO;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;
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

public abstract class AbstractSAMLSSOLoginFormPanel extends BaseSSOLoginFormPanel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSAMLSSOLoginFormPanel.class);

    private static final long serialVersionUID = 1153528484703183466L;

    public AbstractSAMLSSOLoginFormPanel(final String id, final BaseSession session) {
        super(id);

        List<SAML2SP4UIIdPTO> available = session.getAnonymousService(SAML2SP4UIIdPService.class).list();

        Model<SAML2SP4UIIdPTO> model = new Model<>();
        AjaxDropDownChoicePanel<SAML2SP4UIIdPTO> idps =
                new AjaxDropDownChoicePanel<>("idps", "SAML 2.0", model, false);
        idps.setChoices(available);
        idps.setChoiceRenderer(new IChoiceRenderer<>() {

            private static final long serialVersionUID = 1814750973898916102L;

            @Override
            public Object getDisplayValue(final SAML2SP4UIIdPTO object) {
                return object.getName();
            }

            @Override
            public String getIdValue(final SAML2SP4UIIdPTO object, final int index) {
                return object.getEntityID();
            }

            @Override
            public SAML2SP4UIIdPTO getObject(
                final String id, final IModel<? extends List<? extends SAML2SP4UIIdPTO>> choices) {

                return choices.getObject().stream().
                    filter(idp -> idp.getEntityID().equals(id)).findFirst().orElse(null);
            }
        });
        idps.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (model.getObject() != null) {
                    try {
                        RequestCycle.get().scheduleRequestHandlerAfterCurrent(new RedirectRequestHandler(
                                UrlUtils.rewriteToContextRelative(SAML2SP4UIConstants.URL_CONTEXT + "/login?idp="
                                        + URLEncoder.encode(
                                                model.getObject().getEntityID(), StandardCharsets.UTF_8),
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
