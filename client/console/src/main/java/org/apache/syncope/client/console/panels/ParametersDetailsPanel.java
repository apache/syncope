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
import org.apache.syncope.client.console.rest.SchemaRestClient;
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
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class ParametersDetailsPanel extends Panel {

    private static final long serialVersionUID = 7708288006191496557L;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    public ParametersDetailsPanel(final String id, final AttrTO attrTO) {
        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("container");

        container.setOutputMarkupId(true);
        add(container);

        final Form<AttrTO> form = new Form<>("parametersForm");
        form.setMarkupId("parametersForm");
        form.setOutputMarkupId(true);

        form.setModel(new CompoundPropertyModel<>(attrTO));
        container.add(form);

        final AjaxTextFieldPanel schema = new AjaxTextFieldPanel(
                "schema", getString("schema"), new PropertyModel<String>(attrTO, "schema"));
        schema.setEnabled(false);
        form.add(schema);

        form.add(getFieldPanel("panel", attrTO));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Panel getFieldPanel(final String id, final AttrTO attrTO) {
        final String valueHeaderName = getString("values");

        final PlainSchemaTO schemaTO = schemaRestClient.read(SchemaType.PLAIN, attrTO.getSchema());

        final FieldPanel panel;
        switch (schemaTO.getType()) {
            case Date:
                final String dataPattern = schemaTO.getConversionPattern() == null
                        ? SyncopeConstants.DEFAULT_DATE_PATTERN
                        : schemaTO.getConversionPattern();

                if (dataPattern.contains("H")) {
                    panel = new AjaxDateTimeFieldPanel("panel", schemaTO.getKey(), new Model<Date>(), dataPattern);
                } else {
                    panel = new AjaxDateFieldPanel("panel", schemaTO.getKey(), new Model<Date>(), dataPattern);
                }
                break;
            case Boolean:
                panel = new AjaxDropDownChoicePanel<>(id, valueHeaderName, new Model<>(), false);
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
                panel = new AjaxDropDownChoicePanel<>(id, valueHeaderName, new Model<>(), false);
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(SchemaUtils.getEnumeratedValues(schemaTO));

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
                        "false".equalsIgnoreCase(schemaTO.getMandatoryCondition()));
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
        if (schemaTO.isMultivalue()) {
            return new MultiFieldPanel.Builder<>(
                    new PropertyModel<List<String>>(attrTO, "values")).build(id, valueHeaderName, panel);
        } else {
            panel.setNewModel(attrTO.getValues());
        }

        panel.setRequired("true".equalsIgnoreCase(schemaTO.getMandatoryCondition()));
        return panel;
    }
}
