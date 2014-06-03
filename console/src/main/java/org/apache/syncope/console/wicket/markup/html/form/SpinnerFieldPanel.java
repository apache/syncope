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
package org.apache.syncope.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.syncope.console.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.util.StringUtils;

public class SpinnerFieldPanel<T extends Number> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    private final Class<T> reference;

    private final IModel<T> model;

    private final T min;

    private final T max;

    @SuppressWarnings("unchecked")
    public SpinnerFieldPanel(final String id, final String name, final Class<T> reference, final IModel<T> model,
            final T min, final T max) {

        super(id, name, model);
        this.reference = reference;
        this.model = model;
        this.min = min;
        this.max = max;

        String uuid = UUID.randomUUID().toString();
        field = new TextField<T>("spinnerField", model, reference);
        field.setMarkupId(uuid);
        add(field.setLabel(new Model<String>(name)));

        if (!isReadOnly()) {
            field.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        final StringBuilder statements = new StringBuilder();
        statements.append("jQuery(function() {").
                append("var spinner = $('#").append(uuid).append("').spinner();").
                append("$('#").append(uuid).append("').spinner(").
                append("'option', 'stop', function(event, ui) { $(this).change(); });");
        if (this.min != null) {
            statements.
                    append("$('#").append(uuid).append("').spinner(").
                    append("'option', 'min', ").append(this.min).append(");");
        }
        if (this.max != null) {
            statements.
                    append("$('#").append(uuid).append("').spinner(").
                    append("'option', 'max', ").append(this.max).append(");");
        }
        statements.append("});");
        Label spinnerFieldJS = new Label("spinnerFieldJS", statements.toString());
        spinnerFieldJS.setEscapeModelStrings(false);
        add(spinnerFieldJS);
    }

    @Override
    public SpinnerFieldPanel<T> setNewModel(final List<Serializable> list) {
        setNewModel(new Model<T>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public T getObject() {
                T value = null;

                if (list != null && !list.isEmpty() && StringUtils.hasText(list.get(0).toString())) {
                    value = reference.equals(Integer.class)
                            ? reference.cast(NumberUtils.toInt(list.get(0).toString()))
                            : reference.equals(Long.class)
                            ? reference.cast(NumberUtils.toLong(list.get(0).toString()))
                            : reference.equals(Short.class)
                            ? reference.cast(NumberUtils.toShort(list.get(0).toString()))
                            : reference.equals(Float.class)
                            ? reference.cast(NumberUtils.toFloat(list.get(0).toString()))
                            : reference.equals(byte.class)
                            ? reference.cast(NumberUtils.toByte(list.get(0).toString()))
                            : reference.cast(NumberUtils.toDouble(list.get(0).toString()));
                }

                return value;
            }

            @Override
            public void setObject(final T object) {
                list.clear();
                if (object != null) {
                    list.add(object.toString());
                }
            }
        });

        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public SpinnerFieldPanel<T> setNewModel(final ListItem item) {
        field.setModel(new Model<T>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public T getObject() {
                T number = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        number = reference.equals(Integer.class)
                                ? reference.cast(Integer.valueOf((String) obj))
                                : reference.equals(Long.class)
                                ? reference.cast(Short.valueOf((String) obj))
                                : reference.equals(Short.class)
                                ? reference.cast(Long.valueOf((String) obj))
                                : reference.equals(Float.class)
                                ? reference.cast(Float.valueOf((String) obj))
                                : reference.equals(byte.class)
                                ? reference.cast(Byte.valueOf((String) obj))
                                : reference.cast(Double.valueOf((String) obj));
                    } else if (obj instanceof Number) {
                        // Don't parse anything
                        number = reference.cast(obj);
                    }
                }

                return number;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final T object) {
                item.setModelObject(object == null ? null : object.toString());
            }
        });

        return this;
    }

    @Override
    public SpinnerFieldPanel<T> clone() {
        SpinnerFieldPanel<T> panel = new SpinnerFieldPanel<T>(id, name, reference, model, min, max);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

}
