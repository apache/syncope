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
import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
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

/**
 * Password policies page.
 */
public class PasswordPolicyDirectoryPanel extends PolicyDirectoryPanel<PasswordPolicyTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    public PasswordPolicyDirectoryPanel(
            final String id,
            final PolicyRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, PolicyType.PASSWORD, pageRef);

        this.addNewItemPanelBuilder(new PolicyModalPanelBuilder<>(
                PolicyType.PASSWORD, new PasswordPolicyTO(), modal, restClient, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomColumnFields(final List<IColumn<PasswordPolicyTO, String>> columns) {
        columns.add(new PropertyColumn<>(new StringResourceModel(
                "historyLength", this), "historyLength", "historyLength"));

        columns.add(new BooleanPropertyColumn<>(new StringResourceModel(
                "allowNullPassword", this), "allowNullPassword", "allowNullPassword"));
    }

    @Override
    protected void addCustomActions(final ActionsPanel<PasswordPolicyTO> panel, final IModel<PasswordPolicyTO> model) {
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordPolicyTO ignore) {
                target.add(ruleCompositionModal.setContent(new PolicyRuleDirectoryPanel<>(
                        ruleCompositionModal, model.getObject().getKey(), PolicyType.PASSWORD, restClient, pageRef)));

                ruleCompositionModal.header(new StringResourceModel(
                        "policy.rules", PasswordPolicyDirectoryPanel.this, Model.of(model.getObject())));

                MetaDataRoleAuthorizationStrategy.authorize(
                        ruleCompositionModal.getForm(), ENABLE, IdRepoEntitlement.POLICY_UPDATE);

                ruleCompositionModal.show(true);
            }
        }, ActionLink.ActionType.COMPOSE, IdRepoEntitlement.POLICY_UPDATE);
    }
}
