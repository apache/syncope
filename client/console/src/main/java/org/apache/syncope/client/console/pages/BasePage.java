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
package org.apache.syncope.client.console.pages;

import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class BasePage extends AbstractBasePage implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 1571997737305598502L;

    private final UserWorkflowRestClient userWorkflowRestClient = new UserWorkflowRestClient();

    public BasePage() {
        this(null);
    }

    private String getLIContainerId(final String linkId) {
        return linkId + "LI";
    }

    private String getULContainerId(final String linkId) {
        return linkId + "UL";
    }

    public BasePage(final PageParameters parameters) {
        super(parameters);

        // header, footer
        add(new Label("version", SyncopeConsoleApplication.get().getVersion()));
        add(new Label("username", SyncopeConsoleSession.get().getSelfTO().getUsername()));

        final WebMarkupContainer todosContainer = new WebMarkupContainer("todosContainer");
        add(todosContainer);
        Label todos = new Label("todos", "0");
        todosContainer.add(todos);
        if (SyncopeConsoleSession.get().owns(StandardEntitlement.WORKFLOW_FORM_LIST)) {
            todos.setDefaultModelObject(userWorkflowRestClient.getForms().size());
        }
        MetaDataRoleAuthorizationStrategy.authorize(todosContainer, WebPage.RENDER,
                StandardEntitlement.WORKFLOW_FORM_LIST);

        // menu
        WebMarkupContainer liContainer = new WebMarkupContainer(getLIContainerId("dashboard"));
        add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("dashboard", Dashboard.class));

        liContainer = new WebMarkupContainer(getLIContainerId("realms"));
        add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("realms", Realms.class));

        liContainer = new WebMarkupContainer(getLIContainerId("topology"));
        add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("topology", Topology.class));

        liContainer = new WebMarkupContainer(getLIContainerId("reports"));
        add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("reports", Reports.class));

        WebMarkupContainer confLIContainer = new WebMarkupContainer(getLIContainerId("configuration"));
        add(confLIContainer);
        WebMarkupContainer confULContainer = new WebMarkupContainer(getULContainerId("configuration"));
        confLIContainer.add(confULContainer);

        liContainer = new WebMarkupContainer(getLIContainerId("securityQuestions"));
        confULContainer.add(liContainer);
        final BookmarkablePageLink<Page> securityQuestionLink = new BookmarkablePageLink<>(
                "securityQuestions", SecurityQuestions.class);
        liContainer.add(securityQuestionLink);

        liContainer = new WebMarkupContainer(getLIContainerId("workflow"));
        confULContainer.add(liContainer);
        final BookmarkablePageLink<Page> workflowLink = new BookmarkablePageLink<>("workflow", Workflow.class);
        MetaDataRoleAuthorizationStrategy.authorize(
                workflowLink, WebPage.ENABLE, StandardEntitlement.WORKFLOW_DEF_READ);
        liContainer.add(workflowLink);

        liContainer = new WebMarkupContainer(getLIContainerId("logs"));
        confULContainer.add(liContainer);
        final BookmarkablePageLink<Page> logsLink = new BookmarkablePageLink<>("logs", Logs.class);
        MetaDataRoleAuthorizationStrategy.authorize(logsLink, WebPage.ENABLE, StandardEntitlement.LOG_LIST);
        liContainer.add(logsLink);

        liContainer = new WebMarkupContainer(getLIContainerId("types"));
        confULContainer.add(liContainer);
        BookmarkablePageLink<Page> typesLink = new BookmarkablePageLink<>("types", Types.class);
        MetaDataRoleAuthorizationStrategy.authorize(typesLink, WebPage.ENABLE, StandardEntitlement.SCHEMA_LIST);
        liContainer.add(typesLink);

        liContainer = new WebMarkupContainer(getLIContainerId("policies"));
        confULContainer.add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("policies", Policies.class));

        liContainer = new WebMarkupContainer(getLIContainerId("layouts"));
        confULContainer.add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("layouts", Layouts.class));

        liContainer = new WebMarkupContainer(getLIContainerId("notifications"));
        confULContainer.add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("notifications", Notifications.class));

        liContainer = new WebMarkupContainer(getLIContainerId("camelRoutes"));
        add(liContainer);
        liContainer.add(new BookmarkablePageLink<>("camelRoutes", CamelRoutes.class));

        add(new Label("domain", SyncopeConsoleSession.get().getDomain()));
        add(new BookmarkablePageLink<Page>("logout", Logout.class));

        // set 'active' menu item
        // 1. check if current class is set to top-level menu
        Component containingLI = get(getLIContainerId(getClass().getSimpleName().toLowerCase()));
        // 2. if not, check if it is under 'Configuration'
        if (containingLI == null) {
            containingLI = confULContainer.get(getLIContainerId(getClass().getSimpleName().toLowerCase()));
        }
        // 3. when found, set CSS coordinates for menu
        if (containingLI != null) {
            containingLI.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("class", "active");
                }
            });

            if (confULContainer.getId().equals(containingLI.getParent().getId())) {
                confULContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview-menu menu-open");
                        tag.put("style", "display: block;");
                    }

                });

                confLIContainer.add(new Behavior() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("class", "treeview active");
                    }
                });
            }
        }
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return "veil";
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     *
     * @param window window
     * @param container container
     */
    public void setWindowClosedCallback(final ModalWindow window, final WebMarkupContainer container) {
        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                if (container != null) {
                    target.add(container);
                }

                if (isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    setModalResult(false);
                }
            }
        });
    }

    /**
     * Set a WindowClosedCallback for a Modal instance.
     *
     * @param modal window
     * @param container container
     */
    public void setWindowClosedCallback(final BaseModal<?> modal, final WebMarkupContainer container) {
        modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                if (container != null) {
                    target.add(container);
                }

                if (isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    feedbackPanel.refresh(target);
                    setModalResult(false);
                }
            }
        });
    }
}
