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

import java.io.Serializable;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class SchemaTypePanelWithSearch extends Panel {

    private static final long serialVersionUID = 433565065115855671L;

    private final SchemaTypePanel schemaTypePanel;

    public SchemaTypePanelWithSearch(final String id,
            final SchemaType schemaType,
            final PageReference pageReference) {
        super(id);

        schemaTypePanel = new SchemaTypePanel(id, schemaType, pageReference);

        addSearchBox();
        add(schemaTypePanel);
    }

    private void addSearchBox() {
        final Model<String> queryFilter = new Model<>(StringUtils.EMPTY);

        final WebMarkupContainer searchBoxContainer = new WebMarkupContainer("searchBox");
        final Form<?> form = new Form<>("form");
        final AjaxTextFieldPanel filter = new AjaxTextFieldPanel(
                "filter",
                "filter",
                queryFilter,
                false);
        filter.hideLabel().setOutputMarkupId(true);
        form.add(filter);

        form.add(new AjaxSubmitLink("search") {

            private static final long serialVersionUID = -1765773642975892072L;

            @Override
            protected void onAfterSubmit(final AjaxRequestTarget target, final Form<?> form) {
                super.onAfterSubmit(target, form);

                send(SchemaTypePanelWithSearch.this,
                        Broadcast.DEPTH,
                        new SchemaSearchEvent(target, queryFilter.getObject()));
            }
        });
        searchBoxContainer.add(form);

        add(searchBoxContainer);
    }

    public static class SchemaSearchEvent implements Serializable {

        private static final long serialVersionUID = -282052400565266028L;

        private final AjaxRequestTarget target;

        private final String keyword;

        public SchemaSearchEvent(final AjaxRequestTarget target, final String keyword) {
            this.target = target;
            this.keyword = keyword;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public String getKeyword() {
            return keyword;
        }

    }

}
