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
import org.apache.syncope.client.console.commons.TaskDataProvider;
import org.apache.syncope.client.console.panels.AjaxDataTablePanel;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.to.TaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

/**
 * Tasks {@link DirectoryPanel}.
 *
 * @param <T> task type.
 */
public abstract class TaskDirectoryPanel<T extends TaskTO>
        extends DirectoryPanel<T, T, TaskDataProvider<T>, TaskRestClient> implements ModalPanel {

    private static final long serialVersionUID = 4984337552918213290L;

    protected final BaseModal<?> baseModal;

    protected final MultilevelPanel multiLevelPanelRef;

    protected TaskDirectoryPanel(
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final boolean wizardInModal) {

        this(MultilevelPanel.FIRST_LEVEL_ID, restClient, baseModal, multiLevelPanelRef, pageRef, wizardInModal);
    }

    protected TaskDirectoryPanel(final String id, final TaskRestClient restClient, final PageReference pageRef) {
        this(id, restClient, null, null, pageRef, false);
    }

    protected TaskDirectoryPanel(
            final String id,
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final PageReference pageRef,
            final boolean wizardInModal) {

        super(id, restClient, pageRef, wizardInModal);
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        setShowResultPanel(false);
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<T, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(multiLevelPanelRef);
    }

    protected abstract void viewTaskExecs(T taskTO, AjaxRequestTarget target);

    protected abstract class TasksProvider<T extends TaskTO> extends DirectoryDataProvider<T> {

        private static final long serialVersionUID = -20112718133295756L;

        private final TaskType type;

        public TasksProvider(final int paginatorRows, final TaskType type) {
            super(paginatorRows);
            this.type = type;
        }

        @Override
        public long size() {
            return restClient.count(type);
        }

        @Override
        public IModel<T> model(final T object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof ExitEvent) {
            AjaxRequestTarget target = ExitEvent.class.cast(event.getPayload()).getTarget();
            baseModal.show(false);
            baseModal.close(target);
        }
    }
}
