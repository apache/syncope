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
package org.apache.syncope.client.console.wizards.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;

public class ItemTransformersTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    private ItemTO item;

    public ItemTransformersTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super("outer", "itemTransformersTogglePanel", pageRef);

        final LoadableDetachableModel<List<String>> model = new LoadableDetachableModel<List<String>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                // [!] this is required to disable changed with close button
                return item == null
                        ? Collections.<String>emptyList()
                        : new ArrayList<>(item.getTransformerClassNames());
            }
        };

        Form<?> form = new Form<>("form");
        addInnerObject(form);

        form.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).setRenderer(new IChoiceRenderer<String>() {

            private static final long serialVersionUID = 3464376099975468136L;

            private static final int MAX_LENGTH = 50;

            @Override
            public Object getDisplayValue(final String object) {
                if (object.length() > MAX_LENGTH) {
                    return "..." + object.substring(object.length() - MAX_LENGTH);
                } else {
                    return object;
                }
            }

            @Override
            public String getIdValue(final String object, final int index) {
                return object;
            }

            @Override
            public String getObject(final String id, final IModel<? extends List<? extends String>> choices) {
                return id;
            }
        }).build(
                "classes",
                model,
                new ListModel<>(new ArrayList<>(SyncopeConsoleSession.get().getPlatformInfo().getItemTransformers()))).
                hideLabel().setEnabled(true).setOutputMarkupId(true));

        form.add(new AjaxSubmitLink("submit", form) {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                toggle(target, false);

                // [!] this is required to disable changed with close button
                item.getTransformerClassNames().clear();
                item.getTransformerClassNames().addAll(model.getObject());

                target.add(container);
            }

        });
    }

    public ItemTransformersTogglePanel setItem(final AjaxRequestTarget target, final ItemTO item) {
        this.item = item;
        setHeader(target, StringUtils.EMPTY);
        return this;
    }
}
