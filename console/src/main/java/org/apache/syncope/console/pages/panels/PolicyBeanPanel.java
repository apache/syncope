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

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.annotation.SchemaList;
import org.apache.syncope.common.types.AbstractPolicySpec;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.ConflictResolutionAction;
import org.apache.syncope.console.markup.html.list.AltListView;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public class PolicyBeanPanel extends Panel {

    private static final long serialVersionUID = -3035998190456928143L;

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PolicyBeanPanel.class);

    @SpringBean
    private SchemaRestClient schemaRestClient;

    final IModel<List<String>> userSchemas = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = -2012833443695917883L;

        @Override
        protected List<String> load() {
            return schemaRestClient.getSchemaNames(AttributableType.USER);
        }
    };

    final IModel<List<String>> roleSchemas = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return schemaRestClient.getSchemaNames(AttributableType.ROLE);
        }
    };

    public PolicyBeanPanel(final String id, final AbstractPolicySpec policy) {
        super(id);

        final List<FieldWrapper> items = new ArrayList<FieldWrapper>();

        for (Field field : policy.getClass().getDeclaredFields()) {
            if (!"serialVersionUID".equals(field.getName())) {
                FieldWrapper fieldWrapper = new FieldWrapper();
                fieldWrapper.setName(field.getName());
                fieldWrapper.setType(field.getType());

                final SchemaList schemaList = field.getAnnotation(SchemaList.class);
                fieldWrapper.setSchemaList(schemaList);

                items.add(fieldWrapper);
            }
        }

        final ListView<FieldWrapper> policies = new AltListView<FieldWrapper>("policies", items) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<FieldWrapper> item) {

                final FieldWrapper field = item.getModelObject();

                final PropertyDescriptor propDesc = BeanUtils.getPropertyDescriptor(policy.getClass(), field.getName());

                item.add(new Label("label", new ResourceModel(field.getName())));

                final AbstractFieldPanel component;
                try {
                    if (field.getType().equals(ConflictResolutionAction.class)) {
                        component = new AjaxDropDownChoicePanel("field", field.getName(), new PropertyModel(policy,
                                field.getName()));

                        ((AjaxDropDownChoicePanel) component).setChoices(
                                Arrays.asList(ConflictResolutionAction.values()));

                        item.add(component);

                        item.add(getActivationControl(component,
                                (Enum<?>) propDesc.getReadMethod().invoke(policy, new Object[]{}) != null,
                                ConflictResolutionAction.IGNORE, ConflictResolutionAction.IGNORE));

                    } else if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
                        item.add(new AjaxCheckBoxPanel("check", field.getName(), new PropertyModel(policy,
                                field.getName())));

                        item.add(new Label("field", new Model(null)));
                    } else if (Collection.class.isAssignableFrom(field.getType())) {
                        if (field.getSchemaList() != null) {
                            final List<String> values = new ArrayList<String>();
                            if (field.getName().charAt(0) == 'r') {
                                values.addAll(roleSchemas.getObject());

                                if (field.getSchemaList().extended()) {
                                    values.add("name");
                                }
                            } else {
                                values.addAll(userSchemas.getObject());

                                if (field.getSchemaList().extended()) {
                                    values.add("id");
                                    values.add("username");
                                }
                            }

                            Collection collection =
                                    (Collection) propDesc.getReadMethod().invoke(policy, new Object[]{});
                            if (collection == null) {
                                collection = new ArrayList();
                                propDesc.getWriteMethod().invoke(policy, collection);
                            }

                            component = new AjaxPalettePanel("field", new PropertyModel(policy, field.getName()),
                                    new ListModel<String>(values));
                            item.add(component);

                            item.add(getActivationControl(component,
                                    !collection.isEmpty(), new ArrayList<String>(), new ArrayList<String>()));
                        } else {
                            final FieldPanel panel = new AjaxTextFieldPanel("panel", field.getName(),
                                    new Model<String>(null));
                            panel.setRequired(true);

                            component = new MultiValueSelectorPanel<String>("field",
                                    new PropertyModel(policy, field.getName()), panel);

                            item.add(component);

                            final List<String> reinitializedValue = new ArrayList<String>();

                            reinitializedValue.add("");

                            item.add(getActivationControl(component,
                                    !((Collection) propDesc.getReadMethod().invoke(policy, new Object[]{})).isEmpty(),
                                    new ArrayList<String>(), (Serializable) reinitializedValue));
                        }
                    } else if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                        component = new AjaxTextFieldPanel("field", field.getName(),
                                new PropertyModel(policy, field.getName()));

                        item.add(component);

                        item.add(getActivationControl(component,
                                (Integer) propDesc.getReadMethod().invoke(policy, new Object[]{}) > 0, 0, 0));
                    } else {
                        item.add(new AjaxCheckBoxPanel("check", field.getName(), new Model()));
                        item.add(new Label("field", new Model(null)));
                    }
                } catch (Exception e) {
                    LOG.error("Error retrieving policy fields", e);
                }
            }
        };

        add(policies);
    }

    private <T extends Serializable> AjaxCheckBoxPanel getActivationControl(final AbstractFieldPanel panel,
            final Boolean checked, final T defaultModelObject, final T reinitializedValue) {

        final AjaxCheckBoxPanel check = new AjaxCheckBoxPanel("check", "check", new Model(checked));

        panel.setEnabled(checked);

        check.getField().add(new AjaxFormComponentUpdatingBehavior("onchange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (check.getModelObject()) {
                    panel.setEnabled(true);
                    panel.setModelObject(reinitializedValue);
                } else {
                    panel.setModelObject(defaultModelObject);
                    panel.setEnabled(false);
                }

                target.add(panel);
            }
        });

        return check;
    }

    private static class FieldWrapper implements Serializable {

        private static final long serialVersionUID = -6770429509752964215L;

        private Class<?> type;

        private String name;

        private transient SchemaList schemaList;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Class<?> getType() {
            return type;
        }

        public void setType(final Class<?> type) {
            this.type = type;
        }

        public SchemaList getSchemaList() {
            return schemaList;
        }

        public void setSchemaList(final SchemaList schemaList) {
            this.schemaList = schemaList;
        }
    }
}
