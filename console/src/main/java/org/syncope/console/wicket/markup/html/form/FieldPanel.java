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
package org.syncope.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class FieldPanel<T extends Serializable>
        extends AbstractFieldPanel<T> {

    private static final long serialVersionUID = -198988924922541273L;

    protected FormComponent field = null;

    final protected boolean active;

    final protected String id;

    final protected String name;

    protected String title = null;

    protected boolean isRequiredLabelAdded = false;

    public FieldPanel(
            final String id,
            final String name,
            final IModel<T> model,
            final boolean active) {

        super(id, model);

        this.id = id;
        this.name = name;
        this.active = active;

        final Fragment fragment =
                new Fragment("required", "notRequiredFragment", this);

        add(fragment);

        setOutputMarkupId(true);
    }

    public FormComponent getField() {
        return field;
    }

    public FieldPanel setTitle(String title) {
        field.add(AttributeModifier.replace(
                "title", title != null ? title : ""));

        return this;
    }

    public FieldPanel setStyleShet(final String classes) {
        field.add(AttributeModifier.replace(
                "class", classes != null ? classes : ""));

        return this;
    }

    public FieldPanel setRequired(boolean required) {
        field.setRequired(required);

        return this;
    }

    public FieldPanel setReadOnly(boolean readOnly) {
        field.setEnabled(!readOnly);

        return this;
    }

    public FieldPanel setNewModel(final IModel<T> model) {
        field.setModel(model);
        return this;
    }

    @Override
    public FieldPanel setModelObject(T object) {
        field.setModelObject(object);
        return this;
    }

    public T getModelObject() {
        return (T) field.getModelObject();
    }

    public boolean isRequired() {
        return field.isRequired();
    }

    public boolean isReadOnly() {
        return !field.isEnabled();
    }

    /**
     * Userd by MultiValueSelectorPanel to attach items.
     * @param item item to attach.
     * @return updated FieldPanel object.
     */
    public FieldPanel setNewModel(
            final ListItem<T> item, final Class reference) {

        setNewModel(new Model() {

            private static final long serialVersionUID = 6799404673615637845L;

            @Override
            public Serializable getObject() {
                return item.getModelObject();
            }

            @Override
            public void setObject(final Serializable object) {
                item.setModelObject((T) object);
            }
        });
        return this;
    }

    public FieldPanel setNewModel(final List<Serializable> list) {
        setNewModel(new Model() {

            private static final long serialVersionUID = 1088212074765051906L;

            @Override
            public Serializable getObject() {
                return list != null && !list.isEmpty() ? list.get(0) : null;
            }

            @Override
            public void setObject(final Serializable object) {
                list.clear();

                if (object != null) {
                    list.add(object);
                }
            }
        });

        return this;
    }

    @Override
    public FieldPanel clone() {
        final FieldPanel panel;
        try {
            panel = this.getClass().getConstructor(new Class[]{
                        String.class,
                        String.class,
                        IModel.class,
                        boolean.class}).newInstance(
                    id, name, new Model(null), active);
        } catch (Exception e) {
            LOG.error("Error cloning field panel", e);
            return null;
        }

        panel.setRequired(isRequired());
        panel.setReadOnly(isReadOnly());
        panel.setTitle(title);

        if (isRequiredLabelAdded) {
            panel.addRequiredLabel();
        }

        return panel;
    }

    public FieldPanel addRequiredLabel() {
        if (!isRequired()) {
            setRequired(true);
        }

        final Fragment fragment =
                new Fragment("required", "requiredFragment", this);

        fragment.add(new Label("requiredLabel", "*"));

        replace(fragment);

        this.isRequiredLabelAdded = true;

        return this;
    }

    public FieldPanel removeRequiredLabel() {
        if (isRequired()) {
            setRequired(false);
        }

        final Fragment fragment =
                new Fragment("required", "notRequiredFragment", this);

        replace(fragment);

        this.isRequiredLabelAdded = false;

        return this;
    }
}
