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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
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
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
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
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class PlainAttrs extends AbstractAttrs {

    private static final long serialVersionUID = 552437609667518888L;

    private final Mode mode;

    private Map<String, PlainSchemaTO> schemas = new LinkedHashMap<>();

    public <T extends AnyTO> PlainAttrs(
            final T entityTO, final Form<?> form, final Mode mode, final String... anyTypeClass) {
        super(entityTO);
        this.setOutputMarkupId(true);

        this.mode = mode;

        final LoadableDetachableModel<List<AttrTO>> plainAttrTOs = new LoadableDetachableModel<List<AttrTO>>() {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<AttrTO> load() {
                setPlainSchemas(CollectionUtils.collect(anyTypeClassRestClient.list(getAllAuxClasses()),
                        EntityTOUtils.<AnyTypeClassTO>keyTransformer(), new ArrayList<>(Arrays.asList(anyTypeClass))));
                setAttrs();
                return new ArrayList<>(entityTO.getPlainAttrs());
            }
        };

        add(new ListView<AttrTO>("schemas", plainAttrTOs) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            public void renderHead(final IHeaderResponse response) {
                super.renderHead(response);
                if (plainAttrTOs.getObject().isEmpty()) {
                    response.render(OnDomReadyHeaderItem.forScript(
                            String.format("$('#emptyPlaceholder').append(\"%s\")", getString("attribute.empty.list"))));
                }
            }

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void populateItem(final ListItem<AttrTO> item) {
                final AttrTO attributeTO = (AttrTO) item.getDefaultModelObject();

                final FieldPanel panel = getFieldPanel(schemas.get(attributeTO.getSchema()));

                if (mode == Mode.TEMPLATE || !schemas.get(attributeTO.getSchema()).isMultivalue()) {
                    item.add(panel);
                    panel.setNewModel(attributeTO.getValues());
                } else {
                    item.add(new MultiFieldPanel.Builder<>(
                            new PropertyModel<List<String>>(attributeTO, "values")).build(
                            "panel",
                            attributeTO.getSchema(),
                            panel));
                }
            }
        });
    }

    private void setPlainSchemas(final List<String> anyTypeClasses) {
        List<PlainSchemaTO> plainSchemas = Collections.emptyList();
        if (!anyTypeClasses.isEmpty()) {
            plainSchemas = schemaRestClient.getSchemas(SchemaType.PLAIN, anyTypeClasses.toArray(new String[] {}));
        }

        schemas.clear();

        // SYNCOPE-790
        AttrTO attrLayout = null;
        if (attrLayout != null && mode != Mode.TEMPLATE) {
            // 1. remove attributes not selected for display
            schemaRestClient.filter(plainSchemas, attrLayout.getValues(), true);
            // 2. sort remainig attributes according to configuration, e.g. attrLayout
            final Map<String, Integer> attrLayoutMap = new HashMap<>(attrLayout.getValues().size());
            for (int i = 0; i < attrLayout.getValues().size(); i++) {
                attrLayoutMap.put(attrLayout.getValues().get(i), i);
            }
            Collections.sort(plainSchemas, new Comparator<PlainSchemaTO>() {

                @Override
                public int compare(final PlainSchemaTO schema1, final PlainSchemaTO schema2) {
                    int value = 0;

                    if (attrLayoutMap.get(schema1.getKey()) > attrLayoutMap.get(schema2.getKey())) {
                        value = 1;
                    } else if (attrLayoutMap.get(schema1.getKey()) < attrLayoutMap.get(schema2.getKey())) {
                        value = -1;
                    }

                    return value;
                }
            });
        }
        for (PlainSchemaTO schemaTO : plainSchemas) {
            schemas.put(schemaTO.getKey(), schemaTO);
        }
    }

    private void setAttrs() {
        final List<AttrTO> entityData = new ArrayList<>();

        final Map<String, AttrTO> attrMap = entityTO.getPlainAttrMap();

        for (PlainSchemaTO schema : schemas.values()) {
            final AttrTO attributeTO = new AttrTO();
            attributeTO.setSchema(schema.getKey());

            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attributeTO.getValues().add("");

                // is important to set readonly only after values setting
                attributeTO.setReadonly(schema.isReadonly());
            } else {
                attributeTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            entityData.add(attributeTO);
        }

        entityTO.getPlainAttrs().clear();
        entityTO.getPlainAttrs().addAll(entityData);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FieldPanel getFieldPanel(final PlainSchemaTO schemaTO) {
        final boolean required = mode == Mode.TEMPLATE
                ? false
                : schemaTO.getMandatoryCondition().equalsIgnoreCase("true");

        final boolean readOnly = mode == Mode.TEMPLATE ? false : schemaTO.isReadonly();

        final AttrSchemaType type = mode == Mode.TEMPLATE ? AttrSchemaType.String : schemaTO.getType();

        final FieldPanel panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel("panel", schemaTO.getKey(), new Model<Boolean>(), false);
                panel.setRequired(required);
                break;
            case Date:
                final String dataPattern = schemaTO.getConversionPattern() == null
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
