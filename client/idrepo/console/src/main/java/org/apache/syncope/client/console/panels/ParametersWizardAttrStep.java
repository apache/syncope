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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
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

public class ParametersWizardAttrStep extends WizardStep {

    private static final long serialVersionUID = -7843275202297616553L;

    private final AjaxTextFieldPanel schema;

    public ParametersWizardAttrStep(
            final AjaxWizard.Mode mode,
            final ParametersWizardPanel.ParametersForm modelObject) {

        this.setOutputMarkupId(true);

        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        add(content);

        schema = new AjaxTextFieldPanel(
                "schema", getString("schema"), new PropertyModel<>(modelObject.getParam(), "schema"));
        schema.setRequired(true);
        schema.setReadOnly(mode != AjaxWizard.Mode.CREATE);
        content.add(schema);

        LoadableDetachableModel<List<PlainSchemaTO>> schemas = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 7172461137064525667L;

            @Override
            protected List<PlainSchemaTO> load() {
                return List.of(modelObject.getSchema());
            }
        };

        ListView<PlainSchemaTO> attrs = new ListView<>("attrs", schemas) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<PlainSchemaTO> item) {
                item.add(getFieldPanel("panel", modelObject.getParam(), item.getModelObject()));
            }
        };
        content.add(attrs);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Panel getFieldPanel(final String id, final ConfParam param, final PlainSchemaTO plainSchemaTO) {
        String valueHeaderName = getString("values");

        final FieldPanel panel;
        switch (plainSchemaTO.getType()) {
            case Date:
                panel = new AjaxDateTimeFieldPanel(
                        id, valueHeaderName, new Model<>(),
                        DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
                break;

            case Boolean:
                panel = new AjaxDropDownChoicePanel<Boolean>(id, valueHeaderName, new Model<>(), false);
                ((AjaxDropDownChoicePanel<Boolean>) panel).setChoices(List.of(true, false));

                if (!param.getValues().isEmpty()) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<Boolean>() {

                        private static final long serialVersionUID = -8223314361351275865L;

                        @Override
                        public Object getDisplayValue(final Boolean object) {
                            return BooleanUtils.toStringTrueFalse(object);
                        }

                        @Override
                        public String getIdValue(final Boolean object, final int index) {
                            return BooleanUtils.toStringTrueFalse(object);
                        }

                        @Override
                        public Boolean getObject(
                                final String id, final IModel<? extends List<? extends Boolean>> choices) {

                            return BooleanUtils.toBoolean(id);
                        }
                    });
                }
                ((AjaxDropDownChoicePanel<Boolean>) panel).setNullValid(false);
                break;

            case Long:
                panel = new AjaxNumberFieldPanel.Builder<Long>().
                        convertValuesToString(false).
                        build(id, valueHeaderName, Long.class, new Model<>());
                break;

            case Double:
                panel = new AjaxNumberFieldPanel.Builder<Double>().
                        convertValuesToString(false).
                        build(id, valueHeaderName, Double.class, new Model<>());
                break;

            case Binary:
                panel = new BinaryFieldPanel(id, valueHeaderName, new Model<>(),
                        plainSchemaTO.getMimeType(), schema.getModelObject());
                break;

            default:
                panel = new AjaxTextFieldPanel(id, valueHeaderName, new Model<>(), false);
        }

        if (plainSchemaTO.isMultivalue()) {
            return new MultiFieldPanel.Builder<>(
                    new PropertyModel<>(param, "values")).build(id, valueHeaderName, panel);
        }

        panel.setNewModel(param.getValues());
        return panel;
    }
}
