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
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.ResourceStatusDataProvider;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.commons.status.StatusUtils;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class ResourceStatusDirectoryPanel
        extends DirectoryPanel<StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?, ?>>
        implements ModalPanel {

    private static final long serialVersionUID = -9148734710505211261L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private String type;

    private final ResourceTO resourceTO;

    public ResourceStatusDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final String type,
            final ResourceTO resourceTO) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef);
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.type = type;
        this.resourceTO = resourceTO;
        this.itemKeyFieldName = "key";

        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<StatusBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected List<IColumn<StatusBean, String>> getColumns() {
        final List<IColumn<StatusBean, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(
                new StringResourceModel("key", this), "key", "key"));

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

    public void updateResultTable(final String type, final AjaxRequestTarget target) {
        this.type = type;

        if (StringUtils.isNoneEmpty(type)) {
            switch (type) {
                case "USER":
                    this.restClient = new UserRestClient();
                    break;
                case "GROUP":
                    this.restClient = new GroupRestClient();
                    break;
                default:
                    this.restClient = new AnyObjectRestClient();
            }
        }

        super.updateResultTable(target);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        final List<ActionLink.ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionLink.ActionType.UNLINK);
        bulkActions.add(ActionLink.ActionType.DEPROVISION);
        bulkActions.add(ActionLink.ActionType.UNASSIGN);
        return bulkActions;
    }

    @Override
    protected ResourceStatusDataProvider dataProvider() {
        return new ResourceStatusDataProvider(type, resourceTO.getKey(), rows, SyncopeConstants.ROOT_REALM);
    }

    @Override
    protected String paginatorRowsKey() {
        return StringUtils.EMPTY;
    }
}
