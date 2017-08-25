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
package org.apache.syncope.client.console.status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.client.console.commons.status.ConnObjectWrapper;
import org.apache.syncope.client.console.commons.status.Status;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.ConnObjectDetails;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class AnyStatusDirectoryPanel
        extends DirectoryPanel<StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?, ?>>
        implements ModalPanel {

    private static final long serialVersionUID = -9148734710505211261L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private final AnyTO anyTO;

    private final boolean statusOnly;

    private final ResourceRestClient resourceRestClient = new ResourceRestClient();

    public AnyStatusDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final AnyTO anyTO,
            final String itemKeyFieldName,
            final boolean statusOnly) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef);
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.statusOnly = statusOnly;
        this.anyTO = anyTO;
        this.itemKeyFieldName = itemKeyFieldName;

        if (anyTO instanceof UserTO) {
            this.restClient = new UserRestClient();
        } else if (anyTO instanceof GroupTO) {
            this.restClient = new GroupRestClient();
        } else {
            this.restClient = new AnyObjectRestClient();
        }

        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<StatusBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected List<IColumn<StatusBean, String>> getColumns() {
        final List<IColumn<StatusBean, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<StatusBean, String>(
                new StringResourceModel("resource", this), "resource") {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<StatusBean>> cellItem,
                    final String componentId,
                    final IModel<StatusBean> model) {

                cellItem.add(new Label(componentId, model.getObject().getResource()) {

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

        columns.add(new PropertyColumn<>(
                new StringResourceModel("connObjectLink", this), "connObjectLink", "connObjectLink"));

        columns.add(new AbstractColumn<StatusBean, String>(new StringResourceModel("status", this)) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<StatusBean>> cellItem,
                    final String componentId,
                    final IModel<StatusBean> model) {

                if (model.getObject().isLinked()) {
                    cellItem.add(StatusUtils.getStatusImage(componentId, model.getObject().getStatus()));
                } else {
                    cellItem.add(new Label(componentId, ""));
                }
            }
        });
        return columns;
    }

    @Override
    public ActionsPanel<StatusBean> getActions(final IModel<StatusBean> model) {
        final ActionsPanel<StatusBean> panel = super.getActions(model);

        panel.add(new ActionLink<StatusBean>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return bean != null && bean.getConnObjectLink() != null
                        && !bean.getResource().equalsIgnoreCase(Constants.SYNCOPE);
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next(bean.getResource(),
                        new ConnObjectDetails(resourceRestClient.readConnObject(
                                bean.getResource(), anyTO.getType(), anyTO.getKey())), target);
                target.add(multiLevelPanelRef);
                AnyStatusDirectoryPanel.this.getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.RESOURCE_GET_CONNOBJECT);

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionLink.ActionType> bulkActions = new ArrayList<>();
        if (statusOnly) {
            bulkActions.add(ActionLink.ActionType.SUSPEND);
            bulkActions.add(ActionLink.ActionType.REACTIVATE);
        } else {
            bulkActions.add(ActionLink.ActionType.UNLINK);
            bulkActions.add(ActionLink.ActionType.LINK);
            bulkActions.add(ActionLink.ActionType.DEPROVISION);
            bulkActions.add(ActionLink.ActionType.PROVISION);
            bulkActions.add(ActionLink.ActionType.ASSIGN);
            bulkActions.add(ActionLink.ActionType.UNASSIGN);

        }
        return bulkActions;
    }

    @Override
    protected AttributableStatusProvider dataProvider() {
        return new AttributableStatusProvider();
    }

    @Override
    protected String paginatorRowsKey() {
        return StringUtils.EMPTY;
    }

    public class AttributableStatusProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        private final StatusUtils statusUtils;

        AttributableStatusProvider() {
            super(statusOnly ? "resource" : "connObjectLink");
            statusUtils = new StatusUtils();
        }

        @SuppressWarnings("unchecked")
        @Override
        public List<StatusBean> getStatusBeans() {
            // this is required to retrieve updated data by reloading table
            final AnyTO actual = restClient.read(anyTO.getKey());

            final List<String> resources = new ArrayList<>();
            new ResourceRestClient().list().forEach(resourceTO -> {
                resources.add(resourceTO.getKey());
            });

            final List<ConnObjectWrapper> connObjects = statusUtils.getConnectorObjects(actual);

            final List<StatusBean> statusBeans = new ArrayList<>(connObjects.size() + 1);

            connObjects.forEach(entry -> {
                final StatusBean statusBean = statusUtils.getStatusBean(actual,
                        entry.getResourceName(),
                        entry.getConnObjectTO(),
                        actual instanceof GroupTO);

                statusBeans.add(statusBean);
                resources.remove(entry.getResourceName());
            });

            if (statusOnly) {
                final StatusBean syncope = new StatusBean(actual, "Syncope");

                syncope.setConnObjectLink(((UserTO) actual).getUsername());

                Status syncopeStatus = Status.UNDEFINED;
                if (((UserTO) actual).getStatus() != null) {
                    try {
                        syncopeStatus = Status.valueOf(((UserTO) actual).getStatus().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Unexpected status found: {}", ((UserTO) actual).getStatus(), e);
                    }
                }
                syncope.setStatus(syncopeStatus);

                statusBeans.add(syncope);
            } else {
                resources.stream().
                        map(resource -> statusUtils.getStatusBean(actual,
                        resource,
                        null,
                        actual instanceof GroupTO)).
                        map(statusBean -> {
                            statusBean.setLinked(false);
                            return statusBean;
                        }).
                        forEachOrdered(statusBean -> {
                            statusBeans.add(statusBean);
                        });
            }

            return statusBeans;
        }
    }
}
