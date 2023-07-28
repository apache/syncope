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

import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.Attr;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public abstract class AttrWizardBuilder extends BaseAjaxWizardBuilder<Attr> {

    private static final long serialVersionUID = 7618745848468848923L;

    public AttrWizardBuilder(final Attr defaultItem, final PageReference pageRef) {
        super(defaultItem, pageRef);
    }

    @Override
    protected WizardModel buildModelSteps(final Attr modelObject, final WizardModel wizardModel) {
        wizardModel.add(new AttrStep(modelObject));
        return wizardModel;
    }

    protected static class AttrStep extends WizardStep {

        private static final long serialVersionUID = 8145346883748040158L;

        AttrStep(final Attr modelObject) {
            AjaxTextFieldPanel schema = new AjaxTextFieldPanel(
                    Constants.KEY_FIELD_NAME, Constants.KEY_FIELD_NAME, new PropertyModel<>(modelObject, "schema"));
            schema.addRequiredLabel();
            schema.setEnabled(modelObject.getSchema() == null);
            add(schema);

            AjaxTextFieldPanel value = new AjaxTextFieldPanel("panel", "values", new Model<>());
            add(new MultiFieldPanel.Builder<String>(
                    new PropertyModel<>(modelObject, "values")).build("values", "values", value));
        }
    }
}
