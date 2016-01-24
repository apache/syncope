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

import java.util.Arrays;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;

public class ParametersCreateWizardSchemaStep extends WizardStep {

    private static final long serialVersionUID = -7843275202297616553L;

    public ParametersCreateWizardSchemaStep(final ParametersCreateWizardPanel.ParametersForm modelObject) {
        final WebMarkupContainer content = new WebMarkupContainer("content");
        this.setOutputMarkupId(true);
        content.setOutputMarkupId(true);
        add(content);
        final AjaxDropDownChoicePanel<AttrSchemaType> type = new AjaxDropDownChoicePanel<>(
                "type", getString("type"), new PropertyModel<AttrSchemaType>(modelObject.getPlainSchemaTO(), "type"));
        type.setChoices(Arrays.asList(AttrSchemaType.values()));
        content.add(type);

        final AjaxTextFieldPanel mandatoryCondition = new AjaxTextFieldPanel(
                "mandatoryCondition", getString("mandatoryCondition"),
                new PropertyModel<String>(modelObject.getPlainSchemaTO(), "mandatoryCondition"));
        content.add(mandatoryCondition);

        final AjaxCheckBoxPanel multiValue = new AjaxCheckBoxPanel("panel", getString("multivalue"),
                new PropertyModel<Boolean>(modelObject.getPlainSchemaTO(), "multivalue"), false);
        content.add(multiValue);
    }

}
