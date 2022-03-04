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

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class AttrReleasePolicyModalPanel extends AbstractModalPanel<AttrReleasePolicyTO> {

    private static final long serialVersionUID = 1L;

    private final IModel<AttrReleasePolicyTO> model;

    public AttrReleasePolicyModalPanel(
            final BaseModal<AttrReleasePolicyTO> modal,
            final IModel<AttrReleasePolicyTO> model,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.model = model;

        AjaxTextFieldPanel allowedAttr = new AjaxTextFieldPanel("panel", "allowedAttrs", new Model<>());
        add(new MultiFieldPanel.Builder<String>(
                new PropertyModel<>(model.getObject().getConf(), "allowedAttrs")).build(
                "allowedAttrs",
                "allowedAttrs",
                allowedAttr));

        AjaxTextFieldPanel excludedAttr = new AjaxTextFieldPanel("panel", "excludedAttrs", new Model<>());
        add(new MultiFieldPanel.Builder<String>(
                new PropertyModel<>(model.getObject().getConf(), "excludedAttrs")).build(
                "excludedAttrs",
                "excludedAttrs",
                excludedAttr));

        AjaxTextFieldPanel includeOnlyAttr = new AjaxTextFieldPanel("panel", "includeOnlyAttrs", new Model<>());
        add(new MultiFieldPanel.Builder<String>(
                new PropertyModel<>(model.getObject().getConf(), "includeOnlyAttrs")).build(
                "includeOnlyAttrs",
                "includeOnlyAttrs",
                includeOnlyAttr));
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target) {
        try {
            PolicyRestClient.update(PolicyType.ATTR_RELEASE, model.getObject());

            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While updating Attribute Release Policy {}", model.getObject().getKey(), e);
            SyncopeConsoleSession.get().onException(e);
        }
        ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }
}
