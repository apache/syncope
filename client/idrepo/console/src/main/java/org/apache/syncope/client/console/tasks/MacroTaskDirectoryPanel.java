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
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

public class MacroTaskDirectoryPanel extends SchedTaskDirectoryPanel<MacroTaskTO> {

    private static final long serialVersionUID = -6247673131495530094L;

    public MacroTaskDirectoryPanel(final MultilevelPanel mlp, final PageReference pageRef) {
        super(MultilevelPanel.FIRST_LEVEL_ID, null, mlp, TaskType.MACRO, new MacroTaskTO(), pageRef, true);
    }

    @Override
    protected List<IColumn<MacroTaskTO, String>> getFieldColumns() {
        List<IColumn<MacroTaskTO, String>> columns = new ArrayList<>();

        columns.addAll(getHeadingFieldColumns());

        columns.add(new BooleanPropertyColumn<>(
                new ResourceModel("continueOnError"), "continueOnError", "continueOnError"));
        columns.add(new BooleanPropertyColumn<>(
                new ResourceModel("saveExecs"), "saveExecs", "saveExecs"));

        columns.addAll(getTrailingFieldColumns());

        return columns;
    }

    @Override
    protected void viewTaskExecs(final MacroTaskTO taskTO, final AjaxRequestTarget target) {
        multiLevelPanelRef.next(
                new StringResourceModel("task.view", this, new Model<>(Pair.of(null, taskTO))).getObject(),
                new TaskExecutionDetails<>(taskTO, pageRef),
                target);
    }

    @Override
    protected void addFurtherActions(final ActionsPanel<MacroTaskTO> panel, final IModel<MacroTaskTO> model) {
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MacroTaskTO ignore) {
                target.add(modal.setContent(
                        new CommandComposeDirectoryPanel(modal, model.getObject().getKey(), pageRef)));

                modal.header(new StringResourceModel(
                        "command.conf", MacroTaskDirectoryPanel.this, Model.of(model.getObject())));
                modal.show(true);
            }
        }, ActionLink.ActionType.COMPOSE, IdRepoEntitlement.TASK_UPDATE);
    }
}
