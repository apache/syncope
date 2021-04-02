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

import java.io.Serializable;
import org.apache.syncope.client.console.rest.ApplicationRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.JsonEditorPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.common.lib.to.ApplicationTO;
import org.apache.syncope.common.lib.to.PrivilegeTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

public class PrivilegeWizardBuilder extends BaseAjaxWizardBuilder<PrivilegeTO> {

    private static final long serialVersionUID = -1817419622749405208L;

    private final ApplicationTO application;

    public PrivilegeWizardBuilder(
            final ApplicationTO application, final PrivilegeTO privilege, final PageReference pageRef) {

        super(privilege, pageRef);
        this.application = application;
    }

    @Override
    protected WizardModel buildModelSteps(final PrivilegeTO modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Profile(modelObject));
        wizardModel.add(new Spec(modelObject));
        return wizardModel;
    }

    @Override
    protected Serializable onApplyInternal(final PrivilegeTO modelObject) {
        application.getPrivileges().removeIf(privilege -> privilege.getKey().equals(modelObject.getKey()));
        application.getPrivileges().add(modelObject);

        ApplicationRestClient.update(application);

        return modelObject;
    }

    public static class Profile extends WizardStep {

        private static final long serialVersionUID = 11881843064077955L;

        public Profile(final PrivilegeTO privilege) {
            setTitleModel(privilege.getKey() == null
                    ? new StringResourceModel("privilege.new")
                    : new StringResourceModel("privilege.edit", Model.of(privilege)));

            AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME,
                    new PropertyModel<>(privilege, Constants.KEY_FIELD_NAME), false);
            key.setReadOnly(privilege.getKey() != null);
            key.setRequired(true);
            add(key);

            AjaxTextFieldPanel description = new AjaxTextFieldPanel(
                    Constants.DESCRIPTION_FIELD_NAME, Constants.DESCRIPTION_FIELD_NAME,
                    new PropertyModel<>(privilege, Constants.DESCRIPTION_FIELD_NAME), false);
            description.setRequired(false);
            add(description);
        }
    }

    public static class Spec extends WizardStep {

        private static final long serialVersionUID = -3237113253888332643L;

        public Spec(final PrivilegeTO privilege) {
            setTitleModel(privilege.getKey() == null
                    ? new StringResourceModel("privilege.new")
                    : new StringResourceModel("privilege.edit", Model.of(privilege)));

            add(new JsonEditorPanel(new PropertyModel<>(privilege, "spec")));
        }
    }
}
