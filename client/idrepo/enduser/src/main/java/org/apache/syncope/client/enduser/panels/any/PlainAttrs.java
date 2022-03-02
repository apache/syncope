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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.enduser.layout.CustomizationOption;
import org.apache.syncope.client.enduser.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.enduser.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.SchemaUtils;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.Attributable;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.util.ListModel;
import java.util.stream.Collectors;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.EncryptedFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

public class PlainAttrs extends AbstractAttrs<PlainSchemaTO> {

    private static final long serialVersionUID = 552437609667518888L;

    protected final AnyTO previousObject;

    protected String fileKey = "";

    public PlainAttrs(
            final String id,
            final UserWrapper modelObject,
            final List<String> anyTypeClasses,
            final Map<String, CustomizationOption> whichPlainAttrs) throws IllegalArgumentException {

        super(id, modelObject, anyTypeClasses, whichPlainAttrs);

        fileKey = modelObject.getInnerObject().getUsername();

        previousObject = modelObject.getPreviousUserTO();

        add(new PlainSchemasOwn("plainSchemas", schemas, attrs).setOutputMarkupId(true));
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
                        return new PlainSchemasMemberships(
                                panelId,
                                membershipTO.getGroupName(),
                                membershipSchemas.get(membershipTO.getGroupKey()),
                                new LoadableDetachableModel<>() { // SYNCOPE-1439

                            private static final long serialVersionUID = 526768546610546553L;

                            @Override
                            protected Attributable load() {
                                return membershipTO;
                            }

                        });
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
        return anyTO.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<Attr> getAttrsFromTO(final MembershipTO membershipTO) {
        return membershipTO.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected void setAttrs() {
        List<Attr> plainAttrs = new ArrayList<>();

        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(anyTO.getPlainAttrs());

        plainAttrs.addAll(schemas.values().stream().map(schema -> {
            Attr attrTO = new Attr();
            attrTO.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attrTO.getValues().add("");
            } else {
                attrTO = attrMap.get(schema.getKey());
            }
            return attrTO;
        }).collect(Collectors.toList()));

        anyTO.getPlainAttrs().clear();
        anyTO.getPlainAttrs().addAll(plainAttrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        List<Attr> plainAttrs = new ArrayList<>();

        final Map<String, Attr> attrMap;
        if (GroupableRelatableTO.class.cast(anyTO).getMembership(membershipTO.getGroupKey()).isPresent()) {
            attrMap = EntityTOUtils.buildAttrMap(GroupableRelatableTO.class.cast(anyTO)
                    .getMembership(membershipTO.getGroupKey()).get().getPlainAttrs());
        } else {
            attrMap = new HashMap<>();
        }

        plainAttrs.addAll(membershipSchemas.get(membershipTO.getGroupKey()).values().stream().map(schema -> {
            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attr.getValues().add(StringUtils.EMPTY);
            } else {
                attr.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            return attr;
        }).collect(Collectors.toList()));

        membershipTO.getPlainAttrs().clear();
        membershipTO.getPlainAttrs().addAll(plainAttrs);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected FieldPanel getFieldPanel(final PlainSchemaTO schemaTO) {
        return getFieldPanel(schemaTO, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected FieldPanel getFieldPanel(final PlainSchemaTO plainSchema, final String groupName) {
        boolean required = plainSchema.getMandatoryCondition().equalsIgnoreCase("true");
        boolean readOnly = plainSchema.isReadonly() || renderAsReadonly(plainSchema.getKey(), groupName);
        AttrSchemaType type = plainSchema.getType();
        boolean jexlHelp = false;

        FieldPanel panel;
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
                ((AjaxDropDownChoicePanel<String>) panel).setChoices(SchemaUtils.getEnumeratedValues(plainSchema));

                if (StringUtils.isNotBlank(plainSchema.getEnumerationKeys())) {
                    ((AjaxDropDownChoicePanel) panel).setChoiceRenderer(new IChoiceRenderer<String>() {

                        private static final long serialVersionUID = -3724971416312135885L;

                        private final Map<String, String> valueMap = SchemaUtils.getEnumeratedKeyValues(plainSchema);

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
                panel = new AjaxSpinnerFieldPanel.Builder<Long>().enableOnChange().build(
                        "panel",
                        plainSchema.getLabel(SyncopeEnduserSession.get().getLocale()),
                        Long.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new AjaxSpinnerFieldPanel.Builder<Double>().enableOnChange().step(0.1).build(
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
        panel.setMarkupId(StringUtils.isBlank(groupName)
                ? plainSchema.getKey() : groupName + '.' + plainSchema.getKey());

        Label label = (Label) panel.get(AbstractFieldPanel.LABEL);
        label.add(new AttributeModifier("for", FORM_SUFFIX
                + (StringUtils.isBlank(groupName) ? plainSchema.getKey() : groupName + '.' + plainSchema.getKey())));

        return panel;
    }

    protected class PlainSchemasMemberships extends Schemas {

        private static final long serialVersionUID = 456754923340249215L;

        public PlainSchemasMemberships(
                final String id,
                final String groupName,
                final Map<String, PlainSchemaTO> schemas,
                final IModel<Attributable> attributableTO) {

            super(id);

            add(new ListView<>("schemas",
                    new ListModel<>(new ArrayList<>(
                            attributableTO.getObject().getPlainAttrs().stream().sorted(attrComparator).
                                    collect(Collectors.toList())))) {

                private static final long serialVersionUID = 5306618783986001008L;

                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attrTO = item.getModelObject();
                    PlainSchemaTO schema = schemas.get(attrTO.getSchema());

                    // set default values, if any
                    if (attrTO.getValues().stream().noneMatch(StringUtils::isNotBlank)) {
                        attrTO.getValues().clear();
                        attrTO.getValues().addAll(getDefaultValues(attrTO.getSchema(), groupName));
                    }

                    AbstractFieldPanel<?> panel = getFieldPanel(schemas.get(attrTO.getSchema()));
                    if (schemas.get(attrTO.getSchema()).isMultivalue()) {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(
                                        attributableTO.getObject().getPlainAttr(attrTO.getSchema()), "values"))
                                .build("panel", attrTO.getSchema(), FieldPanel.class.cast(panel));
                        // SYNCOPE-1215 the entire multifield panel must be readonly, not only its field
                        ((MultiFieldPanel) panel).setReadOnly(schema == null ? false : schema.isReadonly());
                    } else {
                        FieldPanel.class.cast(panel).setNewModel(attrTO.getValues()).
                                setReadOnly(schema == null ? false : schema.isReadonly());
                    }

                    item.add(panel);
                }
            });
        }
    }

    protected class PlainSchemasOwn extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public PlainSchemasOwn(
                final String id,
                final Map<String, PlainSchemaTO> schemas,
                final IModel<List<Attr>> attrTOs) {

            super(id);

            add(new ListView<>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attrTO = item.getModelObject();
                    PlainSchemaTO schema = schemas.get(attrTO.getSchema());

                    // set default values, if any
                    if (attrTO.getValues().stream().noneMatch(StringUtils::isNotBlank)) {
                        attrTO.getValues().clear();
                        attrTO.getValues().addAll(getDefaultValues(attrTO.getSchema()));
                    }

                    AbstractFieldPanel<?> panel = getFieldPanel(schemas.get(attrTO.getSchema()));
                    if (schemas.get(attrTO.getSchema()).isMultivalue()) {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(attrTO, "values")).build(
                                "panel",
                                attrTO.getSchema(),
                                FieldPanel.class.cast(panel));
                        // SYNCOPE-1215 the entire multifield panel must be readonly, not only its field
                        ((MultiFieldPanel) panel).setReadOnly(schema == null ? false : schema.isReadonly());
                    } else {
                        FieldPanel.class.cast(panel).setNewModel(attrTO.getValues()).
                                setReadOnly(schema == null ? false : schema.isReadonly());
                    }
                    item.add(panel);
                }
            });
        }
    }
}
