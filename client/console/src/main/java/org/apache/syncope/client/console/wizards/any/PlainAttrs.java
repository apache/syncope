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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.SchemaUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.DateTimeFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public class PlainAttrs extends AbstractAttrs<PlainSchemaTO> {

    private static final long serialVersionUID = 552437609667518888L;

    private final AjaxWizard.Mode mode;

    public <T extends AnyTO> PlainAttrs(
            final T anyTO,
            final Form<?> form,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichPlainAttrs) throws IllegalArgumentException {

        super(anyTO, anyTypeClasses, whichPlainAttrs);
        this.mode = mode;

        setTitleModel(new ResourceModel("attributes.plain"));

        add(new Accordion("plainSchemas", Collections.<ITab>singletonList(new AbstractTab(
                new ResourceModel("attributes.accordion", "Plain Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new PlainSchemas(panelId, schemas, attrTOs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<MembershipTO>("membershipsPlainSchemas", membershipTOs) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                final MembershipTO membershipTO = item.getModelObject();
                item.add(new Accordion("membershipPlainSchemas", Collections.<ITab>singletonList(new AbstractTab(
                        new StringResourceModel(
                                "attributes.membership.accordion",
                                PlainAttrs.this,
                                Model.of(membershipTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new PlainSchemas(
                                panelId,
                                membershipSchemas.get(membershipTO.getGroupKey()),
                                new ListModel<AttrTO>(getAttrsFromTO(membershipTO)));
                    }
                }), Model.of(-1)).setOutputMarkupId(true));
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.PLAIN;
    }

    @Override
    protected boolean reoderSchemas() {
        return super.reoderSchemas() && mode != AjaxWizard.Mode.TEMPLATE;
    }

    @Override
    protected List<AttrTO> getAttrsFromTO() {
        final List<AttrTO> res = new ArrayList<>(anyTO.getPlainAttrs());
        Collections.sort(res, new AttrComparator());
        return res;
    }

    @Override
    protected List<AttrTO> getAttrsFromTO(final MembershipTO membershipTO) {
        final List<AttrTO> res = new ArrayList<>(membershipTO.getPlainAttrs());
        Collections.sort(res, new AttrComparator());
        return res;
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

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = membershipTO.getPlainAttrMap();

        for (PlainSchemaTO schema : membershipSchemas.get(membershipTO.getGroupKey()).values()) {
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

        membershipTO.getPlainAttrs().clear();
        membershipTO.getPlainAttrs().addAll(attrs);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private FieldPanel getFieldPanel(final PlainSchemaTO schemaTO) {
        final boolean required;
        final boolean readOnly;
        final AttrSchemaType type;
        final boolean jexlHelp;

        if (mode == AjaxWizard.Mode.TEMPLATE) {
            required = false;
            readOnly = false;
            type = AttrSchemaType.String;
            jexlHelp = true;
        } else {
            required = schemaTO.getMandatoryCondition().equalsIgnoreCase("true");
            readOnly = schemaTO.isReadonly();
            type = schemaTO.getType();
            jexlHelp = false;

        }

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
                panel = new BinaryFieldPanel("panel", schemaTO.getKey(), new Model<String>(), schemaTO.getMimeType());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            default:
                panel = new AjaxTextFieldPanel("panel", schemaTO.getKey(), new Model<String>(), false);

                if (jexlHelp) {
                    AjaxTextFieldPanel.class.cast(panel).enableJexlHelp();
                }

                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);

        return panel;
    }

    public class PlainSchemas extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public PlainSchemas(
                final String id,
                final Map<String, PlainSchemaTO> availableSchemas,
                final IModel<List<AttrTO>> attrTOs) {
            super(id);

            add(new ListView<AttrTO>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                protected void populateItem(final ListItem<AttrTO> item) {
                    AttrTO attrTO = item.getModelObject();

                    FieldPanel panel = getFieldPanel(availableSchemas.get(attrTO.getSchema()));
                    if (mode == AjaxWizard.Mode.TEMPLATE
                            || !availableSchemas.get(attrTO.getSchema()).isMultivalue()) {
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
    }
}
