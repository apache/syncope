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

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.PushTaskTO;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;

/**
 * Push Tasks page.
 */
public abstract class PushTaskDirectoryPanel extends ProvisioningTaskDirectoryPanel<PushTaskTO> {

    private static final long serialVersionUID = 4984337552918213290L;

    protected PushTaskDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String resource,
            final PageReference pageRef) {
        super(baseModal, multiLevelPanelRef, PushTaskTO.class, resource, pageRef);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_PUSH_TASKS_PAGINATOR_ROWS;
    }

    @Override
    protected ProvisioningTasksProvider<PushTaskTO> dataProvider() {
        return new ProvisioningTasksProvider<>(reference, TaskType.PUSH, rows);
    }
}
