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
package org.apache.syncope.client.console.policies;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.HttpRequestAccessPolicyConf;
import org.apache.syncope.common.lib.policy.RemoteEndpointAccessPolicyConf;
import org.apache.syncope.common.lib.policy.TimeBasedAccessPolicyConf;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.UrlValidator;

public class AccessPolicyModalPanel extends AbstractModalPanel<AccessPolicyTO> {

    private static final long serialVersionUID = -6446551344059681908L;

    private final IModel<AccessPolicyTO> model;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public AccessPolicyModalPanel(
            final BaseModal<AccessPolicyTO> modal,
            final IModel<AccessPolicyTO> model,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.model = model;

        List<Component> fields = new ArrayList<>();

        if (model.getObject().getConf() instanceof DefaultAccessPolicyConf) {
            fields.add(new AjaxSpinnerFieldPanel.Builder<Integer>().build(
                    "field",
                    "order",
                    Integer.class,
                    new PropertyModel<>(model.getObject().getConf(), "order")));
            fields.add(new AjaxCheckBoxPanel(
                    "field",
                    "enabled",
                    new PropertyModel<>(model.getObject().getConf(), "enabled"),
                    false));
            fields.add(new AjaxCheckBoxPanel(
                    "field",
                    "ssoEnabled",
                    new PropertyModel<>(model.getObject().getConf(), "ssoEnabled"),
                    false));
            fields.add(new AjaxCheckBoxPanel(
                    "field",
                    "requireAllAttributes",
                    new PropertyModel<>(model.getObject().getConf(), "requireAllAttributes"),
                    false));
            fields.add(new AjaxCheckBoxPanel(
                    "field",
                    "caseInsensitive",
                    new PropertyModel<>(model.getObject().getConf(), "caseInsensitive"),
                    false));
            AjaxTextFieldPanel unauthorizedRedirectUrl = new AjaxTextFieldPanel(
                    "field",
                    "unauthorizedRedirectUrl",
                    new PropertyModel<>(model.getObject().getConf(), "unauthorizedRedirectUrl"),
                    false);
            unauthorizedRedirectUrl.getField().add(new UrlValidator(new String[] { "http", "https" }));
            fields.add(unauthorizedRedirectUrl);
        } else if (model.getObject().getConf() instanceof HttpRequestAccessPolicyConf) {
            fields.add(new AjaxTextFieldPanel("field", "ipAddress",
                    new PropertyModel<>(model.getObject().getConf(), "ipAddress"), false));
            fields.add(new AjaxTextFieldPanel("field", "userAgent",
                    new PropertyModel<>(model.getObject().getConf(), "userAgent"), false));
        } else if (model.getObject().getConf() instanceof RemoteEndpointAccessPolicyConf) {
            AjaxTextFieldPanel endpointUrl = new AjaxTextFieldPanel(
                    "field",
                    "endpointUrl",
                    new PropertyModel<>(model.getObject().getConf(), "endpointUrl"),
                    false);
            endpointUrl.getField().add(new UrlValidator(new String[] { "http", "https" }));
            fields.add(endpointUrl.setRequired(true));

            FieldPanel panel = new AjaxTextFieldPanel(
                    "panel",
                    "acceptableResponseCodes",
                    new PropertyModel<>(model.getObject().getConf(), "acceptableResponseCodes"));
            fields.add(new MultiFieldPanel.Builder<>(
                    new PropertyModel<>(model.getObject().getConf(), "acceptableResponseCodes")).build(
                    "field",
                    "acceptableResponseCodes",
                    panel));
        } else if (model.getObject().getConf() instanceof TimeBasedAccessPolicyConf) {
            fields.add(new AjaxDateTimeFieldPanel(
                    "field",
                    "start",
                    new DateOps.WrappedDateModel(new PropertyModel<>(model.getObject().getConf(), "start")),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT));
            fields.add(new AjaxDateTimeFieldPanel(
                    "field",
                    "end",
                    new DateOps.WrappedDateModel(new PropertyModel<>(model.getObject().getConf(), "end")),
                    DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT));
            fields.add(new AjaxTextFieldPanel("field", "zoneId",
                    new PropertyModel<>(model.getObject().getConf(), "zoneId"), false));
        }

        add(new ListView<>("fields", fields) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<Component> item) {
                item.add(item.getModelObject());
            }
        });
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            PolicyRestClient.update(PolicyType.ACCESS, model.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating Access Policy {}", model.getObject().getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
