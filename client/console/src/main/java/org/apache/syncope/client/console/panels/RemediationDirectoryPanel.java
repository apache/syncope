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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.RemediationDirectoryPanel.RemediationProvider;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.RemediationRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.AnyObjectWizardBuilder;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.client.console.wizards.any.GroupWizardBuilder;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.PasswordPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class RemediationDirectoryPanel
        extends DirectoryPanel<RemediationTO, RemediationTO, RemediationProvider, RemediationRestClient> {

    private static final long serialVersionUID = 8525204188127106587L;

    public RemediationDirectoryPanel(final String id, final PageReference pageReference) {
        super(id, pageReference, true);
        disableCheckBoxes();
        setFooterVisibility(false);
        modal.size(Modal.Size.Large);

        restClient = new RemediationRestClient();

        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.REMEDIATION_REMEDY);
    }

    @Override
    protected List<IColumn<RemediationTO, String>> getColumns() {
        List<IColumn<RemediationTO, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel("key", this), "key"));
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

        panel.add(new ActionLink<RemediationTO>() {

            private static final long serialVersionUID = 6193210574968203299L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                modal.header(new ResourceModel("error"));
                modal.setContent(new ExecMessageModal(model.getObject().getError()));
                modal.show(true);
                target.add(modal);
            }
        }, ActionLink.ActionType.VIEW_DETAILS, StandardEntitlement.REMEDIATION_READ);

        if (model.getObject().getOperation() == ResourceOperation.DELETE) {
            String entitlements = StringUtils.join(new String[] {
                StandardEntitlement.REMEDIATION_REMEDY,
                AnyTypeKind.USER.name().equals(model.getObject().getAnyType())
                ? StandardEntitlement.USER_DELETE
                : AnyTypeKind.GROUP.name().equals(model.getObject().getAnyType())
                ? StandardEntitlement.GROUP_DELETE
                : AnyEntitlement.DELETE.getFor(model.getObject().getAnyType()) }, ",");

            panel.add(new ActionLink<RemediationTO>() {

                private static final long serialVersionUID = 6193210574968203299L;

                @Override
                public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                    try {
                        restClient.remedy(model.getObject().getKey(), model.getObject().getKeyPayload());
                        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                        target.add(container);
                    } catch (SyncopeClientException e) {
                        LOG.error("While performing remediation {}", model.getObject().getKey(), e);
                        SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                ? e.getClass().getName() : e.getMessage());
                    }
                    ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                }
            }, ActionLink.ActionType.CLOSE, entitlements, true);
        } else {
            String entitlements = model.getObject().getOperation() == ResourceOperation.CREATE
                    ? StringUtils.join(new String[] {
                StandardEntitlement.REMEDIATION_REMEDY,
                AnyTypeKind.USER.name().equals(model.getObject().getAnyType())
                ? StandardEntitlement.USER_CREATE
                : AnyTypeKind.GROUP.name().equals(model.getObject().getAnyType())
                ? StandardEntitlement.GROUP_CREATE
                : AnyEntitlement.CREATE.getFor(model.getObject().getAnyType()) }, ",")
                    : StringUtils.join(new String[] {
                StandardEntitlement.REMEDIATION_REMEDY,
                AnyTypeKind.USER.name().equals(model.getObject().getAnyType())
                ? StandardEntitlement.USER_UPDATE
                : AnyTypeKind.GROUP.name().equals(model.getObject().getAnyType())
                ? StandardEntitlement.GROUP_UPDATE
                : AnyEntitlement.UPDATE.getFor(model.getObject().getAnyType()) }, ",");

            panel.add(new ActionLink<RemediationTO>() {

                private static final long serialVersionUID = 6193210574968203299L;

                @Override
                public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                    modal.setFormModel(new CompoundPropertyModel<>(model.getObject()));
                    RemediationTO remediationTO = model.getObject();

                    switch (remediationTO.getAnyType()) {
                        case "USER":
                            UserTO newUserTO;
                            UserTO previousUserTO;
                            if (remediationTO.getAnyPatchPayload() == null) {
                                newUserTO = (UserTO) remediationTO.getAnyTOPayload();
                                previousUserTO = null;
                            } else {
                                previousUserTO = new UserRestClient().
                                        read(remediationTO.getAnyPatchPayload().getKey());
                                newUserTO = AnyOperations.patch(
                                        previousUserTO, (UserPatch) remediationTO.getAnyPatchPayload());
                            }

                            AjaxWizard.EditItemActionEvent<UserTO> userEvent =
                                    new AjaxWizard.EditItemActionEvent<>(newUserTO, target);
                            userEvent.forceModalPanel(new RemediationUserWizardBuilder(
                                    model.getObject(),
                                    previousUserTO,
                                    newUserTO,
                                    new AnyTypeRestClient().read(remediationTO.getAnyType()).getClasses(),
                                    FormLayoutInfoUtils.fetch(Arrays.asList(remediationTO.getAnyType())).getLeft(),
                                    pageRef
                            ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));
                            send(RemediationDirectoryPanel.this, Broadcast.EXACT, userEvent);
                            break;

                        case "GROUP":
                            GroupTO newGroupTO;
                            GroupTO previousGroupTO;
                            if (remediationTO.getAnyPatchPayload() == null) {
                                newGroupTO = (GroupTO) remediationTO.getAnyTOPayload();
                                previousGroupTO = null;
                            } else {
                                previousGroupTO = new GroupRestClient().
                                        read(remediationTO.getAnyPatchPayload().getKey());
                                newGroupTO = AnyOperations.patch(
                                        previousGroupTO, (GroupPatch) remediationTO.getAnyPatchPayload());
                            }

                            AjaxWizard.EditItemActionEvent<GroupTO> groupEvent =
                                    new AjaxWizard.EditItemActionEvent<>(newGroupTO, target);
                            groupEvent.forceModalPanel(new RemediationGroupWizardBuilder(
                                    model.getObject(),
                                    previousGroupTO,
                                    newGroupTO,
                                    new AnyTypeRestClient().read(remediationTO.getAnyType()).getClasses(),
                                    FormLayoutInfoUtils.fetch(Arrays.asList(remediationTO.getAnyType())).getMiddle(),
                                    pageRef
                            ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));
                            send(RemediationDirectoryPanel.this, Broadcast.EXACT, groupEvent);
                            break;

                        default:
                            AnyObjectTO newAnyObjectTO;
                            AnyObjectTO previousAnyObjectTO;
                            if (remediationTO.getAnyPatchPayload() == null) {
                                newAnyObjectTO = (AnyObjectTO) remediationTO.getAnyTOPayload();
                                previousAnyObjectTO = null;
                            } else {
                                previousAnyObjectTO = new AnyObjectRestClient().
                                        read(remediationTO.getAnyPatchPayload().getKey());
                                newAnyObjectTO = AnyOperations.patch(
                                        previousAnyObjectTO, (AnyObjectPatch) remediationTO.getAnyPatchPayload());
                            }

                            AjaxWizard.EditItemActionEvent<AnyObjectTO> anyObjectEvent =
                                    new AjaxWizard.EditItemActionEvent<>(newAnyObjectTO, target);
                            anyObjectEvent.forceModalPanel(new RemediationAnyObjectWizardBuilder(
                                    model.getObject(),
                                    previousAnyObjectTO,
                                    newAnyObjectTO,
                                    new AnyTypeRestClient().read(remediationTO.getAnyType()).getClasses(),
                                    FormLayoutInfoUtils.fetch(Arrays.asList(remediationTO.getAnyType())).
                                            getRight().values().iterator().next(),
                                    pageRef
                            ).build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT));
                            send(RemediationDirectoryPanel.this, Broadcast.EXACT, anyObjectEvent);
                    }
                }
            }, ActionLink.ActionType.EDIT, entitlements);
        }

        panel.add(new ActionLink<RemediationTO>() {

            private static final long serialVersionUID = 6193210574968203299L;

            @Override
            public void onClick(final AjaxRequestTarget target, final RemediationTO ignore) {
                try {
                    restClient.delete(model.getObject().getKey());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", model.getObject().getKey(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, StandardEntitlement.REMEDIATION_DELETE, true);

        return panel;
    }

    @Override
    protected RemediationProvider dataProvider() {
        return new RemediationProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_REMEDIATION_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    public static class RemediationProvider extends DirectoryDataProvider<RemediationTO> {

        private static final long serialVersionUID = -2311716167583335852L;

        private final SortableDataProviderComparator<RemediationTO> comparator;

        private final RemediationRestClient restClient = new RemediationRestClient();

        public RemediationProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("instant", SortOrder.ASCENDING);
            this.comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<RemediationTO> iterator(final long first, final long count) {
            final List<RemediationTO> list = restClient.getRemediations();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getRemediations().size();
        }

        @Override
        public IModel<RemediationTO> model(final RemediationTO remediation) {
            return new AbstractReadOnlyModel<RemediationTO>() {

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
                final PageReference pageRef) {

            super(previousUserTO, userTO, anyTypeClasses, formLayoutInfo, pageRef);
            this.previousUserTO = previousUserTO;
            this.remediationTO = remediationTO;
        }

        @Override
        protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
            UserTO inner = modelObject.getInnerObject();

            ProvisioningResult<UserTO> result;

            if (remediationTO.getAnyPatchPayload() == null) {
                result = restClient.remedy(remediationTO.getKey(), inner);
            } else {
                UserPatch patch = AnyOperations.diff(inner, previousUserTO, false);

                if (StringUtils.isNotBlank(inner.getPassword())) {
                    PasswordPatch passwordPatch = new PasswordPatch.Builder().
                            value(inner.getPassword()).onSyncope(true).resources(inner.
                            getResources()).
                            build();
                    patch.setPassword(passwordPatch);
                }
                // update just if it is changed
                if (patch.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(inner);
                } else {
                    result = restClient.remedy(remediationTO.getKey(), patch);
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

            if (remediationTO.getAnyPatchPayload() == null) {
                result = restClient.remedy(remediationTO.getKey(), inner);
            } else {
                GroupPatch patch = AnyOperations.diff(inner, previousGroupTO, false);

                // update just if it is changed
                if (patch.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(inner);
                } else {
                    result = restClient.remedy(remediationTO.getKey(), patch);
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

            if (remediationTO.getAnyPatchPayload() == null) {
                result = restClient.remedy(remediationTO.getKey(), inner);
            } else {
                AnyObjectPatch patch = AnyOperations.diff(inner, previousAnyObjectTO, false);

                // update just if it is changed
                if (patch.isEmpty()) {
                    result = new ProvisioningResult<>();
                    result.setEntity(inner);
                } else {
                    result = restClient.remedy(remediationTO.getKey(), patch);
                }
            }

            return result;
        }
    }
}
