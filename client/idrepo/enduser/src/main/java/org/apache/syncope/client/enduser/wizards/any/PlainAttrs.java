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
package org.apache.syncope.client.enduser.wizards.any;

import java.util.ArrayList;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.enduser.layout.CustomizationOption;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateFieldPanel;
import org.apache.syncope.client.enduser.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.enduser.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.EncryptedFieldPanel;
import org.apache.syncope.client.ui.commons.SchemaUtils;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.Attributable;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public class PlainAttrs extends AbstractAttrs<PlainSchemaTO> {

    private static final long serialVersionUID = 552437609667518888L;

    protected final AjaxWizard.Mode mode;

    protected final AnyTO previousObject;

    protected String fileKey = "";

    public <T extends AnyTO> PlainAttrs(
            final AnyWrapper<T> modelObject,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final Map<String, CustomizationOption> whichPlainAttrs) throws IllegalArgumentException {

        super(modelObject, anyTypeClasses, whichPlainAttrs);
        this.mode = mode;

        if (modelObject.getInnerObject() instanceof UserTO) {
            fileKey = UserTO.class.cast(modelObject.getInnerObject()).getUsername();
        } else if (modelObject.getInnerObject() instanceof GroupTO) {
            fileKey = GroupTO.class.cast(modelObject.getInnerObject()).getName();
        } else if (modelObject.getInnerObject() instanceof AnyObjectTO) {
            fileKey = AnyObjectTO.class.cast(modelObject.getInnerObject()).getName();
        }

        if (modelObject instanceof UserWrapper) {
            previousObject = UserWrapper.class.cast(modelObject).getPreviousUserTO();
        } else {
            previousObject = null;
        }

        setTitleModel(new ResourceModel("attributes.plain"));

        add(new Accordion("plainSchemas", List.of(new AbstractTab(
                new ResourceModel("attributes.accordion", "Plain Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new PlainSchemasOwn(panelId, schemas, attrs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<MembershipTO>("membershipsPlainSchemas", membershipTOs) {

            private static final long serialVersionUID = 1749643897846L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                final MembershipTO membershipTO = item.getModelObject();
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
                                new LoadableDetachableModel<Attributable>() { // SYNCOPE-1439

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
    protected boolean filterSchemas() {
        return super.filterSchemas() && mode != AjaxWizard.Mode.TEMPLATE;
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
        List<Attr> attrs = new ArrayList<>();

        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(anyTO.getPlainAttrs());

        attrs.addAll(schemas.values().stream().map(schema -> {
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
        anyTO.getPlainAttrs().addAll(attrs);
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

        plainAttrs.addAll(membershipSchemas.get(membershipTO.getGroupKey()).values().stream().
                map(schema -> {
                    Attr attrTO = new Attr();
                    attrTO.setSchema(schema.getKey());
                    if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                        attrTO.getValues().add(StringUtils.EMPTY);
                    } else {
                        attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
                    }
                    return attrTO;
                }).collect(Collectors.toList()));

        membershipTO.getPlainAttrs().clear();
        membershipTO.getPlainAttrs().addAll(plainAttrs);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected FieldPanel getFieldPanel(final PlainSchemaTO schemaTO) {
        return getFieldPanel(schemaTO, null);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected FieldPanel getFieldPanel(final PlainSchemaTO schemaTO, final String groupName) {
        final boolean required = schemaTO.getMandatoryCondition().equalsIgnoreCase("true");
        final boolean readOnly = schemaTO.isReadonly() || renderAsReadonly(schemaTO.getKey(), groupName);
        final AttrSchemaType type = schemaTO.getType();
        final boolean jexlHelp = false;

        FieldPanel panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel(
                        "panel",
                        schemaTO.getLabel(getLocale()),
                        new Model<>(),
                        true);
                panel.setRequired(required);
                break;

            case Date:
                String datePattern = schemaTO.getConversionPattern() == null
                        ? SyncopeConstants.DEFAULT_DATE_PATTERN
                        : schemaTO.getConversionPattern();

                if (datePattern.contains("H")) {
                    panel = new AjaxDateTimeFieldPanel(
                            "panel",
                            schemaTO.getLabel(getLocale()),
                            new Model<>(),
                            FastDateFormat.getInstance(datePattern));
                } else {
                    panel = new AjaxDateFieldPanel(
                            "panel",
                            schemaTO.getLabel(getLocale()),
                            new Model<>(),
                            FastDateFormat.getInstance(datePattern));
                }

                if (required) {
                    panel.addRequiredLabel();
                }

                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<>("panel",
                        schemaTO.getLabel(getLocale()), new Model<>(), true);
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
                panel = new AjaxSpinnerFieldPanel.Builder<Long>().enableOnChange().build(
                        "panel",
                        schemaTO.getLabel(getLocale()),
                        Long.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new AjaxSpinnerFieldPanel.Builder<Double>().enableOnChange().step(0.1).build(
                        "panel",
                        schemaTO.getLabel(getLocale()),
                        Double.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Binary:
                final PageReference pageRef = getPageReference();
                panel = new BinaryFieldPanel(
                        "panel",
                        schemaTO.getLabel(getLocale()),
                        new Model<>(),
                        schemaTO.getMimeType(),
                        fileKey) {

                    private static final long serialVersionUID = -3268213909514986831L;

                    @Override
                    protected PageReference getPageReference() {
                        return pageRef;
                    }

                };
                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Encrypted:
                panel = new EncryptedFieldPanel("panel",
                        schemaTO.getLabel(getLocale()), new Model<>(), true);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            default:
                panel = new AjaxTextFieldPanel("panel",
                        schemaTO.getLabel(getLocale()), new Model<>(), true);

                if (jexlHelp) {
                    AjaxTextFieldPanel.class.cast(panel).enableJexlHelp();
                }

                if (required) {
                    panel.addRequiredLabel();
                }
        }

        panel.setReadOnly(readOnly);
        panel.setMarkupId(StringUtils.isBlank(groupName) ? schemaTO.getKey() : groupName + '.' + schemaTO.getKey());

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

            add(new ListView<Attr>("schemas",
                    new ListModel<Attr>(new ArrayList<Attr>(
                            attributableTO.getObject().getPlainAttrs().stream().sorted(attrComparator).
                                    collect(Collectors.toList())))) {

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

                    AbstractFieldPanel<?> panel = getFieldPanel(schemas.get(attr.getSchema()));
                    if (schemas.get(attr.getSchema()).isMultivalue()) {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(
                                        attributableTO.getObject().getPlainAttr(attr.getSchema()), "values"))
                                .build("panel", attr.getSchema(), FieldPanel.class.cast(panel));
                        // SYNCOPE-1215 the entire multifield panel must be readonly, not only its field
                        ((MultiFieldPanel) panel).setReadOnly(schema == null ? false : schema.isReadonly());
                    } else {
                        FieldPanel.class.cast(panel).setNewModel(attr.getValues()).
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

            add(new ListView<Attr>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                @SuppressWarnings({ "unchecked", "rawtypes" })
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attr = item.getModelObject();
                    PlainSchemaTO schema = schemas.get(attr.getSchema());

                    // set default values, if any
                    if (attr.getValues().stream().noneMatch(StringUtils::isNotBlank)) {
                        attr.getValues().clear();
                        attr.getValues().addAll(getDefaultValues(attr.getSchema()));
                    }

                    AbstractFieldPanel<?> panel = getFieldPanel(schemas.get(attr.getSchema()));
                    if (schemas.get(attr.getSchema()).isMultivalue()) {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<>(attr, "values")).build(
                                "panel",
                                attr.getSchema(),
                                FieldPanel.class.cast(panel));
                        // SYNCOPE-1215 the entire multifield panel must be readonly, not only its field
                        ((MultiFieldPanel) panel).setReadOnly(schema == null ? false : schema.isReadonly());
                    } else {
                        FieldPanel.class.cast(panel).setNewModel(attr.getValues()).
                                setReadOnly(schema == null ? false : schema.isReadonly());
                    }
                    item.add(panel);
                }
            });
        }
    }

}
