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
import org.apache.syncope.client.console.panels.ModalDirectoryPanel;
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.policy.AccessPolicyConf;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class AccessPolicyDirectoryPanel extends PolicyDirectoryPanel<AccessPolicyTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    public AccessPolicyDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, PolicyType.ACCESS, pageRef);

        AccessPolicyTO defaultItem = new AccessPolicyTO();

        this.addNewItemPanelBuilder(
                new PolicyModalPanelBuilder<>(PolicyType.ACCESS, defaultItem, modal, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomColumnFields(final List<IColumn<AccessPolicyTO, String>> columns) {
        columns.add(new PropertyColumn<>(new StringResourceModel("order", this), "order", "order"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("enabled", this), "enabled", "enabled"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("ssoEnabled", this), "ssoEnabled", "ssoEnabled"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("requireAllAttributes", this), "requireAllAttributes", "requireAllAttributes"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("caseInsensitive", this), "caseInsensitive", "caseInsensitive"));
    }

    @Override
    protected void addCustomActions(final ActionsPanel<AccessPolicyTO> panel, final IModel<AccessPolicyTO> model) {
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AccessPolicyTO ignore) {
                model.setObject(PolicyRestClient.read(type, model.getObject().getKey()));
                if (model.getObject().getConf() == null) {
                    model.getObject().setConf(new DefaultAccessPolicyConf());
                }
                target.add(ruleCompositionModal.setContent(new ModalDirectoryPanel<>(
                        ruleCompositionModal,
                        new AccessPolicyAttrsDirectoryPanel(
                                "panel",
                                ruleCompositionModal,
                                model,
                                AccessPolicyConf::getRequiredAttrs,
                                pageRef),
                        pageRef)));
                ruleCompositionModal.header(new Model<>(getString("requiredAttrs.title", model)));
                ruleCompositionModal.show(true);
            }
        }, ActionLink.ActionType.TYPE_EXTENSIONS, IdRepoEntitlement.POLICY_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AccessPolicyTO ignore) {
                model.setObject(PolicyRestClient.read(type, model.getObject().getKey()));
                if (model.getObject().getConf() == null) {
                    model.getObject().setConf(new DefaultAccessPolicyConf());
                }
                target.add(ruleCompositionModal.setContent(new ModalDirectoryPanel<>(
                        ruleCompositionModal,
                        new AccessPolicyAttrsDirectoryPanel(
                                "panel",
                                ruleCompositionModal,
                                model,
                                AccessPolicyConf::getRejectedAttrs,
                                pageRef),
                        pageRef)));
                ruleCompositionModal.header(new Model<>(getString("rejectedAttrs.title", model)));
                ruleCompositionModal.show(true);
            }
        }, ActionLink.ActionType.CLAIM, IdRepoEntitlement.POLICY_UPDATE);
    }
}
