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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdMConstants;
import org.apache.syncope.client.console.commons.status.AbstractStatusBeanProvider;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.LinkedAccountsStatusModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.status.Status;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AnyStatusDirectoryPanel
        extends DirectoryPanel<StatusBean, StatusBean, DirectoryDataProvider<StatusBean>, AbstractAnyRestClient<?>>
        implements ModalPanel {

    private static final long serialVersionUID = -9148734710505211261L;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected ReconStatusUtils reconStatusUtils;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected AnyObjectRestClient anyObjectRestClient;

    protected final MultilevelPanel multiLevelPanelRef;

    protected final AnyTO anyTO;

    protected final AnyTypeKind anyTypeKind;

    protected final boolean statusOnly;

    private final List<String> resources;

    public AnyStatusDirectoryPanel(
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final AnyTO anyTO,
            final String itemKeyFieldName,
            final boolean statusOnly) {

        super(MultilevelPanel.FIRST_LEVEL_ID, null, pageRef);
        this.multiLevelPanelRef = multiLevelPanelRef;
        this.statusOnly = statusOnly;
        this.anyTO = anyTO;
        this.itemKeyFieldName = itemKeyFieldName;

        if (anyTO instanceof UserTO) {
            this.restClient = userRestClient;
            anyTypeKind = AnyTypeKind.USER;
        } else if (anyTO instanceof GroupTO) {
            this.restClient = groupRestClient;
            anyTypeKind = AnyTypeKind.GROUP;
        } else {
            this.restClient = anyObjectRestClient;
            anyTypeKind = AnyTypeKind.ANY_OBJECT;
        }

        resources = resourceRestClient.list().stream().
                filter(resource -> resource.getProvision(anyTO.getType()).isPresent()).
                map(ResourceTO::getKey).collect(Collectors.toList());

        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<StatusBean, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(multiLevelPanelRef);
    }

    @Override
    protected List<IColumn<StatusBean, String>> getColumns() {
        List<IColumn<StatusBean, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(
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
                        if (anyTO.getResources().contains(model.getObject().getResource())
                                || Constants.SYNCOPE.equalsIgnoreCase(model.getObject().getResource())) {

                            super.onComponentTag(tag);
                        } else {
                            tag.put("style", "font-style: italic");
                        }
                    }
                });
            }
        });

        if (statusOnly) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("connObjectLink", this), "connObjectLink", "connObjectLink"));

            columns.add(new AbstractColumn<>(new StringResourceModel("status", this)) {

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
        }

        return columns;
    }

    @Override
    public ActionsPanel<StatusBean> getActions(final IModel<StatusBean> model) {
        final ActionsPanel<StatusBean> panel = super.getActions(model);

        if (!Constants.SYNCOPE.equalsIgnoreCase(model.getObject().getResource())) {
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -7978723352517770645L;

                @Override
                public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                    multiLevelPanelRef.next(bean.getResource(),
                            new ReconStatusPanel(bean.getResource(), anyTO.getType(), anyTO.getKey()),
                            target);
                    target.add(multiLevelPanelRef);
                    AnyStatusDirectoryPanel.this.getTogglePanel().close(target);
                }
            }, ActionLink.ActionType.VIEW, IdMEntitlement.RESOURCE_GET_CONNOBJECT);
        }

        if (!statusOnly) {
            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -7978723352517770645L;

                @Override
                public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                    multiLevelPanelRef.next("PUSH " + bean.getResource(),
                            new ReconTaskPanel(
                                    bean.getResource(),
                                    new PushTaskTO(),
                                    anyTO.getType(),
                                    anyTO.getKey(),
                                    true,
                                    multiLevelPanelRef,
                                    pageRef),
                            target);
                    target.add(multiLevelPanelRef);
                    AnyStatusDirectoryPanel.this.getTogglePanel().close(target);
                }
            }, ActionLink.ActionType.RECONCILIATION_PUSH, IdRepoEntitlement.TASK_EXECUTE);

            panel.add(new ActionLink<>() {

                private static final long serialVersionUID = -7978723352517770645L;

                @Override
                public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                    multiLevelPanelRef.next("PULL " + bean.getResource(),
                            new ReconTaskPanel(
                                    bean.getResource(),
                                    new PullTaskTO(),
                                    anyTO.getType(),
                                    anyTO.getKey(),
                                    true,
                                    multiLevelPanelRef,
                                    pageRef),
                            target);
                    target.add(multiLevelPanelRef);
                    AnyStatusDirectoryPanel.this.getTogglePanel().close(target);
                }
            }, ActionLink.ActionType.RECONCILIATION_PULL, IdRepoEntitlement.TASK_EXECUTE);
        }

        if (anyTO instanceof UserTO && !UserTO.class.cast(anyTO).getLinkedAccounts().isEmpty()) {
            UserTO userTO = UserTO.class.cast(anyTO);

            if (!userTO.getLinkedAccounts().isEmpty()
                    && userTO.getLinkedAccounts().stream().anyMatch(linkedAccountTO -> {
                        return linkedAccountTO.getResource().equals(model.getObject().getResource());
                    })) {

                panel.add(new ActionLink<>() {

                    private static final long serialVersionUID = 5168094747477174155L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final StatusBean bean) {
                        multiLevelPanelRef.next("ACCOUNTS",
                                new LinkedAccountsStatusModalPanel(Model.of(UserTO.class.cast(anyTO)), pageRef),
                                target);
                        target.add(multiLevelPanelRef);
                        AnyStatusDirectoryPanel.this.getTogglePanel().close(target);
                    }
                }, ActionLink.ActionType.MANAGE_ACCOUNTS,
                        String.format("%s,%s,%s", IdRepoEntitlement.USER_READ, IdRepoEntitlement.USER_UPDATE,
                                IdMEntitlement.RESOURCE_GET_CONNOBJECT));
            }
        }

        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        List<ActionLink.ActionType> batches = new ArrayList<>();
        if (statusOnly) {
            batches.add(ActionLink.ActionType.SUSPEND);
            batches.add(ActionLink.ActionType.REACTIVATE);
        } else {
            batches.add(ActionLink.ActionType.UNLINK);
            batches.add(ActionLink.ActionType.LINK);
            batches.add(ActionLink.ActionType.DEPROVISION);
            batches.add(ActionLink.ActionType.PROVISION);
            batches.add(ActionLink.ActionType.ASSIGN);
            batches.add(ActionLink.ActionType.UNASSIGN);
        }
        return batches;
    }

    @Override
    protected AnyStatusProvider dataProvider() {
        return new AnyStatusProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdMConstants.PREF_RESOURCE_STATUS_PAGINATOR_ROWS;
    }

    protected class AnyStatusProvider extends AbstractStatusBeanProvider {

        private static final long serialVersionUID = 4586969457669796621L;

        AnyStatusProvider(final int paginatorRows) {
            super(paginatorRows, "resource");
        }

        @Override
        protected List<StatusBean> getStatusBeans(final long first, final long count) {
            // this is required to retrieve updated data by reloading table
            final AnyTO actual = restClient.read(anyTO.getKey());

            List<StatusBean> statusBeans = actual.getResources().stream().map(resource -> {
                List<Pair<String, ReconStatus>> statuses = List.of();
                if (statusOnly) {
                    statuses = reconStatusUtils.getReconStatuses(anyTO.getType(), anyTO.getKey(), List.of(resource));
                }

                return StatusUtils.getStatusBean(actual,
                        resource,
                        statuses.isEmpty() ? null : statuses.getFirst().getRight().getOnResource(),
                        actual instanceof GroupTO);
            }).collect(Collectors.toList());

            if (statusOnly) {
                StatusBean syncope = new StatusBean(actual, Constants.SYNCOPE);
                switch (anyTypeKind) {
                    case USER:
                        syncope.setConnObjectLink(((UserTO) actual).getUsername());
                        break;

                    case GROUP:
                        syncope.setConnObjectLink(((GroupTO) actual).getName());
                        break;

                    case ANY_OBJECT:
                        syncope.setConnObjectLink(((AnyObjectTO) actual).getName());
                        break;

                    default:
                }

                Status syncopeStatus = Status.UNDEFINED;
                if (actual.getStatus() != null) {
                    try {
                        syncopeStatus = Status.valueOf(actual.getStatus().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Unexpected status found: {}", actual.getStatus(), e);
                    }
                }
                syncope.setStatus(syncopeStatus);

                statusBeans.sort(comparator);
                statusBeans.addFirst(syncope);
            } else {
                statusBeans.addAll(resources.stream().
                        filter(resource -> !actual.getResources().contains(resource)).
                        map(resource -> {
                            StatusBean statusBean = StatusUtils.getStatusBean(actual,
                                    resource,
                                    null,
                                    actual instanceof GroupTO);
                            statusBean.setLinked(false);
                            return statusBean;
                        }).toList());

                statusBeans.sort(comparator);
            }

            return first == -1 && count == -1
                    ? statusBeans
                    : statusBeans.subList((int) first, (int) first + (int) count);
        }
    }
}
