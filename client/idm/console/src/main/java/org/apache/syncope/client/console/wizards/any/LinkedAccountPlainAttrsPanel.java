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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.commons.LinkedAccountPlainAttrProperty;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.EntityWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.LinkedAccountTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public class LinkedAccountPlainAttrsPanel extends AbstractAttrsWizardStep<PlainSchemaTO> {

    private static final long serialVersionUID = -6664931684253730934L;

    protected final LinkedAccountTO linkedAccountTO;

    protected final UserTO userTO;

    protected final Set<Attr> fixedAttrs = new HashSet<>();

    protected final List<LinkedAccountPlainAttrProperty> accountPlainAttrProperties = new ArrayList<>();

    public <T extends AnyTO> LinkedAccountPlainAttrsPanel(
            final EntityWrapper<LinkedAccountTO> modelObject,
            final UserTO userTO,
            final List<String> anyTypeClasses,
            final List<String> whichPlainAttrs) throws IllegalArgumentException {

        super(userTO,
                AjaxWizard.Mode.EDIT,
                anyTypeClasses,
                whichPlainAttrs);

        this.linkedAccountTO = modelObject.getInnerObject();
        this.fixedAttrs.addAll(this.linkedAccountTO.getPlainAttrs().stream().
                filter(attrTO -> checkIsReadonlyAttr(attrTO.getSchema())).
            toList());
        this.userTO = userTO;

        add(new Accordion("plainSchemas", List.of(new AbstractTab(
                new ResourceModel("attributes.accordion", "Plain Attributes")) {

            private static final long serialVersionUID = -7078941093668723016L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new PlainSchemasOwn(panelId, schemas, attrs);
            }
        }), Model.of(0)).setOutputMarkupId(true));
    }

    @Override
    protected FormComponent<?> checkboxToggle(
            final Attr attrTO,
            final AbstractFieldPanel<?> panel,
            final boolean isMultivalue) {

        LinkedAccountPlainAttrProperty property = accountPlainAttrProperties.stream().filter(
                existingProperty -> {
                    return existingProperty.getSchema().equals(attrTO.getSchema());
                }).findFirst().orElseGet(() -> {
                    LinkedAccountPlainAttrProperty newProperty = new LinkedAccountPlainAttrProperty();
                    newProperty.setOverridable(linkedAccountTO.getPlainAttr(attrTO.getSchema()).isPresent());
                    newProperty.setSchema(attrTO.getSchema());
                    newProperty.getValues().addAll(attrTO.getValues());
                    accountPlainAttrProperties.add(newProperty);
                    return newProperty;
                });

        final BootstrapToggleConfig config = new BootstrapToggleConfig().
                withOnStyle(BootstrapToggleConfig.Style.success).
                withOffStyle(BootstrapToggleConfig.Style.danger).
                withSize(BootstrapToggleConfig.Size.mini);

        return new BootstrapToggle("externalAction", new PropertyModel<>(property, "overridable"), config) {

            private static final long serialVersionUID = -875219845189261873L;

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = -1107858522700306810L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        if (isMultivalue) {
                            MultiFieldPanel.class.cast(panel).setFormReadOnly(!model.getObject());
                        } else {
                            FieldPanel.class.cast(panel).setReadOnly(!model.getObject());
                        }

                        updateAccountPlainSchemas(property, model.getObject());
                        target.add(panel);
                    }
                });
                return checkBox;
            }

            @Override
            protected IModel<String> getOnLabel() {
                return Model.of("Override");
            }

            @Override
            protected IModel<String> getOffLabel() {
                return Model.of("Override?");
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.append("class", "overridable", " ");
            }
        };
    }

    private void updateAccountPlainSchemas(final LinkedAccountPlainAttrProperty property, final Boolean modelObject) {
        Set<Attr> withoutCurrentSchema = new HashSet<>(linkedAccountTO.getPlainAttrs().stream().
                filter(attrTO -> !attrTO.getSchema().equals(property.getSchema())
                && checkIsReadonlyAttr(attrTO.getSchema())).
                collect(Collectors.toSet()));
        linkedAccountTO.getPlainAttrs().clear();
        linkedAccountTO.getPlainAttrs().addAll(withoutCurrentSchema);
        if (modelObject) {
            linkedAccountTO.getPlainAttrs().add(
                    fixedAttrs.stream().filter(attrTO -> attrTO.getSchema().equals(property.getSchema())).findFirst().
                            orElseGet(() -> new Attr.Builder(property.getSchema()).
                            values(property.getValues()).build()));
        }
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.PLAIN;
    }

    @Override
    protected void setAttrs() {
        List<Attr> attrs = new ArrayList<>();
        List<PlainSchemaTO> notReadonlyValues = schemas.values().stream().
                filter(schema -> checkIsReadonlyAttr(schema.getKey())).
                collect(Collectors.toList());
        setFixedAttr(notReadonlyValues);
        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(fixedAttrs);

        attrs.addAll(notReadonlyValues.stream().
                map(schema -> {
                    Attr attrTO = new Attr();
                    attrTO.setSchema(schema.getKey());
                    if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                        attrTO.getValues().add("");
                    } else {
                        attrTO = attrMap.get(schema.getKey());
                    }
                    return attrTO;
                }).
            toList());

        fixedAttrs.clear();
        fixedAttrs.addAll(attrs);
    }

    @Override
    protected List<Attr> getAttrsFromTO() {
        return fixedAttrs.stream().sorted(attrComparator).collect(Collectors.toList());
    }

    private void setFixedAttr(final Collection<PlainSchemaTO> values) {
        values.forEach(schema -> linkedAccountTO.getPlainAttr(schema.getKey()).ifPresentOrElse(
                fixedAttrs::add,
                () -> userTO.getPlainAttr(schema.getKey()).ifPresent(fixedAttrs::add)));
    }

    private boolean checkIsReadonlyAttr(final String schema) {
        return schemas.isEmpty() ? true : !schemas.get(schema).isReadonly();
    }

    private class PlainSchemasOwn extends PlainSchemas<List<Attr>> {

        private static final long serialVersionUID = -4730563859116024676L;

        PlainSchemasOwn(
                final String id,
                final Map<String, PlainSchemaTO> schemas,
                final IModel<List<Attr>> attrTOs) {

            super(id);

            add(new ListView<>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attrTO = item.getModelObject();
                    final boolean isMultivalue = schemas.get(attrTO.getSchema()).isMultivalue();

                    AbstractFieldPanel<?> panel = setPanel(
                            schemas,
                            item,
                            linkedAccountTO.getPlainAttr(attrTO.getSchema()).isEmpty());

                    panel.showExternAction(checkboxToggle(attrTO, panel, isMultivalue));
                }
            });
        }
    }
}
