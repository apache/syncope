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
package org.apache.syncope.client.console.wizards.mapping;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ItemTransformersTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    @SpringBean
    protected ImplementationRestClient implementationRestClient;

    protected Item item;

    public ItemTransformersTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super(Constants.OUTER, "itemTransformersTogglePanel", pageRef);

        final LoadableDetachableModel<List<String>> model = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<String> load() {
                // [!] this is required to disable changed with close button
                return item == null
                        ? List.of()
                        : item.getTransformers();
            }
        };

        Form<?> form = new Form<>("form");
        addInnerObject(form);

        List<String> choices = implementationRestClient.list(IdRepoImplementationType.ITEM_TRANSFORMER).stream().
                map(ImplementationTO::getKey).sorted().collect(Collectors.toList());

        form.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).setRenderer(new IChoiceRenderer<>() {

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
                new ListModel<>(choices)).
                hideLabel().setEnabled(true).setOutputMarkupId(true));

        form.add(new AjaxSubmitLink("submit", form) {

            private static final long serialVersionUID = 5538299138211283825L;

            @Override
            public void onSubmit(final AjaxRequestTarget target) {
                toggle(target, false);
                target.add(container);
            }

        });
    }

    public ItemTransformersTogglePanel setItem(final AjaxRequestTarget target, final Item item) {
        this.item = item;
        setHeader(target, StringUtils.EMPTY);
        return this;
    }
}
