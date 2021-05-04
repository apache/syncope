/*
 *  Copyright (C) 2020 Tirasa (info@tirasa.net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.syncope.client.enduser.pages;

import org.apache.syncope.client.enduser.BookmarkablePageLinkBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class SelfResult extends BasePage {

    private static final long serialVersionUID = 3804053409052140145L;

    private static final String RESULT_PAGE = "page.resultPage";

    @SuppressWarnings("unchecked")
    public SelfResult(final PageParameters parameters) {
        super(parameters, RESULT_PAGE);
        
        WebMarkupContainer content = new WebMarkupContainer("content");
        content.setOutputMarkupId(true);
        contentWrapper.add(content);
        Class page;
        try {
            page = Class.forName(parameters.get(Constants.LANDING_PAGE)
                    .toString("org.apache.syncope.client.enduser.pages.Login"));
        } catch (ClassNotFoundException ex) {
            LOG.debug("Landing page not found", ex);
            page = Login.class;
        }
        if (page.equals(Login.class)) {
            BookmarkablePageLink<WebPage> login =
                    new BookmarkablePageLink<>("login", Login.class);
            content.add(login.setOutputMarkupId(true));
            disableSidebar();
        } else {
            content.add(BookmarkablePageLinkBuilder.build("login", page));
        }

        content.add(new Label("resultTitle", parameters.get(Constants.NOTIFICATION_TITLE_PARAM).toString()));
        content.add(new Label("resultMessage", parameters.get(Constants.NOTIFICATION_MSG_PARAM).toString()));
        content.add(new Fragment("statusIcon",
                Constants.OPERATION_SUCCEEDED.equals(parameters.get(Constants.STATUS).toString())
                ? "successIcon" : "errorIcon", content));
    }
}
