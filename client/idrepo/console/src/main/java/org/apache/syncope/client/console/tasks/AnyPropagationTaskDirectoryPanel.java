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

import java.util.Iterator;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;

public abstract class AnyPropagationTaskDirectoryPanel extends PropagationTaskDirectoryPanel {

    private static final long serialVersionUID = -6784307338127527803L;

    private final AnyTypeKind anyTypeKind;

    private final String entityKey;

    protected AnyPropagationTaskDirectoryPanel(
            final TaskRestClient restClient,
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final PageReference pageRef) {

        super(restClient, baseModal, multiLevelPanelRef, null, pageRef);
        this.anyTypeKind = anyTypeKind;
        this.entityKey = entityKey;
    }

    @Override
    protected PropagationTasksProvider dataProvider() {
        return new AnyPropagationTasksProvider(rows);
    }

    protected class AnyPropagationTasksProvider extends PropagationTasksProvider {

        private static final long serialVersionUID = 8975514657807398110L;

        public AnyPropagationTasksProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public long size() {
            return restClient.count(anyTypeKind, entityKey, taskType);
        }

        @Override
        public Iterator<PropagationTaskTO> iterator(final long first, final long count) {
            int page = ((int) first / paginatorRows);
            return restClient.listPropagationTasks(
                    anyTypeKind, entityKey, (page < 0 ? 0 : page) + 1, paginatorRows, getSort()).
                    iterator();
        }
    }
}
