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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.Attributable;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public abstract class FieldPanel<T extends Serializable> extends AbstractFieldPanel<T> implements Cloneable {

    private static final long serialVersionUID = -198988924922541273L;

    protected FormComponent<T> field;

    protected String title;

    private final Model<Integer> index = Model.of(0);

    public FieldPanel(final String id, final IModel<T> model) {
        this(id, id, model);
    }

    public FieldPanel(final String id, final String name, final IModel<T> model) {
        super(id, name, model);
    }

    public FormComponent<T> getField() {
        return field;
    }

    public FieldPanel<T> setPlaceholder(final String id) {
        field.add(new AttributeModifier("placeholder", new ResourceModel(id, id)));
        return this;
    }

    public FieldPanel<T> setTitle(final String title) {
        return setTitle(title, false);
    }

    public FieldPanel<T> setTitle(final String title, final boolean html) {
        this.title = title;
        field.add(new PopoverBehavior(
                Model.of(),
                Optional.ofNullable(title).map(Model::of).orElseGet(Model::of),
                new PopoverConfig().withHtml(html).withHoverTrigger().withPlacement(
                        index.getObject() != null && index.getObject() == 0
                        ? TooltipConfig.Placement.bottom
                        : this instanceof AjaxCheckBoxPanel
                                ? TooltipConfig.Placement.right
                                : TooltipConfig.Placement.top)));
        return this;
    }

    public FieldPanel<T> setStyleSheet(final String... classes) {
        return setStyleSheet(true, classes);
    }

    public FieldPanel<T> setStyleSheet(final boolean replace, final String... classes) {
        if (replace) {
            field.add(AttributeModifier.replace("class", StringUtils.join(classes, ' ')));
        } else {
            field.add(AttributeModifier.append("class", StringUtils.join(classes, ' ')));
        }
        return this;
    }

    @Override
    public FieldPanel<T> setRequired(final boolean required) {
        field.setRequired(required);
        return this;
    }

    @Override
    public FieldPanel<T> setReadOnly(final boolean readOnly) {
        field.setEnabled(!readOnly);
        return this;
    }

    @Override
    public boolean isRequired() {
        return field.isRequired();
    }

    public boolean isReadOnly() {
        return !field.isEnabled();
    }

    @Override
    public FieldPanel<T> setModelObject(final T object) {
        field.setModelObject(object);
        return this;
    }

    public T getModelObject() {
        return this.field.getModelObject();
    }

    public FieldPanel<T> setNewModel(final IModel<T> model) {
        field.setModel(model == null ? new Model<>() : model);
        return this;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public FieldPanel<T> setNewModel(final Attributable attributable, final String schema) {
        field.setModel(new Model() {

            private static final long serialVersionUID = -4214654722524358000L;

            @Override
            public Serializable getObject() {
                return attributable.getPlainAttr(schema).map(Attr::getValues).filter(Predicate.not(List::isEmpty)).
                        map(values -> values.getFirst()).
                        orElse(null);
            }

            @Override
            public void setObject(final Serializable object) {
                attributable.getPlainAttr(schema).ifPresent(plainAttr -> {
                    plainAttr.getValues().clear();
                    Optional.ofNullable(object).ifPresent(o -> plainAttr.getValues().add(o.toString()));
                });
            }
        });

        return this;
    }

    /**
     * Used by MultiFieldPanel to attach items (usually strings).
     * This method has to be overridden in case of type conversion is required.
     *
     * @param item item to attach.
     * @return updated FieldPanel object.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public FieldPanel<T> setNewModel(final ListItem item) {
        return setNewModel(new IModel() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Object getObject() {
                return item.getModelObject();
            }

            @Override
            public void setObject(final Object object) {
                item.setModelObject(object);
            }

            @Override
            public void detach() {
                // no detach
            }
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public FieldPanel<T> setNewModel(final List<Serializable> list) {
        return setNewModel(new Model() {

            private static final long serialVersionUID = 1088212074765051906L;

            @Override
            public Serializable getObject() {
                return list == null || list.isEmpty() ? null : list.getFirst();
            }

            @Override
            public void setObject(final Serializable object) {
                list.clear();

                if (object != null) {
                    list.add(object);
                }
            }
        });
    }

    public FieldPanel<T> setIndex(final int index) {
        this.index.setObject(index);
        return this;
    }

    public int getIndex() {
        return index.getObject();
    }

    /**
     * Override to add settings depending components.
     * It has to be used by default to add components depending by index model.
     *
     * @return the current field panel.
     */
    public FieldPanel<T> settingsDependingComponents() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public FieldPanel<T> clone() {
        final FieldPanel<T> panel = SerializationUtils.clone(this);
        panel.setModelObject(null);
        panel.addLabel();
        return panel;
    }

    protected PageReference getPageReference() {
        // SYNCOPE-1213
        // default implementation does not require to pass page reference, override this method of want otherwise
        return null;
    }
}
