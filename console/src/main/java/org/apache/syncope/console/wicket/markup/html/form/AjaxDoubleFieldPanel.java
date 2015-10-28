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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.console.SyncopeSession;
import org.apache.syncope.console.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class AjaxDoubleFieldPanel extends FieldPanel<Double> {

    private static final long serialVersionUID = 935151916638207380L;

    private static final Pattern ENGLISH_DOUBLE_PATTERN = Pattern.compile("\\d+\\.\\d+");

    private static final Pattern OTHER_DOUBLE_PATTERN = Pattern.compile("\\d+,\\d+");

    private final String name;

    private final Pattern pattern;

    private final DecimalFormat englishDf;

    private final DecimalFormat localeDf;

    public AjaxDoubleFieldPanel(
            final String id, final String name, final String conversionPattern, final IModel<Double> model) {

        super(id, model);

        this.name = name;

        this.pattern = SyncopeSession.get().getLocale().equals(Locale.ENGLISH)
                ? ENGLISH_DOUBLE_PATTERN
                : OTHER_DOUBLE_PATTERN;

        englishDf = new DecimalFormat();
        englishDf.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        if (StringUtils.isNotBlank(conversionPattern)) {
            englishDf.applyPattern(conversionPattern);
        }
        localeDf = new DecimalFormat();
        localeDf.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(SyncopeSession.get().getLocale()));
        if (StringUtils.isNotBlank(conversionPattern)) {
            localeDf.applyPattern(conversionPattern);
        }

        field = new TextField<Double>("doubleField", model) {

            private static final long serialVersionUID = -378877047108711669L;

            @Override
            protected void convertInput() {
                if (StringUtils.isNotBlank(getInput())) {
                    if (pattern.matcher(getInput()).matches()) {
                        Double value;
                        try {
                            value = localeDf.parse(getInput()).doubleValue();
                            setConvertedInput(value);
                        } catch (ParseException e) {
                            error(name + ": " + getString("textField.DoubleValidator"));
                        }
                    } else {
                        error(name + ": " + getString("textField.DoubleValidator"));
                    }
                }
            }
        };
        add(field.setLabel(new Model<String>(name)).setOutputMarkupId(true));

        if (!isReadOnly()) {
            field.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    // nothing to do
                }
            });
        }
    }

    @Override
    public FieldPanel<Double> addRequiredLabel() {
        if (!isRequired()) {
            setRequired(true);
        }

        this.isRequiredLabelAdded = true;

        return this;
    }

    @Override
    public FieldPanel<Double> setNewModel(final List<Serializable> list) {
        setNewModel(new Model<Double>() {

            private static final long serialVersionUID = 527651414610325237L;

            @Override
            public Double getObject() {
                Double value = null;

                if (list != null && !list.isEmpty()) {
                    try {
                        value = englishDf.parse(list.get(0).toString()).doubleValue();
                    } catch (ParseException e) {
                        error(name + ": " + getString("textField.DoubleValidator"));
                    }
                }

                return value;
            }

            @Override
            public void setObject(final Double object) {
                list.clear();
                if (object != null) {
                    list.add(englishDf.format(object));
                }
            }
        });

        return this;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public FieldPanel<Double> setNewModel(final ListItem item) {
        IModel<Double> model = new Model<Double>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Double getObject() {
                Double value = null;

                final Object obj = item.getModelObject();

                if (obj != null && !obj.toString().isEmpty()) {
                    if (obj instanceof String) {
                        try {
                            value = englishDf.parse(obj.toString()).doubleValue();
                        } catch (ParseException e) {
                            error(name + ": " + getString("textField.DoubleValidator"));
                        }
                    } else if (obj instanceof Double) {
                        // Don't parse anything
                        value = (Double) obj;
                    }
                }

                return value;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void setObject(final Double object) {
                item.setModelObject(englishDf.format(object));
            }
        };

        field.setModel(model);
        return this;
    }

}
