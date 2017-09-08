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
package org.apache.syncope.client.console.approvals;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.UserWorkflowRestClient;
import org.apache.syncope.client.console.approvals.ApprovalDirectoryPanel.ApprovalProvider;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.to.WorkflowFormTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class ApprovalDirectoryPanel
        extends DirectoryPanel<WorkflowFormTO, WorkflowFormTO, ApprovalProvider, UserWorkflowRestClient> {

    private static final long serialVersionUID = -7122136682275797903L;

    protected final BaseModal<WorkflowFormTO> manageApprovalModal = new BaseModal<WorkflowFormTO>("outer") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            addSubmitButton();
            size(Modal.Size.Large);
        }

    };

    public ApprovalDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();
        setFooterVisibility(false);
        modal.size(Modal.Size.Large);

        addOuterObject(manageApprovalModal);

        manageApprovalModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);
                ((BasePage) pageReference.getPage()).getApprovalsWidget().refreshLatestAlerts(target);
                manageApprovalModal.show(false);
            }
        });

        restClient = new UserWorkflowRestClient();

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.WORKFLOW_FORM_SUBMIT);
    }

    @Override
    protected List<IColumn<WorkflowFormTO, String>> getColumns() {

        List<IColumn<WorkflowFormTO, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(
                new ResourceModel("taskId"), "taskId", "taskId"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("key"), "key", "key"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("username"), "username", "username"));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("createTime"), "createTime", "createTime"));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("dueDate"), "dueDate", "dueDate"));
        columns.add(new PropertyColumn<>(new ResourceModel("owner"), "owner", "owner"));

        return columns;
    }

    @Override
    public ActionsPanel<WorkflowFormTO> getActions(final IModel<WorkflowFormTO> model) {
        final ActionsPanel<WorkflowFormTO> panel = super.getActions(model);

        panel.add(new ActionLink<WorkflowFormTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final WorkflowFormTO ignore) {
                claimForm(model.getObject().getTaskId());
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                target.add(container);
            }
        }, ActionLink.ActionType.CLAIM, StandardEntitlement.WORKFLOW_FORM_CLAIM);

        panel.add(new ActionLink<WorkflowFormTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final WorkflowFormTO ignore) {
                final IModel<WorkflowFormTO> formModel = new CompoundPropertyModel<>(model.getObject());
                manageApprovalModal.setFormModel(formModel);

                target.add(manageApprovalModal.setContent(new ApprovalModal(manageApprovalModal, pageRef, model.
                        getObject()) {

                    private static final long serialVersionUID = 5546519445061007248L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        try {
                            super.onSubmit(target, form);

                            ApprovalDirectoryPanel.this.getTogglePanel().close(target);
                        } catch (SyncopeClientException e) {
                            SyncopeConsoleSession.get().error(getString(Constants.ERROR) + ": " + e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }

                }));

                manageApprovalModal.header(new Model<>(getString("approval.manage", new Model<>(model.getObject()))));
                manageApprovalModal.show(true);
            }

            @Override
            protected boolean statusCondition(final WorkflowFormTO modelObject) {
                return SyncopeConsoleSession.get().getSelfTO().getUsername().
                        equals(model.getObject().getOwner());
            }

        }, ActionLink.ActionType.MANAGE_APPROVAL, StandardEntitlement.WORKFLOW_FORM_READ);

        // SYNCOPE-1200 edit user while in approval state
        panel.add(new ActionLink<WorkflowFormTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final WorkflowFormTO ignore) {
                final IModel<WorkflowFormTO> formModel = new CompoundPropertyModel<>(model.getObject());
                modal.setFormModel(formModel);

                final WorkflowFormTO formTO = formModel.getObject();
                final UserTO newUserTO;
                final UserTO previousUserTO;
                if (formTO.getUserPatch() == null) {
                    newUserTO = formTO.getUserTO();
                    previousUserTO = null;
                } else if (formTO.getUserTO() == null) {
                    // make it stronger by handling possible NPE
                    previousUserTO = new UserTO();
                    previousUserTO.setKey(formTO.getUserPatch().getKey());
                    newUserTO = AnyOperations.patch(previousUserTO, formTO.getUserPatch());
                } else {
                    formTO.getUserTO().setKey(formTO.getUserPatch().getKey());
                    newUserTO = AnyOperations.patch(formTO.getUserTO(), formTO.getUserPatch());
                    previousUserTO = formTO.getUserTO();
                }

                AjaxWizard.EditItemActionEvent<UserTO> editItemActionEvent =
                        new AjaxWizard.EditItemActionEvent<>(newUserTO, target);

                editItemActionEvent.forceModalPanel(
                        new ApprovalUserWizardBuilder(
                                target,
                                formModel.getObject(),
                                previousUserTO,
                                newUserTO,
                                new AnyTypeRestClient().read(AnyTypeKind.USER.name()).getClasses(),
                                FormLayoutInfoUtils.fetch(Collections.singletonList(AnyTypeKind.USER.name())).getLeft(),
                                pageRef
                        ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));

                send(ApprovalDirectoryPanel.this, Broadcast.EXACT, editItemActionEvent);
            }

            @Override
            protected boolean statusCondition(final WorkflowFormTO modelObject) {
                return SyncopeConsoleSession.get().getSelfTO().getUsername().
                        equals(model.getObject().getOwner());
            }

        }, ActionLink.ActionType.EDIT_APPROVAL, StandardEntitlement.WORKFLOW_FORM_SUBMIT);

        return panel;
    }

    @Override
    protected ApprovalProvider dataProvider() {
        return new ApprovalProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_WORKFLOW_FORM_PAGINATOR_ROWS;
    }

    public static class ApprovalProvider extends DirectoryDataProvider<WorkflowFormTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private final SortableDataProviderComparator<WorkflowFormTO> comparator;

        private final UserWorkflowRestClient restClient = new UserWorkflowRestClient();

        public ApprovalProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("createTime", SortOrder.ASCENDING);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<WorkflowFormTO> iterator(final long first, final long count) {
            final List<WorkflowFormTO> list = restClient.getForms();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getForms().size();
        }

        @Override
        public IModel<WorkflowFormTO> model(final WorkflowFormTO configuration) {
            return new AbstractReadOnlyModel<WorkflowFormTO>() {

                private static final long serialVersionUID = -2566070996511906708L;

                @Override
                public WorkflowFormTO getObject() {
                    return configuration;
                }
            };
        }
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    private void claimForm(final String taskId) {
        try {
            restClient.claimForm(taskId);
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
        } catch (SyncopeClientException scee) {
            SyncopeConsoleSession.get().error(getString(Constants.ERROR) + ": " + scee.getMessage());
        }
    }

    private class ApprovalUserWizardBuilder extends UserWizardBuilder {

        private static final long serialVersionUID = 1854981134836384069L;

        private final WorkflowFormTO formTO;

        private final AjaxRequestTarget target;

        ApprovalUserWizardBuilder(
                final AjaxRequestTarget target,
                final WorkflowFormTO formTO,
                final UserTO previousUserTO,
                final UserTO userTO,
                final List<String> anyTypeClasses,
                final UserFormLayoutInfo formLayoutInfo,
                final PageReference pageRef) {

            super(previousUserTO, userTO, anyTypeClasses, formLayoutInfo, pageRef);
            this.formTO = formTO;
            this.target = target;
        }

        @Override
        protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
            UserTO inner = modelObject.getInnerObject();

            ProvisioningResult<UserTO> actual;

            if (formTO.getUserPatch() == null) {
                actual = new ProvisioningResult<>();
                UserTO user = new UserWorkflowRestClient().executeTask("default", inner);
                actual.setEntity(user);
                claimForm(restClient.getFormForUser(actual.getEntity().getKey()).getTaskId());
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            } else {
                UserPatch patch = AnyOperations.diff(inner, formTO.getUserTO(), false);

                if (StringUtils.isNotBlank(inner.getPassword())) {
                    PasswordPatch passwordPatch = new PasswordPatch.Builder().
                            value(inner.getPassword()).onSyncope(true).resources(inner.
                            getResources()).
                            build();
                    patch.setPassword(passwordPatch);
                }
                // update just if it is changed
                if (patch.isEmpty()) {
                    actual = new ProvisioningResult<>();
                    actual.setEntity(inner);
                } else {
                    actual = userRestClient.update(getOriginalItem().getInnerObject().getETagValue(), patch);
                    claimForm(restClient.getFormForUser(actual.getEntity().getKey()).getTaskId());
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }

            }

            return actual;
        }
    }

}
