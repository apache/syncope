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
package org.apache.syncope.client.console.wizards;

import java.util.ArrayList;
import org.apache.syncope.client.console.panels.search.AnySelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.panels.search.UserSelectionDirectoryPanel;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class UserSelectionWizardStep extends WizardStep {

    private static final long serialVersionUID = 36221031226727L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    protected final IModel<String> model;

    protected final UserSearchPanel userSearchPanel;

    protected final UserSelectionDirectoryPanel userDirectoryPanel;

    public UserSelectionWizardStep(
            final IModel<String> title, final IModel<String> model, final PageReference pageRef) {

        super();
        setOutputMarkupId(true);

        this.model = model;
        setTitleModel(title);

        userSearchPanel = UserSearchPanel.class.cast(new UserSearchPanel.Builder(
                new ListModel<>(new ArrayList<>()), pageRef).required(false).enableSearch(UserSelectionWizardStep.this).
                build("usersearch"));
        add(userSearchPanel);

        AnyTypeTO anyTypeTO = anyTypeRestClient.read(AnyTypeKind.USER.name());
        userDirectoryPanel = UserSelectionDirectoryPanel.class.cast(new UserSelectionDirectoryPanel.Builder(
                anyTypeClassRestClient.list(anyTypeTO.getClasses()), userRestClient, anyTypeTO.getKey(), pageRef).
                build("searchResult"));
        add(userDirectoryPanel);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
            AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();
            String fiql = SearchUtils.buildFIQL(
                    userSearchPanel.getModel().getObject(), SyncopeClient.getUserSearchConditionBuilder());
            userDirectoryPanel.search(fiql, target);
        } else if (event.getPayload() instanceof AnySelectionDirectoryPanel.ItemSelection) {
            @SuppressWarnings("unchecked")
            AnySelectionDirectoryPanel.ItemSelection<UserTO> payload =
                    (AnySelectionDirectoryPanel.ItemSelection<UserTO>) event.getPayload();

            UserTO selected = payload.getSelection();
            this.model.setObject(selected.getKey());

            String tableId = ((Component) event.getSource()).
                    get("container:content:searchContainer:resultTable:tablePanel:groupForm:checkgroup:dataTable").
                    getMarkupId();
            String js = "$('#" + tableId + " tr').removeClass('active');";
            js += "$('#" + tableId + " td[title=" + selected.getKey() + "]').parent().addClass('active');";
            payload.getTarget().prependJavaScript(js);
        }
    }
}
