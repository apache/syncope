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
import com.googlecode.wicket.kendo.ui.form.NumberTextField;
import com.googlecode.wicket.kendo.ui.resource.KendoCultureResourceReference;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.Attributable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public final class AjaxNumberFieldPanel<T extends Number & Comparable<T>> extends FieldPanel<T> {

    private static final long serialVersionUID = 6413819574530703577L;

    @SuppressWarnings("unchecked")
    public static <T extends Number & Comparable<T>> Class<T> cast(final Class<?> clazz) {
        return (Class<T>) clazz;
    }

    private final Class<T> reference;

    private final IModel<T> model;

    private final Options options;

    private final boolean enableOnChange;

    private final boolean convertValuesToString;

    private AjaxNumberFieldPanel(
            final String id,
            final String name,
            final Class<T> reference,
            final IModel<T> model,
            final Options options,
            final boolean enableOnChange,
            final boolean convertValuesToString) {

        super(id, name, model);

        this.reference = reference;
        this.model = model;
        this.options = options;
        this.enableOnChange = enableOnChange;
        this.convertValuesToString = convertValuesToString;

        field = new NumberTextField<>("numberTextField", model, reference, options);

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
    }

    @Override
    public FieldPanel<T> setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        NumberTextField.class.cast(field).setEnabled(!readOnly);
        return this;
    }

    @Override
    public AjaxNumberFieldPanel<T> setNewModel(final List<Serializable> list) {
        setNewModel(new Model<>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public T getObject() {
                T value = null;

                if (list != null && !list.isEmpty() && list.getFirst() != null
                    && !list.getFirst().toString().isEmpty()) {
                    value = reference.equals(Integer.class)
                            ? reference.cast(NumberUtils.toInt(list.getFirst().toString()))
                            : reference.equals(Long.class)
                            ? reference.cast(NumberUtils.toLong(list.getFirst().toString()))
                            : reference.equals(Short.class)
                            ? reference.cast(NumberUtils.toShort(list.getFirst().toString()))
                            : reference.equals(Float.class)
                            ? reference.cast(NumberUtils.toFloat(list.getFirst().toString()))
                            : reference.equals(byte.class)
                            ? reference.cast(NumberUtils.toByte(list.getFirst().toString()))
                            : reference.cast(NumberUtils.toDouble(list.getFirst().toString()));
                }

                return value;
            }

            @Override
            public void setObject(final T object) {
                list.clear();
                Optional.ofNullable(object).ifPresent(v -> list.add(convertValuesToString ? v.toString() : v));
            }
        });

        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public AjaxNumberFieldPanel<T> setNewModel(final ListItem item) {
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
                    } else if (obj instanceof final Number number1) {
                        number = reference.equals(Integer.class)
                                ? reference.cast(number1.intValue())
                                : reference.equals(Long.class)
                                ? reference.cast(number1.longValue())
                                : reference.equals(Short.class)
                                ? reference.cast(number1.shortValue())
                                : reference.equals(Float.class)
                                ? reference.cast(number1.floatValue())
                                : reference.equals(byte.class)
                                ? reference.cast(number1.byteValue())
                                : reference.cast(number1.doubleValue());
                    }
                }

                return number;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final T object) {
                item.setModelObject(Optional.ofNullable(object).
                        map(v -> convertValuesToString ? v.toString() : v).
                        orElse(null));
            }
        });

        return this;
    }

    @Override
    public FieldPanel<T> setNewModel(final Attributable attributable, final String schema) {
        field.setModel(new Model<>() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public T getObject() {
                return attributable.getPlainAttr(schema).map(Attr::getValues).filter(Predicate.not(List::isEmpty)).
                        map(values -> reference.equals(Integer.class)
                        ? reference.cast(NumberUtils.toInt(values.getFirst()))
                        : reference.equals(Long.class)
                        ? reference.cast(NumberUtils.toLong(values.getFirst()))
                        : reference.equals(Short.class)
                        ? reference.cast(NumberUtils.toShort(values.getFirst()))
                        : reference.equals(Float.class)
                        ? reference.cast(NumberUtils.toFloat(values.getFirst()))
                        : reference.equals(byte.class)
                        ? reference.cast(NumberUtils.toByte(values.getFirst()))
                        : reference.cast(NumberUtils.toDouble(values.getFirst()))).
                        orElse(null);
            }

            @Override
            public void setObject(final T object) {
                attributable.getPlainAttr(schema).ifPresent(plainAttr -> {
                    plainAttr.getValues().clear();
                    Optional.ofNullable(object).ifPresent(o -> plainAttr.getValues().add(o.toString()));
                });
            }
        });

        return this;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forReference(new KendoCultureResourceReference(getLocale())));
    }

    @Override
    public AjaxNumberFieldPanel<T> clone() {
        AjaxNumberFieldPanel<T> panel = new AjaxNumberFieldPanel<>(
                getId(), name, reference, model, options, enableOnChange, convertValuesToString);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

    public static class Builder<T extends Number & Comparable<T>> {

        private final Options options = new Options();

        private boolean enableOnChange = false;

        private boolean convertValuesToString = true;

        public Builder<T> min(final T min) {
            options.set("min", min);
            return this;
        }

        public Builder<T> max(final T max) {
            options.set("max", max);
            return this;
        }

        public Builder<T> format(final String format) {
            options.set("format", format);
            return this;
        }

        public Builder<T> step(final T step) {
            options.set("step", step);
            return this;
        }

        public Builder<T> options(final Options options) {
            options.entries().forEach(e -> this.options.set(e.getKey(), e.getValue()));
            return this;
        }

        public Builder<T> enableOnChange() {
            enableOnChange = true;
            return this;
        }

        public Builder<T> convertValuesToString(final boolean convertValuesToString) {
            this.convertValuesToString = convertValuesToString;
            return this;
        }

        public AjaxNumberFieldPanel<T> build(
                final String id,
                final String name,
                final Class<T> reference,
                final IModel<T> model) {

            if (options.entries().stream().noneMatch(e -> "decimals".equals(e.getKey()))) {
                options.set("decimals", "10");
            }

            if (options.entries().stream().noneMatch(o -> "format".equalsIgnoreCase(o.getKey()))) {
                if (reference.equals(Integer.class) || reference.equals(Long.class) || reference.equals(Short.class)) {
                    options.set("format", "'#'");
                } else {
                    options.set("format", "'#.##########'");
                }
            }
            if (options.entries().stream().noneMatch(o -> "step".equalsIgnoreCase(o.getKey()))) {
                if (reference.equals(Integer.class) || reference.equals(Long.class) || reference.equals(Short.class)) {
                    options.set("step", "1");
                } else {
                    options.set("step", "0.000000001");
                }
            }

            return new AjaxNumberFieldPanel<>(
                    id, name, reference, model, options, enableOnChange, convertValuesToString);
        }
    }
}
