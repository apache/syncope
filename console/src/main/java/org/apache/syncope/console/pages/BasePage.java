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
package org.apache.syncope.console.pages;

import org.apache.syncope.console.SyncopeApplication;
import org.apache.syncope.console.commons.Constants;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Syncope Wicket base-page.
 */
public class BasePage extends AbstractBasePage implements IAjaxIndicatorAware {

    private static final long serialVersionUID = 1571997737305598502L;

    public BasePage() {
        super();

        pageSetup();
    }

    public BasePage(final PageParameters parameters) {
        super(parameters);

        pageSetup();
    }

    private void pageSetup() {
        ((SyncopeApplication) getApplication()).setupNavigationPanel(this, xmlRolesReader, true);

        final String kind = getClass().getSimpleName().toLowerCase();
        final BookmarkablePageLink kindLink = (BookmarkablePageLink) get(kind);
        if (kindLink != null) {
            kindLink.add(new Behavior() {

                private static final long serialVersionUID = 1469628524240283489L;

                @Override
                public void onComponentTag(final Component component, final ComponentTag tag) {
                    tag.put("class", kind);
                }
            });

            Component kindIcon = kindLink.get(0);
            if (kindIcon != null) {
                kindIcon.add(new Behavior() {

                    private static final long serialVersionUID = 1469628524240283489L;

                    @Override
                    public void onComponentTag(final Component component, final ComponentTag tag) {
                        tag.put("src", "../.." + SyncopeApplication.IMG_PREFIX + kind + Constants.PNG_EXT);
                    }
                });
            }
        }

        ((SyncopeApplication) getApplication()).setupEditProfileModal(this, userRestClient);
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return "veil";
    }

    /**
     * Set a WindowClosedCallback for a ModalWindow instance.
     *
     * @param window window
     * @param container container
     */
    protected void setWindowClosedCallback(final ModalWindow window, final WebMarkupContainer container) {

        window.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                target.add(container);
                if (isModalResult()) {
                    info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(feedbackPanel);
                    setModalResult(false);
                }
            }
        });
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(new PriorityHeaderItem(meta));
    }
}
