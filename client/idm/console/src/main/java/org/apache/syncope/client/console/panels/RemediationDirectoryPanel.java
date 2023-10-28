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
import org.apache.syncope.client.console.commons.IdMConstants;
import org.apache.syncope.client.console.layout.AnyLayoutUtils;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.RemediationDirectoryPanel.RemediationProvider;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.RemediationRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.any.AnyObjectWizardBuilder;
import org.apache.syncope.client.console.wizards.any.GroupWizardBuilder;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.PasswordPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RemediationDirectoryPanel
        extends DirectoryPanel<RemediationTO, RemediationTO, RemediationProvider, RemediationRestClient> {

    private static final long serialVersionUID = 8525204188127106587L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected RoleRestClient roleRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected AnyObjectRestClient anyObjectRestClient;

    public RemediationDirectoryPanel(
            final String id,
            final RemediationRestClient restClient,
            final PageReference pageRef) {

        super(id, restClient, pageRef, true);
        disableCheckBoxes();
        setFooterVisibility(false);
        modal.size(Modal.Size.Large);

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdMEntitlement.REMEDIATION_REMEDY);
    }

    @Override
    protected List<IColumn<RemediationTO, String>> getColumns() {
        List<IColumn<RemediationTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));
        columns.add(new PropertyColumn<>(
                new ResourceModel("operation"), "operation", "operation"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("anyType"), "anyType", "anyType"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("remoteName"), "remoteName", "remoteName"));
        columns.add(new PropertyColumn<>(
                new ResourceModel("resource"), "resource", "resource"));
        columns.add(new DatePropertyColumn<>(
                new ResourceModel("instant"), "instant", "instant"));

        return columns;
    }

    @Override
    protected ActionsPanel<RemediationTO> getActions(final IModel<RemediationTO> model) {
        ActionsPanel<RemediationTO> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 6193210574968203299L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                modal.header(new ResourceModel("error"));
                modal.setContent(new ExecMessageModal(model.getObject().getError()));
                modal.show(true);
                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW_DETAILS, IdMEntitlement.REMEDIATION_READ);

        if (model.getObject().getOperation() == ResourceOperation.DELETE) {
            String entitlements = StringUtils.join(new String[] {
                IdMEntitlement.REMEDIATION_REMEDY,
                AnyTypeKind.USER.name().equals(model.getObject().getAnyType())
                ? IdRepoEntitlement.USER_DELETE
                : AnyTypeKind.GROUP.name().equals(model.getObject().getAnyType())
                ? IdRepoEntitlement.GROUP_DELETE
                : AnyEntitlement.DELETE.getFor(model.getObject().getAnyType()) }, ",");

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = 6193210574968203299L;

                @Override
                public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                    try {
                        restClient.remedy(model.getObject().getKey(), model.getObject().getKeyPayload());

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                        target.add(container);
                    } catch (SyncopeClientException e) {
                        LOG.error("While performing remediation {}", model.getObject().getKey(), e);
                        SyncopeConsoleSession.get().onException(e);
                    }
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }
            }, ActionLink.ActionType.CLOSE, entitlements, true);
        } else {
            String entitlements = model.getObject().getOperation() == ResourceOperation.CREATE
                    ? StringUtils.join(new String[] {
                IdMEntitlement.REMEDIATION_REMEDY,
                AnyTypeKind.USER.name().equals(model.getObject().getAnyType())
                ? IdRepoEntitlement.USER_CREATE
                : AnyTypeKind.GROUP.name().equals(model.getObject().getAnyType())
                ? IdRepoEntitlement.GROUP_CREATE
                : AnyEntitlement.CREATE.getFor(model.getObject().getAnyType()) }, ",")
                    : StringUtils.join(new String[] {
                IdMEntitlement.REMEDIATION_REMEDY,
                AnyTypeKind.USER.name().equals(model.getObject().getAnyType())
                ? IdRepoEntitlement.USER_UPDATE
                : AnyTypeKind.GROUP.name().equals(model.getObject().getAnyType())
                ? IdRepoEntitlement.GROUP_UPDATE
                : AnyEntitlement.UPDATE.getFor(model.getObject().getAnyType()) }, ",");

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = 6193210574968203299L;

                @Override
                public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                    modal.setFormModel(new CompoundPropertyModel<>(model.getObject()));
                    RemediationTO remediationTO = model.getObject();

                    switch (remediationTO.getAnyType()) {
                        case "USER":
                            UserTO newUserTO;
                            UserTO previousUserTO;
                            if (remediationTO.getAnyURPayload() == null) {
                                newUserTO = new UserTO();
                                EntityTOUtils.toAnyTO(remediationTO.getAnyCRPayload(), newUserTO);
                                previousUserTO = null;
                            } else {
                                previousUserTO = userRestClient.read(remediationTO.getAnyURPayload().getKey());
                                newUserTO = AnyOperations.patch(
                                        previousUserTO, (UserUR) remediationTO.getAnyURPayload());
                            }

                            AjaxWizard.EditItemActionEvent<UserTO> userEvent =
                                    new AjaxWizard.EditItemActionEvent<>(newUserTO, target);
                            userEvent.forceModalPanel(new RemediationUserWizardBuilder(
                                    model.getObject(),
                                    previousUserTO,
                                    newUserTO,
                                    anyTypeRestClient.read(remediationTO.getAnyType()).getClasses(),
                                    AnyLayoutUtils.fetch(roleRestClient, List.of(remediationTO.getAnyType())).getUser(),
                                    userRestClient,
                                    pageRef
                            ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));
                            send(RemediationDirectoryPanel.this, Broadcast.EXACT, userEvent);
                            break;

                        case "GROUP":
                            GroupTO newGroupTO;
                            GroupTO previousGroupTO;
                            if (remediationTO.getAnyURPayload() == null) {
                                newGroupTO = new GroupTO();
                                EntityTOUtils.toAnyTO(remediationTO.getAnyCRPayload(), newGroupTO);
                                previousGroupTO = null;
                            } else {
                                previousGroupTO = groupRestClient.read(remediationTO.getAnyURPayload().getKey());
                                newGroupTO = AnyOperations.patch(
                                        previousGroupTO, (GroupUR) remediationTO.getAnyURPayload());
                            }

                            AjaxWizard.EditItemActionEvent<GroupTO> groupEvent =
                                    new AjaxWizard.EditItemActionEvent<>(newGroupTO, target);
                            groupEvent.forceModalPanel(new RemediationGroupWizardBuilder(
                                    model.getObject(),
                                    previousGroupTO,
                                    newGroupTO,
                                    anyTypeRestClient.read(remediationTO.getAnyType()).getClasses(),
                                    AnyLayoutUtils.fetch(
                                            roleRestClient, List.of(remediationTO.getAnyType())).getGroup(),
                                    pageRef
                            ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));
                            send(RemediationDirectoryPanel.this, Broadcast.EXACT, groupEvent);
                            break;

                        default:
                            AnyObjectTO newAnyObjectTO;
                            AnyObjectTO previousAnyObjectTO;
                            if (remediationTO.getAnyURPayload() == null) {
                                newAnyObjectTO = new AnyObjectTO();
                                EntityTOUtils.toAnyTO(remediationTO.getAnyCRPayload(), newAnyObjectTO);
                                previousAnyObjectTO = null;
                            } else {
                                previousAnyObjectTO = anyObjectRestClient.
                                        read(remediationTO.getAnyURPayload().getKey());
                                newAnyObjectTO = AnyOperations.patch(
                                        previousAnyObjectTO, (AnyObjectUR) remediationTO.getAnyURPayload());
                            }

                            AjaxWizard.EditItemActionEvent<AnyObjectTO> anyObjectEvent =
                                    new AjaxWizard.EditItemActionEvent<>(newAnyObjectTO, target);
                            anyObjectEvent.forceModalPanel(new RemediationAnyObjectWizardBuilder(
                                    model.getObject(),
                                    previousAnyObjectTO,
                                    newAnyObjectTO,
                                    anyTypeRestClient.read(remediationTO.getAnyType()).getClasses(),
                                    AnyLayoutUtils.fetch(roleRestClient, List.of(remediationTO.getAnyType())).
                                            getAnyObjects().get(remediationTO.getAnyType()),
                                    pageRef
                            ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));
                            send(RemediationDirectoryPanel.this, Broadcast.EXACT, anyObjectEvent);
                    }
                }
            }, ActionLink.ActionType.EDIT, entitlements);
        }

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = 6193210574968203299L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdMEntitlement.REMEDIATION_DELETE, true);

        return panel;
    }

    @Override
    protected RemediationProvider dataProvider() {
        return new RemediationProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdMConstants.PREF_REMEDIATION_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    public class RemediationProvider extends DirectoryDataProvider<RemediationTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        public RemediationProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("instant", SortOrder.ASCENDING);
        }

        @Override
        public Iterator<RemediationTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.getRemediations((page < 0 ? 0 : page) + 1,
                    paginatorRows, getSort()).iterator();
        }

        @Override
        public long size() {
            return restClient.countRemediations();
        }

        @Override
        public IModel<RemediationTO> model(final RemediationTO remediation) {
            return new IModel<>() {

                private static final long serialVersionUID = -2566070996511906708L;

                @Override
                public RemediationTO getObject() {
                    return remediation;
                }
            };
        }
    }

    private class RemediationUserWizardBuilder extends UserWizardBuilder {

        private static final long serialVersionUID = 6840699724316612700L;

        private final UserTO previousUserTO;

        private final RemediationTO remediationTO;

        RemediationUserWizardBuilder(
                final RemediationTO remediationTO,
                final UserTO previousUserTO,
                final UserTO userTO,
                final List<String> anyTypeClasses,
                final UserFormLayoutInfo formLayoutInfo,
                final UserRestClient userRestClient,
                final PageReference pageRef) {

            super(previousUserTO, userTO, anyTypeClasses, formLayoutInfo, userRestClient, pageRef);
            this.previousUserTO = previousUserTO;
            this.remediationTO = remediationTO;
        }

        @Override
        protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
            UserTO inner = modelObject.getInnerObject();

            ProvisioningResult<UserTO> result;

            if (remediationTO.getAnyURPayload() == null) {
                UserCR req = new UserCR();
                EntityTOUtils.toAnyCR(inner, req);

                result = restClient.remedy(remediationTO.getKey(), req);
            } else {
                UserUR req = AnyOperations.diff(inner, previousUserTO, false);

                if (StringUtils.isNotBlank(inner.getPassword())) {
                    PasswordPatch passwordPatch = new PasswordPatch.Builder().
                            value(inner.getPassword()).onSyncope(true).resources(inner.
                            getResources()).
                            build();
                    req.setPassword(passwordPatch);
                }
                // update just if it is changed
                if (req.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(inner);
                } else {
                    result = restClient.remedy(remediationTO.getKey(), req);
                }
            }

            return result;
        }
    }

    private class RemediationGroupWizardBuilder extends GroupWizardBuilder {

        private static final long serialVersionUID = -5233791906979150786L;

        private final GroupTO previousGroupTO;

        private final RemediationTO remediationTO;

        RemediationGroupWizardBuilder(
                final RemediationTO remediationTO,
                final GroupTO previousGroupTO,
                final GroupTO groupTO,
                final List<String> anyTypeClasses,
                final GroupFormLayoutInfo formLayoutInfo,
                final PageReference pageRef) {

            super(previousGroupTO, groupTO, anyTypeClasses, formLayoutInfo, pageRef);
            this.previousGroupTO = previousGroupTO;
            this.remediationTO = remediationTO;
        }

        @Override
        protected Serializable onApplyInternal(final AnyWrapper<GroupTO> modelObject) {
            GroupTO inner = modelObject.getInnerObject();

            ProvisioningResult<GroupTO> result;

            if (remediationTO.getAnyURPayload() == null) {
                GroupCR req = new GroupCR();
                EntityTOUtils.toAnyCR(inner, req);

                result = restClient.remedy(remediationTO.getKey(), req);
            } else {
                GroupUR req = AnyOperations.diff(inner, previousGroupTO, false);

                // update just if it is changed
                if (req.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(inner);
                } else {
                    result = restClient.remedy(remediationTO.getKey(), req);
                }
            }

            return result;
        }
    }

    private class RemediationAnyObjectWizardBuilder extends AnyObjectWizardBuilder {

        private static final long serialVersionUID = 6993139499479015083L;

        private final AnyObjectTO previousAnyObjectTO;

        private final RemediationTO remediationTO;

        RemediationAnyObjectWizardBuilder(
                final RemediationTO remediationTO,
                final AnyObjectTO previousAnyObjectTO,
                final AnyObjectTO anyObjectTO,
                final List<String> anyTypeClasses,
                final AnyObjectFormLayoutInfo formLayoutInfo,
                final PageReference pageRef) {

            super(previousAnyObjectTO, anyObjectTO, anyTypeClasses, formLayoutInfo, pageRef);
            this.previousAnyObjectTO = previousAnyObjectTO;
            this.remediationTO = remediationTO;
        }

        @Override
        protected Serializable onApplyInternal(final AnyWrapper<AnyObjectTO> modelObject) {
            AnyObjectTO inner = modelObject.getInnerObject();

            ProvisioningResult<AnyObjectTO> result;

            if (remediationTO.getAnyURPayload() == null) {
                AnyObjectCR req = new AnyObjectCR();
                EntityTOUtils.toAnyCR(inner, req);

                result = restClient.remedy(remediationTO.getKey(), req);
            } else {
                AnyObjectUR req = AnyOperations.diff(inner, previousAnyObjectTO, false);

                // update just if it is changed
                if (req.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(inner);
                } else {
                    result = restClient.remedy(remediationTO.getKey(), req);
                }
            }

            return result;
        }
    }
}
