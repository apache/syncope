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

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormChoiceComponentUpdatingBehavior;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AttributableTO;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class AjaxTripleStateButtonPanel extends FieldPanel<Boolean> {

    private static final long serialVersionUID = 5433965057104106773L;

    protected final WebMarkupContainer unsetContainer;

    protected final Radio<Boolean> unsetOption;

    private final boolean enableOnChange;

    private final boolean convertValuesToString;

    public AjaxTripleStateButtonPanel(final String id, final String name, final IModel<Boolean> model) {
        this(id, name, model, true);
    }

    public AjaxTripleStateButtonPanel(
            final String id,
            final String name,
            final IModel<Boolean> model,
            final boolean enableOnChange) {

        this(id, name, model, enableOnChange, true);
    }

    private AjaxTripleStateButtonPanel(
            final String id,
            final String name,
            final IModel<Boolean> model,
            final boolean enableOnChange,
            final boolean convertValuesToString) {

        super(id, name, model);

        this.enableOnChange = enableOnChange;
        this.convertValuesToString = convertValuesToString;

        field = new RadioGroup<>("tripleStateField", model);
        field.setRenderBodyOnly(false);
        add(field.setLabel(new ResourceModel(name, name)).setOutputMarkupId(true));
        field.add(AttributeModifier.replace("aria-label", field.getLabel()));
        field.add(AttributeModifier.replace("aria-required", (IModel<String>) () -> Boolean.toString(isRequired())));

        Radio<Boolean> trueOption = new Radio<>("trueOption", Model.of(Boolean.TRUE));
        Radio<Boolean> falseOption = new Radio<>("falseOption", Model.of(Boolean.FALSE));
        field.add(trueOption.setOutputMarkupId(true), falseOption.setOutputMarkupId(true));

        WebMarkupContainer trueLabel = new WebMarkupContainer("trueLabel");
        trueLabel.add(AttributeModifier.replace("for", (IModel<String>) trueOption::getMarkupId));
        trueLabel.add(AttributeModifier.replace("title", new ResourceModel("tristate.true")));
        field.add(trueLabel);

        WebMarkupContainer falseLabel = new WebMarkupContainer("falseLabel");
        falseLabel.add(AttributeModifier.replace("for", (IModel<String>) falseOption::getMarkupId));
        falseLabel.add(AttributeModifier.replace("title", new ResourceModel("tristate.false")));
        field.add(falseLabel);

        unsetContainer = new WebMarkupContainer("unsetContainer");
        field.add(unsetContainer);

        unsetOption = new Radio<>("unsetOption", Model.of((Boolean) null));
        unsetContainer.add(unsetOption.setOutputMarkupId(true));

        WebMarkupContainer unsetLabel = new WebMarkupContainer("unsetLabel");
        unsetLabel.add(AttributeModifier.replace("for", (IModel<String>) unsetOption::getMarkupId));
        unsetLabel.add(AttributeModifier.replace("title", new ResourceModel("tristate.unset")));
        unsetContainer.add(unsetLabel);

        if (enableOnChange && !isReadOnly()) {
            field.add(new IndicatorAjaxFormChoiceComponentUpdatingBehavior() {

                private static final long serialVersionUID = -8888833255711609077L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    doUpdate(target);
                }
            });
        }
    }

    protected void doUpdate(final AjaxRequestTarget target) {
        // nothing to do
    }

    @Override
    public FieldPanel<Boolean> setRequired(final boolean required) {
        unsetContainer.setVisible(!required);
        unsetOption.setEnabled(!required);
        return super.setRequired(required);
    }

    @Override
    public FieldPanel<Boolean> setNewModel(final List<Serializable> values) {
        return setNewModel(new Model<>() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public Boolean getObject() {
                return values == null || values.isEmpty() || values.getFirst() == null
                        ? null : BooleanUtils.toBooleanObject(values.getFirst().toString());
            }

            @Override
            public void setObject(final Boolean object) {
                values.clear();
                if (object != null) {
                    values.add(convertValuesToString ? object.toString() : object);
                }
            }
        });
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public FieldPanel<Boolean> setNewModel(final ListItem item) {
        return setNewModel(new Model<>() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Boolean getObject() {
                Object value = item.getModelObject();
                return value == null ? null : BooleanUtils.toBooleanObject(value.toString());
            }

            @Override
            public void setObject(final Boolean object) {
                item.setModelObject(Optional.ofNullable(object).
                        map(value -> convertValuesToString ? value.toString() : value).orElse(null));
            }
        });
    }

    @Override
    public FieldPanel<Boolean> setNewModel(final AttributableTO attributable, final String schema) {
        return setNewModel(new Model<>() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public Boolean getObject() {
                return attributable.getPlainAttr(schema).map(Attr::getValues).filter(values -> !values.isEmpty()).
                        map(values -> BooleanUtils.toBooleanObject(values.getFirst())).orElse(null);
            }

            @Override
            public void setObject(final Boolean object) {
                attributable.getPlainAttr(schema).ifPresent(attr -> {
                    attr.getValues().clear();
                    if (object != null) {
                        attr.getValues().add(object.toString());
                    }
                });
            }
        });
    }

    @Override
    public FieldPanel<Boolean> clone() {
        AjaxTripleStateButtonPanel panel = new AjaxTripleStateButtonPanel(
                getId(), name, new Model<>(), enableOnChange, convertValuesToString);

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

    public static class Builder {

        private boolean enableOnChange = false;

        private boolean convertValuesToString = true;

        public Builder enableOnChange() {
            enableOnChange = true;
            return this;
        }

        public Builder convertValuesToString(final boolean convertValuesToString) {
            this.convertValuesToString = convertValuesToString;
            return this;
        }

        public AjaxTripleStateButtonPanel build(final String id, final String name, final IModel<Boolean> model) {
            return new AjaxTripleStateButtonPanel(id, name, model, enableOnChange, convertValuesToString);
        }
    }
}
