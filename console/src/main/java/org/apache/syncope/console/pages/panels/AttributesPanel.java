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
package org.apache.syncope.console.pages.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.AbstractAttributableTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.console.commons.JexlHelpUtil;
import org.apache.syncope.console.pages.Schema;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.DateTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class AttributesPanel extends Panel {

    private static final long serialVersionUID = 552437609667518888L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    private final boolean templateMode;

    public <T extends AbstractAttributableTO> AttributesPanel(final String id, final T entityTO, final Form form,
            final boolean templateMode) {

        super(id);
        this.templateMode = templateMode;

        final IModel<Map<String, SchemaTO>> schemas = new LoadableDetachableModel<Map<String, SchemaTO>>() {

            private static final long serialVersionUID = -2012833443695917883L;

            @Override
            protected Map<String, SchemaTO> load() {
                final List<SchemaTO> schemaTOs;
                if (entityTO instanceof RoleTO) {
                    schemaTOs = schemaRestClient.getSchemas(AttributableType.ROLE);
                } else if (entityTO instanceof UserTO) {
                    schemaTOs = schemaRestClient.getSchemas(AttributableType.USER);
                } else {
                    schemaTOs = schemaRestClient.getSchemas(AttributableType.MEMBERSHIP);
                }

                final Map<String, SchemaTO> schemas = new HashMap<String, SchemaTO>();

                for (SchemaTO schemaTO : schemaTOs) {
                    schemas.put(schemaTO.getName(), schemaTO);
                }

                return schemas;
            }
        };

        initEntityData(entityTO, schemas.getObject().values());

        final ListView<AttributeTO> attributeView = new ListView<AttributeTO>("schemas",
                new PropertyModel<List<? extends AttributeTO>>(entityTO, "attributes")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<AttributeTO> item) {
                final AttributeTO attributeTO = (AttributeTO) item.getDefaultModelObject();

                final StringBuilder text = new StringBuilder(attributeTO.getSchema());

                final WebMarkupContainer jexlHelp = JexlHelpUtil.getJexlHelpWebContainer();
                item.add(jexlHelp);

                final AjaxLink questionMarkJexlHelp = JexlHelpUtil.getAjaxLink(jexlHelp);
                item.add(questionMarkJexlHelp);

                if (!templateMode) {
                    questionMarkJexlHelp.setVisible(false);
                }

                item.add(new Label("name", text.toString()));

                final FieldPanel panel =
                        getFieldPanel(schemas.getObject().get(attributeTO.getSchema()), form, attributeTO);

                if (templateMode || !schemas.getObject().get(attributeTO.getSchema()).isMultivalue()) {
                    item.add(panel);
                } else {
                    item.add(new MultiValueSelectorPanel<String>(
                            "panel", new PropertyModel(attributeTO, "values"), panel));
                }
            }
        };

        add(attributeView);
    }

    private List<AttributeTO> initEntityData(final AbstractAttributableTO entityTO,
            final Collection<SchemaTO> schemas) {

        final List<AttributeTO> entityData = new ArrayList<AttributeTO>();

        final Map<String, AttributeTO> attrMap = entityTO.getAttributeMap();

        for (SchemaTO schema : schemas) {
            AttributeTO attributeTO = new AttributeTO();
            attributeTO.setSchema(schema.getName());

            if (attrMap.get(schema.getName()) == null || attrMap.get(schema.getName()).getValues().isEmpty()) {

                List<String> values = new ArrayList<String>();
                values.add("");
                attributeTO.setValues(values);

                // is important to set readonly only after values setting
                attributeTO.setReadonly(schema.isReadonly());
            } else {
                attributeTO.setValues(attrMap.get(schema.getName()).getValues());
            }
            entityData.add(attributeTO);
        }

        entityTO.setAttributes(entityData);

        return entityData;
    }

    private FieldPanel getFieldPanel(final SchemaTO schemaTO, final Form form, final AttributeTO attributeTO) {

        final FieldPanel panel;

        final boolean required = templateMode ? false : schemaTO.getMandatoryCondition().equalsIgnoreCase("true");

        final boolean readOnly = templateMode ? false : schemaTO.isReadonly();

        final SchemaType type = templateMode ? SchemaType.String : schemaTO.getType();

        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel("panel", schemaTO.getName(), new Model());
                panel.setRequired(required);
                break;

            case Date:
                final String dataPattern = schemaTO.getConversionPattern() != null
                        ? schemaTO.getConversionPattern()
                        : SyncopeConstants.DEFAULT_DATE_PATTERN;

                if (!dataPattern.contains("H")) {
                    panel = new DateTextFieldPanel("panel", schemaTO.getName(), new Model(), dataPattern);

                    if (required) {
                        panel.addRequiredLabel();
                    }
                } else {
                    panel = new DateTimeFieldPanel("panel", schemaTO.getName(), new Model(), dataPattern);

                    if (required) {
                        panel.addRequiredLabel();
                        ((DateTimeFieldPanel) panel).setFormValidator(form);
                    }
                    panel.setStyleSheet("ui-widget-content ui-corner-all");
                }
                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<String>("panel", schemaTO.getName(), new Model<String>());
                ((AjaxDropDownChoicePanel) panel).setChoices(getEnumeratedValues(schemaTO));

                if (StringUtils.isNotBlank(schemaTO.getEnumerationKeys())) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        final Map<String, String> valueMap = getEnumeratedKeyValues(schemaTO);

                        @Override
                        public String getDisplayValue(final String value) {
                            return valueMap.get(value) == null ? value : valueMap.get(value);
                        }

                        @Override
                        public String getIdValue(final String value, int i) {
                            return value;
                        }
                    });
                }

                if (required) {
                    panel.addRequiredLabel();
                }

                break;

            default:
                panel = new AjaxTextFieldPanel("panel", schemaTO.getName(), new Model());
                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);
        panel.setNewModel(attributeTO.getValues());

        return panel;
    }

    private Map<String, String> getEnumeratedKeyValues(final SchemaTO schemaTO) {
        final Map<String, String> res = new HashMap<String, String>();

        final String[] values = StringUtils.isBlank(schemaTO.getEnumerationValues())
                ? new String[0]
                : schemaTO.getEnumerationValues().split(Schema.enumValuesSeparator);

        final String[] keys = StringUtils.isBlank(schemaTO.getEnumerationKeys())
                ? new String[0]
                : schemaTO.getEnumerationKeys().split(Schema.enumValuesSeparator);

        for (int i = 0; i < values.length; i++) {
            res.put(values[i].trim(), keys.length > i ? keys[i].trim() : null);
        }

        return res;
    }

    private List<String> getEnumeratedValues(final SchemaTO schemaTO) {
        final List<String> res = new ArrayList<String>();

        final String[] values = StringUtils.isBlank(schemaTO.getEnumerationValues())
                ? new String[0]
                : schemaTO.getEnumerationValues().split(Schema.enumValuesSeparator);

        for (String value : values) {
            res.add(value.trim());
        }

        return res;
    }
}
