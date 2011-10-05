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
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

public class MultiValueSelectorPanel<E> extends AbstractFieldPanel {

    private static final long serialVersionUID = -6322397761456513324L;

    final ListView<E> view;

    final WebMarkupContainer container;

    public MultiValueSelectorPanel(
            final String id,
            final IModel<List<E>> model,
            final Class reference,
            final FieldPanel panelTemplate) {

        super(id, model);

        // -----------------------
        // Object container definition
        // -----------------------
        container = new WebMarkupContainer("multiValueContainer");
        container.setOutputMarkupId(true);
        add(container);
        // -----------------------

        view = new ListView<E>("view", model) {

            private static final long serialVersionUID = -9180479401817023838L;

            @Override
            protected void populateItem(final ListItem<E> item) {

                final FieldPanel fieldPanel = panelTemplate.clone();
                fieldPanel.setNewModel(item, reference);
                item.add(fieldPanel);

                AjaxLink minus = new IndicatingAjaxLink("drop") {

                    private static final long serialVersionUID =
                            -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        //Drop current component
                        model.getObject().remove(item.getModelObject());

                        target.add(container);
                    }
                };

                item.add(minus);

                if (model.getObject().size() <= 1) {
                    minus.setVisible(false);
                    minus.setEnabled(false);
                } else {
                    minus.setVisible(true);
                    minus.setEnabled(true);
                }

                if (item.getIndex() == model.getObject().size() - 1) {
                    final AjaxLink plus = new IndicatingAjaxLink("add") {

                        private static final long serialVersionUID =
                                -7978723352517770644L;

                        @Override
                        public void onClick(final AjaxRequestTarget target) {

                            //Add current component
                            model.getObject().add(null);

                            target.add(container);
                        }
                    };

                    final Fragment fragment = new Fragment(
                            "panelPlus", "fragmentPlus", container);

                    fragment.add(plus);
                    item.add(fragment);
                } else {
                    final Fragment fragment = new Fragment(
                            "panelPlus", "emptyFragment", container);
                    item.add(fragment);
                }
            }
        };

        container.add(view.setOutputMarkupId(true));
        setOutputMarkupId(true);
    }

    public ListView<E> getView() {
        return view;
    }

    public WebMarkupContainer getContainer() {
        return container;
    }

    @Override
    public MultiValueSelectorPanel<E> setModelObject(Serializable object) {
        view.setModelObject((List<E>) object);
        return this;
    }
}
