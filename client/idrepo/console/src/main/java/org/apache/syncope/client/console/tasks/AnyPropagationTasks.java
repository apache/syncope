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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.PropagationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class AnyPropagationTasks extends AbstractPropagationTasks {

    private static final long serialVersionUID = -4013796607157549641L;

    public <T extends AnyTO> AnyPropagationTasks(
            final BaseModal<?> baseModal,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final PageReference pageRef) {

        super(BaseModal.CONTENT_ID);

        MultilevelPanel mlp = new MultilevelPanel("tasks");
        mlp.setFirstLevel(new AnyPropagationTaskDirectoryPanel(
                taskRestClient, baseModal, mlp, anyTypeKind, entityKey, pageRef) {

            private static final long serialVersionUID = -2195387360323687302L;

            @Override
            protected void viewTaskExecs(final PropagationTaskTO taskTO, final AjaxRequestTarget target) {
                mlp.next(
                        new StringResourceModel("task.view", this, new Model<>(Pair.of(null, taskTO))).getObject(),
                        new TaskExecutionDetails<>(taskTO, pageRef),
                        target);
            }

            @Override
            protected void viewTaskDetails(final PropagationTaskTO taskTO, final AjaxRequestTarget target) {
                mlp.next(
                        new StringResourceModel("task.view.details", this, new Model<>(Pair.of(null, taskTO))).
                                getObject(),
                        new PropagationDataView(taskTO),
                        target);
            }
        });
        add(mlp);
    }
}
