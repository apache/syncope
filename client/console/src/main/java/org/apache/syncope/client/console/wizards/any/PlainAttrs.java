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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.commons.SchemaUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class PlainAttrs extends AbstractAttrs<PlainSchemaTO> {

    private static final long serialVersionUID = 552437609667518888L;

    private final Mode mode;

    public <T extends AnyTO> PlainAttrs(
            final T anyTO,
            final Form<?> form,
            final Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichPlainAttrs) {

        super(anyTO, anyTypeClasses, whichPlainAttrs);
        this.mode = mode;

        add(new ListView<AttrTO>("schemas", attrTOs) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                if (attrTOs.getObject().isEmpty()) {
                    response.render(OnDomReadyHeaderItem.forScript(
                            String.format("$('#emptyPlaceholder').append(\"%s\")", getString("attribute.empty.list"))));
                }
            }

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void populateItem(final ListItem<AttrTO> item) {
                AttrTO attrTO = item.getModelObject();

                FieldPanel panel = getFieldPanel(schemas.get(attrTO.getSchema()));
                if (mode == Mode.TEMPLATE || !schemas.get(attrTO.getSchema()).isMultivalue()) {
                    item.add(panel);
                    panel.setNewModel(attrTO.getValues());
                } else {
                    item.add(new MultiFieldPanel.Builder<>(
                            new PropertyModel<List<String>>(attrTO, "values")).build(
                            "panel",
                            attrTO.getSchema(),
                            panel));
                }
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.PLAIN;
    }

    @Override
    protected boolean reoderSchemas() {
        return super.reoderSchemas() && mode != Mode.TEMPLATE;
    }

    @Override
    protected Set<AttrTO> getAttrsFromAnyTO() {
        return anyTO.getPlainAttrs();
    }

    @Override
    protected void setAttrs() {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = anyTO.getPlainAttrMap();

        for (PlainSchemaTO schema : schemas.values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());

            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attrTO.getValues().add("");

                // is important to set readonly only after values setting
                attrTO.setReadonly(schema.isReadonly());
            } else {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            attrs.add(attrTO);
        }

        anyTO.getPlainAttrs().clear();
        anyTO.getPlainAttrs().addAll(attrs);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FieldPanel getFieldPanel(final PlainSchemaTO schemaTO) {
        boolean required = mode == Mode.TEMPLATE
                ? false
                : schemaTO.getMandatoryCondition().equalsIgnoreCase("true");

        boolean readOnly = mode == Mode.TEMPLATE ? false : schemaTO.isReadonly();

        AttrSchemaType type = mode == Mode.TEMPLATE ? AttrSchemaType.String : schemaTO.getType();

        FieldPanel panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel("panel", schemaTO.getKey(), new Model<Boolean>(), false);
                panel.setRequired(required);
                break;

            case Date:
                String dataPattern = schemaTO.getConversionPattern() == null
                        ? SyncopeConstants.DEFAULT_DATE_PATTERN
                        : schemaTO.getConversionPattern();

                if (dataPattern.contains("H")) {
                    panel = new DateTimeFieldPanel("panel", schemaTO.getKey(), new Model<Date>(), dataPattern);
                } else {
                    panel = new DateTextFieldPanel("panel", schemaTO.getKey(), new Model<Date>(), dataPattern);
                }

                if (required) {
                    panel.addRequiredLabel();
                }

                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<>("panel", schemaTO.getKey(), new Model<String>(), false);
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(SchemaUtils.getEnumeratedValues(schemaTO));

                if (StringUtils.isNotBlank(schemaTO.getEnumerationKeys())) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        private final Map<String, String> valueMap = SchemaUtils.getEnumeratedKeyValues(schemaTO);

                        @Override
                        public String getDisplayValue(final String value) {
                            return valueMap.get(value) == null ? value : valueMap.get(value);
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

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Long:
                panel = new AjaxSpinnerFieldPanel.Builder<Long>()
                        .build("panel", schemaTO.getKey(), Long.class, new Model<Long>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new AjaxSpinnerFieldPanel.Builder<Double>().step(0.1)
                        .build("panel", schemaTO.getKey(), Double.class, new Model<Double>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Binary:
                panel = new BinaryFieldPanel("panel", schemaTO.getKey(), new Model<String>(),
                        schemas.containsKey(schemaTO.getKey())
                        ? schemas.get(schemaTO.getKey()).getMimeType()
                        : null);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            default:
                panel = new AjaxTextFieldPanel("panel", schemaTO.getKey(), new Model<String>(), false);

                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);

        return panel;
    }
}
