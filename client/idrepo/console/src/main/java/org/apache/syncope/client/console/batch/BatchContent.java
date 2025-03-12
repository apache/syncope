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
package org.apache.syncope.client.console.batch;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cxf.helpers.CastUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BatchResponseColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.rest.RestClient;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.common.lib.request.BooleanReplacePatchItem;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.rest.api.beans.ExecSpecs;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class BatchContent<T extends Serializable, S> extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = 4114026480146090963L;

    private static final Logger LOG = LoggerFactory.getLogger(BatchContent.class);

    private WebMarkupContainer container;

    private ActionsPanel<Serializable> actionPanel;

    private SortableDataProvider<T, S> dataProvider;

    public BatchContent(
            final List<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final RestClient batchExecutor,
            final String keyFieldName) {

        this(MultilevelPanel.SECOND_LEVEL_ID, items, columns, actions, batchExecutor, keyFieldName);
    }

    public BatchContent(
            final String id,
            final List<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final RestClient batchExecutor,
            final String keyFieldName) {

        super(id);

        setup(items, columns);

        for (ActionLink.ActionType action : actions) {
            actionPanel.add(new ActionLink<>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                protected boolean statusCondition(final Serializable modelObject) {
                    return !CollectionUtils.isEmpty(items);
                }

                @Override
                @SuppressWarnings("unchecked")
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    if (CollectionUtils.isEmpty(items)) {
                        throw new IllegalArgumentException("Invalid items");
                    }

                    Map<String, String> results;
                    try {
                        T singleItem = items.getFirst();

                        if (singleItem instanceof ExecTO) {
                            results = new HashMap<>();
                            items.forEach(item -> {
                                ExecTO exec = ExecTO.class.cast(item);

                                try {
                                    batchExecutor.getClass().getMethod("deleteExecution", String.class).
                                            invoke(batchExecutor, exec.getKey());
                                    results.put(exec.getKey(), ExecStatus.SUCCESS.name());
                                } catch (Exception e) {
                                    LOG.error("Error deleting execution {}", exec.getKey(), e);
                                    results.put(exec.getKey(), ExecStatus.FAILURE.name());
                                }
                            });
                        } else if (singleItem instanceof StatusBean) {
                            AbstractAnyRestClient<?> anyRestClient = AbstractAnyRestClient.class.cast(batchExecutor);

                            // Group bean information by anyKey
                            Map<String, List<StatusBean>> beans = new HashMap<>();
                            items.stream().map(StatusBean.class::cast).
                                    forEachOrdered(sb -> {
                                        final List<StatusBean> sblist;
                                        if (beans.containsKey(sb.getKey())) {
                                            sblist = beans.get(sb.getKey());
                                        } else {
                                            sblist = new ArrayList<>();
                                            beans.put(sb.getKey(), sblist);
                                        }
                                        sblist.add(sb);
                                    });

                            results = new HashMap<>();
                            beans.forEach((key, value) -> {
                                String etag = anyRestClient.read(key).getETagValue();

                                switch (action) {
                                    case DEPROVISION:
                                        results.putAll(anyRestClient.deassociate(
                                                ResourceDeassociationAction.DEPROVISION, etag, key, value));
                                        break;

                                    case UNASSIGN:
                                        results.putAll(anyRestClient.deassociate(
                                                ResourceDeassociationAction.UNASSIGN, etag, key, value));
                                        break;

                                    case UNLINK:
                                        results.putAll(anyRestClient.deassociate(
                                                ResourceDeassociationAction.UNLINK, etag, key, value));
                                        break;

                                    case ASSIGN:
                                        results.putAll(anyRestClient.associate(
                                                ResourceAssociationAction.ASSIGN, etag, key, value));
                                        break;

                                    case LINK:
                                        results.putAll(anyRestClient.associate(
                                                ResourceAssociationAction.LINK, etag, key, value));
                                        break;

                                    case PROVISION:
                                        results.putAll(anyRestClient.associate(
                                                ResourceAssociationAction.PROVISION, etag, key, value));
                                        break;

                                    case SUSPEND:
                                        results.putAll(((UserRestClient) anyRestClient).suspend(etag, key, value));
                                        break;

                                    case REACTIVATE:
                                        results.putAll(((UserRestClient) anyRestClient).reactivate(etag, key, value));
                                        break;

                                    default:
                                }
                            });
                        } else {
                            BatchRequest batch = SyncopeConsoleSession.get().batch();

                            UserService batchUserService = batch.getService(UserService.class);
                            GroupService batchGroupService = batch.getService(GroupService.class);
                            AnyObjectService batchAnyObjectService = batch.getService(AnyObjectService.class);
                            AnyService<?> batchAnyService = singleItem instanceof UserTO
                                    ? batchUserService
                                    : singleItem instanceof GroupTO
                                            ? batchGroupService
                                            : batchAnyObjectService;
                            TaskService batchTaskService = batch.getService(TaskService.class);
                            ReportService batchReportService = batch.getService(ReportService.class);

                            Set<String> deletedAnys = new HashSet<>();

                            switch (action) {
                                case MUSTCHANGEPASSWORD:
                                    items.forEach(item -> {
                                        UserTO user = (UserTO) item;

                                        UserUR req = new UserUR();
                                        req.setKey(user.getKey());
                                        req.setMustChangePassword(new BooleanReplacePatchItem.Builder().
                                                value(!user.isMustChangePassword()).build());

                                        batchUserService.update(req);
                                    });
                                    break;

                                case SUSPEND:
                                    items.forEach(item -> {
                                        UserTO user = (UserTO) item;

                                        StatusR req = new StatusR.Builder(user.getKey(), StatusRType.SUSPEND).
                                                onSyncope(true).
                                                resources(user.getResources()).
                                                build();

                                        batchUserService.status(req);
                                    });
                                    break;

                                case REACTIVATE:
                                    items.forEach(item -> {
                                        UserTO user = (UserTO) item;

                                        StatusR req = new StatusR.Builder(user.getKey(), StatusRType.REACTIVATE).
                                                onSyncope(true).
                                                resources(user.getResources()).
                                                build();

                                        batchUserService.status(req);
                                    });
                                    break;

                                case DELETE:
                                    items.forEach(item -> {
                                        if (singleItem instanceof AnyTO) {
                                            AnyTO any = (AnyTO) item;

                                            batchAnyService.delete(any.getKey());
                                            deletedAnys.add(any.getKey());
                                        } else if (singleItem instanceof TaskTO) {
                                            TaskTO task = (TaskTO) item;

                                            batchTaskService.delete(
                                                    TaskType.fromTOClass(task.getClass()),
                                                    task.getKey());
                                        } else if (singleItem instanceof ReportTO) {
                                            ReportTO report = (ReportTO) item;

                                            batchReportService.delete(report.getKey());
                                        } else {
                                            LOG.warn("Unsupported for DELETE: {}", singleItem.getClass().getName());
                                        }
                                    });

                                    break;

                                case DRYRUN:
                                    items.forEach(item -> {
                                        TaskTO task = (TaskTO) item;

                                        batchTaskService.execute(
                                                new ExecSpecs.Builder().dryRun(true).key(task.getKey()).build());
                                    });
                                    break;

                                case EXECUTE:
                                    items.forEach(item -> {
                                        if (singleItem instanceof TaskTO) {
                                            TaskTO task = (TaskTO) item;

                                            batchTaskService.execute(
                                                    new ExecSpecs.Builder().dryRun(false).key(task.getKey()).build());
                                        } else if (singleItem instanceof ReportTO) {
                                            ReportTO report = (ReportTO) item;

                                            batchReportService.execute(
                                                    new ExecSpecs.Builder().key(report.getKey()).build());
                                        }
                                    });
                                    break;

                                default:
                            }

                            results = CastUtils.cast(Map.class.cast(
                                    batchExecutor.getClass().getMethod("batch",
                                            BatchRequest.class).invoke(batchExecutor, batch)));

                            if (singleItem instanceof AnyTO) {
                                AbstractAnyRestClient<? extends AnyTO> anyRestClient = singleItem instanceof UserTO
                                        ? UserRestClient.class.cast(batchExecutor)
                                        : singleItem instanceof GroupTO
                                                ? GroupRestClient.class.cast(batchExecutor)
                                                : AnyObjectRestClient.class.cast(batchExecutor);
                                for (int i = 0; i < items.size(); i++) {
                                    String key = ((AnyTO) items.get(i)).getKey();
                                    if (!deletedAnys.contains(key)) {
                                        items.set(i, (T) anyRestClient.read(key));
                                    }
                                }
                            }
                        }

                        List<IColumn<T, S>> newColumnList = new ArrayList<>(columns);
                        newColumnList.add(newColumnList.size(), new BatchResponseColumn<>(results, keyFieldName));

                        container.addOrReplace(new AjaxFallbackDefaultDataTable<>(
                                "selectedObjects",
                                newColumnList,
                                dataProvider,
                                Integer.MAX_VALUE).setVisible(!items.isEmpty()));

                        actionPanel.setEnabled(false);
                        actionPanel.setVisible(false);
                        target.add(container);
                        target.add(actionPanel);

                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    } catch (Exception e) {
                        LOG.error("Batch failure", e);
                        SyncopeConsoleSession.get().error("Operation " + action.getActionId() + " failed");
                    }
                    ((BasePage) getPage()).getNotificationPanel().refresh(target);
                }
            }, action, null).hideLabel();
        }
    }

    public BatchContent(
            final String id,
            final List<T> items,
            final List<IColumn<T, S>> columns,
            final Map<String, String> results,
            final String keyFieldName,
            final AjaxRequestTarget target,
            final PageReference pageRef) {

        super(id);

        List<IColumn<T, S>> newColumnList = new ArrayList<>(columns);
        newColumnList.add(newColumnList.size(), new BatchResponseColumn<>(results, keyFieldName));
        setup(items, newColumnList);

        actionPanel.setEnabled(false);
        actionPanel.setVisible(false);
        target.add(container);
        target.add(actionPanel);

        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    private void setup(
            final List<T> items,
            final List<IColumn<T, S>> columns) {

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        dataProvider = new SortableDataProvider<>() {

            private static final long serialVersionUID = 5291903859908641954L;

            @Override
            public Iterator<? extends T> iterator(final long first, final long count) {
                return items.iterator();
            }

            @Override
            public long size() {
                return items.size();
            }

            @Override
            public IModel<T> model(final T object) {
                return new CompoundPropertyModel<>(object);
            }
        };

        container.add(new AjaxFallbackDefaultDataTable<>(
                "selectedObjects",
                columns,
                dataProvider,
                Integer.MAX_VALUE).setMarkupId("selectedObjects").setVisible(!CollectionUtils.isEmpty(items)));

        actionPanel = new ActionsPanel<>("actions", null);
        container.add(actionPanel);
    }
}
