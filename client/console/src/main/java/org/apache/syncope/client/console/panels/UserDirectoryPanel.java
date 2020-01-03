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
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.notifications.NotificationTasks;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.status.AnyStatusModal;
import org.apache.syncope.client.console.status.ChangePasswordModal;
import org.apache.syncope.client.console.tasks.AnyPropagationTasks;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AuditElements;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserDirectoryPanel extends AnyDirectoryPanel<UserTO, UserRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected final BaseModal<Serializable> wizardWrapperModal = new BaseModal<Serializable>(Constants.OUTER) {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
        }

    };

    protected UserDirectoryPanel(final String id, final Builder builder) {
        this(id, builder, true);
    }

    protected UserDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);

        altDefaultModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);
                modal.show(false);
            }
        });

        wizardWrapperModal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = -6109847349558471532L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                updateResultTable(target);
                modal.show(false);
            }
        });

        wizardWrapperModal.size(Modal.Size.Large);
        addOuterObject(wizardWrapperModal);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_USERS_PAGINATOR_ROWS;
    }

    @Override
    protected String[] getDefaultAttributeSelection() {
        return UserDisplayAttributesModalPanel.DEFAULT_SELECTION;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        List<ActionType> batches = new ArrayList<>();
        batches.add(ActionType.MUSTCHANGEPASSWORD);
        batches.add(ActionType.DELETE);
        batches.add(ActionType.SUSPEND);
        batches.add(ActionType.REACTIVATE);
        return batches;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        final ActionsPanel<Serializable> panel = super.getHeader(componentId);

        panel.add(new ActionLink<Serializable>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(displayAttributeModal.setContent(new UserDisplayAttributesModalPanel<>(
                        displayAttributeModal, page.getPageReference(), pSchemaNames, dSchemaNames)));

                displayAttributeModal.header(new ResourceModel("any.attr.display"));
                displayAttributeModal.addSubmitButton();
                displayAttributeModal.show(true);
            }

            @Override
            protected boolean statusCondition(final Serializable modelObject) {
                return wizardInModal;
            }
        }, ActionType.CHANGE_VIEW, StandardEntitlement.USER_READ).hideLabel();
        return panel;
    }

    @Override
    public ActionsPanel<UserTO> getActions(final IModel<UserTO> model) {
        final ActionsPanel<UserTO> panel = super.getActions(model);

        panel.add(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                send(UserDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new UserWrapper(restClient.read(model.getObject().getKey())),
                                target));
            }
        }, ActionType.EDIT,
                String.format("%s,%s", StandardEntitlement.USER_READ, StandardEntitlement.USER_UPDATE)).
                setRealms(realm, model.getObject().getDynRealms());

        panel.add(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                try {
                    restClient.mustChangePassword(
                            model.getObject().getETagValue(),
                            !model.getObject().isMustChangePassword(),
                            model.getObject().getKey());

                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While actioning object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionType.MUSTCHANGEPASSWORD, StandardEntitlement.USER_UPDATE).
                setRealms(realm, model.getObject().getDynRealms());

        if (wizardInModal) {
            panel.add(new ActionLink<UserTO>() {

                private static final long serialVersionUID = -4875218360625971340L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    IModel<AnyWrapper<UserTO>> formModel = new CompoundPropertyModel<>(
                            new AnyWrapper<>(model.getObject()));
                    displayAttributeModal.setFormModel(formModel);

                    target.add(displayAttributeModal.setContent(new ChangePasswordModal(
                            displayAttributeModal,
                            pageRef,
                            new UserWrapper(model.getObject()))));

                    displayAttributeModal.header(new Model<>(
                            getString("any.edit", new Model<>(new AnyWrapper<>(model.getObject())))));

                    displayAttributeModal.show(true);
                }
            }, ActionType.PASSWORD_MANAGEMENT, StandardEntitlement.USER_UPDATE).
                    setRealms(realm, model.getObject().getDynRealms());

            if (SyncopeConsoleSession.get().getPlatformInfo().isPwdResetAllowed()
                    && !SyncopeConsoleSession.get().getPlatformInfo().isPwdResetRequiringSecurityQuestions()) {

                panel.add(new ActionLink<UserTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        try {
                            SyncopeConsoleSession.get().getAnonymousClient().getService(UserSelfService.class).
                                    requestPasswordReset(model.getObject().getUsername(), null);

                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            LOG.error("While actioning object {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(
                                    StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionType.REQUEST_PASSWORD_RESET, StandardEntitlement.USER_UPDATE).
                        setRealms(realm, model.getObject().getDynRealms());
            }

            panel.add(new ActionLink<UserTO>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    IModel<AnyWrapper<UserTO>> formModel = new CompoundPropertyModel<>(
                            new AnyWrapper<>(model.getObject()));
                    altDefaultModal.setFormModel(formModel);

                    target.add(altDefaultModal.setContent(new AnyStatusModal<>(
                            altDefaultModal,
                            pageRef,
                            formModel.getObject().getInnerObject(),
                            "resource",
                            true)));

                    altDefaultModal.header(new Model<>(
                            getString("any.edit", new Model<>(new AnyWrapper<>(model.getObject())))));

                    altDefaultModal.show(true);
                }
            }, ActionType.ENABLE, StandardEntitlement.USER_UPDATE).
                    setRealms(realm, model.getObject().getDynRealms());

            panel.add(new ActionLink<UserTO>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    model.setObject(restClient.read(model.getObject().getKey()));
                    IModel<AnyWrapper<UserTO>> formModel = new CompoundPropertyModel<>(
                            new AnyWrapper<>(model.getObject()));
                    altDefaultModal.setFormModel(formModel);

                    target.add(altDefaultModal.setContent(new AnyStatusModal<>(
                            altDefaultModal,
                            pageRef,
                            formModel.getObject().getInnerObject(),
                            "resource",
                            false)));

                    altDefaultModal.header(new Model<>(
                            getString("any.edit", new Model<>(new AnyWrapper<>(model.getObject())))));

                    altDefaultModal.show(true);
                }
            }, ActionType.MANAGE_RESOURCES,
                    String.format("%s,%s", StandardEntitlement.USER_READ, StandardEntitlement.USER_UPDATE)).
                    setRealms(realm, model.getObject().getDynRealms());

            panel.add(new ActionLink<UserTO>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    target.add(utilityModal.setContent(new AnyPropagationTasks(
                            utilityModal, AnyTypeKind.USER, model.getObject().getKey(), pageRef)));

                    utilityModal.header(new StringResourceModel("any.propagation.tasks", model));
                    utilityModal.show(true);
                }
            }, ActionType.PROPAGATION_TASKS, StandardEntitlement.TASK_LIST);

            panel.add(new ActionLink<UserTO>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    target.add(utilityModal.setContent(
                            new NotificationTasks(AnyTypeKind.USER, model.getObject().getKey(), pageRef)));

                    utilityModal.header(new StringResourceModel("any.notification.tasks", model));
                    utilityModal.show(true);
                    target.add(utilityModal);
                }
            }, ActionType.NOTIFICATION_TASKS, StandardEntitlement.TASK_LIST);

            panel.add(new ActionLink<UserTO>() {

                private static final long serialVersionUID = 8011039414597736111L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    model.setObject(restClient.read(model.getObject().getKey()));
                    target.add(wizardWrapperModal.setContent(
                            new LinkedAccountModalPanel(wizardWrapperModal, model, pageRef, false)));
                    wizardWrapperModal.header(new ResourceModel("linkedAccounts.title"));
                    wizardWrapperModal.show(true);
                }
            }, ActionType.MANAGE_ACCOUNTS,
                    String.format("%s,%s,%s", StandardEntitlement.USER_READ, StandardEntitlement.USER_UPDATE,
                            StandardEntitlement.RESOURCE_GET_CONNOBJECT));

            panel.add(new ActionLink<UserTO>() {
                private static final long serialVersionUID = 8011039414597736111L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    model.setObject(UserRestClient.class.cast(restClient).read(model.getObject().getKey()));
                    target.add(wizardWrapperModal.setContent(
                      new MergeLinkedAccountsModalPanel(wizardWrapperModal, model, pageRef)));
                    wizardWrapperModal.header(new ResourceModel("mergeLinkedAccounts.title"));
                    wizardWrapperModal.show(true);
                }
                }, ActionType.MERGE_ACCOUNTS,
                        String.format("%s,%s,%s", StandardEntitlement.USER_READ, StandardEntitlement.USER_UPDATE,
                            StandardEntitlement.RESOURCE_GET_CONNOBJECT));
        }

        panel.add(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -1978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                model.setObject(restClient.read(model.getObject().getKey()));
                target.add(altDefaultModal.setContent(new AuditHistoryModal<UserTO>(
                        altDefaultModal,
                        AuditElements.EventCategoryType.LOGIC,
                        "UserLogic",
                        model.getObject(),
                        StandardEntitlement.USER_UPDATE,
                        pageRef) {

                    private static final long serialVersionUID = 959378158400669867L;

                    @Override
                    protected void restore(final String json, final AjaxRequestTarget target) {
                        // The original audit record masks the password and the security
                        // answer; so we cannot use the audit record to resurrect the entry based on mask data.
                        //
                        // The method behavior below will reset the audit record such that the current security
                        // answer and the password for the object are always maintained, and such properties for the
                        // user cannot be restored using audit records.
                        UserTO original = model.getObject();
                        try {
                            UserTO updated = MAPPER.readValue(json, UserTO.class);
                            UserPatch userPatch = AnyOperations.diff(updated, original, false);
                            userPatch.setPassword(null);
                            userPatch.setSecurityAnswer(null);
                            ProvisioningResult<UserTO> result = restClient.update(original.getETagValue(), userPatch);
                            model.getObject().setLastChangeDate(result.getEntity().getLastChangeDate());

                            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            LOG.error("While restoring user {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().error(
                                    StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }));

                altDefaultModal.header(new Model<>(
                        getString("auditHistory.title", new Model<>(new AnyWrapper<>(model.getObject())))));

                altDefaultModal.show(true);
            }
        }, ActionType.VIEW_AUDIT_HISTORY,
                String.format("%s,%s", StandardEntitlement.USER_READ, StandardEntitlement.AUDIT_LIST)).
                setRealms(realm, model.getObject().getDynRealms());

        panel.add(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                UserTO clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                clone.setUsername(model.getObject().getUsername() + "_clone");
                send(UserDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.NewItemActionEvent<>(new UserWrapper(clone), target));
            }

            @Override
            protected boolean statusCondition(final UserTO modelObject) {
                return addAjaxLink.isVisibleInHierarchy() && realm.startsWith(SyncopeConstants.ROOT_REALM);
            }
        }, ActionType.CLONE, StandardEntitlement.USER_CREATE).setRealm(realm);

        panel.add(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                try {
                    restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());

                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting user {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected boolean statusCondition(final UserTO modelObject) {
                return realm.startsWith(SyncopeConstants.ROOT_REALM);
            }
        }, ActionType.DELETE, StandardEntitlement.USER_DELETE, true).setRealm(realm);

        return panel;
    }

    public static class Builder extends AnyDirectoryPanel.Builder<UserTO, UserRestClient> {

        private static final long serialVersionUID = -6603152478702381900L;

        public Builder(final List<AnyTypeClassTO> anyTypeClassTOs, final String type, final PageReference pageRef) {
            super(anyTypeClassTOs, new UserRestClient(), type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<UserTO>> newInstance(final String id, final boolean wizardInModal) {
            return new UserDirectoryPanel(id, this, wizardInModal);
        }
    }
}
