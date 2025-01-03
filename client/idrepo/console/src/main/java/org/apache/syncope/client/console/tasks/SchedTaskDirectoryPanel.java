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
package org.apache.syncope.client.console.tasks;

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.io.Serializable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.TaskDataProvider;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.RealmRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.widgets.JobActionPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Tasks page.
 *
 * @param <T> Sched task type.
 */
public abstract class SchedTaskDirectoryPanel<T extends SchedTaskTO>
        extends TaskDirectoryPanel<T> implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    @SpringBean
    protected RealmRestClient realmRestClient;

    @SpringBean
    protected TaskRestClient taskRestClient;

    protected final TaskType taskType;

    protected final T schedTaskTO;

    protected final TaskStartAtTogglePanel startAt;

    protected final TemplatesTogglePanel templates;

    protected SchedTaskDirectoryPanel(
            final String id,
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final TaskType taskType,
            final T newTaskTO,
            final PageReference pageRef,
            final boolean wizardInModal) {

        super(id, restClient, baseModal, multiLevelPanelRef, pageRef, wizardInModal);
        this.taskType = taskType;
        this.schedTaskTO = newTaskTO;

        modal.size(Modal.Size.Large);

        addNewItemPanelBuilder(new SchedTaskWizardBuilder<>(
                taskType, schedTaskTO, realmRestClient, taskRestClient, pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.TASK_CREATE);

        setFooterVisibility(false);

        initResultTable();

        container.add(new IndicatorAjaxTimerBehavior(Duration.of(10, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = -4661303265651934868L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                container.modelChanged();
                target.add(container);
            }
        });

        startAt = new TaskStartAtTogglePanel(container, pageRef);
        addInnerObject(startAt);

        templates = new TemplatesTogglePanel(getActualId(), this, pageRef) {

            private static final long serialVersionUID = -8765794727538618705L;

            @Override
            protected Serializable onApplyInternal(
                    final TemplatableTO targetObject, final String type, final AnyTO anyTO) {

                targetObject.getTemplates().put(type, anyTO);
                restClient.update(taskType, SchedTaskTO.class.cast(targetObject));
                return targetObject;
            }
        };
        addInnerObject(templates);
    }

    protected List<IColumn<T, String>> getHeadingFieldColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.NAME_FIELD_NAME, this),
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));

        return columns;
    }

    protected List<IColumn<T, String>> getTrailingFieldColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("lastExec", this), null, "lastExec"));

        columns.add(new DatePropertyColumn<>(
                new StringResourceModel("nextExec", this), null, "nextExec"));

        columns.add(new PropertyColumn<>(
                new StringResourceModel("latestExecStatus", this), "latestExecStatus", "latestExecStatus"));

        columns.add(new BooleanPropertyColumn<>(
                new StringResourceModel("active", this), "active", "active"));

        columns.add(new AbstractColumn<>(new Model<>(""), "running") {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<T>> cellItem,
                    final String componentId,
                    final IModel<T> rowModel) {

                Component panel;
                try {
                    JobTO jobTO = restClient.getJob(rowModel.getObject().getKey());
                    panel = new JobActionPanel(componentId, jobTO, false, SchedTaskDirectoryPanel.this);
                    MetaDataRoleAuthorizationStrategy.authorize(
                            panel, WebPage.ENABLE,
                            String.format("%s,%s", IdRepoEntitlement.TASK_EXECUTE, IdRepoEntitlement.TASK_UPDATE));
                } catch (Exception e) {
                    LOG.error("Could not get job for task {}", rowModel.getObject().getKey(), e);
                    panel = new Label(componentId, Model.of());
                }
                cellItem.add(panel);
            }

            @Override
            public String getCssClass() {
                return "running-col";
            }
        });

        return columns;
    }

    protected List<IColumn<T, String>> getFieldColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.addAll(getHeadingFieldColumns());

        columns.add(new PropertyColumn<>(new StringResourceModel("jobDelegate", this), "jobDelegate", "jobDelegate") {

            private static final long serialVersionUID = -3223917055078733093L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<T>> item,
                    final String componentId,
                    final IModel<T> rowModel) {

                IModel<?> model = getDataModel(rowModel);
                if (model != null && model.getObject() instanceof String) {
                    String value = String.class.cast(model.getObject());
                    if (value.length() > 20) {
                        item.add(new Label(componentId, new Model<>("..." + value.substring(value.length() - 17))));
                    } else {
                        item.add(new Label(componentId, getDataModel(rowModel)));
                    }
                } else {
                    super.populateItem(item, componentId, rowModel);
                }
            }
        });

        columns.addAll(getTrailingFieldColumns());

        return columns;
    }

    @Override
    protected final List<IColumn<T, String>> getColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.addAll(getFieldColumns());

        return columns;
    }

    protected void onDelete(final AjaxRequestTarget target) {
        // nothing to do
    }

    @Override
    public ActionsPanel<T> getActions(final IModel<T> model) {
        ActionsPanel<T> panel = super.getActions(model);
        T taskTO = model.getObject();

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                SchedTaskDirectoryPanel.this.getTogglePanel().close(target);
                viewTaskExecs(taskTO, target);
            }
        }, ActionLink.ActionType.VIEW_EXECUTIONS, IdRepoEntitlement.TASK_READ);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                SchedTaskDirectoryPanel.this.getTogglePanel().close(target);
                send(SchedTaskDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(
                                restClient.readTask(taskType, model.getObject().getKey()), target).setTitleModel(
                                new StringResourceModel("inner.task.edit",
                                        SchedTaskDirectoryPanel.this,
                                        Model.of(Pair.of(ActionLink.ActionType.EDIT, model.getObject())))));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.TASK_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                SchedTaskDirectoryPanel.this.getTogglePanel().close(target);
                T clone = SerializationUtils.clone(model.getObject());
                clone.setKey(null);
                send(SchedTaskDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(clone, target).setTitleModel(
                                new StringResourceModel("inner.task.clone",
                                        SchedTaskDirectoryPanel.this,
                                        Model.of(Pair.of(ActionLink.ActionType.CLONE, model.getObject())))));
            }
        }, ActionLink.ActionType.CLONE, IdRepoEntitlement.TASK_CREATE);

        addFurtherActions(panel, model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                SchedTaskDirectoryPanel.this.getTogglePanel().close(target);
                startAt.setExecutionDetail(model.getObject().getKey(), model.getObject().getName(), target);
                startAt.toggle(target, true);
            }
        }, ActionLink.ActionType.EXECUTE, IdRepoEntitlement.TASK_EXECUTE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final T ignore) {
                try {
                    restClient.delete(taskType, taskTO.getKey());

                    onDelete(target);

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                    SchedTaskDirectoryPanel.this.getTogglePanel().close(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting propagation task {}", taskTO.getKey(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.TASK_DELETE, true);

        return panel;
    }

    protected void addFurtherActions(final ActionsPanel<T> panel, final IModel<T> model) {
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_SCHED_TASKS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionType> getBatches() {
        List<ActionType> batches = new ArrayList<>();
        batches.add(ActionType.DELETE);
        batches.add(ActionType.EXECUTE);
        batches.add(ActionType.DRYRUN);
        return batches;
    }

    @Override
    protected SchedTasksProvider<T> dataProvider() {
        return new SchedTasksProvider<>(taskType, rows);
    }

    protected class SchedTasksProvider<T extends SchedTaskTO> extends TaskDataProvider<T> {

        private static final long serialVersionUID = 4725679400450513556L;

        public SchedTasksProvider(final TaskType taskType, final int paginatorRows) {
            super(paginatorRows, taskType);
            setSort(Constants.NAME_FIELD_NAME, SortOrder.ASCENDING);
        }

        @Override
        public long size() {
            return restClient.count(taskType);
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.<T>list(
                    taskType, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }
    }
}
