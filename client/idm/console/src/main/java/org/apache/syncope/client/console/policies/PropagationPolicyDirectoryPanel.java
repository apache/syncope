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
import org.apache.syncope.common.lib.policy.PropagationPolicyTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.StringResourceModel;

public class PropagationPolicyDirectoryPanel extends PolicyDirectoryPanel<PropagationPolicyTO> {

    private static final long serialVersionUID = 25188602686577L;

    public PropagationPolicyDirectoryPanel(
            final String id,
            final PolicyRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, PolicyType.PROPAGATION, pageRef);

        PropagationPolicyTO defaultItem = new PropagationPolicyTO();

        this.addNewItemPanelBuilder(
                new PolicyModalPanelBuilder<>(PolicyType.PROPAGATION, defaultItem, modal, restClient, pageRef), true);
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.POLICY_CREATE);

        initResultTable();
    }

    @Override
    protected void addCustomColumnFields(final List<IColumn<PropagationPolicyTO, String>> columns) {
        columns.add(new BooleanPropertyColumn<>(new StringResourceModel(
                "fetchAroundProvisioning", this), "fetchAroundProvisioning", "fetchAroundProvisioning"));
        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("updateDelta", this), "updateDelta", "updateDelta"));
        columns.add(new PropertyColumn<>(
                new StringResourceModel("maxAttempts", this), "maxAttempts", "maxAttempts"));
    }
}
