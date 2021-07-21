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
package org.apache.syncope.client.ui.commons.markup.html.form;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.jquery.ui.JQueryUIBehavior;
import com.googlecode.wicket.jquery.ui.form.spinner.AjaxSpinner;
import com.googlecode.wicket.jquery.ui.form.spinner.SpinnerAdapter;
import com.googlecode.wicket.jquery.ui.form.spinner.SpinnerBehavior;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.common.lib.Attributable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public final class AjaxSpinnerFieldPanel<T extends Number> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    private final Class<T> reference;

    private final IModel<T> model;

    private final Options options;

    private SpinnerBehavior behavior;

    private AjaxSpinnerFieldPanel(
            final String id,
            final String name,
            final Class<T> reference,
            final IModel<T> model,
            final Options options,
            final boolean enableOnChange) {

        super(id, name, model);

        field = new AjaxSpinner<>("spinner", model, options, reference) {

            private static final long serialVersionUID = -3624755213720060594L;

            @Override
            public JQueryUIBehavior newWidgetBehavior(final String selector) {
                behavior = new SpinnerBehavior(selector, new SpinnerAdapter());
                behavior.setOptions(options);
                return behavior;
            }
        };

        if (enableOnChange && !isReadOnly()) {
            field.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }

        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));

        this.name = name;
        this.model = model;
        this.reference = reference;
        this.options = options;
    }

    @Override
    public AjaxSpinnerFieldPanel<T> setNewModel(final List<Serializable> list) {
        setNewModel(new Model<>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public T getObject() {
                T value = null;

                if (list != null && !list.isEmpty()
                    && list.get(0) != null && StringUtils.isNotBlank(list.get(0).toString())) {

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
    public AjaxSpinnerFieldPanel<T> setNewModel(final ListItem item) {
        field.setModel(new Model<>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public T getObject() {
                T number = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        try {
                            number = reference.equals(Integer.class)
                                ? reference.cast(Integer.valueOf((String) obj))
                                : reference.equals(Long.class)
                                ? reference.cast(Long.valueOf((String) obj))
                                : reference.equals(Short.class)
                                ? reference.cast(Short.valueOf((String) obj))
                                : reference.equals(Float.class)
                                ? reference.cast(Float.valueOf((String) obj))
                                : reference.equals(byte.class)
                                ? reference.cast(Byte.valueOf((String) obj))
                                : reference.cast(Double.valueOf((String) obj));
                        } catch (NumberFormatException e) {
                            LOG.error("While attempting to parse {}", obj, e);
                        }
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
                item.setModelObject(Optional.ofNullable(object).map(Object::toString).orElse(null));
            }
        });

        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public FieldPanel<T> setNewModel(final Attributable attributableTO, final String schema) {
        field.setModel(new Model() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public Serializable getObject() {
                List<String> values = attributableTO.getPlainAttr(schema).get().getValues();
                if (!values.isEmpty()) {
                    return reference.equals(Integer.class)
                            ? reference.cast(NumberUtils.toInt(values.get(0)))
                            : reference.equals(Long.class)
                            ? reference.cast(NumberUtils.toLong(values.get(0)))
                            : reference.equals(Short.class)
                            ? reference.cast(NumberUtils.toShort(values.get(0)))
                            : reference.equals(Float.class)
                            ? reference.cast(NumberUtils.toFloat(values.get(0)))
                            : reference.equals(byte.class)
                            ? reference.cast(NumberUtils.toByte(values.get(0)))
                            : reference.cast(NumberUtils.toDouble(values.get(0)));
                }
                return null;
            }

            @Override
            public void setObject(final Serializable object) {
                attributableTO.getPlainAttr(schema).get().getValues().clear();
                if (object != null) {
                    attributableTO.getPlainAttr(schema).get().getValues().add(object.toString());
                }
            }
        });

        return this;
    }

    @Override
    public AjaxSpinnerFieldPanel<T> clone() {
        AjaxSpinnerFieldPanel<T> panel = new AjaxSpinnerFieldPanel<>(getId(), name, reference, model, options, false);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

    @Override
    public FieldPanel<T> setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        AjaxSpinner.class.cast(field).setEnabled(!readOnly);
        options.set("disabled", readOnly);
        if (behavior != null) {
            behavior.setOptions(options);
        }
        return this;
    }

    public static class Builder<T extends Number> {

        private final Options options = new Options();

        private boolean enableOnChange = false;

        public Builder<T> min(final T min) {
            options.set("min", min);
            return this;
        }

        public Builder<T> max(final T max) {
            options.set("max", max);
            return this;
        }

        public Builder<T> step(final T step) {
            options.set("step", step);
            return this;
        }

        public Builder<T> enableOnChange() {
            enableOnChange = true;
            return this;
        }

        public AjaxSpinnerFieldPanel<T> build(
                final String id,
                final String name,
                final Class<T> reference,
                final IModel<T> model) {

            return new AjaxSpinnerFieldPanel<>(id, name, reference, model, options, enableOnChange);
        }
    }
}
