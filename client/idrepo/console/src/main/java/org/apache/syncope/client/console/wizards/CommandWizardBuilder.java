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
package org.apache.syncope.client.console.wizards;

import java.io.Serializable;
import org.apache.syncope.client.console.panels.BeanPanel;
import org.apache.syncope.client.console.rest.CommandRestClient;
import org.apache.syncope.common.lib.command.CommandTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;

public class CommandWizardBuilder extends BaseAjaxWizardBuilder<CommandTO> {

    private static final long serialVersionUID = 5288806466136582164L;

    protected final CommandRestClient commandRestClient;

    public CommandWizardBuilder(
            final CommandTO defaultItem,
            final CommandRestClient commandRestClient,
            final PageReference pageRef) {

        super(defaultItem, pageRef);
        this.commandRestClient = commandRestClient;
    }

    @Override
    protected Serializable onApplyInternal(final CommandTO modelObject) {
        return commandRestClient.run(modelObject).getOutput();
    }

    @Override
    protected WizardModel buildModelSteps(final CommandTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new CommandArgs(modelObject));
        return wizardModel;
    }

    public class CommandArgs extends WizardStep {

        private static final long serialVersionUID = -785981096328637758L;

        public CommandArgs(final CommandTO command) {
            LoadableDetachableModel<Serializable> bean = new LoadableDetachableModel<>() {

                private static final long serialVersionUID = -1096114645494621802L;

                @Override
                protected Serializable load() {
                    return command.getArgs();
                }
            };
            add(new BeanPanel<>("bean", bean, pageRef).setRenderBodyOnly(true));
        }
    }
}
