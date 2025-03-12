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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.BinaryFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
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
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class AbstractAttrsWizardStep<S extends SchemaTO> extends WizardStep implements ICondition {

    private static final long serialVersionUID = 8931397230194043674L;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    protected final Comparator<Attr> attrComparator = new AttrComparator();

    protected final AnyTO anyTO;

    protected AnyTO previousObject;

    protected final List<String> whichAttrs;

    protected final Map<String, S> schemas = new LinkedHashMap<>();

    protected final IModel<List<Attr>> attrs;

    protected final List<String> anyTypeClasses;

    protected String fileKey = "";

    protected final AjaxWizard.Mode mode;

    public AbstractAttrsWizardStep(
            final AnyTO anyTO,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichAttrs) {

        super();
        this.anyTypeClasses = anyTypeClasses;
        this.attrs = new ListModel<>(List.of());

        this.setOutputMarkupId(true);

        this.mode = mode;
        this.anyTO = anyTO;
        this.whichAttrs = whichAttrs;
    }

    protected List<Attr> loadAttrs() {
        List<String> classes = new ArrayList<>(anyTypeClasses);
        classes.addAll(anyTypeClassRestClient.list(anyTO.getAuxClasses()).stream().
                map(AnyTypeClassTO::getKey).toList());
        setSchemas(classes);
        setAttrs();
        return getAttrsFromTO();
    }

    protected boolean reoderSchemas() {
        return !whichAttrs.isEmpty();
    }

    protected abstract SchemaType getSchemaType();

    protected void setSchemas(final List<String> anyTypeClasses) {
        setSchemas(anyTypeClasses, schemas);
    }

    protected void setSchemas(final List<String> anyTypeClasses, final Map<String, S> scs) {
        List<S> allSchemas = anyTypeClasses.isEmpty()
                ? List.of()
                : schemaRestClient.getSchemas(getSchemaType(), null, anyTypeClasses.toArray(String[]::new));

        scs.clear();

        if (!allSchemas.isEmpty() && reoderSchemas()) {
            // remove attributes not selected for display
            allSchemas.removeAll(allSchemas.stream().
                    filter(schemaTO -> !whichAttrs.contains(schemaTO.getKey())).collect(Collectors.toSet()));
        }

        allSchemas.forEach(schemaTO -> scs.put(schemaTO.getKey(), schemaTO));
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (CollectionUtils.isEmpty(attrs.getObject())) {
            response.render(OnDomReadyHeaderItem.forScript(
                    String.format("$('#emptyPlaceholder').append(\"%s\"); $('#attributes').hide();",
                            getString("attribute.empty.list"))));
        }
    }

    protected abstract void setAttrs();

    protected abstract List<Attr> getAttrsFromTO();

    @Override
    public boolean evaluate() {
        this.attrs.setObject(loadAttrs());
        return !attrs.getObject().isEmpty();
    }

    public PageReference getPageReference() {
        // SYNCOPE-1213
        // default implementation does not require to pass page reference, override this method if want otherwise
        return null;
    }

    @SuppressWarnings("unchecked")
    protected AbstractFieldPanel<?> getFieldPanel(final PlainSchemaTO plainSchema) {
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
            required = plainSchema.getMandatoryCondition().equalsIgnoreCase("true");
            readOnly = plainSchema.isReadonly();
            type = plainSchema.getType();
            jexlHelp = false;
        }

        AbstractFieldPanel<?> panel;
        switch (type) {
            case Boolean:
                panel = new AjaxCheckBoxPanel(
                        "panel",
                        plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()),
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
                            plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()),
                            new Model<>(),
                            FastDateFormat.getInstance(datePattern));
                } else {
                    panel = new AjaxDateFieldPanel(
                            "panel",
                            plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()),
                            new Model<>(),
                            FastDateFormat.getInstance(datePattern));
                }

                if (required) {
                    panel.addRequiredLabel();
                }

                break;

            case Enum:
                panel = new AjaxDropDownChoicePanel<>("panel",
                        plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()), new Model<>(), true);
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
                            setName(plainSchema.getLabel(SyncopeConsoleSession.get().getLocale())).
                            build("panel", new ListModel<>(), new ListModel<>(dropdownValues));
                } else {
                    panel = new AjaxDropDownChoicePanel<>("panel",
                            plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()), new Model<>(), true);
                    ((AjaxDropDownChoicePanel<String>) panel).setChoices(dropdownValues);
                }

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Long:
                panel = new AjaxNumberFieldPanel.Builder<Long>().enableOnChange().build(
                        "panel",
                        plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()),
                        Long.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Double:
                panel = new AjaxNumberFieldPanel.Builder<Double>().enableOnChange().step(0.1).build(
                        "panel",
                        plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()),
                        Double.class,
                        new Model<>());

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            case Binary:
                PageReference pageRef = getPageReference();
                panel = new BinaryFieldPanel(
                        "panel",
                        plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()),
                        new Model<>(),
                        plainSchema.getMimeType(),
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
                panel = SyncopeConstants.ENCRYPTED_DECODE_CONVERSION_PATTERN.equals(plainSchema.getConversionPattern())
                        ? new AjaxTextFieldPanel("panel",
                                plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()), new Model<>(), true)
                        : new EncryptedFieldPanel("panel",
                                plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()), new Model<>(), true);

                if (required) {
                    panel.addRequiredLabel();
                }
                break;

            default:
                panel = new AjaxTextFieldPanel("panel",
                        plainSchema.getLabel(SyncopeConsoleSession.get().getLocale()), new Model<>(), true);

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

    protected FormComponent<?> checkboxToggle(
            final Attr attr,
            final AbstractFieldPanel<?> panel,
            final boolean isMultivalue) {

        // do nothing
        return null;
    }

    private class AttrComparator implements Comparator<Attr>, Serializable {

        private static final long serialVersionUID = -5105030477767941060L;

        @Override
        public int compare(final Attr left, final Attr right) {
            if (left == null || StringUtils.isEmpty(left.getSchema())) {
                return -1;
            }
            if (right == null || StringUtils.isEmpty(right.getSchema())) {
                return 1;
            } else if (AbstractAttrsWizardStep.this.reoderSchemas()) {
                int leftIndex = AbstractAttrsWizardStep.this.whichAttrs.indexOf(left.getSchema());
                int rightIndex = AbstractAttrsWizardStep.this.whichAttrs.indexOf(right.getSchema());

                if (leftIndex > rightIndex) {
                    return 1;
                } else if (leftIndex < rightIndex) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                return left.getSchema().compareTo(right.getSchema());
            }
        }
    }

    public static class Schemas extends Panel {

        private static final long serialVersionUID = -2447602429647965090L;

        public Schemas(final String id) {
            super(id);
        }
    }

    protected abstract class PlainSchemas<T> extends Schemas {

        private static final long serialVersionUID = 8315035592714180404L;

        public PlainSchemas(final String id) {
            super(id);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        protected AbstractFieldPanel<?> setPanel(
                final Map<String, PlainSchemaTO> schemas,
                final ListItem<Attr> item,
                final boolean setReadOnly) {

            Attr attr = item.getModelObject();
            PlainSchemaTO schema = schemas.get(attr.getSchema());

            AbstractFieldPanel<?> panel = getFieldPanel(schema);
            panel.setReadOnly(setReadOnly);
            if (mode != AjaxWizard.Mode.TEMPLATE
                    && schema.isMultivalue()
                    && schema.getType() != AttrSchemaType.Dropdown) {

                // SYNCOPE-1476 set form as multipart to properly manage membership attributes
                panel = new MultiFieldPanel.Builder<>(
                        new PropertyModel<>(attr, "values")).build(
                        "panel",
                        schema.getLabel(SyncopeConsoleSession.get().getLocale()),
                        FieldPanel.class.cast(panel)).setFormAsMultipart(true);
                // SYNCOPE-1215 the entire multifield panel must be readonly, not only its field
                MultiFieldPanel.class.cast(panel).setFormReadOnly(setReadOnly);
            } else if (panel instanceof AjaxPalettePanel ajaxPalettePanel) {
                ajaxPalettePanel.setModelObject(attr.getValues());
            } else {
                FieldPanel.class.cast(panel).setNewModel(attr.getValues());
            }
            item.add(panel);

            setExternalAction(attr, panel);

            return panel;
        }

        protected void setExternalAction(final Attr attr, final AbstractFieldPanel<?> panel) {
            if (previousObject == null) {
                return;
            }

            Optional<Attr> prevAttr = previousObject.getPlainAttr(attr.getSchema());
            if (prevAttr.map(a -> !ListUtils.isEqualList(
                    a.getValues().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList()),
                    attr.getValues().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList()))).
                    orElseGet(() -> attr.getValues().stream().anyMatch(StringUtils::isNotBlank))) {

                List<String> oldValues = prevAttr.map(Attr::getValues).orElseGet(List::of);
                panel.showExternAction(new LabelInfo("externalAction", oldValues));
            }
        }
    }
}
