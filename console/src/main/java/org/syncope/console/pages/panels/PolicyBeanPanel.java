/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages.panels;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.syncope.client.to.SchemaTO;
import org.syncope.console.commons.XMLRolesReader;
import org.syncope.console.rest.SchemaRestClient;
import org.syncope.console.wicket.markup.html.form.AbstractFieldPanel;
import org.syncope.console.wicket.markup.html.form.MultiValueSelectorPanel;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.console.wicket.markup.html.form.FieldPanel;
import org.syncope.types.AbstractPolicy;

public class PolicyBeanPanel extends Panel {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            PolicyBeanPanel.class);

    private static final long serialVersionUID = -3035998190456928143L;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    final IModel<List<String>> schemas =
            new LoadableDetachableModel<List<String>>() {

                private static final long serialVersionUID =
                        -2012833443695917883L;

                @Override
                protected List<String> load() {
                    final List<SchemaTO> schemaTOs;
                    schemaTOs = schemaRestClient.getSchemas("user");

                    final List<String> schemas = new ArrayList<String>();

                    for (SchemaTO schemaTO : schemaTOs) {
                        schemas.add(schemaTO.getName());
                    }

                    return schemas;
                }
            };

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    public PolicyBeanPanel(
            final String id, final AbstractPolicy policy) {
        super(id);

        FieldWrapper fieldWrapper = null;
        final List<FieldWrapper> items = new ArrayList<FieldWrapper>();


        for (Field field : policy.getClass().getDeclaredFields()) {
            if (!"serialVersionUID".equals(field.getName())) {
                fieldWrapper = new FieldWrapper();
                fieldWrapper.setName(field.getName());
                fieldWrapper.setType(field.getType());
                items.add(fieldWrapper);
            }
        }

        final ListView<FieldWrapper> policies = new ListView<FieldWrapper>("policies", items) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(ListItem<FieldWrapper> item) {

                final FieldWrapper field = item.getModelObject();

                item.add(new Label(
                        "label", new Model(getString(field.getName()))));

                final AbstractFieldPanel component;
                Method method;

                if (field.getType().equals(boolean.class)
                        || field.getType().equals(Boolean.class)) {

                    item.add(new AjaxCheckBoxPanel(
                            "check",
                            field.getName(),
                            new PropertyModel(policy, field.getName()),
                            false));

                    item.add(new Label("field", new Model(null)));

                } else if (field.getType().equals(List.class)
                        || field.getType().equals(Set.class)) {
                    try {

                        method = policy.getClass().getMethod(
                                "get" + StringUtils.capitalize(field.getName()),
                                new Class[]{});

                        if ("schemasNotPermitted".equals(field.getName())) {
                            component = new AjaxPalettePanel(
                                    "field",
                                    new PropertyModel(policy, field.getName()),
                                    new ListModel<String>(schemas.getObject()));

                            item.add(component);

                            item.add(getActivationControl(
                                    component,
                                    !((List) method.invoke(
                                    policy, new Object[]{})).isEmpty(),
                                    new ArrayList<String>(),
                                    new ArrayList<String>()));
                        } else {
                            final FieldPanel panel = new AjaxTextFieldPanel(
                                    "panel",
                                    field.getName(),
                                    new Model(null),
                                    true);

                            panel.setRequired(true);

                            component = new MultiValueSelectorPanel<String>(
                                    "field",
                                    new PropertyModel(policy, field.getName()),
                                    String.class,
                                    panel);

                            item.add(component);

                            final List<String> reinitializedValue =
                                    new ArrayList<String>();

                            reinitializedValue.add("");

                            item.add(getActivationControl(
                                    component,
                                    !((List<String>) method.invoke(
                                    policy, new Object[]{})).isEmpty(),
                                    (Serializable) new ArrayList<String>(),
                                    (Serializable) reinitializedValue));
                        }
                    } catch (Exception e) {
                        LOG.error("Error retrieving password policy fields", e);
                    }
                } else if (field.getType().equals(int.class)
                        || field.getType().equals(Integer.class)) {
                    try {

                        method = policy.getClass().getMethod(
                                "get" + StringUtils.capitalize(field.getName()),
                                new Class[]{});

                        component = new AjaxTextFieldPanel(
                                "field",
                                field.getName(),
                                new PropertyModel(policy, field.getName()),
                                false);

                        item.add(getActivationControl(
                                component,
                                (Integer) method.invoke(
                                policy, new Object[]{}) > 0,
                                0,
                                0));

                        item.add(component);
                    } catch (Exception e) {
                        LOG.error("Error retrieving password policy fields", e);
                    }
                } else {
                    item.add(new AjaxCheckBoxPanel(
                            "check", field.getName(), new Model(), false));
                    item.add(new Label("field", new Model(null)));
                }
            }
        };

        add(policies);
    }

    private <T extends Serializable> AjaxCheckBoxPanel getActivationControl(
            final AbstractFieldPanel panel,
            final Boolean checked,
            final T defaultModelObject,
            final T reinitializedValue) {

        final AjaxCheckBoxPanel check = new AjaxCheckBoxPanel(
                "check",
                "check",
                new Model(checked),
                false);

        panel.setEnabled(checked);

        check.getField().add(new AjaxFormComponentUpdatingBehavior("onChange") {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (check.getModelObject()) {
                    panel.setEnabled(true);
                    panel.setModelObject(reinitializedValue);
                } else {
                    panel.setModelObject(defaultModelObject);
                    panel.setEnabled(false);
                }

                target.addComponent(panel);
            }
        });

        return check;
    }

    private class FieldWrapper implements Serializable {

        private static final long serialVersionUID = -6770429509752964215L;

        private Class type;

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Class getType() {
            return type;
        }

        public void setType(Class type) {
            this.type = type;
        }
    }
}
