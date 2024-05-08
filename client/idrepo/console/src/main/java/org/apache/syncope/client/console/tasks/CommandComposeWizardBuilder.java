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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.CommandRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.MacroTaskTO;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

public class CommandComposeWizardBuilder extends BaseAjaxWizardBuilder<CommandWrapper> {

    private static final long serialVersionUID = -2300926041782845851L;

    protected final LoadableDetachableModel<List<ImplementationTO>> commands = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 4659376149825914247L;

        @Override
        protected List<ImplementationTO> load() {
            return implementationRestClient.list(IdRepoImplementationType.COMMAND);
        }
    };

    protected final String task;

    protected final ImplementationRestClient implementationRestClient;

    protected final TaskRestClient taskRestClient;

    protected final CommandRestClient commandRestClient;

    public CommandComposeWizardBuilder(
            final String task,
            final CommandWrapper defaultItem,
            final ImplementationRestClient implementationRestClient,
            final TaskRestClient taskRestClient,
            final CommandRestClient commandRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);

        this.task = task;
        this.implementationRestClient = implementationRestClient;
        this.taskRestClient = taskRestClient;
        this.commandRestClient = commandRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final CommandWrapper modelObject) {
        if (modelObject.getCommand().getArgs() == null) {
            throw new IllegalArgumentException("Incorrect Command definition");
        }

        MacroTaskTO taskTO = taskRestClient.readTask(TaskType.MACRO, task);
        if (modelObject.isNew()) {
            taskTO.getCommands().add(modelObject.getCommand());
        } else {
            taskTO.getCommands().stream().
                    filter(cmd -> cmd.getKey().equals(modelObject.getCommand().getKey())).
                    findFirst().
                    ifPresent(cmd -> cmd.setArgs(modelObject.getCommand().getArgs()));
        }

        taskRestClient.update(TaskType.MACRO, taskTO);
        return modelObject;
    }

    @Override
    protected WizardModel buildModelSteps(final CommandWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new CommandArgs(modelObject));
        return wizardModel;
    }

    public class Profile extends WizardStep {

        private static final long serialVersionUID = -3043839139187792810L;

        private final CommandWrapper command;

        public Profile(final CommandWrapper command) {
            this.command = command;

            AutoCompleteSettings settings = new AutoCompleteSettings();
            settings.setShowCompleteListOnFocusGain(false);
            settings.setShowListOnEmptyInput(false);

            AutoCompleteTextField<String> args = new AutoCompleteTextField<>(
                    "command", new PropertyModel<>(command, "command.key"), settings) {

                private static final long serialVersionUID = -6556576139048844857L;

                @Override
                protected Iterator<String> getChoices(final String input) {
                    return commands.getObject().stream().
                            map(ImplementationTO::getKey).
                            filter(cmd -> cmd.contains(input)).
                            sorted().iterator();
                }
            };
            args.setRequired(true);
            args.setEnabled(command.isNew());
            args.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -6139318907146065915L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    try {
                        CommandTO cmd = commandRestClient.read(command.getCommand().getKey());
                        command.getCommand().setArgs(cmd.getArgs());
                    } catch (Exception e) {
                        LOG.error("While attempting to read Command {}", command.getCommand().getKey(), e);
                    }
                }
            });
            add(args);
        }

        @Override
        public void applyState() {
            commands.getObject().stream().
                    filter(cmd -> cmd.getKey().equals(command.getCommand().getKey())
                    && cmd.getEngine() == ImplementationEngine.GROOVY).
                    findFirst().ifPresent(cmd -> getWizardModel().finish());
        }
    }

    public class CommandArgs extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public CommandArgs(final CommandWrapper command) {
            LoadableDetachableModel<Serializable> bean = new LoadableDetachableModel<>() {

                private static final long serialVersionUID = 2092144708018739371L;

                @Override
                protected Serializable load() {
                    return command.getCommand().getArgs();
                }
            };
            add(new BeanPanel<>("bean", bean, pageRef).setRenderBodyOnly(true));
        }
    }
}
