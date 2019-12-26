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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.panels.search.UserSelectionDirectoryPanel;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wizards.any.LinkedAccountDetailsPanel;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class MergeLinkAccountsSearchPanel extends WizardStep {
    private static final long serialVersionUID = 1221037007528732347L;

    private static final Logger LOG = LoggerFactory.getLogger(LinkedAccountDetailsPanel.class);

    private final UserTO userTO;

    private final WebMarkupContainer ownerContainer;

    private final UserSearchPanel userSearchPanel;

    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    private final UserSelectionDirectoryPanel userDirectoryPanel;

    private final Fragment userSearchFragment;

    public MergeLinkAccountsSearchPanel(final UserTO userTO, final PageReference pageRef) {
        super();
        setOutputMarkupId(true);

        setTitleModel(new ResourceModel("mergeLinkedAccounts.searchUser"));
        this.userTO = userTO;

        ownerContainer = new WebMarkupContainer("ownerContainer");
        ownerContainer.setOutputMarkupId(true);
        add(ownerContainer);

        userSearchFragment = new Fragment("search", "userSearchFragment", this);
        userSearchPanel = UserSearchPanel.class.cast(new UserSearchPanel.Builder(
            new ListModel<>(new ArrayList<>())).required(false).enableSearch(MergeLinkAccountsSearchPanel.this).
            build("usersearch"));
        userSearchFragment.add(userSearchPanel.setRenderBodyOnly(true));

        AnyTypeTO anyTypeTO = anyTypeRestClient.read(AnyTypeKind.USER.name());

        userDirectoryPanel = UserSelectionDirectoryPanel.class.cast(new UserSelectionDirectoryPanel.Builder(
            anyTypeClassRestClient.list(anyTypeTO.getClasses()), anyTypeTO.getKey(), pageRef).
            build("searchResult"));

        userSearchFragment.add(userDirectoryPanel);

        ownerContainer.add(userSearchFragment);
    }
}
