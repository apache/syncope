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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.syncope.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.console.commons.status.Status;
import org.apache.syncope.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.console.pages.panels.ActionDataTablePanel;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
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

public class StatusModalPage<T extends AbstractAttributableTO> extends AbstractStatusModalPage {

    private static final long serialVersionUID = -9148734710505211261L;

    private final AbstractAttributableTO attributableTO;

    private int rowsPerPage = 10;

    final StatusUtils statusUtils;

    final boolean statusOnly;

    // --------------------------------
    // password management fields ..
    // --------------------------------
    final ClearIndicatingAjaxButton cancel;

    final WebMarkupContainer pwdMgt;

    final Form<?> pwdMgtForm;

    final AjaxCheckBoxPanel changepwd;

    final PasswordTextField password;

    final PasswordTextField confirm;
    // --------------------------------

    final PageReference pageRef;

    final ModalWindow window;

    final ActionDataTablePanel<StatusBean, String> table;

    final List<IColumn<StatusBean, String>> columns;

    public StatusModalPage(
            final PageReference pageRef,
            final ModalWindow window,
            final AbstractAttributableTO attributableTO) {
        this(pageRef, window, attributableTO, false);
    }

    public StatusModalPage(
            final PageReference pageRef,
            final ModalWindow window,
            final AbstractAttributableTO attributableTO,
            final boolean statusOnly) {

        super();

        this.pageRef = pageRef;
        this.window = window;
        this.statusOnly = statusOnly;
        this.attributableTO = attributableTO;

        statusUtils = new StatusUtils(attributableTO instanceof UserTO ? userRestClient : roleRestClient);

        columns = new ArrayList<IColumn<StatusBean, String>>();
        columns.add(new AbstractColumn<StatusBean, String>(
                new StringResourceModel("resourceName", this, null, "Resource name"), "resourceName") {

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
                new StringResourceModel("accountLink", this, null, "Account link"), "accountLink", "accountLink"));

        columns.add(new AbstractColumn<StatusBean, String>(
                new StringResourceModel("status", this, null, "")) {

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

        final String pageId = attributableTO instanceof RoleTO ? "Roles" : "Users";

        final Fragment pwdMgtFragment = new Fragment("pwdMgtFields", "pwdMgtFragment", this);
        addOrReplace(pwdMgtFragment);

        pwdMgt = new WebMarkupContainer("pwdMgt");
        pwdMgtFragment.add(pwdMgt.setOutputMarkupId(true));

        pwdMgtForm = new Form("pwdMgtForm");
        pwdMgtForm.setVisible(false).setEnabled(false);
        pwdMgt.add(pwdMgtForm);

        password = new PasswordTextField("password", new Model<String>());
        pwdMgtForm.add(password.setRequired(false).setEnabled(false));

        confirm = new PasswordTextField("confirm", new Model<String>());
        pwdMgtForm.add(confirm.setRequired(false).setEnabled(false));

        changepwd = new AjaxCheckBoxPanel("changepwd", "changepwd", new Model<Boolean>(false));
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

        cancel = new ClearIndicatingAjaxButton("cancel", new ResourceModel("cancel"), pageRef) {

            private static final long serialVersionUID = -2341391430136818026L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                // ignore
                window.close(target);
            }
        }.feedbackPanelAutomaticReload(false);

        pwdMgtForm.add(cancel);

        final ClearIndicatingAjaxButton goon =
                new ClearIndicatingAjaxButton("continue", new ResourceModel("continue"), pageRef) {

                    private static final long serialVersionUID = -2341391430136818027L;

                    @Override
                    protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                        // none
                    }
                };

        pwdMgtForm.add(goon);

        if (statusOnly) {
            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        userRestClient.reactivate(
                                attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));

                        ((BasePage) pageRef.getPage()).setModalResult(true);

                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error enabling resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.REACTIVATE, pageId);

            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        userRestClient.suspend(
                                attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));

                        if (pageRef.getPage() instanceof BasePage) {
                            ((BasePage) pageRef.getPage()).setModalResult(true);
                        }

                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error disabling resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.SUSPEND, pageId);
        } else {
            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        if (attributableTO instanceof UserTO) {
                            userRestClient.unlink(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        } else {
                            roleRestClient.unlink(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        }

                        ((BasePage) pageRef.getPage()).setModalResult(true);
                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error unlinking resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.UNLINK, pageId);

            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        if (attributableTO instanceof UserTO) {
                            userRestClient.link(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        } else {
                            roleRestClient.link(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        }

                        ((BasePage) pageRef.getPage()).setModalResult(true);
                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error linking resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.LINK, pageId);

            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        final BulkActionResult bulkActionResult;
                        if (attributableTO instanceof UserTO) {
                            bulkActionResult = userRestClient.deprovision(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        } else {
                            bulkActionResult = roleRestClient.deprovision(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        }

                        ((BasePage) pageRef.getPage()).setModalResult(true);
                        loadBulkActionResultPage(table.getModelObject(), bulkActionResult);
                    } catch (Exception e) {
                        LOG.error("Error de-provisioning user", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.DEPROVISION, pageId);

            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {

                    if (attributableTO instanceof UserTO) {
                        StatusModalPage.this.passwordManagement(
                                target, ResourceAssociationActionType.PROVISION, table.getModelObject());
                    } else {
                        try {
                            final BulkActionResult bulkActionResult = roleRestClient.provision(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));

                            ((BasePage) pageRef.getPage()).setModalResult(true);
                            loadBulkActionResultPage(table.getModelObject(), bulkActionResult);
                        } catch (Exception e) {
                            LOG.error("Error provisioning user", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            target.add(feedbackPanel);
                        }
                    }
                }
            }.feedbackPanelAutomaticReload(!(attributableTO instanceof UserTO)), ActionLink.ActionType.PROVISION, pageId);

            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        final BulkActionResult bulkActionResult;
                        if (attributableTO instanceof UserTO) {
                            bulkActionResult = userRestClient.unassign(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        } else {
                            bulkActionResult = roleRestClient.unassign(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        }

                        ((BasePage) pageRef.getPage()).setModalResult(true);
                        loadBulkActionResultPage(table.getModelObject(), bulkActionResult);
                    } catch (Exception e) {
                        LOG.error("Error unassigning resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.UNASSIGN, pageId);

            table.addAction(new ActionLink() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    if (attributableTO instanceof UserTO) {
                        StatusModalPage.this.passwordManagement(
                                target, ResourceAssociationActionType.ASSIGN, table.getModelObject());
                    } else {
                        try {
                            final BulkActionResult bulkActionResult = roleRestClient.assign(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));

                            ((BasePage) pageRef.getPage()).setModalResult(true);
                            loadBulkActionResultPage(table.getModelObject(), bulkActionResult);
                        } catch (Exception e) {
                            LOG.error("Error assigning resources", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            target.add(feedbackPanel);
                        }
                    }
                }
            }.feedbackPanelAutomaticReload(!(attributableTO instanceof UserTO)), ActionLink.ActionType.ASSIGN, pageId);
        }

        table.addCancelButton(window);
        add(table);
    }

    private class AttributableStatusProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        public AttributableStatusProvider() {
            super(statusOnly ? "resourceName" : "accountLink");
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<StatusBean> getStatusBeans() {
            final List<String> resources = new ArrayList<String>();
            for (ResourceTO resourceTO : resourceRestClient.getAllResources()) {
                resources.add(resourceTO.getName());
            }

            final List<ConnObjectWrapper> connObjects = statusUtils.getConnectorObjects(attributableTO);

            final List<StatusBean> statusBeans = new ArrayList<StatusBean>(connObjects.size() + 1);

            for (ConnObjectWrapper entry : connObjects) {
                final StatusBean statusBean = statusUtils.getStatusBean(
                        attributableTO,
                        entry.getResourceName(),
                        entry.getConnObjectTO(),
                        attributableTO instanceof RoleTO);

                statusBeans.add(statusBean);
                resources.remove(entry.getResourceName());
            }

            if (statusOnly) {
                final StatusBean syncope = new StatusBean(attributableTO, "Syncope");

                syncope.setAccountLink(((UserTO) attributableTO).getUsername());

                Status syncopeStatus = Status.UNDEFINED;
                if (((UserTO) attributableTO).getStatus() != null) {
                    try {
                        syncopeStatus = Status.valueOf(((UserTO) attributableTO).getStatus().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Unexpected status found: {}", ((UserTO) attributableTO).getStatus(), e);
                    }
                }
                syncope.setStatus(syncopeStatus);

                statusBeans.add(syncope);
            } else {
                for (String resource : resources) {
                    final StatusBean statusBean = statusUtils.getStatusBean(
                            attributableTO,
                            resource,
                            null,
                            attributableTO instanceof RoleTO);

                    statusBean.setLinked(false);
                    statusBeans.add(statusBean);
                }
            }

            return statusBeans;
        }
    }

    private void passwordManagement(
            final AjaxRequestTarget target,
            final ResourceAssociationActionType type,
            final Collection<StatusBean> selection) {

        final ClearIndicatingAjaxButton goon =
                new ClearIndicatingAjaxButton("continue", new ResourceModel("continue", "Continue"), pageRef) {

                    private static final long serialVersionUID = -2341391430136818027L;

                    @Override
                    protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                        try {
                            if (StringUtils.isNotBlank(password.getModelObject())
                            && !password.getModelObject().equals(confirm.getModelObject())) {
                                throw new Exception(getString("passwordMismatch"));
                            }

                            final BulkActionResult bulkActionResult;
                            switch (type) {
                                case ASSIGN:
                                    bulkActionResult = userRestClient.assign(
                                            attributableTO.getId(),
                                            new ArrayList<StatusBean>(selection),
                                            changepwd.getModelObject(),
                                            password.getModelObject());
                                    break;
                                case PROVISION:
                                    bulkActionResult = userRestClient.provision(
                                            attributableTO.getId(),
                                            new ArrayList<StatusBean>(selection),
                                            changepwd.getModelObject(),
                                            password.getModelObject());
                                    break;
                                default:
                                    bulkActionResult = null;
                                // ignore
                            }

                            ((BasePage) pageRef.getPage()).setModalResult(true);

                            if (bulkActionResult != null) {
                                loadBulkActionResultPage(selection, bulkActionResult);
                            } else {

                                target.add(((BasePage) pageRef.getPage()).getFeedbackPanel());
                                window.close(target);
                            }
                        } catch (Exception e) {
                            LOG.error("Error provisioning resources", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            target.add(feedbackPanel);
                        }
                    }
                }.feedbackPanelAutomaticReload(false);

        pwdMgtForm.addOrReplace(goon);

        table.setVisible(false);
        pwdMgtForm.setVisible(true).setEnabled(true);

        target.add(table);
        target.add(pwdMgt);
    }

    private void loadBulkActionResultPage(
            final Collection<StatusBean> selection, final BulkActionResult bulkActionResult) {
        final List<String> resources = new ArrayList<String>(selection.size());
        for (StatusBean statusBean : selection) {
            resources.add(statusBean.getResourceName());
        }

        final List<ConnObjectWrapper> connObjects =
                statusUtils.getConnectorObjects(Collections.singletonList(attributableTO), resources);

        final List<StatusBean> statusBeans = new ArrayList<StatusBean>(connObjects.size());

        for (ConnObjectWrapper entry : connObjects) {
            final StatusBean statusBean = statusUtils.getStatusBean(
                    attributableTO,
                    entry.getResourceName(),
                    entry.getConnObjectTO(),
                    attributableTO instanceof RoleTO);

            statusBeans.add(statusBean);
        }

        setResponsePage(new BulkActionResultModalPage<StatusBean, String>(
                window,
                statusBeans,
                columns,
                bulkActionResult,
                "resourceName"));
    }
}
