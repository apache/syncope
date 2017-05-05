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

import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.policy.PullPolicySpec;
import org.apache.syncope.common.lib.policy.PullPolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * Pull policies page.
 */
public class PullPolicyDirectoryPanel extends PolicyDirectoryPanel<PullPolicyTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    public PullPolicyDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, PolicyType.PULL, pageRef);

        final PullPolicyTO defaultItem = new PullPolicyTO();
        defaultItem.setSpecification(new PullPolicySpec());

        this.addNewItemPanelBuilder(
                new PolicyModalPanelBuilder<>(defaultItem, modal, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomActions(final ActionsPanel<PullPolicyTO> panel, final IModel<PullPolicyTO> model) {
        panel.add(new ActionLink<PullPolicyTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final PullPolicyTO ignore) {
                target.add(policySpecModal.setContent(
                        new PolicySpecModalPanel(model.getObject(), policySpecModal, pageRef)));

                policySpecModal.header(new StringResourceModel(
                        "policy.rule.conf", PullPolicyDirectoryPanel.this, Model.of(model.getObject())));

                MetaDataRoleAuthorizationStrategy.authorize(
                        policySpecModal.getForm(), ENABLE, StandardEntitlement.POLICY_UPDATE);

                policySpecModal.show(true);
            }
        }, ActionLink.ActionType.COMPOSE, StandardEntitlement.POLICY_UPDATE);
    }
}
