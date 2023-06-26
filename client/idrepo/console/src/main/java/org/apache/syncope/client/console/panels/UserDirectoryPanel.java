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
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.audit.AuditHistoryModal;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.notifications.NotificationTasks;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.status.ChangePasswordModal;
import org.apache.syncope.client.console.tasks.AnyPropagationTasks;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.info.PlatformInfo;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class UserDirectoryPanel extends AnyDirectoryPanel<UserTO, UserRestClient> {

    private static final long serialVersionUID = -1100228004207271270L;

    protected UserDirectoryPanel(final String id, final Builder builder) {
        this(id, builder, true);
    }

    protected UserDirectoryPanel(final String id, final Builder builder, final boolean wizardInModal) {
        super(id, builder, wizardInModal);

        altDefaultModal.setWindowClosedCallback(target -> {
            updateResultTable(target);
            modal.show(false);
        });
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_USERS_PAGINATOR_ROWS;
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

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                target.add(displayAttributeModal.setContent(new UserDisplayAttributesModalPanel<>(
                        displayAttributeModal,
                        page.getPageReference(),
                        plainSchemas.stream().map(PlainSchemaTO::getKey).collect(Collectors.toList()),
                        derSchemas.stream().map(DerSchemaTO::getKey).collect(Collectors.toList()))));

                displayAttributeModal.header(new ResourceModel("any.attr.display"));
                displayAttributeModal.addSubmitButton();
                displayAttributeModal.show(true);
            }

            @Override
            protected boolean statusCondition(final Serializable modelObject) {
                return wizardInModal;
            }
        }, ActionType.CHANGE_VIEW, IdRepoEntitlement.USER_READ).hideLabel();
        return panel;
    }

    @Override
    public ActionsPanel<UserTO> getActions(final IModel<UserTO> model) {
        final ActionsPanel<UserTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                send(UserDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                new UserWrapper(restClient.read(model.getObject().getKey())), target));
            }
        }, ActionType.EDIT,
                String.format("%s,%s", IdRepoEntitlement.USER_READ, IdRepoEntitlement.USER_UPDATE)).
                setRealms(realm, model.getObject().getDynRealms());

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                try {
                    model.setObject(restClient.read(model.getObject().getKey()));
                    restClient.mustChangePassword(
                            model.getObject().getETagValue(),
                            !model.getObject().isMustChangePassword(),
                            model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While actioning object {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionType.MUSTCHANGEPASSWORD, IdRepoEntitlement.USER_UPDATE).
                setRealms(realm, model.getObject().getDynRealms());

        if (wizardInModal) {
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -4875218360625971340L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    model.setObject(restClient.read(model.getObject().getKey()));
                    IModel<AnyWrapper<UserTO>> formModel = new CompoundPropertyModel<>(
                            new UserWrapper(model.getObject()));
                    displayAttributeModal.setFormModel(formModel);

                    target.add(displayAttributeModal.setContent(new ChangePasswordModal(
                            displayAttributeModal,
                            new UserWrapper(model.getObject()),
                            pageRef)));

                    displayAttributeModal.header(new Model<>(
                            getString("any.edit", new Model<>(new UserWrapper(model.getObject())))));

                    displayAttributeModal.size(Modal.Size.Large);
                    displayAttributeModal.show(true);
                }
            }, ActionType.PASSWORD_MANAGEMENT, IdRepoEntitlement.USER_UPDATE).
                    setRealms(realm, model.getObject().getDynRealms());

            PlatformInfo platformInfo = SyncopeConsoleSession.get().getAnonymousClient().platform();
            if (platformInfo.isPwdResetAllowed() && !platformInfo.isPwdResetRequiringSecurityQuestions()) {
                panel.add(new ActionLink<>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                        try {
                            SyncopeConsoleSession.get().getAnonymousClient().getService(UserSelfService.class).
                                    requestPasswordReset(model.getObject().getUsername(), null);

                            SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            LOG.error("While actioning object {}", model.getObject().getKey(), e);
                            SyncopeConsoleSession.get().onException(e);
                        }
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionType.REQUEST_PASSWORD_RESET, IdRepoEntitlement.USER_UPDATE).
                        setRealms(realm, model.getObject().getDynRealms());
            }

            SyncopeWebApplication.get().getAnyDirectoryPanelAdditionalActionLinksProvider().get(
                    model,
                    realm,
                    altDefaultModal,
                    getString("any.edit", new Model<>(new UserWrapper(model.getObject()))),
                    this,
                    pageRef).forEach(panel::add);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    target.add(utilityModal.setContent(new AnyPropagationTasks(
                            utilityModal, AnyTypeKind.USER, model.getObject().getKey(), pageRef)));

                    utilityModal.header(new StringResourceModel("any.propagation.tasks", model));
                    utilityModal.show(true);
                }
            }, ActionType.PROPAGATION_TASKS, IdRepoEntitlement.TASK_LIST);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    target.add(utilityModal.setContent(
                            new NotificationTasks(AnyTypeKind.USER, model.getObject().getKey(), pageRef)));
                    utilityModal.header(new StringResourceModel("any.notification.tasks", model));
                    utilityModal.show(true);
                    target.add(utilityModal);
                }
            }, ActionType.NOTIFICATION_TASKS, IdRepoEntitlement.TASK_LIST);
        }

        if (wizardInModal) {
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -1978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                    model.setObject(restClient.read(model.getObject().getKey()));
                    target.add(altDefaultModal.setContent(new AuditHistoryModal<>(
                            null,
                            null,
                            model.getObject(),
                            IdRepoEntitlement.USER_UPDATE,
                            auditRestClient) {

                        private static final long serialVersionUID = 959378158400669867L;

                        @Override
                        protected void restore(final String json, final AjaxRequestTarget target) {
                            // The original audit record masks the password and the security
                            // answer; so we cannot use the audit record to resurrect the entry
                            // based on mask data.
                            //
                            // The method behavior below will reset the audit record such
                            // that the current security answer and the password for the object
                            // are always maintained, and such properties for the
                            // user cannot be restored using audit records.
                            UserTO original = model.getObject();
                            try {
                                UserTO updated = MAPPER.readValue(json, UserTO.class);
                                UserUR updateReq = AnyOperations.diff(updated, original, false);
                                updateReq.setPassword(null);
                                updateReq.setSecurityAnswer(null);
                                ProvisioningResult<UserTO> result =
                                        restClient.update(original.getETagValue(), updateReq);
                                model.getObject().setLastChangeDate(result.getEntity().getLastChangeDate());

                                SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                                target.add(container);
                            } catch (Exception e) {
                                LOG.error("While restoring user {}", model.getObject().getKey(), e);
                                SyncopeConsoleSession.get().onException(e);
                            }
                            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        }
                    }));

                    altDefaultModal.header(new Model<>(
                            getString("auditHistory.title", new Model<>(new UserWrapper(model.getObject())))));

                    altDefaultModal.show(true);
                }
            }, ActionType.VIEW_AUDIT_HISTORY,
                    String.format("%s,%s", IdRepoEntitlement.USER_READ, IdRepoEntitlement.AUDIT_LIST)).
                    setRealms(realm, model.getObject().getDynRealms());
        }

        panel.add(new ActionLink<>() {

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
        }, ActionType.CLONE, IdRepoEntitlement.USER_CREATE).setRealm(realm);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                try {
                    restClient.delete(model.getObject().getETagValue(), model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting user {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected boolean statusCondition(final UserTO modelObject) {
                return realm.startsWith(SyncopeConstants.ROOT_REALM);
            }
        }, ActionType.DELETE, IdRepoEntitlement.USER_DELETE, true).setRealm(realm);

        return panel;
    }

    public static class Builder extends AnyDirectoryPanel.Builder<UserTO, UserRestClient> {

        private static final long serialVersionUID = -6603152478702381900L;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final UserRestClient restClient,
                final String type,
                final PageReference pageRef) {

            super(anyTypeClassTOs, restClient, type, pageRef);
            setShowResultPage(true);
        }

        @Override
        protected WizardMgtPanel<AnyWrapper<UserTO>> newInstance(final String id, final boolean wizardInModal) {
            return new UserDirectoryPanel(id, this, wizardInModal);
        }
    }
}
