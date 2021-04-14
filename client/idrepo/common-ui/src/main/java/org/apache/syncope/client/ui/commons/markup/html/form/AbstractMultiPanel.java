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

import java.util.List;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.markup.html.IndicatorAjaxSubmitLink;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.CollectionUtils;

public abstract class AbstractMultiPanel<INNER> extends AbstractFieldPanel<List<INNER>> {

    private static final long serialVersionUID = -6322397761456513324L;

    private final ListView<INNER> view;

    private final WebMarkupContainer container;

    private final Form<?> form;

    public AbstractMultiPanel(
            final String id,
            final String name,
            final IModel<List<INNER>> model) {

        super(id, name, model);

        // -----------------------
        // Object container definition
        // -----------------------
        container = new WebMarkupContainer("multiValueContainer");
        container.setOutputMarkupId(true);
        add(container);

        form = new Form<>("innerForm");
        form.setDefaultButton(null);
        container.add(form);
        // -----------------------

        view = new InnerView("view", name, model);

        List<INNER> obj = model.getObject();
        if (CollectionUtils.isEmpty(obj)) {
            form.addOrReplace(getNoDataFragment(model, name));
        } else {
            form.addOrReplace(getDataFragment());
        }
    }

    // SYNCOPE-1476
    public AbstractMultiPanel<INNER> setFormAsMultipart(final boolean multipart) {
        form.setMultiPart(multipart);
        return this;
    }

    private Fragment getNoDataFragment(final IModel<List<INNER>> model, final String label) {
        Fragment fragment = new Fragment("content", "noDataFragment", AbstractMultiPanel.this);
        fragment.add(new Label("field-label", new ResourceModel(label, label)));
        fragment.add(getPlusFragment(model));
        return fragment;
    }

    private Fragment getDataFragment() {
        Fragment contentFragment = new Fragment("content", "dataFragment", AbstractMultiPanel.this);
        contentFragment.add(view.setOutputMarkupId(true));
        return contentFragment;
    }

    private Fragment getPlusFragment(final IModel<List<INNER>> model) {
        IndicatorAjaxSubmitLink plus = new IndicatorAjaxSubmitLink("add") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                //Add current component
                model.getObject().add(newModelObject());

                if (model.getObject().size() == 1) {
                    form.addOrReplace(getDataFragment());
                }

                target.add(container);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                sendError(getString(Constants.OPERATION_ERROR));
                super.onError(target);
                ((BaseWebPage) getPage()).getNotificationPanel().refresh(target);
            }

        };

        Fragment fragment = new Fragment("panelPlus", "fragmentPlus", AbstractMultiPanel.this);
        fragment.addOrReplace(plus);
        fragment.setRenderBodyOnly(true);

        return fragment;
    }

    public ListView<INNER> getView() {
        return view;
    }

    @Override
    public AbstractMultiPanel<INNER> setModelObject(final List<INNER> object) {
        view.setModelObject(object);
        return this;
    }

    protected abstract INNER newModelObject();

    protected abstract void sendError(String message);

    private final class InnerView extends ListView<INNER> {

        private static final long serialVersionUID = -9180479401817023838L;

        private final String label;

        private final IModel<List<INNER>> model;

        private InnerView(final String id, final String label, final IModel<List<INNER>> model) {
            super(id, model);
            this.label = label;
            this.model = model;
        }

        @Override
        protected void populateItem(final ListItem<INNER> item) {
            Panel panel = getItemPanel(item);

            item.add(panel.setRenderBodyOnly(true));

            IndicatorAjaxSubmitLink minus = new IndicatorAjaxSubmitLink("drop") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                protected void onSubmit(final AjaxRequestTarget target) {
                    //Drop current component
                    model.getObject().remove(item.getModelObject());
                    clearInput(panel);

                    if (model.getObject().isEmpty()) {
                        form.addOrReplace(getNoDataFragment(model, label));
                    }

                    target.add(container);
                }

                @Override
                protected void onError(final AjaxRequestTarget target) {
                    onSubmit(target);
                }
            };

            item.add(minus);

            Fragment fragment = item.getIndex() == model.getObject().size() - 1
                    ? getPlusFragment(model)
                    : new Fragment("panelPlus", "emptyFragment", AbstractMultiPanel.this);

            item.add(fragment.setRenderBodyOnly(true));
        }
    }

    protected abstract Panel getItemPanel(ListItem<INNER> item);

    protected void clearInput(final Panel panel) {
        // do nothing by default
    }

    @Override
    public AbstractFieldPanel<List<INNER>> setReadOnly(final boolean readOnly) {
        container.setEnabled(!readOnly);
        return this;
    }

    public AbstractFieldPanel<List<INNER>> setFormReadOnly(final boolean readOnly) {
        form.setEnabled(!readOnly);
        return this;
    }
}
