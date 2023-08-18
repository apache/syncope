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

import java.util.List;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.PropertyModel;

public class ParametersWizardSchemaStep extends WizardStep {

    private static final long serialVersionUID = -7843275202297616553L;

    public ParametersWizardSchemaStep(
            final AjaxWizard.Mode mode,
            final ParametersWizardPanel.ParametersForm modelObject) {

        setOutputMarkupId(true);

        WebMarkupContainer content = new WebMarkupContainer("content");
        add(content.setOutputMarkupId(true));

        AjaxDropDownChoicePanel<AttrSchemaType> type = new AjaxDropDownChoicePanel<>(
                "type", getString("type"), new PropertyModel<>(modelObject.getSchema(), "type"));
        type.setReadOnly(mode != AjaxWizard.Mode.CREATE);
        type.setChoices(List.of(
                AttrSchemaType.String,
                AttrSchemaType.Long,
                AttrSchemaType.Double,
                AttrSchemaType.Boolean,
                AttrSchemaType.Date,
                AttrSchemaType.Binary));
        content.add(type);

        content.add(new AjaxCheckBoxPanel("multivalue", getString("multivalue"),
                new PropertyModel<>(modelObject.getSchema(), "multivalue")));
    }
}
