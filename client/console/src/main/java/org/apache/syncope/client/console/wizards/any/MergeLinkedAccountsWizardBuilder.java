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
package org.apache.syncope.client.console.wizards.any;

import org.apache.syncope.client.console.panels.MergeLinkAccountsSearchPanel;
import org.apache.syncope.client.console.wizards.AjaxWizardBuilder;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.model.IModel;

public class MergeLinkedAccountsWizardBuilder extends AjaxWizardBuilder<UserTO> {
    private static final long serialVersionUID = -9142332740863374891L;

    private final IModel<UserTO> model;

    public MergeLinkedAccountsWizardBuilder(final IModel<UserTO> model, final PageReference pageRef) {
        super(new UserTO(), pageRef);
        this.model = model;
    }

    @Override
    protected WizardModel buildModelSteps(final UserTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new MergeLinkAccountsSearchPanel(modelObject, getPageReference()));
        return wizardModel;
    }
}
