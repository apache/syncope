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
package org.apache.syncope.client.ui.commons.panels;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.ui.commons.MapChoiceRenderer;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.form.FormProperty;
import org.apache.syncope.common.lib.form.FormPropertyValue;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeFormPanel<F extends SyncopeForm> extends Panel {

    private static final long serialVersionUID = -8847854414429745216L;

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeFormPanel.class);

    public SyncopeFormPanel(final String id, final F form) {
        super(id);

        IModel<List<FormProperty>> formProps = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 3169142472626817508L;

            @Override
            protected List<FormProperty> load() {
                return form.getProperties();
            }
        };

        ListView<FormProperty> propView = new ListView<>("propView", formProps) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            protected void populateItem(final ListItem<FormProperty> item) {
                FormProperty prop = item.getModelObject();

                String label = StringUtils.isBlank(prop.getName()) ? prop.getId() : prop.getName();

                FieldPanel field;
                switch (prop.getType()) {
                    case Boolean:
                        field = new AjaxDropDownChoicePanel("value", label, new PropertyModel<String>(prop, "value") {

                            private static final long serialVersionUID = -3743432456095828573L;

                            @Override
                            public String getObject() {
                                return StringUtils.isBlank(prop.getValue())
                                        ? null
                                        : prop.getValue().equals("true") ? "Yes" : "No";
                            }

                            @Override
                            public void setObject(final String object) {
                                prop.setValue(String.valueOf(object.equalsIgnoreCase("yes")));
                            }

                        }, false).setChoices(List.of("Yes", "No"));
                        break;

                    case Date:
                        FastDateFormat formatter = FastDateFormat.getInstance(prop.getDatePattern());
                        field = new AjaxDateTimeFieldPanel("value", label, new PropertyModel<>(prop, "value") {

                            private static final long serialVersionUID = -3743432456095828573L;

                            @Override
                            public Date getObject() {
                                try {
                                    return StringUtils.isBlank(prop.getValue())
                                            ? null
                                            : formatter.parse(prop.getValue());
                                } catch (ParseException e) {
                                    LOG.error("Unparsable date: {}", prop.getValue(), e);
                                    return null;
                                }
                            }

                            @Override
                            public void setObject(final Date object) {
                                prop.setValue(formatter.format(object));
                            }
                        }, formatter);
                        break;

                    case Enum:
                        field = new AjaxDropDownChoicePanel(
                                "value", label, new PropertyModel<String>(prop, "value"), false).
                                setChoiceRenderer(new MapChoiceRenderer(prop.getEnumValues().stream().
                                        collect(Collectors.toMap(
                                                FormPropertyValue::getKey,
                                                FormPropertyValue::getValue)))).
                                setChoices(prop.getEnumValues().stream().
                                        map(FormPropertyValue::getKey).collect(Collectors.toList()));
                        break;

                    case Dropdown:
                        field = new AjaxDropDownChoicePanel(
                                "value", label, new PropertyModel<String>(prop, "value"), false).
                                setChoiceRenderer(new MapChoiceRenderer(prop.getDropdownValues().stream().
                                        collect(Collectors.toMap(
                                                FormPropertyValue::getKey,
                                                FormPropertyValue::getValue)))).
                                setChoices(prop.getDropdownValues().stream().
                                        map(FormPropertyValue::getKey).collect(Collectors.toList()));
                        break;

                    case Long:
                        field = new AjaxSpinnerFieldPanel.Builder<Long>().build(
                                "value",
                                label,
                                Long.class,
                                new PropertyModel<>(prop, "value") {

                            private static final long serialVersionUID = -7688359318035249200L;

                            @Override
                            public Long getObject() {
                                return StringUtils.isBlank(prop.getValue())
                                        ? null
                                        : NumberUtils.toLong(prop.getValue());
                            }

                            @Override
                            public void setObject(final Long object) {
                                prop.setValue(String.valueOf(object));
                            }
                        });
                        break;

                    case Password:
                        field = new AjaxPasswordFieldPanel("value", label, new PropertyModel<>(prop, "value"), false);
                        break;

                    case String:
                    default:
                        field = new AjaxTextFieldPanel("value", label, new PropertyModel<>(prop, "value"), false);
                        break;
                }

                field.setReadOnly(!prop.isWritable());
                if (prop.isRequired()) {
                    field.addRequiredLabel();
                }

                item.add(field);
            }
        };

        add(propView);
    }
}
