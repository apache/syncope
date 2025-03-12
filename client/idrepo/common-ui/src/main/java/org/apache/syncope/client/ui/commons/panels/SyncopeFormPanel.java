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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.syncope.client.ui.commons.MapChoiceRenderer;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.form.FormProperty;
import org.apache.syncope.common.lib.form.FormPropertyValue;
import org.apache.syncope.common.lib.form.SyncopeForm;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.PatternValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SyncopeFormPanel<F extends SyncopeForm> extends Panel {

    private static final long serialVersionUID = -8847854414429745216L;

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeFormPanel.class);

    public SyncopeFormPanel(final String id, final F form) {
        super(id);

        ListModel<FormProperty> model = new ListModel<>(new ArrayList<>());
        model.getObject().addAll(form.getProperties());

        ListView<FormProperty> propView = new ListView<>("propView", model) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<FormProperty> item) {
                FormProperty prop = item.getModelObject();

                String label = StringUtils.isBlank(prop.getName()) ? prop.getId() : prop.getName();

                AbstractFieldPanel<?> field;
                switch (prop.getType()) {
                    case Boolean:
                        field = new AjaxCheckBoxPanel("value", label, new PropertyModel<Boolean>(prop, "value") {

                            private static final long serialVersionUID = -3743432456095828573L;

                            @Override
                            public Boolean getObject() {
                                return BooleanUtils.toBoolean(prop.getValue());
                            }

                            @Override
                            public void setObject(final Boolean object) {
                                prop.setValue(BooleanUtils.toStringTrueFalse(object));
                            }
                        }, false);
                        break;

                    case Date:
                        FastDateFormat formatter = StringUtils.isBlank(prop.getDatePattern())
                                ? FastDateFormat.getInstance()
                                : FastDateFormat.getInstance(prop.getDatePattern());

                        PropertyModel<Date> dateModel = new PropertyModel<>(prop, "value") {

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
                                Optional.ofNullable(object).ifPresent(date -> prop.setValue(formatter.format(date)));
                            }
                        };

                        if (StringUtils.containsIgnoreCase(prop.getDatePattern(), "H")) {
                            field = new AjaxDateTimeFieldPanel("value", label, dateModel, formatter);
                        } else {
                            field = new AjaxDateFieldPanel("value", label, dateModel, formatter);
                        }
                        break;

                    case Enum:
                        field = new AjaxDropDownChoicePanel<>(
                                "value", label, new PropertyModel<String>(prop, "value"), false).
                                setChoiceRenderer(new MapChoiceRenderer(prop.getEnumValues().stream().
                                        collect(Collectors.toMap(
                                                FormPropertyValue::getKey,
                                                FormPropertyValue::getValue)))).
                                setChoices(prop.getEnumValues().stream().map(FormPropertyValue::getKey).toList());
                        break;

                    case Dropdown:
                        if (prop.isDropdownFreeForm()) {
                            field = new AjaxTextFieldPanel("value", label, new PropertyModel<>(prop, "value"), false);
                            ((AjaxTextFieldPanel) field).setChoices(prop.getDropdownValues().stream().
                                    map(FormPropertyValue::getKey).toList());
                        } else if (prop.isDropdownSingleSelection()) {
                            field = new AjaxDropDownChoicePanel<>(
                                    "value", label, new PropertyModel<String>(prop, "value"), false).
                                    setChoiceRenderer(new MapChoiceRenderer(prop.getDropdownValues().stream().
                                            collect(Collectors.toMap(
                                                    FormPropertyValue::getKey,
                                                    FormPropertyValue::getValue)))).
                                    setChoices(prop.getDropdownValues().stream().
                                            map(FormPropertyValue::getKey).toList());
                        } else {
                            field = new AjaxPalettePanel.Builder<String>().setName(label).
                                    setRenderer(new MapChoiceRenderer(prop.getDropdownValues().stream().
                                            collect(Collectors.toMap(
                                                    FormPropertyValue::getKey,
                                                    FormPropertyValue::getValue)))).build(
                                    "value",
                                    new IModel<List<String>>() {

                                private static final long serialVersionUID = 1015030402166681242L;

                                @Override
                                public List<String> getObject() {
                                    return Optional.ofNullable(prop.getValue()).
                                            map(v -> List.of(v.split(";"))).
                                        orElseGet(List::of);
                                }

                                @Override
                                public void setObject(final List<String> object) {
                                    prop.setValue(Optional.ofNullable(object).
                                            map(v -> String.join(";", v)).
                                            orElse(null));
                                }
                            }, new ListModel<>(prop.getDropdownValues().stream().
                                            map(FormPropertyValue::getKey).toList()));
                        }
                        break;

                    case Long:
                        field = new AjaxNumberFieldPanel.Builder<Long>().build(
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
                        field = new AjaxPasswordFieldPanel("value", label, new PropertyModel<>(prop, "value"), false).
                                setResetPassword(false);
                        break;

                    case String:
                    default:
                        field = new AjaxTextFieldPanel("value", label, new PropertyModel<>(prop, "value"), false);
                        Optional.ofNullable(prop.getStringRegEx()).
                                ifPresent(re -> ((AjaxTextFieldPanel) field).addValidator(new PatternValidator(re)));
                        break;
                }

                field.setReadOnly(!prop.isWritable());
                if (prop.isRequired()) {
                    field.addRequiredLabel();
                }

                item.add(field);
            }
        };
        add(propView.setReuseItems(true));
    }
}
