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
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class UserRequestsPanel extends Panel {

    private static final long serialVersionUID = -896040867024301443L;

    public UserRequestsPanel(final String id, final DirectoryPanel<?, ?, ?, ?> directoryPanel) {
        super(id);

        Model<String> keywordModel = new Model<>(StringUtils.EMPTY);

        WebMarkupContainer searchBoxContainer = new WebMarkupContainer("searchBox");
        add(searchBoxContainer);

        Form<?> form = new Form<>("form");
        searchBoxContainer.add(form);

        AjaxTextFieldPanel filter = new AjaxTextFieldPanel("filter", "filter", keywordModel, true);
        form.add(filter.setPlaceholder("username / key").hideLabel().setOutputMarkupId(true));

        AjaxButton search = new AjaxButton("search") {

            private static final long serialVersionUID = 8390605330558248736L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                send(UserRequestsPanel.this, Broadcast.DEPTH,
                        new UserRequestSearchEvent(target, keywordModel.getObject()));
            }
        };
        search.setOutputMarkupId(true);
        form.add(search);
        form.setDefaultButton(search);

        add(directoryPanel);
    }

    public static class UserRequestSearchEvent implements Serializable {

        private static final long serialVersionUID = 5063826346823013424L;

        private final transient AjaxRequestTarget target;

        private final String keyword;

        UserRequestSearchEvent(final AjaxRequestTarget target, final String keyword) {
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
