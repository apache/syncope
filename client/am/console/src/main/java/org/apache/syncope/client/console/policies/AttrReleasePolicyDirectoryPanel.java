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

import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.wicket.PageReference;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;

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
}
