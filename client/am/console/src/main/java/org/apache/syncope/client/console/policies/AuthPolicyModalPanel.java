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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.AuthModuleRestClient;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AuthPolicyModalPanel extends AbstractModalPanel<AuthPolicyTO> {

    private static final long serialVersionUID = -7210166323800567306L;

    @SpringBean
    protected PolicyRestClient policyRestClient;

    @SpringBean
    protected AuthModuleRestClient authModuleRestClient;

    protected final IModel<List<String>> allAuthModules = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected List<String> load() {
            return authModuleRestClient.list().stream().map(AuthModuleTO::getKey).sorted().collect(Collectors.toList());
        }
    };

    private final IModel<AuthPolicyTO> model;

    public AuthPolicyModalPanel(
            final BaseModal<AuthPolicyTO> modal,
            final IModel<AuthPolicyTO> model,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.model = model;

        add(new AjaxCheckBoxPanel(
                "tryAll",
                "tryAll",
                new PropertyModel<>(model.getObject().getConf(), "tryAll"),
                false));

        add(new AjaxPalettePanel.Builder<String>().setName("authModules").build(
                "authModules",
                new PropertyModel<>(model.getObject().getConf(), "authModules"),
                allAuthModules));

        add(new AjaxCheckBoxPanel(
                "bypassEnabled",
                "bypassEnabled",
                new PropertyModel<>(model.getObject().getConf(), "bypassEnabled"),
                false));

        add(new AjaxCheckBoxPanel(
                "forceMfaExecution",
                "forceMfaExecution",
                new PropertyModel<>(model.getObject().getConf(), "forceMfaExecution"),
                false));
        
        add(new AjaxTextFieldPanel(
                "bypassPrincipalAttributeName",
                "bypassPrincipalAttributeName",
                new PropertyModel<>(model.getObject().getConf(), "bypassPrincipalAttributeName"),
                false));

        add(new AjaxTextFieldPanel(
                "bypassPrincipalAttributeValue",
                "bypassPrincipalAttributeValue",
                new PropertyModel<>(model.getObject().getConf(), "bypassPrincipalAttributeValue"),
                false));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            policyRestClient.update(PolicyType.AUTH, model.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating Auth Policy {}", model.getObject().getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
