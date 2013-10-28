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

import static org.apache.syncope.console.pages.AbstractBasePage.LOG;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.StatusBean;
import org.apache.syncope.console.commons.StatusUtils;
import org.apache.syncope.console.commons.StatusUtils.ConnObjectWrapper;
import org.apache.syncope.console.commons.StatusUtils.Status;
import org.apache.syncope.console.commons.StatusUtils.StatusBeanProvider;
import org.apache.syncope.console.pages.panels.ActionDataTablePanel;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class StatusModalPage<T extends AbstractAttributableTO> extends AbstractStatusModlaPage {

    private static final long serialVersionUID = 4114026480146090961L;

    private final AbstractAttributableTO attributableTO;

    private int rowsPerPage = 25;

    final StatusUtils statusUtils;

    final boolean statusOnly;

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

        this.statusOnly = statusOnly;
        this.attributableTO = attributableTO;

        statusUtils = new StatusUtils(attributableTO instanceof UserTO ? userRestClient : roleRestClient);

        final List<IColumn<StatusBean, String>> columns = new ArrayList<IColumn<StatusBean, String>>();
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("resourceName", this, null, "Resource name"), "resourceName", "resourceName"));
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

                cellItem.add(statusUtils.getStatusImagePanel(componentId, model.getObject().getStatus()));
            }
        });

        final ActionDataTablePanel<StatusBean, String> table = new ActionDataTablePanel<StatusBean, String>(
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

        final String pageId = attributableTO instanceof RoleTO ? "Roles" : "Users";

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
                        LOG.error("Error unlinkink resources", e);
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
                            userRestClient.deprovision(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        } else {
                            roleRestClient.deprovision(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        }

                        ((BasePage) pageRef.getPage()).setModalResult(true);

                        window.close(target);
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
                    try {
                        if (attributableTO instanceof UserTO) {
                            userRestClient.unassign(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        } else {
                            roleRestClient.unassign(
                                    attributableTO.getId(), new ArrayList<StatusBean>(table.getModelObject()));
                        }

                        ((BasePage) pageRef.getPage()).setModalResult(true);

                        window.close(target);
                    } catch (Exception e) {
                        LOG.error("Error unassigning resources", e);
                        error(getString(Constants.ERROR) + ": " + e.getMessage());
                        target.add(feedbackPanel);
                    }
                }
            }, ActionLink.ActionType.UNASSIGN, pageId);
        }

        table.addCancelButton(window);

        add(table);
    }

    class AttributableStatusProvider extends StatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        public AttributableStatusProvider() {
            super("resourceName");
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<StatusBean> getStatusBeans() {

            final List<ConnObjectWrapper> connObjects =
                    statusUtils.getConnectorObjects(Collections.<AbstractAttributableTO>singleton(attributableTO));

            final List<StatusBean> statusBeans = new ArrayList<StatusBean>(connObjects.size() + 1);

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
            }

            for (ConnObjectWrapper entry : connObjects) {
                final StatusBean statusBean = statusUtils.getStatusBean(
                        entry.getAttributable(),
                        entry.getResourceName(),
                        entry.getConnObjectTO(),
                        attributableTO instanceof RoleTO);

                statusBeans.add(statusBean);
            }

            return statusBeans;
        }
    }
}
