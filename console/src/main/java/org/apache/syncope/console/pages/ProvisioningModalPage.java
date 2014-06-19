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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.to.AbstractSubjectTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.wrap.AbstractWrappable;
import org.apache.syncope.common.wrap.SubjectId;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.console.commons.status.StatusBean;
import org.apache.syncope.console.commons.status.StatusUtils;
import org.apache.syncope.console.commons.status.ConnObjectWrapper;
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
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class ProvisioningModalPage<T extends AbstractAttributableTO> extends AbstractStatusModalPage {

    private static final long serialVersionUID = -4285220460543213901L;

    private static final int ROWS_PER_PAGE = 10;

    private final ResourceTO resourceTO;

    private final Class<? extends AbstractAttributableTO> typeRef;

    private final PageReference pageRef;

    private final ModalWindow window;

    private final StatusUtils statusUtils;

    public ProvisioningModalPage(
            final PageReference pageRef,
            final ModalWindow window,
            final ResourceTO resourceTO,
            final Class<T> typeRef) {

        super();

        this.pageRef = pageRef;
        this.window = window;
        this.resourceTO = resourceTO;
        this.typeRef = typeRef;

        statusUtils = new StatusUtils((UserTO.class.isAssignableFrom(typeRef) ? userRestClient : roleRestClient));

        final List<IColumn<StatusBean, String>> columns = new ArrayList<IColumn<StatusBean, String>>();
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("id", this, null, "Attributable id"),
                "attributableId", "attributableId"));
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("name", this, null, "Attributable name"),
                "attributableName", "attributableName"));
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("resourceName", this, null, "Resource name"),
                "resourceName", "resourceName"));
        columns.add(new PropertyColumn<StatusBean, String>(
                new StringResourceModel("accountLink", this, null, "Account link"),
                "accountLink", "accountLink"));
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
                                cellItem.
                                add(statusUtils.getStatusImagePanel(componentId, model.getObject().getStatus()));
                            }
                });

        final ActionDataTablePanel<StatusBean, String> table = new ActionDataTablePanel<StatusBean, String>(
                "resourceDatatable",
                columns,
                (ISortableDataProvider<StatusBean, String>) new StatusBeanProvider(),
                ROWS_PER_PAGE,
                pageRef);

        final String pageId = "Resources";

        table.addAction(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    bulkAssociationAction(target, ResourceDeassociationActionType.UNLINK, table, columns);
                } catch (Exception e) {
                    LOG.error("Error unlinkink resources", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        }, ActionLink.ActionType.UNLINK, pageId);

        table.addAction(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    bulkAssociationAction(target, ResourceDeassociationActionType.DEPROVISION, table, columns);
                } catch (Exception e) {
                    LOG.error("Error de-provisioning user", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        }, ActionLink.ActionType.DEPROVISION, pageId);

        table.addAction(new ActionLink() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                try {
                    bulkAssociationAction(target, ResourceDeassociationActionType.UNASSIGN, table, columns);
                } catch (Exception e) {
                    LOG.error("Error unassigning resources", e);
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }
        }, ActionLink.ActionType.UNASSIGN, pageId);

        table.addCancelButton(window);

        add(table);
    }

    private class StatusBeanProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4287357360778016173L;

        public StatusBeanProvider() {
            super("accountLink");
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<StatusBean> getStatusBeans() {
            final String fiql = SyncopeClient.getUserSearchConditionBuilder().hasResources(resourceTO.getName()).query();

            final List<T> subjects = new ArrayList<T>();
            if (UserTO.class.isAssignableFrom(typeRef)) {
                subjects.addAll((List<T>) userRestClient.search(fiql, 1, ROWS_PER_PAGE,
                        new SortParam<String>("id", true)));
            } else {
                subjects.addAll((List<T>) roleRestClient.search(fiql, 1, ROWS_PER_PAGE,
                        new SortParam<String>("id", true)));
            }

            final List<ConnObjectWrapper> connObjects = statusUtils.getConnectorObjects(
                    (List<AbstractSubjectTO>) subjects, Collections.<String>singleton(resourceTO.getName()));

            final List<StatusBean> statusBeans = new ArrayList<StatusBean>(connObjects.size() + 1);
            final LinkedHashMap<String, StatusBean> initialStatusBeanMap = new LinkedHashMap<String, StatusBean>(
                    connObjects.size());

            for (ConnObjectWrapper entry : connObjects) {
                final StatusBean statusBean = statusUtils.getStatusBean(
                        entry.getAttributable(),
                        entry.getResourceName(),
                        entry.getConnObjectTO(),
                        RoleTO.class.isAssignableFrom(typeRef));

                initialStatusBeanMap.put(entry.getResourceName(), statusBean);
                statusBeans.add(statusBean);
            }

            return statusBeans;
        }
    }

    private void bulkAssociationAction(
            final AjaxRequestTarget target,
            final ResourceDeassociationActionType type,
            final ActionDataTablePanel<StatusBean, String> table,
            final List<IColumn<StatusBean, String>> columns) {

        final List<StatusBean> beans = new ArrayList<StatusBean>(table.getModelObject());
        List<SubjectId> subjectIds = new ArrayList<SubjectId>();
        for (StatusBean bean : beans) {
            LOG.debug("Selected bean {}", bean);
            subjectIds.add(AbstractWrappable.getInstance(SubjectId.class, bean.getAttributableId()));
        }

        if (beans.isEmpty()) {
            window.close(target);
        } else {
            final BulkActionResult res = resourceRestClient.bulkAssociationAction(
                    resourceTO.getName(), typeRef, type, subjectIds);

            ((BasePage) pageRef.getPage()).setModalResult(true);

            setResponsePage(new BulkActionResultModalPage<StatusBean, String>(
                    window, beans, columns, res, "attributableId"));
        }
    }
}
