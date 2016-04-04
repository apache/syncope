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
package org.apache.syncope.client.console.panels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.console.wicket.markup.html.list.ConnConfPropertyListView;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

public abstract class AbstractConnectorConfPanel<T extends AbstractBaseBean> extends Panel {

    private static final long serialVersionUID = -2025535531121434050L;

    protected final WebMarkupContainer propertiesContainer;

    protected final AjaxButton check;

    protected final IModel<T> model;

    public AbstractConnectorConfPanel(final String id, final IModel<T> model) {
        super(id, model);
        this.model = model;
        setOutputMarkupId(true);

        propertiesContainer = new WebMarkupContainer("connectorPropertiesContainer");
        propertiesContainer.setOutputMarkupId(true);
        add(propertiesContainer);

        check = new IndicatingAjaxButton("check", new ResourceModel("check")) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                check(target);
            }
        };
        propertiesContainer.add(check);
    }

    protected void setConfPropertyListView(final String modelExpression, final boolean withOverridable) {
        propertiesContainer.add(new ConnConfPropertyListView(
                "connectorProperties",
                new PropertyModel<List<ConnConfProperty>>(model.getObject(), modelExpression) {

            private static final long serialVersionUID = -7809699384012595307L;

            @Override
            public List<ConnConfProperty> getObject() {
                final List<ConnConfProperty> res = new ArrayList<>((Set<ConnConfProperty>) super.getObject());

                // re-order properties
                Collections.sort(res, new Comparator<ConnConfProperty>() {

                    @Override
                    public int compare(final ConnConfProperty left, final ConnConfProperty right) {
                        if (left == null) {
                            return -1;
                        } else {
                            return left.compareTo(right);
                        }
                    }
                });

                return res;
            }
        }, withOverridable).setOutputMarkupId(true));
    }

    protected abstract void check(final AjaxRequestTarget taget);

    protected abstract List<ConnConfProperty> getConnProperties(final T instance);
}
