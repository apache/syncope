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
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.ajax.IndicatorAjaxTimerBehavior;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.widgets.JobActionPanel;
import org.apache.syncope.common.lib.to.InboundTaskTO;
import org.apache.syncope.common.lib.to.ProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
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
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final TaskType taskType,
            final T newTaskTO,
            final String resource,
            final PageReference pageRef) {

        super(
                MultilevelPanel.FIRST_LEVEL_ID,
                restClient,
                baseModal,
                multiLevelPanelRef,
                taskType,
                newTaskTO,
                pageRef,
                false);
        this.resource = resource;

        this.schedTaskTO.setResource(resource);

        // super in order to call the parent implementation
        enableUtilityButton();
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

        columns.addAll(getHeadingFieldColumns());

        if (schedTaskTO instanceof InboundTaskTO) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("destinationRealm", this), "destinationRealm", "destinationRealm"));
        } else if (schedTaskTO instanceof PushTaskTO) {
            columns.add(new PropertyColumn<>(
                    new StringResourceModel("sourceRealm", this), "sourceRealm", "sourceRealm"));
        }

        columns.addAll(getTrailingFieldColumns());

        return columns;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof JobActionPanel.JobActionPayload payload) {
            container.modelChanged();
            payload.getTarget().add(container);
        } else {
            super.onEvent(event);
        }
    }

    protected class ProvisioningTasksProvider<T extends ProvisioningTaskTO> extends SchedTasksProvider<T> {

        private static final long serialVersionUID = 4725679400450513556L;

        public ProvisioningTasksProvider(final TaskType taskType, final int paginatorRows) {
            super(taskType, paginatorRows);
        }

        @Override
        public long size() {
            return restClient.count(resource, taskType);
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.<T>list(
                    resource, taskType, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }
    }
}
