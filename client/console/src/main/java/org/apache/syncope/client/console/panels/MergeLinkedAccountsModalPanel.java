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
package org.apache.syncope.client.console.panels;

import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksTogglePanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.MergeLinkedAccountsWizardBuilder;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class MergeLinkedAccountsModalPanel extends Panel implements ModalPanel {
    private static final long serialVersionUID = -4603032036433309900L;

    public MergeLinkedAccountsModalPanel(final BaseModal<?> baseModal,
                                         final IModel<UserTO> model,
                                         final PageReference pageRef) {
        super(BaseModal.getContentId(), model);
        final MultilevelPanel mlp = new MultilevelPanel("mlpContainer");
        mlp.setOutputMarkupId(true);

        ActionLinksTogglePanel<UserTO> actionTogglePanel = new ActionLinksTogglePanel<>("toggle", pageRef);
        add(actionTogglePanel);

        setOutputMarkupId(true);
        MergeLinkedAccountsWizardBuilder builder = new MergeLinkedAccountsWizardBuilder(model, pageRef);
        AjaxWizard<UserTO> wizard = builder.build(MultilevelPanel.FIRST_LEVEL_ID, AjaxWizard.Mode.CREATE);
        wizard.getButtonBar().setVisible(false);
        wizard.setOutputMarkupId(true);
        add(mlp.setFirstLevel(wizard));
    }
}
