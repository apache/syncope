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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class SchemasPanel extends Panel {

    private static final long serialVersionUID = -1140213992451232279L;

    private static final String SEARCH_SUBMIT_LINK = "searchSubmitLink";

    private final PageReference pageReference;

    public SchemasPanel(final String id, final PageReference pageRef) {
        super(id);

        this.pageReference = pageRef;

        final Model<String> keywordModel = new Model<>(StringUtils.EMPTY);

        WebMarkupContainer searchBoxContainer = new WebMarkupContainer("searchBox");
        add(searchBoxContainer);

        final Form<?> form = new Form<>("form");
        searchBoxContainer.add(form);

        final AjaxTextFieldPanel searchPanel = new AjaxTextFieldPanel(
                "filter", "filter", keywordModel, true);
        form.add(searchPanel.hideLabel().setOutputMarkupId(true));

        final AjaxSubmitLink submitLink = new AjaxSubmitLink("search") {

            private static final long serialVersionUID = -1765773642975892072L;

            @Override
            protected void onAfterSubmit(final AjaxRequestTarget target) {
                super.onAfterSubmit(target);

                send(SchemasPanel.this, Broadcast.DEPTH,
                        new SchemaTypePanel.SchemaSearchEvent(target, keywordModel.getObject()));
            }
        };
        submitLink.setOutputMarkupId(true);
        submitLink.setMarkupId(SEARCH_SUBMIT_LINK);
        form.add(submitLink);

        searchPanel.getField().add(AttributeModifier.replace(
                "onkeydown",
                Model.of("if(event.keyCode == 13) {event.preventDefault();}")));

        searchPanel.getField().add(new AjaxEventBehavior("onkeydown") {

            private static final long serialVersionUID = -7133385027739964990L;

            @Override
            protected void onEvent(final AjaxRequestTarget target) {
                target.appendJavaScript("$('#" + SEARCH_SUBMIT_LINK + "').click();");
            }

            @Override
            protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);

                attributes.getAjaxCallListeners().add(new AjaxCallListener() {

                    private static final long serialVersionUID = 7160235486520935153L;

                    @Override
                    public CharSequence getPrecondition(final Component component) {
                        return "if (Wicket.Event.keyCode(attrs.event)  == 13) { return true; } else { return false; }";
                    }
                });
            }
        });

        Accordion accordion = new Accordion("accordionPanel", buildTabList());
        accordion.setOutputMarkupId(true);
        add(accordion);
    }

    private List<ITab> buildTabList() {
        List<ITab> tabs = new ArrayList<>();

        for (final SchemaType schemaType : SchemaType.values()) {
            tabs.add(new AbstractTab(new Model<>(schemaType.name())) {

                private static final long serialVersionUID = 1037272333056449378L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new SchemaTypePanel(panelId, schemaType, pageReference);
                }
            });
        }

        return tabs;
    }
}
