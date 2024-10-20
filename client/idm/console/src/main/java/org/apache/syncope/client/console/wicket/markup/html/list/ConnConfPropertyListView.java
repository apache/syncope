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
package org.apache.syncope.client.console.wicket.markup.html.list;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.IdMConstants;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnConfPropertyListView extends ListView<ConnConfProperty> {

    private static final long serialVersionUID = -5239334900329150316L;

    private static final Logger LOG = LoggerFactory.getLogger(ConnConfPropertyListView.class);

    private final boolean withOverridable;

    public ConnConfPropertyListView(
            final String id,
            final IModel<? extends List<ConnConfProperty>> model,
            final boolean withOverridable) {

        super(id, model);
        this.withOverridable = withOverridable;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void populateItem(final ListItem<ConnConfProperty> item) {
        final ConnConfProperty property = item.getModelObject();

        final String label = StringUtils.isBlank(property.getSchema().getDisplayName())
                ? property.getSchema().getName() : property.getSchema().getDisplayName();

        final FieldPanel<? extends Serializable> field;
        boolean required = false;
        boolean isArray = false;

        if (property.getSchema().isConfidential()
                || IdMConstants.GUARDED_STRING.equalsIgnoreCase(property.getSchema().getType())
                || IdMConstants.GUARDED_BYTE_ARRAY.equalsIgnoreCase(property.getSchema().getType())) {

            field = new AjaxPasswordFieldPanel("panel", label, Model.of(), false);
            ((PasswordTextField) field.getField()).setResetPassword(false);

            required = property.getSchema().isRequired();
        } else {
            Class<?> propertySchemaClass;
            try {
                propertySchemaClass = ClassUtils.getClass(property.getSchema().getType());
                if (ClassUtils.isPrimitiveOrWrapper(propertySchemaClass)) {
                    propertySchemaClass = ClassUtils.primitiveToWrapper(propertySchemaClass);
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Error parsing attribute type", e);
                propertySchemaClass = String.class;
            }

            if (ClassUtils.isAssignable(Number.class, propertySchemaClass)) {
                field = new AjaxNumberFieldPanel.Builder<>().build(
                        "panel", label, AjaxNumberFieldPanel.cast(propertySchemaClass), new Model<>());
                required = property.getSchema().isRequired();
            } else if (ClassUtils.isAssignable(Boolean.class, propertySchemaClass)) {
                field = new AjaxCheckBoxPanel("panel", label, new Model<>());
            } else {
                field = new AjaxTextFieldPanel("panel", label, new Model<>());
                required = property.getSchema().isRequired();
            }

            if (propertySchemaClass.isArray()) {
                isArray = true;
            }
        }

        field.setIndex(item.getIndex());
        field.setTitle(property.getSchema().getHelpMessage(), true);

        final AbstractFieldPanel<? extends Serializable> fieldPanel;
        if (isArray) {
            final MultiFieldPanel multiFieldPanel = new MultiFieldPanel.Builder(
                    new PropertyModel<>(property, "values")).setEventTemplate(true).build("panel", label, field);
            item.add(multiFieldPanel);
            fieldPanel = multiFieldPanel;
        } else {
            setNewFieldModel(field, property.getValues());
            item.add(field);
            fieldPanel = field;
        }

        if (required) {
            fieldPanel.addRequiredLabel();
        }

        if (withOverridable) {
            fieldPanel.showExternAction(addCheckboxToggle(property));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setNewFieldModel(final FieldPanel field, final List<Object> values) {
        field.setNewModel(values);
    }

    private static FormComponent<?> addCheckboxToggle(final ConnConfProperty property) {
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

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (getModelObject().isEmpty()) {
            response.render(OnDomReadyHeaderItem.forScript(
                    String.format("$('#emptyPlaceholder').append(\"%s\")", getString("property.empty.list"))));
        }
    }
}
