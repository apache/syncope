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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.panels.DirectoryPanel;
import org.apache.syncope.client.console.rest.CommandRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class CommandComposeDirectoryPanel extends DirectoryPanel<
        CommandWrapper, CommandWrapper, DirectoryDataProvider<CommandWrapper>, CommandRestClient>
        implements ModalPanel {

    private static final long serialVersionUID = 8899580817658145305L;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    @SpringBean
    protected TaskRestClient taskRestClient;

    @SpringBean
    protected CommandRestClient commandRestClient;

    protected final String task;

    protected final BaseModal<MacroTaskTO> baseModal;

    public CommandComposeDirectoryPanel(
            final String task,
            final CommandRestClient restClient,
            final BaseModal<MacroTaskTO> baseModal,
            final PageReference pageRef) {

        super(BaseModal.CONTENT_ID, restClient, pageRef, false);

        disableCheckBoxes();

        this.task = task;
        this.baseModal = baseModal;

        enableUtilityButton();

        addNewItemPanelBuilder(new CommandComposeWizardBuilder(
                task,
                new CommandWrapper(true),
                implementationRestClient,
                taskRestClient,
                commandRestClient,
                pageRef), true);

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.TASK_UPDATE);
        initResultTable();
    }

    @Override
    protected List<IColumn<CommandWrapper, String>> getColumns() {
        List<IColumn<CommandWrapper, String>> columns = new ArrayList<>();

        columns.add(new AbstractColumn<>(new ResourceModel(Constants.KEY_FIELD_NAME), Constants.KEY_FIELD_NAME) {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<CommandWrapper>> cellItem,
                    final String componentId,
                    final IModel<CommandWrapper> rowModel) {

                cellItem.add(new Label(componentId, rowModel.getObject().getCommand().getKey()));
            }
        });

        columns.add(new AbstractColumn<>(new ResourceModel("arguments"), "arguments") {

            private static final long serialVersionUID = -4008579357070833846L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<CommandWrapper>> cellItem,
                    final String componentId,
                    final IModel<CommandWrapper> rowModel) {

                cellItem.add(new Label(componentId, rowModel.getObject().getCommand().getArgs().getClass().getName()));
            }
        });

        return columns;
    }

    @Override
    public ActionsPanel<CommandWrapper> getActions(final IModel<CommandWrapper> model) {
        ActionsPanel<CommandWrapper> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final CommandWrapper ignore) {
                CommandComposeDirectoryPanel.this.getTogglePanel().close(target);
                send(CommandComposeDirectoryPanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.TASK_UPDATE);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final CommandWrapper ignore) {
                try {
                    MacroTaskTO actual = taskRestClient.readTask(TaskType.MACRO, task);

                    // cannot rely on actual.getCommands().remove(model.getObject().getCommand())
                    // since CommandArgs instances could not be implementing equals() / hashCode()
                    Integer idx = null;
                    for (int i = 0; i < actual.getCommands().size() && idx == null; i++) {
                        CommandTO actualCmd = actual.getCommands().get(i);
                        try {
                            if (actualCmd.getKey().equals(model.getObject().getCommand().getKey())
                                    && MAPPER.writeValueAsString(actualCmd.getArgs()).equals(
                                            MAPPER.writeValueAsString(model.getObject().getCommand().getArgs()))) {

                                idx = i;
                            }
                        } catch (JsonProcessingException e) {
                            LOG.error("While comparing command arguments", e);
                        }
                    }
                    if (idx == null) {
                        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_NO_OP));
                    } else {
                        actual.getCommands().remove(idx.intValue());
                        taskRestClient.update(TaskType.MACRO, actual);
                        SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    }

                    customActionOnFinishCallback(target);
                } catch (SyncopeClientException e) {
                    LOG.error("While deleting {}", model.getObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.TASK_UPDATE, true);

        return panel;
    }

    @Override
    public ActionsPanel<Serializable> getHeader(final String componentId) {
        ActionsPanel<Serializable> panel = new ActionsPanel<>(componentId, null);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                if (target != null) {
                    customActionOnFinishCallback(target);
                }
            }
        }, ActionLink.ActionType.RELOAD, IdRepoEntitlement.TASK_READ).hideLabel();
        return panel;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected CommandComposeDataProvider dataProvider() {
        return new CommandComposeDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_COMMAND_PAGINATOR_ROWS;
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

    protected class CommandComposeDataProvider extends DirectoryDataProvider<CommandWrapper> {

        private static final long serialVersionUID = 4725679400450513556L;

        public CommandComposeDataProvider(final int paginatorRows) {
            super(paginatorRows);
        }

        @Override
        public Iterator<CommandWrapper> iterator(final long first, final long count) {
            MacroTaskTO actual = taskRestClient.readTask(TaskType.MACRO, task);
            return actual.getCommands().subList((int) first, (int) (first + count)).stream().
                    map(command -> new CommandWrapper(false).setCommand(command)).
                    iterator();
        }

        @Override
        public long size() {
            MacroTaskTO actual = taskRestClient.readTask(TaskType.MACRO, task);
            return actual.getCommands().size();
        }

        @Override
        public IModel<CommandWrapper> model(final CommandWrapper object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
