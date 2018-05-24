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
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.status.AbstractStatusBeanProvider;
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
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

public class ResourceStatusDirectoryPanel
        extends DirectoryPanel<StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?>>
        implements ModalPanel {

    private static final long serialVersionUID = -9148734710505211261L;

    private final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    private String type;

    private final ResourceTO resource;

    public ResourceStatusDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final String type,
            final ResourceTO resource) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef);
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.type = type;
        this.resource = resource;
        this.itemKeyFieldName = "resource";

        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<StatusBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    protected List<IColumn<StatusBean, String>> getColumns() {
        final List<IColumn<StatusBean, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(new StringResourceModel("name", this), "name", "name"));

        return columns;
    }

    private AnyTypeKind getAnyTypeKind() {
        if (StringUtils.isBlank(type)) {
            return null;
        }

        switch (type) {
            case "USER":
                return AnyTypeKind.USER;

            case "GROUP":
                return AnyTypeKind.GROUP;

            default:
                return AnyTypeKind.ANY_OBJECT;
        }
    }

    @Override
    public ActionsPanel<StatusBean> getActions(final IModel<StatusBean> model) {
        final ActionsPanel<StatusBean> panel = super.getActions(model);

        panel.add(new ActionLink<StatusBean>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return getAnyTypeKind() != null;
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next(bean.getResource(),
                        new ReconStatusPanel(bean.getResource(), getAnyTypeKind(), bean.getKey()),
                        target);
                target.add(multiLevelPanelRef);
                getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.RESOURCE_GET_CONNOBJECT);

        panel.add(new ActionLink<StatusBean>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return getAnyTypeKind() != null;
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next("PUSH " + bean.getResource(),
                        new ReconTaskPanel(
                                bean.getResource(),
                                new PushTaskTO(),
                                getAnyTypeKind(),
                                bean.getKey(),
                                multiLevelPanelRef,
                                pageRef),
                        target);
                target.add(multiLevelPanelRef);
                getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.RECONCILIATION_PUSH, StandardEntitlement.TASK_EXECUTE);

        panel.add(new ActionLink<StatusBean>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return getAnyTypeKind() != null;
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next("PULL " + bean.getResource(),
                        new ReconTaskPanel(
                                bean.getResource(),
                                new PullTaskTO(),
                                getAnyTypeKind(),
                                bean.getKey(),
                                multiLevelPanelRef,
                                pageRef),
                        target);
                target.add(multiLevelPanelRef);
                getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.RECONCILIATION_PULL, StandardEntitlement.TASK_EXECUTE);

        return panel;
    }

    public void updateResultTable(final String type, final AjaxRequestTarget target) {
        this.type = type;

        if (StringUtils.isNotBlank(type)) {
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
        List<ActionLink.ActionType> bulkActions = new ArrayList<>();
        bulkActions.add(ActionLink.ActionType.UNLINK);
        bulkActions.add(ActionLink.ActionType.LINK);
        bulkActions.add(ActionLink.ActionType.DEPROVISION);
        bulkActions.add(ActionLink.ActionType.PROVISION);
        bulkActions.add(ActionLink.ActionType.ASSIGN);
        bulkActions.add(ActionLink.ActionType.UNASSIGN);
        return bulkActions;
    }

    @Override
    protected ResourceStatusDataProvider dataProvider() {
        return new ResourceStatusDataProvider();
    }

    @Override
    protected String paginatorRowsKey() {
        return StringUtils.EMPTY;
    }

    protected class ResourceStatusDataProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        private final String fiql;

        private final AbstractAnyRestClient<? extends AnyTO> restClient;

        public ResourceStatusDataProvider() {
            super(AnyTypeKind.USER.name().equals(type) ? "username" : "name");

            if (StringUtils.isEmpty(type)) {
                fiql = null;
                restClient = null;
            } else {
                AbstractFiqlSearchConditionBuilder bld;
                switch (type) {
                    case "USER":
                        bld = SyncopeClient.getUserSearchConditionBuilder();
                        restClient = new UserRestClient();
                        break;

                    case "GROUP":
                        bld = SyncopeClient.getGroupSearchConditionBuilder();
                        restClient = new GroupRestClient();
                        break;

                    default:
                        bld = SyncopeClient.getAnyObjectSearchConditionBuilder(type);
                        restClient = new AnyObjectRestClient();
                }
                fiql = bld.isNotNull("key").query();
            }
        }

        @Override
        protected List<StatusBean> getStatusBeans(final long first, final long count) {
            List<StatusBean> statusBeans = new ArrayList<>();

            if (fiql != null && restClient != null) {
                int page = (int) first / paginatorRows;
                List<? extends AnyTO> result = restClient.search(
                        SyncopeConstants.ROOT_REALM, fiql, (page < 0 ? 0 : page) + 1, paginatorRows, getSort(), type);

                statusBeans.addAll(result.stream().map(any -> StatusUtils.getStatusBean(
                        any,
                        resource.getKey(),
                        null,
                        any instanceof GroupTO)).collect(Collectors.toList()));
            }

            return statusBeans;
        }

        @Override
        public long size() {
            return fiql == null
                    ? 0
                    : restClient.count(SyncopeConstants.ROOT_REALM, fiql, type);
        }
    }
}
