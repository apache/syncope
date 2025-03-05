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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.UserRequestFormDirectoryPanel.UserRequestFormProvider;
import org.apache.syncope.client.console.panels.UserRequestsPanel.UserRequestSearchEvent;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.widgets.UserRequestFormsWidget;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class UserRequestFormDirectoryPanel
        extends DirectoryPanel<UserRequestForm, UserRequestForm, UserRequestFormProvider, UserRequestRestClient> {

    private static final long serialVersionUID = -7122136682275797903L;

    protected static final String PREF_USER_REQUEST_FORM_PAGINATOR_ROWS = "userrequestform.paginator.rows";

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected RoleRestClient roleRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    protected final BaseModal<UserRequestForm> manageFormModal = new BaseModal<>("outer") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            addSubmitButton();
            size(Modal.Size.Large);
        }
    };

    protected String keyword;

    public UserRequestFormDirectoryPanel(
            final String id,
            final UserRequestRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        disableCheckBoxes();
        setFooterVisibility(false);
        modal.size(Modal.Size.Large);

        addOuterObject(manageFormModal);

        manageFormModal.setWindowClosedCallback(target -> {
            updateResultTable(target);

            Serializable widget = SyncopeConsoleSession.get().getAttribute(UserRequestFormsWidget.class.getName());
            if (widget instanceof final UserRequestFormsWidget components) {
                components.refreshLatestAlerts(target);
            }

            manageFormModal.show(false);
        });

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, FlowableEntitlement.USER_REQUEST_FORM_SUBMIT);
    }

    @Override
    protected List<IColumn<UserRequestForm, String>> getColumns() {
        List<IColumn<UserRequestForm, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new ResourceModel("bpmnProcess"), "bpmnProcess", "bpmnProcess"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("key"), "formKey", "formKey"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("username"), "username"));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("createTime"), "createTime", "createTime"));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("dueDate"), "dueDate", "dueDate"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("assignee"), "assignee", "assignee"));

        return columns;
    }

    @Override
    protected ActionLinksTogglePanel<UserRequestForm> actionTogglePanel() {
        return new ActionLinksTogglePanel<>(Constants.OUTER, pageRef) {

            private static final long serialVersionUID = -7688359318035249200L;

            @Override
            public void updateHeader(final AjaxRequestTarget target, final Serializable object) {
                if (object instanceof final UserRequestForm userRequestForm) {
                    setHeader(target, StringUtils.abbreviate(
                            userRequestForm.getUsername(), HEADER_FIRST_ABBREVIATION));
                } else {
                    super.updateHeader(target, object);
                }
            }
        };
    }

    @Override
    public ActionsPanel<UserRequestForm> getActions(final IModel<UserRequestForm> model) {
        final ActionsPanel<UserRequestForm> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                try {
                    restClient.claimForm(model.getObject().getTaskId());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

        }, ActionLink.ActionType.CLAIM, FlowableEntitlement.USER_REQUEST_FORM_CLAIM);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -4496313424398213416L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                try {
                    restClient.unclaimForm(model.getObject().getTaskId());
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    UserRequestFormDirectoryPanel.this.getTogglePanel().close(target);
                    target.add(container);
                } catch (Exception e) {
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected boolean statusCondition(final UserRequestForm modelObject) {
                return SyncopeConsoleSession.get().getSelfTO().getUsername().
                        equals(model.getObject().getAssignee());
            }

        }, ActionLink.ActionType.UNCLAIM, FlowableEntitlement.USER_REQUEST_FORM_UNCLAIM);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                manageFormModal.setFormModel(new CompoundPropertyModel<>(model.getObject()));

                target.add(manageFormModal.setContent(
                        new UserRequestFormModal(manageFormModal, pageRef, model.getObject()) {

                    private static final long serialVersionUID = 5546519445061007248L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target) {
                        try {
                            super.onSubmit(target);

                            UserRequestFormDirectoryPanel.this.getTogglePanel().close(target);
                        } catch (SyncopeClientException e) {
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                manageFormModal.header(new Model<>(getString("form.manage", new Model<>(model.getObject()))));
                manageFormModal.show(true);
            }

            @Override
            protected boolean statusCondition(final UserRequestForm modelObject) {
                return SyncopeConsoleSession.get().getSelfTO().getUsername().
                        equals(model.getObject().getAssignee());
            }

        }, ActionLink.ActionType.MANAGE_APPROVAL, FlowableEntitlement.USER_REQUEST_FORM_SUBMIT);

        // SYNCOPE-1200 edit user while in approval state
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                modal.setFormModel(new CompoundPropertyModel<>(model.getObject()));

                UserRequestForm formTO = model.getObject();
                UserTO newUserTO;
                if (formTO.getUserUR() == null) {
                    newUserTO = formTO.getUserTO();
                    if (newUserTO != null) {
                        // SYNCOPE-1563 do not use the password into formTO.getUserTO()
                        newUserTO.setPassword(null);
                    }
                } else if (formTO.getUserTO() == null) {
                    // make it stronger by handling possible NPE
                    UserTO previousUserTO = new UserTO();
                    previousUserTO.setKey(formTO.getUserUR().getKey());
                    newUserTO = AnyOperations.patch(previousUserTO, formTO.getUserUR());
                } else {
                    formTO.getUserTO().setKey(formTO.getUserUR().getKey());
                    formTO.getUserTO().setPassword(null);
                    newUserTO = AnyOperations.patch(formTO.getUserTO(), formTO.getUserUR());
                }

                AjaxWizard.EditItemActionEvent<UserTO> editItemActionEvent =
                        new AjaxWizard.EditItemActionEvent<>(newUserTO, target);
                editItemActionEvent.forceModalPanel(AnyLayoutUtils.newLayoutInfo(newUserTO,
                        anyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses(),
                        AnyLayoutUtils.fetch(roleRestClient, List.of(AnyTypeKind.USER.name())).getUser(),
                        userRestClient,
                        pageRef).
                        build(BaseModal.CONTENT_ID, 0, AjaxWizard.Mode.EDIT_APPROVAL));

                send(UserRequestFormDirectoryPanel.this, Broadcast.EXACT, editItemActionEvent);
            }

            @Override
            protected boolean statusCondition(final UserRequestForm modelObject) {
                return SyncopeConsoleSession.get().getSelfTO().getUsername().
                        equals(model.getObject().getAssignee());
            }

        }, ActionLink.ActionType.EDIT_APPROVAL, FlowableEntitlement.USER_REQUEST_FORM_SUBMIT);

        return panel;
    }

    @Override
    protected UserRequestFormProvider dataProvider() {
        return new UserRequestFormProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return PREF_USER_REQUEST_FORM_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof UserRequestSearchEvent) {
            UserRequestSearchEvent payload = UserRequestSearchEvent.class.cast(event.getPayload());
            keyword = payload.getKeyword();

            updateResultTable(payload.getTarget());
        } else {
            super.onEvent(event);
        }
    }

    protected final class UserRequestFormProvider extends DirectoryDataProvider<UserRequestForm> {

        private static final long serialVersionUID = -2311716167583335852L;

        public UserRequestFormProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("createTime", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<UserRequestForm> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.listForms(
                    keyword, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).iterator();
        }

        @Override
        public long size() {
            return restClient.countForms(keyword);
        }

        @Override
        public IModel<UserRequestForm> model(final UserRequestForm form) {
            return Model.of(form);
        }
    }
}
