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
package org.apache.syncope.client.enduser.panels.any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.layout.CustomizationOption;
import org.apache.syncope.client.enduser.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.enduser.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.EncryptedFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public class PlainAttrs extends AbstractAttrs<PlainSchemaTO> {

    private static final long serialVersionUID = 552437609667518888L;

    protected final AnyTO anyTO;

    protected final AnyTO previousObject;

    protected String fileKey = "";

    public PlainAttrs(
            final String id,
            final UserWrapper modelObject,
            final List<String> anyTypeClasses,
            final Map<String, CustomizationOption> whichPlainAttrs) throws IllegalArgumentException {

        super(id, modelObject, anyTypeClasses, whichPlainAttrs);

        anyTO = modelObject.getInnerObject();
        previousObject = modelObject.getPreviousUserTO();

        fileKey = modelObject.getInnerObject().getUsername();

        add(new PlainSchemas("plainSchemas", null, schemas, attrs).setOutputMarkupId(true));
        add(new ListView<>("membershipsPlainSchemas", membershipTOs) {

            private static final long serialVersionUID = 6741044372185745296L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                MembershipTO membershipTO = item.getModelObject();
                item.add(new Accordion("membershipPlainSchemas", List.of(new AbstractTab(
                        new StringResourceModel(
                                "attributes.membership.accordion",
                                PlainAttrs.this,
                                Model.of(membershipTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new PlainSchemas(
                                panelId,
                                membershipTO.getGroupName(),
                                membershipSchemas.get(membershipTO.getGroupKey()),
                                new ListModel<>(membershipTO.getPlainAttrs().stream().
                                        sorted(attrComparator).
                                        collect(Collectors.toList())));
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
    protected List<Attr> getAttrsFromTO() {
        return userTO.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<Attr> getAttrsFromTO(final MembershipTO membershipTO) {
        return membershipTO.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected void setAttrs() {
        List<Attr> plainAttrs = new ArrayList<>();

        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(userTO.getPlainAttrs());

        plainAttrs.addAll(schemas.values().stream().map(schema -> {
            Attr attrTO = new Attr();
            attrTO.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attrTO.getValues().add("");
            } else {
                attrTO = attrMap.get(schema.getKey());
            }
            return attrTO;
        }).toList());

        userTO.getPlainAttrs().clear();
        userTO.getPlainAttrs().addAll(plainAttrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        Map<String, Attr> attrMap = GroupableRelatableTO.class.cast(userTO).getMembership(membershipTO.getGroupKey()).
                map(gr -> EntityTOUtils.buildAttrMap(gr.getPlainAttrs())).
                orElseGet(HashMap::new);

        List<Attr> plainAttrs = membershipSchemas.get(membershipTO.getGroupKey()).values().stream().map(schema -> {
            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attr.getValues().add(StringUtils.EMPTY);
            } else {
                attr.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            return attr;
        }).toList();

        membershipTO.getPlainAttrs().clear();
        membershipTO.getPlainAttrs().addAll(plainAttrs);
    }

    @SuppressWarnings("unchecked")
    protected AbstractFieldPanel<?> getFieldPanel(final PlainSchemaTO plainSchema) {
        boolean required = plainSchema.getMandatoryCondition().equalsIgnoreCase("true");
        boolean readOnly = plainSchema.isReadonly() || renderAsReadonly(plainSchema.getKey(), null);
        AttrSchemaType type = plainSchema.getType();
        boolean jexlHelp = false;

        AbstractFieldPanel<?> panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel(
                        "panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                        new Model<>(),
                        true);
                panel.setRequired(required);
                break;

            case Date:
                String datePattern = plainSchema.getConversionPattern() == null
                        ? DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.getPattern()
                        : plainSchema.getConversionPattern();

                if (StringUtils.containsIgnoreCase(datePattern, "H")) {
                    panel = new AjaxDateTimeFieldPanel(
                            "panel",
                            plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                            new Model<>(),
                            FastDateFormat.getInstance(datePattern));
                } else {
                    panel = new AjaxDateFieldPanel(
                            "panel",
                            plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                            new Model<>(),
                            FastDateFormat.getInstance(datePattern));
                }

                if (required) {
                    panel.addRequiredLabel();
                }

                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<>("panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()), new Model<>(), true);
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(
                        plainSchema.getEnumValues().keySet().stream().sorted().toList());

                if (!plainSchema.getEnumValues().isEmpty()) {
                    Map<String, String> valueMap = plainSchema.getEnumValues();
                    ((AjaxDropDownChoicePanel<String>) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

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

            case Dropdown:
                List<String> dropdownValues = schemaRestClient.getDropdownValues(plainSchema.getKey(), anyTO);
                if (plainSchema.isMultivalue()) {
                    panel = new AjaxPalettePanel.Builder<String>().
                            setName(plainSchema.getLabel(SyncopeEnduserSession.get().getLocale())).
                            build("panel", new ListModel<>(), new ListModel<>(dropdownValues));
                } else {
                    panel = new AjaxDropDownChoicePanel<>("panel",
                            plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()), new Model<>(), true);
                    ((AjaxDropDownChoicePanel<String>) panel).setChoices(dropdownValues);
                }

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Long:
                panel = new AjaxNumberFieldPanel.Builder<Long>().enableOnChange().build(
                        "panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                        Long.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new AjaxNumberFieldPanel.Builder<Double>().enableOnChange().step(0.1).build(
                        "panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                        Double.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Binary:
                panel = new BinaryFieldPanel(
                        "panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                        new Model<>(),
                        plainSchema.getMimeType(),
                        fileKey);
                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Encrypted:
                panel = new EncryptedFieldPanel("panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()), new Model<>(), true);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            default:
                panel = new AjaxTextFieldPanel("panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()), new Model<>(), true);

                if (jexlHelp) {
                    AjaxTextFieldPanel.class.cast(panel).enableJexlHelp();
                }

                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);
        panel.setMarkupId(plainSchema.getKey());

        Label label = (Label) panel.get(AbstractFieldPanel.LABEL);
        label.add(new AttributeModifier("for", FORM_SUFFIX + plainSchema.getKey()));

        return panel;
    }

    protected class PlainSchemas extends Schemas {

        private static final long serialVersionUID = 456754923340249215L;

        protected PlainSchemas(
                final String id,
                final String groupName,
                final Map<String, PlainSchemaTO> schemas,
                final IModel<List<Attr>> attrs) {

            super(id);

            add(new ListView<>("schemas", attrs) {

                private static final long serialVersionUID = 5306618783986001008L;

                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attr = item.getModelObject();
                    PlainSchemaTO schema = schemas.get(attr.getSchema());

                    // set default values, if any
                    if (attr.getValues().stream().noneMatch(StringUtils::isNotBlank)) {
                        attr.getValues().clear();
                        attr.getValues().addAll(getDefaultValues(attr.getSchema(), groupName));
                    }

                    AbstractFieldPanel<?> panel = getFieldPanel(schema);
                    panel.setReadOnly(schema.isReadonly());
                    if (schema.isMultivalue() && schema.getType() != AttrSchemaType.Dropdown) {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(attr, "values")).build(
                                "panel",
                                schema.getLabel(SyncopeEnduserSession.get().getLocale()),
                                FieldPanel.class.cast(panel));
                    } else if (panel instanceof AjaxPalettePanel ajaxPalettePanel) {
                        ajaxPalettePanel.setModelObject(attr.getValues());
                    } else {
                        FieldPanel.class.cast(panel).setNewModel(attr.getValues());
                    }

                    item.add(panel);
                }
            });
        }
    }
}
