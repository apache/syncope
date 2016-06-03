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
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
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

    public PasswordPolicyDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, PolicyType.PASSWORD, pageRef);

        this.addNewItemPanelBuilder(
                new PolicyModalPanelBuilder<PasswordPolicyTO>(new PasswordPolicyTO(), modal, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomColumnFields(final List<IColumn<PasswordPolicyTO, String>> columns) {
        columns.add(new PropertyColumn<PasswordPolicyTO, String>(new StringResourceModel(
                "historyLength", this, null), "historyLength", "historyLength"));

        columns.add(new BooleanPropertyColumn<PasswordPolicyTO>(new StringResourceModel(
                "allowNullPassword", this, null), "allowNullPassword", "allowNullPassword"));
    }

    @Override
    protected void addCustomActions(
            final ActionLinksPanel.Builder<PasswordPolicyTO> panel, final IModel<PasswordPolicyTO> model) {
        panel.add(new ActionLink<PasswordPolicyTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PasswordPolicyTO ignore) {
                target.add(ruleCompositionModal.setContent(new PolicyRuleDirectoryPanel<PasswordPolicyTO>(
                        ruleCompositionModal, model.getObject().getKey(), PolicyType.PASSWORD, pageRef)));

                ruleCompositionModal.header(new StringResourceModel(
                        "policy.rule.conf", PasswordPolicyDirectoryPanel.this, Model.of(model.getObject())));

                MetaDataRoleAuthorizationStrategy.authorize(
                        ruleCompositionModal.getForm(), ENABLE, StandardEntitlement.POLICY_UPDATE);

                ruleCompositionModal.show(true);
            }
        }, ActionLink.ActionType.COMPOSE, StandardEntitlement.POLICY_UPDATE);
    }
}
