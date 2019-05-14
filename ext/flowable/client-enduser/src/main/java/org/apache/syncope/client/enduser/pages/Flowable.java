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
import java.util.Iterator;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.annotations.ExtPage;
import org.apache.syncope.client.enduser.markup.html.form.BpmnProcessesAjaxPanel;
import org.apache.syncope.client.enduser.rest.BpmnProcessRestClient;
import org.apache.syncope.client.enduser.rest.UserRequestRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserRequest;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.ext.client.common.ui.panels.UserRequestFormPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.navigation.paging.AjaxPagingNavigator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@ExtPage(label = "User Requests")
public class Flowable extends BaseExtPage {

    private static final long serialVersionUID = -8781434495150074529L;

    private final int rowsPerPage = 10;

    private final Model<String> bpmnProcessModel = new Model<>();

    private final BpmnProcessRestClient restClient = new BpmnProcessRestClient();

    private final UserRequestRestClient userRequestRestClient = new UserRequestRestClient();

    private final WebMarkupContainer paginationContainer;

    private final DataView<UserRequest> urDataView;

    public Flowable(final PageParameters parameters) {
        super(parameters);

        paginationContainer = new WebMarkupContainer("content");
        paginationContainer.setOutputMarkupId(true);

        // list of accordions containing request form (if any) and delete button
        urDataView = new DataView<UserRequest>("userRequests", new URDataProvider(rowsPerPage, "bpmnProcess")) {

            private static final long serialVersionUID = -5002600396458362774L;

            @Override
            protected void populateItem(final Item<UserRequest> item) {
                final UserRequest userRequest = item.getModelObject();
                item.add(new Accordion("userRequestDetails", Collections.<ITab>singletonList(new AbstractTab(
                        new StringResourceModel("user.requests.accordion", paginationContainer,
                                Model.of(userRequest))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        // find the form associated to the current request, if any
                        return new UserRequestDetails(panelId, userRequest);
                    }
                }), Model.of(-1)).setOutputMarkupId(true));
            }
        };

        urDataView.setItemsPerPage(rowsPerPage);
        urDataView.setOutputMarkupId(true);
        paginationContainer.add(urDataView);
        paginationContainer.add(new AjaxPagingNavigator("navigator", urDataView));

        // autocomplete select with bpmnProcesses
        final BpmnProcessesAjaxPanel bpmnProcesses =
                new BpmnProcessesAjaxPanel("bpmnProcesses", "bpmnProcesses", bpmnProcessModel,
                        new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        if (StringUtils.isNotBlank(bpmnProcessModel.getObject())) {
                            try {
                                userRequestRestClient.start(bpmnProcessModel.getObject(), null);
                            } catch (Exception e) {
                                LOG.error("Unable to start bpmnProcess [{}]", bpmnProcessModel.getObject(), e);
                                SyncopeEnduserSession.get()
                                        .error(String.format("Unable to start bpmnProcess [%s]", e.getMessage()));
                                notificationPanel.refresh(target);
                            }
                            target.add(paginationContainer);
                        }
                    }
                });
        bpmnProcesses.setChoices(restClient.getDefinitions().stream()
                .filter(definition -> !definition.isUserWorkflow())
                .map(definition -> definition.getKey()).collect(Collectors.toList()));
        paginationContainer.add(bpmnProcesses);

        body.add(paginationContainer);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        navbar.setActiveNavItem(getClass().getSimpleName().toLowerCase());
    }

    public class UserRequestDetails extends Panel {

        private static final long serialVersionUID = -2447602429647965090L;

        public UserRequestDetails(final String id, final UserRequest userRequest) {
            super(id);

            final UserRequestForm formTO = userRequest.getHasForm()
                    ? userRequestRestClient.getForm(userRequest.getTaskId()).orElse(null)
                    : null;

            add(formTO == null || formTO.getProperties() == null || formTO.getProperties().isEmpty()
                    ? new Fragment("fragContainer", "formDetails", UserRequestDetails.this)
                            .add(new Label("executionId", userRequest.getExecutionId()))
                            .add(new Label("startTime", userRequest.getStartTime()))
                    : new Fragment("fragContainer", "formProperties", UserRequestDetails.this)
                            .add(new Form<>("userRequestWrapForm")
                                    .add(new UserRequestFormPanel(
                                            "userRequestFormPanel",
                                            getPageReference(),
                                            formTO,
                                            false) {

                                        private static final long serialVersionUID = 3617895525072546591L;

                                        @Override
                                        protected void viewDetails(final AjaxRequestTarget target) {
                                            // do nothing
                                        }
                                    })
                                    .add(new AjaxButton("submit") {

                                        private static final long serialVersionUID = 4284361595033427185L;

                                        @Override
                                        protected void onSubmit(final AjaxRequestTarget target) {
                                            try {
                                                userRequestRestClient.claimForm(formTO.getTaskId());
                                                userRequestRestClient.submitForm(formTO);
                                                target.add(paginationContainer);
                                            } catch (SyncopeClientException sce) {
                                                LOG.error("Unable to submit user request form for BPMN process [{}]",
                                                        formTO.getBpmnProcess(), sce);
                                                SyncopeEnduserSession.get().error(StringUtils.isBlank(sce.getMessage())
                                                        ? sce.getClass().getName()
                                                        : sce.getMessage());
                                                notificationPanel.refresh(target);
                                            }
                                        }

                                    }.setOutputMarkupId(true))));

            add(new AjaxLink<Void>("delete") {

                private static final long serialVersionUID = 3669569969172391336L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    userRequestRestClient.cancelRequest(userRequest.getExecutionId(), null);
                    target.add(paginationContainer);
                }

            });
        }
    }

    public class URDataProvider implements IDataProvider<UserRequest> {

        private static final long serialVersionUID = 1169386589403139714L;

        protected final int paginatorRows;

        protected final String sortParam;

        public URDataProvider(final int paginatorRows, final String sortParam) {
            this.paginatorRows = paginatorRows;
            this.sortParam = sortParam;
        }

        @Override
        public Iterator<UserRequest> iterator(final long first, final long count) {
            final int page = ((int) first / paginatorRows);
            return userRequestRestClient.getUserRequests((page < 0 ? 0 : page) + 1,
                    paginatorRows,
                    SyncopeEnduserSession.get().getSelfTO().getUsername(),
                    new SortParam<>(sortParam, true)).iterator();
        }

        @Override
        public long size() {
            return userRequestRestClient.countUserRequests();
        }

        @Override
        public IModel<UserRequest> model(final UserRequest ur) {
            return Model.of(ur);
        }
    }

}
