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

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class ConnConfPropertyListView extends AltListView<ConnConfProperty> {

    private static final long serialVersionUID = -5239334900329150316L;

    private static final Logger LOG = LoggerFactory.getLogger(ConnConfPropertyListView.class);

    private final boolean withOverridable;

    private final Set<ConnConfProperty> configuration;

    public ConnConfPropertyListView(final String id, final IModel<? extends List<ConnConfProperty>> model,
            final boolean withOverridable, final Set<ConnConfProperty> configuration) {

        super(id, model);
        this.configuration = configuration;
        this.withOverridable = withOverridable;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void populateItem(final ListItem<ConnConfProperty> item) {
        final ConnConfProperty property = item.getModelObject();

        final Label label = new Label("connPropAttrSchema",
                StringUtils.isBlank(property.getSchema().getDisplayName())
                        ? property.getSchema().getName()
                        : property.getSchema().getDisplayName());
        item.add(label);

        FieldPanel<? extends Serializable> field;
        boolean required = false;
        boolean isArray = false;

        if (property.getSchema().isConfidential()
                || Constants.GUARDED_STRING.equalsIgnoreCase(property.getSchema().getType())
                || Constants.GUARDED_BYTE_ARRAY.equalsIgnoreCase(property.getSchema().getType())) {

            field = new AjaxPasswordFieldPanel("panel",
                    label.getDefaultModelObjectAsString(), new Model<String>());
            ((PasswordTextField) field.getField()).setResetPassword(false);

            required = property.getSchema().isRequired();
        } else {
            Class<?> propertySchemaClass;
            try {
                propertySchemaClass = ClassUtils.forName(property.getSchema().getType(), ClassUtils.
                        getDefaultClassLoader());
                if (ClassUtils.isPrimitiveOrWrapper(propertySchemaClass)) {
                    propertySchemaClass = org.apache.commons.lang3.ClassUtils.primitiveToWrapper(propertySchemaClass);
                }
            } catch (Exception e) {
                LOG.error("Error parsing attribute type", e);
                propertySchemaClass = String.class;
            }

            if (ClassUtils.isAssignable(Number.class, propertySchemaClass)) {
                @SuppressWarnings("unchecked")
                final Class<Number> numberClass = (Class<Number>) propertySchemaClass;
                field = new SpinnerFieldPanel<Number>("panel",
                        label.getDefaultModelObjectAsString(), numberClass, new Model<Number>(), null, null);

                required = property.getSchema().isRequired();
            } else if (ClassUtils.isAssignable(Boolean.class, propertySchemaClass)) {
                field = new AjaxCheckBoxPanel("panel",
                        label.getDefaultModelObjectAsString(), new Model<Boolean>());
            } else {
                field = new AjaxTextFieldPanel("panel",
                        label.getDefaultModelObjectAsString(), new Model<String>());

                required = property.getSchema().isRequired();
            }

            if (propertySchemaClass.isArray()) {
                isArray = true;
            }
        }

        field.setTitle(property.getSchema().getHelpMessage());

        if (required) {
            field.addRequiredLabel();
        }

        if (isArray) {
            if (property.getValues().isEmpty()) {
                property.getValues().add(null);
            }

            final MultiFieldPanel multiFieldPanel = new MultiFieldPanel("panel", "connPropAttrSchema",
                    new PropertyModel<List<String>>(property, "values"), field);
            item.add(multiFieldPanel);
        } else {
            setNewFieldModel(field, property.getValues());
            item.add(field);
        }

        if (withOverridable) {
            item.add(new AjaxCheckBoxPanel("connPropAttrOverridable",
                    "connPropAttrOverridable", new PropertyModel<Boolean>(property, "overridable")));
        }

        configuration.add(property);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setNewFieldModel(final FieldPanel field, final List<Object> values) {
        field.setNewModel(values);
    }

}
