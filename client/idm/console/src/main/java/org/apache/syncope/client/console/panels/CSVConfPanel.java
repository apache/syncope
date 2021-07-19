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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCharacterFieldPanel;
import org.apache.syncope.client.console.wizards.CSVPullWizardBuilder;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.rest.api.beans.AbstractCSVSpec;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.StringValidator;

public class CSVConfPanel extends Panel {

    private static final long serialVersionUID = -1562409114958459620L;

    public CSVConfPanel(final String id, final AbstractCSVSpec spec) {
        super(id);

        AjaxCharacterFieldPanel columnSeparator = new AjaxCharacterFieldPanel(
                "columnSeparator", "columnSeparator", new PropertyModel<>(spec, "columnSeparator"));
        columnSeparator.setChoices(List.of(',', ';', ' '));
        columnSeparator.setRequired(true);
        add(columnSeparator);

        AjaxTextFieldPanel arrayElementSeparator = new AjaxTextFieldPanel(
                "arrayElementSeparator", "arrayElementSeparator",
                new PropertyModel<>(spec, "arrayElementSeparator"));
        arrayElementSeparator.setChoices(List.of(";"));
        arrayElementSeparator.setRequired(true);
        arrayElementSeparator.addValidator(new StringValidator(1, 1));
        add(arrayElementSeparator);

        AjaxCharacterFieldPanel quoteChar = new AjaxCharacterFieldPanel(
                "quoteChar", "quoteChar", new PropertyModel<>(spec, "quoteChar"));
        quoteChar.setChoices(List.of('"'));
        quoteChar.setRequired(true);
        add(quoteChar);

        AjaxCharacterFieldPanel escapeChar = new AjaxCharacterFieldPanel(
                "escapeChar", "escapeChar", new PropertyModel<>(spec, "escapeChar"));
        escapeChar.setRequired(false);
        add(escapeChar);

        AjaxDropDownChoicePanel<String> lineSeparator = new AjaxDropDownChoicePanel<>(
                "lineSeparator", "lineSeparator", new PropertyModel<>(spec, "lineSeparator"), false);
        lineSeparator.setChoices(Stream.of(CSVPullWizardBuilder.LineSeparator.values()).
                map(CSVPullWizardBuilder.LineSeparator::name).collect(Collectors.toList()));
        lineSeparator.setChoiceRenderer(new IChoiceRenderer<>() {

            private static final long serialVersionUID = 8551710814349123350L;

            @Override
            public Object getDisplayValue(final String object) {
                return CSVPullWizardBuilder.LineSeparator.valueOf(object).name();
            }

            @Override
            public String getIdValue(final String object, final int index) {
                return object;
            }

            @Override
            public String getObject(
                final String id, final IModel<? extends List<? extends String>> choices) {

                return CSVPullWizardBuilder.LineSeparator.valueOf(id).getRepr();
            }
        });
        lineSeparator.setRequired(true);
        add(lineSeparator);

        AjaxTextFieldPanel nullValue = new AjaxTextFieldPanel(
                "nullValue", "nullValue", new PropertyModel<>(spec, "nullValue"));
        nullValue.setRequired(false);
        add(nullValue);

        AjaxCheckBoxPanel allowComments = new AjaxCheckBoxPanel(
                "allowComments", "allowComments", new PropertyModel<>(spec, "allowComments"), true);
        add(allowComments);
    }
}
