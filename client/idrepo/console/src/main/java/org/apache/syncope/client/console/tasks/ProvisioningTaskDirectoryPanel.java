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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.widgets.JobActionPanel;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * Tasks page.
 *
 * @param <T> Sched task type.
 */
public abstract class ProvisioningTaskDirectoryPanel<T extends ProvisioningTaskTO>
        extends SchedTaskDirectoryPanel<T> {

    private static final long serialVersionUID = 4984337552918213290L;

    private final String resource;

    protected ProvisioningTaskDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final TaskType taskType,
            final Class<T> reference,
            final String resource,
            final PageReference pageRef) {

        super(baseModal, multiLevelPanelRef, taskType, reference, pageRef);
        this.resource = resource;

        this.schedTaskTO.setResource(resource);

        // super in order to call the parent implementation
        super.initResultTable();

        container.add(new IndicatorAjaxTimerBehavior(java.time.Duration.of(10, ChronoUnit.SECONDS)) {

            private static final long serialVersionUID = -4661303265651934868L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                container.modelChanged();
                target.add(container);
            }
        });
    }

    @Override
    protected void initResultTable() {
        // Do nothing in order to disable the call performed by the parent
    }

    @Override
    protected List<IColumn<T, String>> getFieldColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<>(
                new StringResourceModel(Constants.KEY_FIELD_NAME, this), Constants.KEY_FIELD_NAME));

        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.NAME_FIELD_NAME, this),
                Constants.NAME_FIELD_NAME, Constants.NAME_FIELD_NAME));

        columns.add(new PropertyColumn<>(
                new StringResourceModel(Constants.DESCRIPTION_FIELD_NAME, this),
                Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME));

        if (reference == PullTaskTO.class) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("destinationRealm", this), "destinationRealm", "destinationRealm"));
        } else if (reference == PushTaskTO.class) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("sourceRealm", this), "sourceRealm", "sourceRealm"));
        }

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
                    JobTO jobTO = TaskRestClient.getJob(rowModel.getObject().getKey());
                    panel = new JobActionPanel(componentId, jobTO, false, ProvisioningTaskDirectoryPanel.this);
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
                return "col-xs-1";
            }
        });

        return columns;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof JobActionPanel.JobActionPayload) {
            container.modelChanged();
            JobActionPanel.JobActionPayload.class.cast(event.getPayload()).getTarget().add(container);
        } else {
            super.onEvent(event);
        }
    }

    protected class ProvisioningTasksProvider<T extends ProvisioningTaskTO> extends SchedTasksProvider<T> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final Class<T> reference;

        public ProvisioningTasksProvider(final Class<T> reference, final TaskType id, final int paginatorRows) {
            super(reference, id, paginatorRows);
            this.reference = reference;
        }

        @Override
        public long size() {
            return TaskRestClient.count(resource, taskType);
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return TaskRestClient.list(
                    resource, reference, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }
    }
}
