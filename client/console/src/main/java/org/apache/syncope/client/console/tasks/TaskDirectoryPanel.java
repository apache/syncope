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

import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.commons.TaskDataProvider;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AbstractTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

/**
 * Tasks page.
 *
 * @param <T> task type.
 */
public abstract class TaskDirectoryPanel<T extends AbstractTaskTO>
        extends DirectoryPanel<T, T, TaskDataProvider<T>, TaskRestClient> implements ModalPanel<T> {

    private static final long serialVersionUID = 4984337552918213290L;

    private final BaseModal<?> baseModal;

    protected final MultilevelPanel multiLevelPanelRef;

    protected TaskDirectoryPanel(
            final BaseModal<?> baseModal, final MultilevelPanel multiLevelPanelRef, final PageReference pageRef) {
        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef, false);
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        restClient = new TaskRestClient();
        setShowResultPage(false);
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<T, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public T getItem() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    protected abstract void viewTask(final T taskTO, final AjaxRequestTarget target);

    protected abstract class TasksProvider<T extends AbstractTaskTO> extends DirectoryDataProvider<T> {

        private static final long serialVersionUID = -20112718133295756L;

        private final SortableDataProviderComparator<T> comparator;

        private final TaskType id;

        public TasksProvider(final int paginatorRows, final TaskType id) {
            super(paginatorRows);

            //Default sorting
            setSort("key", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<>(this);
            this.id = id;
        }

        public SortableDataProviderComparator<T> getComparator() {
            return comparator;
        }

        @Override
        public long size() {
            return restClient.count(id);
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<>(object);
        }
    }

}
