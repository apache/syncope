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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.AttrLayoutType;
import org.apache.syncope.client.console.commons.JexlHelpUtil;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.panels.AttrTemplatesPanel.RoleAttrTemplatesChange;
import org.apache.syncope.client.console.rest.ConfigurationRestClient;
import org.apache.syncope.client.console.rest.RoleRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.list.AltListView;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConfTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class PlainAttrsPanel extends Panel {

    private static final long serialVersionUID = 552437609667518888L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    @SpringBean
    private ConfigurationRestClient confRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    private final AbstractAttributableTO entityTO;

    private final Mode mode;

    private final AttrTemplatesPanel attrTemplates;

    private Map<String, PlainSchemaTO> schemas = new LinkedHashMap<>();

    public <T extends AbstractAttributableTO> PlainAttrsPanel(final String id, final T entityTO,
            final Form<?> form, final Mode mode) {

        this(id, entityTO, form, mode, null);
    }

    public <T extends AbstractAttributableTO> PlainAttrsPanel(final String id, final T entityTO,
            final Form<?> form, final Mode mode, final AttrTemplatesPanel attrTemplates) {

        super(id);
        this.entityTO = entityTO;
        this.mode = mode;
        this.attrTemplates = attrTemplates;
        this.setOutputMarkupId(true);

        setSchemas();
        setAttrs();

        add(new AltListView<AttrTO>("schemas", new PropertyModel<List<? extends AttrTO>>(entityTO, "attrs")) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void populateItem(final ListItem<AttrTO> item) {
                final AttrTO attributeTO = (AttrTO) item.getDefaultModelObject();

                final WebMarkupContainer jexlHelp = JexlHelpUtil.getJexlHelpWebContainer("jexlHelp");

                final AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtil.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
                item.add(questionMarkJexlHelp);
                questionMarkJexlHelp.add(jexlHelp);

                if (mode != Mode.TEMPLATE) {
                    questionMarkJexlHelp.setVisible(false);
                }

                item.add(new Label("name", attributeTO.getSchema()));

                final FieldPanel panel = getFieldPanel(schemas.get(attributeTO.getSchema()), form, attributeTO);

                if (mode == Mode.TEMPLATE || !schemas.get(attributeTO.getSchema()).isMultivalue()) {
                    item.add(panel);
                } else {
                    item.add(new MultiFieldPanel<String>(
                            "panel", new PropertyModel<List<String>>(attributeTO, "values"), panel));
                }
            }
        }
        );
    }

    private void setSchemas() {
        AttrTO attrLayout = null;
        List<PlainSchemaTO> schemaTOs;

        if (entityTO instanceof RoleTO) {
            final RoleTO roleTO = (RoleTO) entityTO;

            attrLayout = confRestClient.readAttrLayout(AttrLayoutType.valueOf(mode, AttributableType.ROLE));
            schemaTOs = schemaRestClient.getSchemas(AttributableType.ROLE);
            Set<String> allowed;
            if (attrTemplates == null) {
                allowed = new HashSet<>(roleTO.getRPlainAttrTemplates());
            } else {
                allowed = new HashSet<>(attrTemplates.getSelected(AttrTemplatesPanel.Type.rPlainAttrTemplates));
                if (roleTO.isInheritTemplates() && roleTO.getParent() != 0) {
                    allowed.addAll(roleRestClient.read(roleTO.getParent()).getRPlainAttrTemplates());
                }
            }
            schemaRestClient.filter(schemaTOs, allowed, true);
        } else if (entityTO instanceof UserTO) {
            attrLayout = confRestClient.readAttrLayout(AttrLayoutType.valueOf(mode, AttributableType.USER));
            schemaTOs = schemaRestClient.getSchemas(AttributableType.USER);
        } else if (entityTO instanceof MembershipTO) {
            attrLayout = confRestClient.readAttrLayout(AttrLayoutType.valueOf(mode, AttributableType.MEMBERSHIP));
            schemaTOs = schemaRestClient.getSchemas(AttributableType.MEMBERSHIP);
            Set<String> allowed = new HashSet<>(
                    roleRestClient.read(((MembershipTO) entityTO).getRoleId()).getMPlainAttrTemplates());
            schemaRestClient.filter(schemaTOs, allowed, true);
        } else {
            schemas = new TreeMap<>();
            schemaTOs = schemaRestClient.getSchemas(AttributableType.CONFIGURATION);
            for (Iterator<PlainSchemaTO> it = schemaTOs.iterator(); it.hasNext();) {
                PlainSchemaTO schemaTO = it.next();
                for (AttrLayoutType type : AttrLayoutType.values()) {
                    if (type.getConfKey().equals(schemaTO.getKey())) {
                        it.remove();
                    }
                }
            }
        }

        schemas.clear();

        if (attrLayout != null && mode != Mode.TEMPLATE && !(entityTO instanceof ConfTO)) {
            // 1. remove attributes not selected for display
            schemaRestClient.filter(schemaTOs, attrLayout.getValues(), true);
            // 2. sort remainig attributes according to configuration, e.g. attrLayout
            final Map<String, Integer> attrLayoutMap = new HashMap<>(attrLayout.getValues().size());
            for (int i = 0; i < attrLayout.getValues().size(); i++) {
                attrLayoutMap.put(attrLayout.getValues().get(i), i);
            }
            Collections.sort(schemaTOs, new Comparator<PlainSchemaTO>() {

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
        for (PlainSchemaTO schemaTO : schemaTOs) {
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
    private FieldPanel getFieldPanel(final PlainSchemaTO schemaTO, final Form form, final AttrTO attributeTO) {
        final boolean required = mode == Mode.TEMPLATE
                ? false
                : schemaTO.getMandatoryCondition().equalsIgnoreCase("true");

        final boolean readOnly = mode == Mode.TEMPLATE ? false : schemaTO.isReadonly();

        final AttrSchemaType type = mode == Mode.TEMPLATE ? AttrSchemaType.String : schemaTO.getType();

        final FieldPanel panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel("panel", schemaTO.getKey(), new Model<Boolean>());
                panel.setRequired(required);
                break;

            case Date:
                final String dataPattern = schemaTO.getConversionPattern() == null
                        ? SyncopeConstants.DEFAULT_DATE_PATTERN
                        : schemaTO.getConversionPattern();

                if (dataPattern.contains("H")) {
                    panel = new DateTimeFieldPanel("panel", schemaTO.getKey(), new Model<Date>(), dataPattern);

                    if (required) {
                        panel.addRequiredLabel();
                        ((DateTimeFieldPanel) panel).setFormValidator(form);
                    }
                    panel.setStyleSheet("ui-widget-content ui-corner-all");
                } else {
                    panel = new DateTextFieldPanel("panel", schemaTO.getKey(), new Model<Date>(), dataPattern);

                    if (required) {
                        panel.addRequiredLabel();
                    }
                }
                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<String>("panel", schemaTO.getKey(), new Model<String>());
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(getEnumeratedValues(schemaTO));

                if (StringUtils.isNotBlank(schemaTO.getEnumerationKeys())) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        private final Map<String, String> valueMap = getEnumeratedKeyValues(schemaTO);

                        @Override
                        public String getDisplayValue(final String value) {
                            return valueMap.get(value) == null ? value : valueMap.get(value);
                        }

                        @Override
                        public String getIdValue(final String value, final int i) {
                            return value;
                        }
                    });
                }

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Long:
                panel = new SpinnerFieldPanel<Long>("panel", schemaTO.getKey(),
                        Long.class, new Model<Long>(), null, null);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new SpinnerFieldPanel<Double>("panel", schemaTO.getKey(),
                        Double.class, new Model<Double>(), null, null);

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
                panel = new AjaxTextFieldPanel("panel", schemaTO.getKey(), new Model<String>());

                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);
        panel.setNewModel(attributeTO.getValues());

        return panel;
    }

    private Map<String, String> getEnumeratedKeyValues(final PlainSchemaTO schemaTO) {
        final Map<String, String> res = new HashMap<>();

        final String[] values = StringUtils.isBlank(schemaTO.getEnumerationValues())
                ? new String[0]
                : schemaTO.getEnumerationValues().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);

        final String[] keys = StringUtils.isBlank(schemaTO.getEnumerationKeys())
                ? new String[0]
                : schemaTO.getEnumerationKeys().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);

        for (int i = 0; i < values.length; i++) {
            res.put(values[i].trim(), keys.length > i ? keys[i].trim() : null);
        }

        return res;
    }

    private List<String> getEnumeratedValues(final PlainSchemaTO schemaTO) {
        final List<String> res = new ArrayList<>();

        final String[] values = StringUtils.isBlank(schemaTO.getEnumerationValues())
                ? new String[0]
                : schemaTO.getEnumerationValues().split(SyncopeConstants.ENUM_VALUES_SEPARATOR);

        for (String value : values) {
            res.add(value.trim());
        }

        return res;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if ((event.getPayload() instanceof RoleAttrTemplatesChange)) {
            final RoleAttrTemplatesChange update = (RoleAttrTemplatesChange) event.getPayload();
            if (attrTemplates != null && update.getType() == AttrTemplatesPanel.Type.rPlainAttrTemplates) {
                setSchemas();
                setAttrs();
                update.getTarget().add(this);
            }
        }
    }
}
