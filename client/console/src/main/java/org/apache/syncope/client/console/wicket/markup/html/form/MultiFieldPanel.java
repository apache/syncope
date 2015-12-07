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
package org.apache.syncope.client.console.wicket.markup.html.form;

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public final class MultiFieldPanel<E extends Serializable> extends AbstractFieldPanel<List<E>> {

    private static final long serialVersionUID = -6322397761456513324L;

    private ListView<E> view;

    private final FieldPanel<? extends Serializable> panelTemplate;

    private final boolean eventTemplate;

    private final WebMarkupContainer container;

    private final Form<?> form;

    private MultiFieldPanel(
            final String id,
            final String name,
            final IModel<List<E>> model,
            final FieldPanel<? extends Serializable> panelTemplate,
            final boolean eventTemplate) {

        super(id, name, model);

        this.panelTemplate = panelTemplate;
        this.eventTemplate = eventTemplate;

        // -----------------------
        // Object container definition
        // -----------------------
        container = new WebMarkupContainer("multiValueContainer");
        container.setOutputMarkupId(true);
        add(container);

        form = new Form<>("innerForm");
        container.add(form);
        // -----------------------

        if (model.getObject() != null && model.getObject().isEmpty()) {
            form.addOrReplace(getNoDataFragment(model, name));
        } else {
            form.addOrReplace(getDataFragment(model, name));
        }
    }

    private Fragment getNoDataFragment(final IModel<List<E>> model, final String label) {
        final Fragment fragment = new Fragment("content", "noDataFragment", MultiFieldPanel.this);
        fragment.add(new Label("field-label", new ResourceModel(label, label)));
        fragment.add(getPlusFragment(model, label));
        return fragment;
    }

    private Fragment getDataFragment(final IModel<List<E>> model, final String label) {
        final Fragment contentFragment = new Fragment("content", "dataFragment", MultiFieldPanel.this);

        view = new ListView<E>("view", model) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<E> item) {

                final FieldPanel<? extends Serializable> fieldPanel = panelTemplate.clone();

                if (eventTemplate) {
                    fieldPanel.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                        private static final long serialVersionUID = -1107858522700306810L;

                        @Override
                        protected void onUpdate(final AjaxRequestTarget target) {
                            send(getPage(), Broadcast.BREADTH, new MultiValueSelectorEvent(target));
                        }
                    });
                }

                fieldPanel.setNewModel(item);
                item.add(fieldPanel.hideLabel().setRenderBodyOnly(true));

                final AjaxSubmitLink minus = new AjaxSubmitLink("drop") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        //Drop current component
                        model.getObject().remove(item.getModelObject());
                        fieldPanel.getField().clearInput();

                        if (model.getObject().isEmpty()) {
                            form.addOrReplace(getNoDataFragment(model, label));
                        }

                        target.add(container);

                        if (eventTemplate) {
                            send(getPage(), Broadcast.BREADTH, new MultiValueSelectorEvent(target));
                        }
                    }
                };

                item.add(minus);

                final Fragment fragment;
                if (item.getIndex() == model.getObject().size() - 1) {
                    fragment = getPlusFragment(model, label);
                } else {
                    fragment = new Fragment("panelPlus", "emptyFragment", MultiFieldPanel.this);
                }

                item.add(fragment);
            }
        };

        contentFragment.add(view.setOutputMarkupId(true));

        return contentFragment;
    }

    private Fragment getPlusFragment(final IModel<List<E>> model, final String label) {
        final AjaxSubmitLink plus = new AjaxSubmitLink("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                //Add current component
                model.getObject().add(null);

                if (model.getObject().size() == 1) {
                    form.addOrReplace(getDataFragment(model, label));
                }

                target.add(container);
            }
        };

        final Fragment fragment = new Fragment("panelPlus", "fragmentPlus", MultiFieldPanel.this);
        fragment.add(plus);
        fragment.setRenderBodyOnly(true);

        return fragment;
    }

    public ListView<E> getView() {
        return view;
    }

    @Override
    public MultiFieldPanel<E> setModelObject(final List<E> object) {
        view.setModelObject(object);
        return this;
    }

    public static class MultiValueSelectorEvent {

        private final AjaxRequestTarget target;

        public MultiValueSelectorEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }
    }

    public static class Builder<E extends Serializable> {

        private final IModel<List<E>> model;

        private boolean plusAlwaysAvailable;

        private boolean minusAwaysAvailable;

        private boolean multivalue;

        private boolean eventTemplate = false;

        public Builder(final IModel<List<E>> model) {
            this.model = model;
        }

        /**
         * Show plus button icon to ad more elements.
         *
         * @param plusAlwaysAvailable whether plus button must always available
         * @return this instance, for fluent building
         */
        public Builder<E> setPlusAlwaysAvailable(final boolean plusAlwaysAvailable) {
            this.plusAlwaysAvailable = plusAlwaysAvailable;
            return this;
        }

        /**
         * Show minus button icon to remove elements included the latest one in order to empty the list explicitly.
         *
         * @param minusAwaysAvailable whether minus button must always available
         * @return this instance, for fluent building
         */
        public Builder<E> setMinusAwaysAvailable(final boolean minusAwaysAvailable) {
            this.minusAwaysAvailable = minusAwaysAvailable;
            return this;
        }

        /**
         * Set multivalue field.
         *
         * @param multivalue whether this field is multivalue
         * @return this instance, for fluent building
         */
        public Builder<E> setMultivalue(final boolean multivalue) {
            this.multivalue = multivalue;
            return this;
        }

        /**
         * Set on_change event in order to send MultiValueSelectorEvent to page.
         *
         * @see MultiValueSelectorEvent
         * @param eventTemplate whether this is an event template
         * @return this instance, for fluent building
         */
        public Builder<E> setEventTemplate(final boolean eventTemplate) {
            this.eventTemplate = eventTemplate;
            return this;
        }

        public MultiFieldPanel<E> build(final String id, final String name, final FieldPanel<E> panelTemplate) {
            return new MultiFieldPanel<>(id, name, model, panelTemplate, eventTemplate);
        }
    }
}
