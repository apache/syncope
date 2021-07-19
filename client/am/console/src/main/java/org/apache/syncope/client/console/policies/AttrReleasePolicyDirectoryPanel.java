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
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AttrReleasePolicyDirectoryPanel extends PolicyDirectoryPanel<AttrReleasePolicyTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    public AttrReleasePolicyDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, PolicyType.ATTR_RELEASE, pageRef);

        AttrReleasePolicyTO defaultItem = new AttrReleasePolicyTO();

        this.addNewItemPanelBuilder(
                new PolicyModalPanelBuilder<>(PolicyType.ATTR_RELEASE, defaultItem, modal, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomActions(
            final ActionsPanel<AttrReleasePolicyTO> panel,
            final IModel<AttrReleasePolicyTO> model) {

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AttrReleasePolicyTO ignore) {
                model.setObject(PolicyRestClient.read(type, model.getObject().getKey()));
                if (model.getObject().getConf() == null) {
                    model.getObject().setConf(new DefaultAttrReleasePolicyConf());
                }
                target.add(policySpecModal.setContent(
                    new AttrReleasePolicyModalPanel(policySpecModal, model, pageRef)));
                policySpecModal.header(new Model<>(getString("attrReleasePolicyConf.title", model)));
                policySpecModal.show(true);
            }
        }, ActionLink.ActionType.TYPE_EXTENSIONS, IdRepoEntitlement.POLICY_UPDATE);
    }
}
