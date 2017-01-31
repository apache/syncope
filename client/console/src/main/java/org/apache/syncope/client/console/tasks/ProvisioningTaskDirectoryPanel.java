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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.DatePropertyColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.KeyPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AbstractProvisioningTaskTO;
import org.apache.syncope.common.lib.to.PullTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.StringResourceModel;

/**
 * Tasks page.
 *
 * @param <T> Sched task type.
 */
public abstract class ProvisioningTaskDirectoryPanel<T extends AbstractProvisioningTaskTO>
        extends SchedTaskDirectoryPanel<T> {

    private static final long serialVersionUID = 4984337552918213290L;

    private final String resource;

    protected ProvisioningTaskDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final Class<T> reference,
            final String resource,
            final PageReference pageRef) {
        super(baseModal, multiLevelPanelRef, reference, pageRef);
        this.resource = resource;

        this.schedTaskTO.setResource(resource);

        // super in order to call the parent implementation
        super.initResultTable();
    }

    @Override
    protected void initResultTable() {
        // Do nothing in order to disable the call performed by the parent
    }

    @Override
    protected List<IColumn<T, String>> getFieldColumns() {
        List<IColumn<T, String>> columns = new ArrayList<>();

        columns.add(new KeyPropertyColumn<T>(
                new StringResourceModel("key", this), "key", "key"));

        columns.add(new PropertyColumn<T, String>(
                new StringResourceModel("name", this), "name", "name"));

        columns.add(new PropertyColumn<T, String>(
                new StringResourceModel("description", this), "description", "description"));

        if (reference == PullTaskTO.class) {
            columns.add(new PropertyColumn<T, String>(
                    new StringResourceModel("destinationRealm", this), "destinationRealm", "destinationRealm"));
        }

        columns.add(new DatePropertyColumn<T>(
                new StringResourceModel("lastExec", this), "lastExec", "lastExec"));

        columns.add(new DatePropertyColumn<T>(
                new StringResourceModel("nextExec", this), "nextExec", "nextExec"));

        columns.add(new PropertyColumn<T, String>(
                new StringResourceModel("latestExecStatus", this), "latestExecStatus", "latestExecStatus"));

        columns.add(new BooleanPropertyColumn<T>(
                new StringResourceModel("active", this), "active", "active"));

        return columns;
    }

    protected class ProvisioningTasksProvider<T extends AbstractProvisioningTaskTO> extends SchedTasksProvider<T> {

        private static final long serialVersionUID = 4725679400450513556L;

        private final Class<T> reference;

        public ProvisioningTasksProvider(final Class<T> reference, final TaskType id, final int paginatorRows) {
            super(reference, id, paginatorRows);
            this.reference = reference;
        }

        @Override
        public long size() {
            return restClient.count(resource, taskType);
        }

        @Override
        public Iterator<T> iterator(final long first, final long count) {
            final int page = ((int) first / paginatorRows);

            final List<T> tasks = restClient.list(
                    resource, reference, (page < 0 ? 0 : page) + 1, paginatorRows, getSort());

            Collections.sort(tasks, getComparator());
            return tasks.iterator();
        }
    }
}
