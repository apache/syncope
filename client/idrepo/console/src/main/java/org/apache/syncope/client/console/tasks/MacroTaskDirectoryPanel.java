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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.CommandRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.Action;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class MacroTaskDirectoryPanel extends SchedTaskDirectoryPanel<MacroTaskTO> {

    private static final long serialVersionUID = -6247673131495530094L;

    protected class ExecModalEventSink implements IEventSink, Serializable {

        private static final long serialVersionUID = -5961049309874978659L;

        @Override
        public void onEvent(final IEvent<?> event) {
            if (event.getPayload() instanceof AjaxWizard.NewItemCancelEvent
                    || event.getPayload() instanceof AjaxWizard.NewItemFinishEvent) {

                AjaxWizard.NewItemEvent<?> nie = AjaxWizard.NewItemEvent.class.cast(event.getPayload());
                nie.getTarget().ifPresent(execModal::close);
            }

            MacroTaskDirectoryPanel.this.onEvent(event);
        }
    }

    @SpringBean
    protected CommandRestClient commandRestClient;

    protected final BaseModal<MacroTaskTO> formPropertyDefModal = new BaseModal<>(Constants.OUTER);

    protected final BaseModal<MacroTaskTO> execModal;

    public MacroTaskDirectoryPanel(
            final TaskRestClient restClient,
            final MultilevelPanel mlp,
            final PageReference pageRef) {

        super(MultilevelPanel.FIRST_LEVEL_ID, restClient, null, mlp, TaskType.MACRO, new MacroTaskTO(), pageRef, true);

        formPropertyDefModal.size(Modal.Size.Extra_large);
        formPropertyDefModal.addSubmitButton();
        setWindowClosedReloadCallback(formPropertyDefModal);
        addOuterObject(formPropertyDefModal);

        execModal = new BaseModal<>(Constants.OUTER) {

            private static final long serialVersionUID = 389935548143327858L;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setFooterVisible(false);
            }
        };
        execModal.size(Modal.Size.Large);
        setWindowClosedReloadCallback(execModal);
        addOuterObject(execModal);
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
    public ActionsPanel<MacroTaskTO> getActions(final IModel<MacroTaskTO> model) {
        ActionsPanel<MacroTaskTO> panel = super.getActions(model);

        panel.getActions().removeIf(action -> action.getType() == ActionLink.ActionType.EXECUTE);

        Action<MacroTaskTO> execute = new Action<>(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MacroTaskTO ignore) {
                model.setObject(restClient.readTask(TaskType.MACRO, model.getObject().getKey()));
                MacroTaskExecWizardBuilder wb = new MacroTaskExecWizardBuilder(model.getObject(), restClient, pageRef);
                wb.setEventSink(new ExecModalEventSink());

                target.add(execModal.setContent(wb.build(BaseModal.CONTENT_ID, AjaxWizard.Mode.EDIT)));

                execModal.header(new StringResourceModel(
                        "exec", MacroTaskDirectoryPanel.this, Model.of(model.getObject())));
                execModal.show(true);
            }
        }, ActionLink.ActionType.EXECUTE);
        execute.setEntitlements(IdRepoEntitlement.TASK_EXECUTE);
        execute.setOnConfirm(false);

        panel.add(panel.getActions().size() - 1, execute);

        return panel;
    }

    @Override
    protected void addFurtherActions(final ActionsPanel<MacroTaskTO> panel, final IModel<MacroTaskTO> model) {
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MacroTaskTO ignore) {
                model.setObject(restClient.readTask(TaskType.MACRO, model.getObject().getKey()));
                target.add(modal.setContent(new CommandComposeDirectoryPanel(
                        model.getObject().getKey(), commandRestClient, modal, pageRef)));

                modal.header(new StringResourceModel(
                        "command.conf", MacroTaskDirectoryPanel.this, Model.of(model.getObject())));
                modal.show(true);
            }
        }, ActionLink.ActionType.COMPOSE, IdRepoEntitlement.TASK_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final MacroTaskTO ignore) {
                model.setObject(restClient.readTask(TaskType.MACRO, model.getObject().getKey()));
                target.add(formPropertyDefModal.setContent(
                        new FormPropertyDefsPanel(model.getObject(), formPropertyDefModal, pageRef)));

                formPropertyDefModal.header(new StringResourceModel(
                        "form.def", MacroTaskDirectoryPanel.this, Model.of(model.getObject())));
                formPropertyDefModal.show(true);
            }
        }, ActionLink.ActionType.MAPPING, IdRepoEntitlement.TASK_UPDATE);
    }
}
