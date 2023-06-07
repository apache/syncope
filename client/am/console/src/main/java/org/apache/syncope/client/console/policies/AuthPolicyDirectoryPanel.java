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

import org.apache.syncope.client.console.rest.PolicyRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AuthPolicyDirectoryPanel extends PolicyDirectoryPanel<AuthPolicyTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    public AuthPolicyDirectoryPanel(final String id, final PolicyRestClient restClient, final PageReference pageRef) {
        super(id, restClient, PolicyType.AUTH, pageRef);

        AuthPolicyTO defaultItem = new AuthPolicyTO();

        this.addNewItemPanelBuilder(
                new PolicyModalPanelBuilder<>(PolicyType.AUTH, defaultItem, modal, restClient, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomActions(final ActionsPanel<AuthPolicyTO> panel, final IModel<AuthPolicyTO> model) {

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AuthPolicyTO ignore) {
                model.setObject(restClient.read(type, model.getObject().getKey()));
                if (model.getObject().getConf() == null) {
                    model.getObject().setConf(new DefaultAuthPolicyConf());
                }
                target.add(policySpecModal.setContent(
                        new AuthPolicyModalPanel(policySpecModal, model, pageRef)));
                policySpecModal.header(new Model<>(getString("authPolicyConf.title", model)));
                policySpecModal.show(true);
            }
        }, ActionLink.ActionType.CHANGE_VIEW, IdRepoEntitlement.POLICY_UPDATE);
    }
}
