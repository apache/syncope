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
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.common.lib.to.OIDCC4UIProviderTO;
import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;
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

public abstract class AbstractOIDCSSOLoginFormPanel extends BaseSSOLoginFormPanel {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractOIDCSSOLoginFormPanel.class);

    private static final long serialVersionUID = -7756088163755119603L;

    public AbstractOIDCSSOLoginFormPanel(final String id, final BaseSession session) {
        super(id);

        List<OIDCC4UIProviderTO> available = session.getAnonymousService(OIDCC4UIProviderService.class).list();

        final Model<OIDCC4UIProviderTO> model = new Model<>();
        AjaxDropDownChoicePanel<OIDCC4UIProviderTO> ops =
                new AjaxDropDownChoicePanel<>("ops", "OpenID Connect", model, false);
        ops.setChoices(available);
        ops.setChoiceRenderer(new IChoiceRenderer<>() {

            private static final long serialVersionUID = 1814750973898916102L;

            @Override
            public Object getDisplayValue(final OIDCC4UIProviderTO object) {
                return object.getName();
            }

            @Override
            public String getIdValue(final OIDCC4UIProviderTO object, final int index) {
                return object.getName();
            }

            @Override
            public OIDCC4UIProviderTO getObject(final String id,
                                                final IModel<? extends List<? extends OIDCC4UIProviderTO>> choices) {

                return choices.getObject().stream().
                    filter(object -> object.getName().equals(id)).findFirst().orElse(null);
            }
        });

        ops.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (model.getObject() != null) {
                    try {
                        RequestCycle.get().scheduleRequestHandlerAfterCurrent(new RedirectRequestHandler(
                                UrlUtils.rewriteToContextRelative(OIDCC4UIConstants.URL_CONTEXT + "/login?op="
                                        + URLEncoder.encode(model.getObject().getName(), StandardCharsets.UTF_8),
                                        RequestCycle.get())));
                    } catch (Exception e) {
                        LOG.error("Could not redirect to the selected OP {}", model.getObject().getName(), e);
                    }
                }
            }
        });
        ops.setOutputMarkupPlaceholderTag(true);
        ops.setVisible(!available.isEmpty());
        add(ops);
    }
}
