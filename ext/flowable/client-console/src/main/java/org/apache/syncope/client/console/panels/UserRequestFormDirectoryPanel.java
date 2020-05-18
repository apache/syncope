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
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.rest.UserRequestRestClient;
import org.apache.syncope.client.console.panels.UserRequestFormDirectoryPanel.UserRequestFormProvider;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal.WindowClosedCallback;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.widgets.UserRequestFormsWidget;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.FlowableEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class UserRequestFormDirectoryPanel
        extends DirectoryPanel<UserRequestForm, UserRequestForm, UserRequestFormProvider, UserRequestRestClient> {

    private static final long serialVersionUID = -7122136682275797903L;

    private static final String PREF_USER_REQUEST_FORM_PAGINATOR_ROWS = "userrequestform.paginator.rows";

    protected final BaseModal<UserRequestForm> manageFormModal = new BaseModal<UserRequestForm>("outer") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            addSubmitButton();
            size(Modal.Size.Large);
        }

    };

    public UserRequestFormDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();
        setFooterVisibility(false);
        modal.size(Modal.Size.Large);

        addOuterObject(manageFormModal);

        manageFormModal.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);

                Serializable widget = SyncopeConsoleSession.get().getAttribute(UserRequestFormsWidget.class.getName());
                if (widget instanceof UserRequestFormsWidget) {
                    ((UserRequestFormsWidget) widget).refreshLatestAlerts(target);
                }

                manageFormModal.show(false);
            }
        });

        restClient = new UserRequestRestClient();

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
    public ActionsPanel<UserRequestForm> getActions(final IModel<UserRequestForm> model) {
        final ActionsPanel<UserRequestForm> panel = super.getActions(model);

        panel.add(new ActionLink<UserRequestForm>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                claimForm(model.getObject().getTaskId());
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                target.add(container);
            }

        }, ActionLink.ActionType.CLAIM, FlowableEntitlement.USER_REQUEST_FORM_CLAIM);

        panel.add(new ActionLink<UserRequestForm>() {

            private static final long serialVersionUID = -4496313424398213416L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                unclaimForm(model.getObject().getTaskId());
                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                UserRequestFormDirectoryPanel.this.getTogglePanel().close(target);
                target.add(container);
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected boolean statusCondition(final UserRequestForm modelObject) {
                return SyncopeConsoleSession.get().getSelfTO().getUsername().
                        equals(model.getObject().getAssignee());
            }

        }, ActionLink.ActionType.UNCLAIM, FlowableEntitlement.USER_REQUEST_FORM_UNCLAIM);

        panel.add(new ActionLink<UserRequestForm>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                manageFormModal.setFormModel(new CompoundPropertyModel<>(model.getObject()));

                target.add(manageFormModal.setContent(new UserRequestFormModal(manageFormModal, pageRef, model.
                        getObject()) {

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
        panel.add(new ActionLink<UserRequestForm>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserRequestForm ignore) {
                modal.setFormModel(new CompoundPropertyModel<>(model.getObject()));

                UserRequestForm formTO = model.getObject();
                UserTO newUserTO;
                UserTO previousUserTO;
                if (formTO.getUserUR() == null) {
                    newUserTO = formTO.getUserTO();
                    if (newUserTO != null) {
                        // SYNCOPE-1563 do not use the password into formTO.getUserTO()
                        newUserTO.setPassword(null);
                    }
                    previousUserTO = null;
                } else if (formTO.getUserTO() == null) {
                    // make it stronger by handling possible NPE
                    previousUserTO = new UserTO();
                    previousUserTO.setKey(formTO.getUserUR().getKey());
                    newUserTO = AnyOperations.patch(previousUserTO, formTO.getUserUR());
                } else {
                    previousUserTO = formTO.getUserTO();
                    formTO.getUserTO().setKey(formTO.getUserUR().getKey());
                    formTO.getUserTO().setPassword(null);
                    newUserTO = AnyOperations.patch(formTO.getUserTO(), formTO.getUserUR());
                }

                AjaxWizard.EditItemActionEvent<UserTO> editItemActionEvent =
                        new AjaxWizard.EditItemActionEvent<>(newUserTO, target);
                editItemActionEvent.forceModalPanel(new FormUserWizardBuilder(
                        model.getObject(),
                        previousUserTO,
                        newUserTO,
                        AnyTypeRestClient.read(AnyTypeKind.USER.name()).getClasses(),
                        AnyLayoutUtils.fetch(List.of(AnyTypeKind.USER.name())).getUser(),
                        pageRef
                ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));

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

    protected static class UserRequestFormProvider extends DirectoryDataProvider<UserRequestForm> {

        private static final long serialVersionUID = -2311716167583335852L;

        public UserRequestFormProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("createTime", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<UserRequestForm> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return UserRequestRestClient.getForms((page < 0 ? 0 : page) + 1, paginatorRows, getSort()).iterator();
        }

        @Override
        public long size() {
            return UserRequestRestClient.countForms();
        }

        @Override
        public IModel<UserRequestForm> model(final UserRequestForm form) {
            return new IModel<UserRequestForm>() {

                private static final long serialVersionUID = -2566070996511906708L;

                @Override
                public UserRequestForm getObject() {
                    return form;
                }
            };
        }
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    private void claimForm(final String taskId) {
        try {
            UserRequestRestClient.claimForm(taskId);
        } catch (SyncopeClientException scee) {
            SyncopeConsoleSession.get().onException(scee);
        }
    }

    private void unclaimForm(final String taskId) {
        try {
            UserRequestRestClient.unclaimForm(taskId);
        } catch (SyncopeClientException scee) {
            SyncopeConsoleSession.get().onException(scee);
        }
    }

    private class FormUserWizardBuilder extends UserWizardBuilder {

        private static final long serialVersionUID = 1854981134836384069L;

        private final UserRequestForm formTO;

        FormUserWizardBuilder(
                final UserRequestForm formTO,
                final UserTO previousUserTO,
                final UserTO userTO,
                final List<String> anyTypeClasses,
                final UserFormLayoutInfo formLayoutInfo,
                final PageReference pageRef) {

            super(previousUserTO, userTO, anyTypeClasses, formLayoutInfo, pageRef);
            this.formTO = formTO;
        }

        @Override
        protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
            UserTO inner = modelObject.getInnerObject();

            UserUR userUR = AnyOperations.diff(inner, formTO.getUserTO(), false);

            if (StringUtils.isNotBlank(inner.getPassword())) {
                PasswordPatch passwordPatch = new PasswordPatch.Builder().
                        value(inner.getPassword()).onSyncope(true).resources(inner.
                        getResources()).
                        build();
                userUR.setPassword(passwordPatch);
            }

            // update just if it is changed
            ProvisioningResult<UserTO> result;
            if (userUR.isEmpty()) {
                result = new ProvisioningResult<>();
                result.setEntity(inner);
            } else {
                result = userRestClient.update(getOriginalItem().getInnerObject().getETagValue(), userUR);
                UserRequestRestClient.getForm(result.getEntity().getKey())
                        .ifPresent(form -> claimForm(form.getTaskId()));
            }

            return result;
        }
    }
}
