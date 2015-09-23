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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class MultiFieldPanel<E extends Serializable> extends AbstractFieldPanel<List<E>> {

    private static final long serialVersionUID = -6322397761456513324L;

    private ListView<E> view;

    private final FieldPanel<E> panelTemplate;

    private final boolean eventTemplate;

    private WebMarkupContainer container;

    public MultiFieldPanel(
            final String id, final String name, final IModel<List<E>> model, final FieldPanel<E> panelTemplate) {
        this(id, name, model, panelTemplate, false);
    }

    public MultiFieldPanel(
            final String id,
            final String name,
            final IModel<List<E>> model,
            final FieldPanel<E> panelTemplate,
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
        // -----------------------

        if (model.getObject().isEmpty()) {
            container.addOrReplace(getNoDataFragment(model, name));
        } else {
            container.addOrReplace(getDataFragment(model, name));
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

                final FieldPanel<E> fieldPanel = panelTemplate.clone();

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

                final AjaxLink<Void> minus = new IndicatingAjaxLink<Void>("drop") {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        //Drop current component
                        model.getObject().remove(item.getModelObject());
                        fieldPanel.getField().clearInput();

                        if (model.getObject().isEmpty()) {
                            container.addOrReplace(getNoDataFragment(model, label));
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
        final AjaxLink<Void> plus = new IndicatingAjaxLink<Void>("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                //Add current component
                model.getObject().add(null);

                if (model.getObject().size() == 1) {
                    container.addOrReplace(getDataFragment(model, label));
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

    public WebMarkupContainer getContainer() {
        return container;
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
}
