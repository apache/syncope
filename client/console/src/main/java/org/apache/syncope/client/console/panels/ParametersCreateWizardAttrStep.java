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
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.console.commons.SchemaUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDateFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class ParametersCreateWizardAttrStep extends WizardStep {

    private static final long serialVersionUID = -7843275202297616553L;

    public ParametersCreateWizardAttrStep(final ParametersCreateWizardPanel.ParametersForm modelObject) {
        this.setOutputMarkupId(true);

        final WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        final AjaxTextFieldPanel schema = new AjaxTextFieldPanel(
                "schema", getString("schema"), new PropertyModel<String>(modelObject.getAttrTO(), "schema"));
        schema.setRequired(true);
        content.add(schema);

        final LoadableDetachableModel<List<PlainSchemaTO>> loadableDetachableModel =
                new LoadableDetachableModel<List<PlainSchemaTO>>() {

            private static final long serialVersionUID = 7172461137064525667L;

            @Override
            protected List<PlainSchemaTO> load() {
                return Arrays.asList(modelObject.getPlainSchemaTO());
            }

        };

        final ListView<PlainSchemaTO> listView = new ListView<PlainSchemaTO>("attrs", loadableDetachableModel) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<PlainSchemaTO> item) {
                final Panel panel = getFieldPanel("panel", modelObject.getAttrTO(), item.getModelObject());
                item.add(panel);
            }
        };

        content.add(listView);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Panel getFieldPanel(final String id, final AttrTO attrTO, final PlainSchemaTO plainSchemaTO) {

        final String valueHeaderName = getString("values");

        final FieldPanel panel;
        switch (plainSchemaTO.getType()) {
            case Date:
                final String dataPattern = plainSchemaTO.getConversionPattern() == null
                        ? SyncopeConstants.DEFAULT_DATE_PATTERN
                        : plainSchemaTO.getConversionPattern();

                if (dataPattern.contains("H")) {
                    panel = new AjaxDateTimeFieldPanel(
                            id, valueHeaderName, new Model<Date>(), dataPattern);
                } else {
                    panel = new AjaxDateFieldPanel(
                            "panel", valueHeaderName, new Model<Date>(), dataPattern);
                }
                break;
            case Boolean:
                panel = new AjaxDropDownChoicePanel<>(id, valueHeaderName, new Model<String>(), false);
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(Arrays.asList("true", "false"));

                if (!attrTO.getValues().isEmpty()) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        @Override
                        public String getDisplayValue(final String value) {
                            return value;
                        }

                        @Override
                        public String getIdValue(final String value, final int i) {
                            return value;
                        }

                        @Override
                        public String getObject(
                                final String id, final IModel<? extends List<? extends String>> choices) {
                            return id;
                        }
                    });
                }
                ((AjaxDropDownChoicePanel<String>) panel).setNullValid(false);
                break;
            case Enum:
                panel = new AjaxDropDownChoicePanel<>(id, valueHeaderName, new Model<String>(), false);
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(SchemaUtils.getEnumeratedValues(plainSchemaTO));

                if (!attrTO.getValues().isEmpty()) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        @Override
                        public String getDisplayValue(final String value) {
                            return value;
                        }

                        @Override
                        public String getIdValue(final String value, final int i) {
                            return value;
                        }

                        @Override
                        public String getObject(
                                final String id, final IModel<? extends List<? extends String>> choices) {
                            return id;
                        }
                    });
                }
                ((AjaxDropDownChoicePanel<String>) panel).setNullValid(
                        "true".equalsIgnoreCase(plainSchemaTO.getMandatoryCondition()));
                break;

            case Long:
                panel = new AjaxSpinnerFieldPanel.Builder<Long>()
                        .build(id, valueHeaderName, Long.class, new Model<Long>());
                break;

            case Double:
                panel = new AjaxSpinnerFieldPanel.Builder<Double>()
                        .build(id, valueHeaderName, Double.class, new Model<Double>());
                break;

            default:
                panel = new AjaxTextFieldPanel(id, valueHeaderName, new Model<String>(), false);
        }
        if (plainSchemaTO.isMultivalue()) {
            return new MultiFieldPanel.Builder<>(
                    new PropertyModel<List<String>>(attrTO, "values")).build(id, valueHeaderName, panel);
        } else {
            panel.setNewModel(attrTO.getValues());
        }

        panel.setRequired("true".equalsIgnoreCase(plainSchemaTO.getMandatoryCondition()));
        return panel;
    }
}
