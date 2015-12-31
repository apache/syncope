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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.commons.status.Status;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.panels.ActionDataTablePanel;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class StatusModal<T extends Serializable> extends AbstractStatusModalPage<T> {

    private static final long serialVersionUID = -9148734710505211261L;

    private final UserRestClient userRestClient = new UserRestClient();

    private final GroupRestClient groupRestClient = new GroupRestClient();

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    private final AnyTO anyTO;

    private int rowsPerPage = 10;

    private final StatusUtils statusUtils;

    private final boolean statusOnly;

    // --------------------------------
    // password management fields ..
    // --------------------------------
    private final IndicatingAjaxButton cancel;

    private final WebMarkupContainer pwdMgt;

    private final Form<?> pwdMgtForm;

    private final AjaxCheckBoxPanel changepwd;

    private final PasswordTextField password;

    private final PasswordTextField confirm;
    // --------------------------------

    private final ActionDataTablePanel<StatusBean, String> table;

    private final List<IColumn<StatusBean, String>> columns;

    public StatusModal(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final AnyTO anyTO) {

        this(modal, pageRef, anyTO, false);
    }

    public StatusModal(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final AnyTO anyTO,
            final boolean statusOnly) {

        super(modal, pageRef);

        this.statusOnly = statusOnly;
        this.anyTO = anyTO;

        statusUtils = new StatusUtils(anyTO instanceof UserTO ? userRestClient : groupRestClient);

        add(new Label("displayName", anyTO.getKey() + " "
                + (anyTO instanceof UserTO ? ((UserTO) anyTO).getUsername() : ((GroupTO) anyTO).getName())));

        columns = new ArrayList<>();
        columns.add(new AbstractColumn<StatusBean, String>(
                new StringResourceModel("resourceName", this, null), "resourceName") {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<StatusBean>> cellItem,
                    final String componentId,
                    final IModel<StatusBean> model) {

                cellItem.add(new Label(componentId, model.getObject().getResourceName()) {

                    private static final long serialVersionUID = 8432079838783825801L;

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        if (model.getObject().isLinked()) {
                            super.onComponentTag(tag);
                        } else {
                            tag.put("style", "color: #DDDDDD");
                        }
                    }
                });
            }
        });

        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("connObjectLink", this, null), "connObjectLink", "connObjectLink"));

        columns.add(new AbstractColumn<StatusBean, String>(
                new StringResourceModel("status", this, null)) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(
                    final Item<ICellPopulator<StatusBean>> cellItem,
                    final String componentId,
                    final IModel<StatusBean> model) {

                if (model.getObject().isLinked()) {
                    cellItem.add(statusUtils.getStatusImagePanel(componentId, model.getObject().
                            getStatus()));
                } else {
                    cellItem.add(new Label(componentId, ""));
                }
            }
        });

        table = new ActionDataTablePanel<StatusBean, String>(
                "resourceDatatable",
                columns,
                (ISortableDataProvider<StatusBean, String>) new AttributableStatusProvider(),
                rowsPerPage,
                pageRef) {

            private static final long serialVersionUID = 6510391461033818316L;

            @Override
            public boolean isElementEnabled(final StatusBean element) {
                return !statusOnly || element.getStatus() != Status.OBJECT_NOT_FOUND;
            }
        };
        table.setOutputMarkupId(true);

        final String pageId = anyTO instanceof GroupTO ? "Groups" : "Users";

        final Fragment pwdMgtFragment = new Fragment("pwdMgtFields", "pwdMgtFragment", this);
        addOrReplace(pwdMgtFragment);

        pwdMgt = new WebMarkupContainer("pwdMgt");
        pwdMgtFragment.add(pwdMgt.setOutputMarkupId(true));

        pwdMgtForm = new Form<>("pwdMgtForm");
        pwdMgtForm.setVisible(false).setEnabled(false);
        pwdMgt.add(pwdMgtForm);

        password = new PasswordTextField("password", new Model<String>());
        pwdMgtForm.add(password.setRequired(false).setEnabled(false));

        confirm = new PasswordTextField("confirm", new Model<String>());
        pwdMgtForm.add(confirm.setRequired(false).setEnabled(false));

        changepwd = new AjaxCheckBoxPanel("changepwd", "changepwd", new Model<>(false));
        pwdMgtForm.add(changepwd.setModelObject(false));
        pwdMgtForm.add(new Label("changePwdLabel", new ResourceModel("changePwdLabel", "Password propagation")));

        changepwd.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                password.setEnabled(changepwd.getModelObject());
                confirm.setEnabled(changepwd.getModelObject());
                target.add(pwdMgt);
            }
        });

        cancel = new IndicatingAjaxButton("cancel", new ResourceModel("cancel")) {

            private static final long serialVersionUID = -2341391430136818026L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                // ignore
                modal.close(target);
            }
        };

        pwdMgtForm.add(cancel);

        final IndicatingAjaxButton goon = new IndicatingAjaxButton("continue", new ResourceModel("continue")) {

            private static final long serialVersionUID = -2341391430136818027L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                // none
            }
        };

        pwdMgtForm.add(goon);

        if (statusOnly) {
            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        userRestClient.reactivate(
                                anyTO.getETagValue(),
                                anyTO.getKey(),
                                new ArrayList<>(table.getModelObject()));

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        modal.close(target);
                    } catch (Exception e) {
                        LOG.error("Error enabling resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        modal.getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.REACTIVATE, pageId);

            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        userRestClient.suspend(
                                anyTO.getETagValue(),
                                anyTO.getKey(),
                                new ArrayList<>(table.getModelObject()));

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        modal.close(target);
                    } catch (Exception e) {
                        LOG.error("Error disabling resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        modal.getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.SUSPEND, pageId);
        } else {
            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        if (anyTO instanceof UserTO) {
                            userRestClient.unlink(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        } else {
                            groupRestClient.unlink(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        modal.close(target);
                    } catch (Exception e) {
                        LOG.error("Error unlinking resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        modal.getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.UNLINK, pageId);

            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        if (anyTO instanceof UserTO) {
                            userRestClient.link(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        } else {
                            groupRestClient.link(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        modal.close(target);
                    } catch (Exception e) {
                        LOG.error("Error linking resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        modal.getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.LINK, pageId);

            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        BulkActionResult bulkActionResult;
                        if (anyTO instanceof UserTO) {
                            bulkActionResult = userRestClient.deprovision(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        } else {
                            bulkActionResult = groupRestClient.deprovision(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        loadBulkActionResultPage(target, table.getModelObject(), bulkActionResult);
                    } catch (Exception e) {
                        LOG.error("Error de-provisioning user", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        modal.getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.DEPROVISION, pageId);

            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {

                    if (anyTO instanceof UserTO) {
                        StatusModal.this.passwordManagement(
                                target, ResourceAssociationAction.PROVISION, table.getModelObject());
                    } else {
                        try {
                            final BulkActionResult bulkActionResult = groupRestClient.provision(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));

                            info(getString(Constants.OPERATION_SUCCEEDED));
                            loadBulkActionResultPage(target, table.getModelObject(), bulkActionResult);
                        } catch (Exception e) {
                            LOG.error("Error provisioning user", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            modal.getNotificationPanel().refresh(target);
                        }
                    }
                }
            }.feedbackPanelAutomaticReload(!(anyTO instanceof UserTO)), ActionLink.ActionType.PROVISION, pageId);

            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        final BulkActionResult bulkActionResult;
                        if (anyTO instanceof UserTO) {
                            bulkActionResult = userRestClient.unassign(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        } else {
                            bulkActionResult = groupRestClient.unassign(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        loadBulkActionResultPage(target, table.getModelObject(), bulkActionResult);
                    } catch (Exception e) {
                        LOG.error("Error unassigning resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        modal.getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.UNASSIGN, pageId);

            table.addAction(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    if (anyTO instanceof UserTO) {
                        StatusModal.this.passwordManagement(
                                target, ResourceAssociationAction.ASSIGN, table.getModelObject());
                    } else {
                        try {
                            final BulkActionResult bulkActionResult = groupRestClient.assign(
                                    anyTO.getETagValue(),
                                    anyTO.getKey(),
                                    new ArrayList<>(table.getModelObject()));

                            info(getString(Constants.OPERATION_SUCCEEDED));
                            loadBulkActionResultPage(target, table.getModelObject(), bulkActionResult);
                        } catch (Exception e) {
                            LOG.error("Error assigning resources", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            modal.getNotificationPanel().refresh(target);
                        }
                    }
                }
            }.feedbackPanelAutomaticReload(!(anyTO instanceof UserTO)), ActionLink.ActionType.ASSIGN, pageId);
        }

        table.addCancelButton(modal);
        add(table);
    }

    private class AttributableStatusProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        AttributableStatusProvider() {
            super(statusOnly ? "resourceName" : "connObjectLink");
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<StatusBean> getStatusBeans() {
            final List<String> resources = new ArrayList<>();
            for (ResourceTO resourceTO : resourceRestClient.getAll()) {
                resources.add(resourceTO.getKey());
            }

            final List<ConnObjectWrapper> connObjects = statusUtils.getConnectorObjects(anyTO);

            final List<StatusBean> statusBeans = new ArrayList<>(connObjects.size() + 1);

            for (ConnObjectWrapper entry : connObjects) {
                final StatusBean statusBean = statusUtils.getStatusBean(anyTO,
                        entry.getResourceName(),
                        entry.getConnObjectTO(),
                        anyTO instanceof GroupTO);

                statusBeans.add(statusBean);
                resources.remove(entry.getResourceName());
            }

            if (statusOnly) {
                final StatusBean syncope = new StatusBean(anyTO, "Syncope");

                syncope.setConnObjectLink(((UserTO) anyTO).getUsername());

                Status syncopeStatus = Status.UNDEFINED;
                if (((UserTO) anyTO).getStatus() != null) {
                    try {
                        syncopeStatus = Status.valueOf(((UserTO) anyTO).getStatus().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Unexpected status found: {}", ((UserTO) anyTO).getStatus(), e);
                    }
                }
                syncope.setStatus(syncopeStatus);

                statusBeans.add(syncope);
            } else {
                for (String resource : resources) {
                    final StatusBean statusBean = statusUtils.getStatusBean(anyTO,
                            resource,
                            null,
                            anyTO instanceof GroupTO);

                    statusBean.setLinked(false);
                    statusBeans.add(statusBean);
                }
            }

            return statusBeans;
        }
    }

    private void passwordManagement(
            final AjaxRequestTarget target,
            final ResourceAssociationAction type,
            final Collection<StatusBean> selection) {

        final IndicatingAjaxButton goon
                = new IndicatingAjaxButton("continue", new ResourceModel("continue", "Continue")) {

            private static final long serialVersionUID = -2341391430136818027L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    if (StringUtils.isNotBlank(password.getModelObject())
                            && !password.getModelObject().equals(confirm.getModelObject())) {
                        throw new Exception(getString("passwordMismatch"));
                    }

                    final BulkActionResult bulkActionResult;
                    switch (type) {
//                                case ASSIGN:
//                                    bulkActionResult = userRestClient.assign(
//                                            anyTO.getETagValue(),
//                                            anyTO.getKey(),
//                                            new ArrayList<>(selection),
//                                            changepwd.getModelObject(),
//                                            password.getModelObject());
//                                    break;
//                                case PROVISION:
//                                    bulkActionResult = userRestClient.provision(
//                                            anyTO.getETagValue(),
//                                            anyTO.getKey(),
//                                            new ArrayList<>(selection),
//                                            changepwd.getModelObject(),
//                                            password.getModelObject());
//                                    break;
                        default:
                            bulkActionResult = null;
                        // ignore
                    }

                    if (bulkActionResult != null) {
                        loadBulkActionResultPage(target, selection, bulkActionResult);
                    } else {
                        modal.getNotificationPanel().refresh(target);
                        modal.close(target);
                    }
                } catch (Exception e) {
                    LOG.error("Error provisioning resources", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    modal.getNotificationPanel().refresh(target);
                }
            }
        };

        pwdMgtForm.addOrReplace(goon);

        table.setVisible(false);
        pwdMgtForm.setVisible(true).setEnabled(true);

        target.add(table);
        target.add(pwdMgt);
    }

    private void loadBulkActionResultPage(
            final AjaxRequestTarget target,
            final Collection<StatusBean> selection,
            final BulkActionResult bulkActionResult) {
        final List<String> resources = new ArrayList<>(selection.size());
        for (StatusBean statusBean : selection) {
            resources.add(statusBean.getResourceName());
        }

        final List<ConnObjectWrapper> connObjects = statusUtils.getConnectorObjects(Collections.singletonList(anyTO),
                resources);

        final List<StatusBean> statusBeans = new ArrayList<>(connObjects.size());

        for (ConnObjectWrapper entry : connObjects) {
            final StatusBean statusBean = statusUtils.getStatusBean(anyTO,
                    entry.getResourceName(),
                    entry.getConnObjectTO(),
                    anyTO instanceof GroupTO);

            statusBeans.add(statusBean);
        }

        target.add(modal.setContent(new BulkActionResultModalPage<>(
                modal,
                pageRef,
                statusBeans,
                columns,
                bulkActionResult,
                "resourceName")));
    }
}
