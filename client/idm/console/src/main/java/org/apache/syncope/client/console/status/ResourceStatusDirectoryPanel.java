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
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdMConstants;
import org.apache.syncope.client.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.AbstractFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ResourceStatusDirectoryPanel
        extends DirectoryPanel<StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?>>
        implements ModalPanel {

    private static final long serialVersionUID = -9148734710505211261L;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected AnyObjectRestClient anyObjectRestClient;

    protected final MultilevelPanel multiLevelPanelRef;

    protected String type;

    protected final ResourceTO resource;

    public ResourceStatusDirectoryPanel(
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final String type,
            final ResourceTO resource) {

        super(MultilevelPanel.FIRST_LEVEL_ID, null, pageRef);
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.type = type;
        this.resource = resource;
        this.itemKeyFieldName = "resource";

        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<StatusBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(multiLevelPanelRef);
    }

    @Override
    protected List<IColumn<StatusBean, String>> getColumns() {
        List<IColumn<StatusBean, String>> columns = new ArrayList<>();

        columns.add(new PropertyColumn<>(new StringResourceModel(Constants.NAME_FIELD_NAME, this),
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));

        return columns;
    }

    @Override
    public ActionsPanel<StatusBean> getActions(final IModel<StatusBean> model) {
        final ActionsPanel<StatusBean> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return StringUtils.isNotBlank(type);
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next(bean.getResource(),
                        new ReconStatusPanel(bean.getResource(), type, bean.getKey()),
                        target);
                target.add(multiLevelPanelRef);
                getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.VIEW, IdMEntitlement.RESOURCE_GET_CONNOBJECT);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return StringUtils.isNotBlank(type);
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next("PUSH " + bean.getResource(),
                        new ReconTaskPanel(
                                bean.getResource(),
                                new PushTaskTO(),
                                type,
                                bean.getKey(),
                                true,
                                multiLevelPanelRef,
                                pageRef),
                        target);
                target.add(multiLevelPanelRef);
                getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.RECONCILIATION_PUSH, IdRepoEntitlement.TASK_EXECUTE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            protected boolean statusCondition(final StatusBean bean) {
                return StringUtils.isNotBlank(type);
            }

            @Override
            public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                multiLevelPanelRef.next("PULL " + bean.getResource(),
                        new ReconTaskPanel(
                                bean.getResource(),
                                new PullTaskTO(),
                                type,
                                bean.getKey(),
                                true,
                                multiLevelPanelRef,
                                pageRef),
                        target);
                target.add(multiLevelPanelRef);
                getTogglePanel().close(target);
            }
        }, ActionLink.ActionType.RECONCILIATION_PULL, IdRepoEntitlement.TASK_EXECUTE);

        return panel;
    }

    public void updateResultTable(final String type, final AjaxRequestTarget target) {
        this.type = type;

        if (StringUtils.isNotBlank(type)) {
            switch (type) {
                case "USER":
                    restClient = userRestClient;
                    break;

                case "GROUP":
                    restClient = groupRestClient;
                    break;

                default:
                    restClient = anyObjectRestClient;
            }
        }

        synchronized (this) {
            dataProvider = dataProvider();
        }

        super.updateResultTable(target);
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        List<ActionLink.ActionType> batches = new ArrayList<>();
        batches.add(ActionLink.ActionType.UNLINK);
        batches.add(ActionLink.ActionType.LINK);
        batches.add(ActionLink.ActionType.DEPROVISION);
        batches.add(ActionLink.ActionType.PROVISION);
        batches.add(ActionLink.ActionType.ASSIGN);
        batches.add(ActionLink.ActionType.UNASSIGN);
        return batches;
    }

    @Override
    protected ResourceStatusDataProvider dataProvider() {
        return new ResourceStatusDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdMConstants.PREF_RECONCILIATION_PAGINATOR_ROWS;
    }

    protected class ResourceStatusDataProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        private final String fiql;

        private final AbstractAnyRestClient<? extends AnyTO> restClient;

        public ResourceStatusDataProvider(final int paginatorRows) {
            super(paginatorRows, AnyTypeKind.USER.name().equals(type)
                    ? Constants.USERNAME_FIELD_NAME : Constants.NAME_FIELD_NAME);

            if (StringUtils.isEmpty(type)) {
                fiql = null;
                restClient = null;
            } else {
                AbstractFiqlSearchConditionBuilder<?, ?, ?> bld;
                switch (type) {
                    case "USER":
                        bld = SyncopeClient.getUserSearchConditionBuilder();
                        restClient = userRestClient;
                        break;

                    case "GROUP":
                        bld = SyncopeClient.getGroupSearchConditionBuilder();
                        restClient = groupRestClient;
                        break;

                    default:
                        bld = SyncopeClient.getAnyObjectSearchConditionBuilder(type);
                        restClient = anyObjectRestClient;
                }
                fiql = bld.isNotNull(Constants.KEY_FIELD_NAME).query();
            }
        }

        @Override
        protected List<StatusBean> getStatusBeans(final long first, final long count) {
            List<StatusBean> statusBeans = new ArrayList<>();

            if (fiql != null && restClient != null) {
                int page = (int) first / paginatorRows;
                List<? extends AnyTO> result = restClient.search(
                        SyncopeConstants.ROOT_REALM, fiql, (page < 0 ? 0 : page) + 1, paginatorRows, getSort(), type);

                statusBeans.addAll(result.stream().map(any -> StatusUtils.getStatusBean(any,
                        resource.getKey(),
                        null,
                        any instanceof GroupTO)).toList());
            }

            return statusBeans;
        }

        @Override
        public long size() {
            return Optional.ofNullable(fiql).
                    map(s -> restClient.count(SyncopeConstants.ROOT_REALM, s, type)).
                    orElse(0L);
        }
    }
}
