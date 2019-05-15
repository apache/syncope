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
package org.apache.syncope.client.enduser.pages;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.markup.html.form.BpmnProcessesAjaxPanel;
import org.apache.syncope.client.enduser.rest.BpmnProcessRestClient;
import org.apache.syncope.client.enduser.rest.UserRequestRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.annotations.ExtPage;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@ExtPage(label = "User Requests",
        listEntitlement = FlowableEntitlement.BPMN_PROCESS_GET, priority = 200)
public class Flowable extends BaseExtPage {

    private static final long serialVersionUID = -8781434495150074529L;

    private final int rowsPerPage = 10;

    protected final IModel<List<UserRequest>> userRequests;

    private final Model<String> bpmnProcessModel = new Model<>();

    private final BpmnProcessRestClient restClient = new BpmnProcessRestClient();

    private final UserRequestRestClient userRequestRestClient = new UserRequestRestClient();

    public Flowable(final PageParameters parameters) {
        super(parameters);

        this.userRequests = new ListModel<>(userRequestRestClient.getUserRequests(1, rowsPerPage,
                SyncopeEnduserSession.get().getSelfTO().getUsername(), new SortParam<>("name", true)));
        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);

        // list of accordions containing request form (if any) and delete button
        PageableListView<UserRequest> userReqsListView = new PageableListView<>("userRequests", userRequests,
                rowsPerPage) {

            private static final long serialVersionUID = -5002600396458362774L;

            @Override
            protected void populateItem(final ListItem<UserRequest> item) {
                final UserRequest userRequest = item.getModelObject();
                item.add(new Accordion("userRequestDetails", Collections.<ITab>singletonList(new AbstractTab(
                        new StringResourceModel("user.requests.accordion", content, Model.of(userRequest))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        // find the form associated to the current request
                        return new Panel("userRequestForm") {

                            private static final long serialVersionUID = -957948639666058749L;

                        };
                    }
                }), Model.of(-1)).setOutputMarkupId(true));
            }

        };

        userReqsListView.setOutputMarkupId(true);
        content.add(userReqsListView);
        content.add(new AjaxPagingNavigator("navigator", userReqsListView));

        // autocomplete select with bpmnProcesses
        final BpmnProcessesAjaxPanel bpmnProcesses =
                new BpmnProcessesAjaxPanel("bpmnProcesses", "bpmnProcesses", bpmnProcessModel,
                        new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        userRequestRestClient.start(bpmnProcessModel.getObject(), null);
                        target.add(userReqsListView);
                    }
                });
        bpmnProcesses.setChoices(restClient.getDefinitions().stream()
                .filter(definition -> !definition.isUserWorkflow())
                .map(definition -> definition.getKey()).collect(Collectors.toList()));
        content.add(bpmnProcesses);

        body.add(content);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        navbar.setActiveNavItem(getClass().getSimpleName().toLowerCase());
    }

}
