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
package org.apache.syncope.client.enduser.pages;

import java.io.Serializable;
import java.util.List;
import org.apache.syncope.client.enduser.SyncopeEnduserSession;
import org.apache.syncope.client.enduser.SyncopeWebApplication;
import org.apache.syncope.client.enduser.commons.EnduserConstants;
import org.apache.syncope.client.enduser.init.ClassPathScanImplementationLookup;
import org.apache.syncope.client.enduser.panels.Sidebar;
import org.apache.syncope.client.enduser.wicket.markup.head.MetaHeaderItem;
import org.apache.syncope.client.ui.commons.BaseSession;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxCallListener;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class BasePage extends BaseWebPage {

    private static final long serialVersionUID = 1571997737305598502L;

    protected static final HeaderItem META_IE_EDGE = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    @SpringBean
    private ClassPathScanImplementationLookup lookup;

    protected final Sidebar sidebar;

    protected final WebMarkupContainer contentWrapper;

    protected final WebMarkupContainer navbar;

    protected final AjaxLink<Void> collapse;

    public BasePage() {
        this(null, null);
    }

    public BasePage(final PageParameters parameters, final String name) {
        super(parameters);

        Serializable leftMenuCollapse = SyncopeEnduserSession.get().getAttribute(Constants.MENU_COLLAPSE);
        if ((leftMenuCollapse instanceof final Boolean b) && b) {
            body.add(new AttributeAppender("class", " sidebar-collapse"));
        }

        // sidebar
        Class<? extends Sidebar> clazz = SyncopeWebApplication.get().getSidebar();

        try {
            sidebar = clazz.getConstructor(
                    String.class,
                    PageReference.class,
                    List.class).
                    newInstance("sidebar", getPageReference(), lookup.getExtPageClasses());
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not instantiate " + clazz.getName(), e);
        }

        sidebar.setOutputMarkupPlaceholderTag(true);
        body.add(sidebar);

        // contentWrapper
        contentWrapper = new WebMarkupContainer("contentWrapper");
        contentWrapper.setOutputMarkupPlaceholderTag(true);
        body.add(contentWrapper);

        navbar = new WebMarkupContainer("navbar");
        navbar.setOutputMarkupPlaceholderTag(true);
        navbar.setOutputMarkupId(true);
        body.add(navbar);

        //pageTitle
        addPageTitle(name);

        // collapse
        collapse = new AjaxLink<>("collapse") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                Session.get().setAttribute(Constants.MENU_COLLAPSE,
                        Session.get().getAttribute(Constants.MENU_COLLAPSE) == null
                        ? true
                        : !(Boolean) Session.get().getAttribute(Constants.MENU_COLLAPSE));
            }
        };
        collapse.setOutputMarkupPlaceholderTag(true);
        navbar.add(collapse);

        @SuppressWarnings("unchecked")
        Class<? extends WebPage> beforeLogout = (Class<? extends WebPage>) Session.get().
                getAttribute(Constants.BEFORE_LOGOUT_PAGE);
        if (beforeLogout == null) {
            navbar.add(new BookmarkablePageLink<>("logout", Logout.class));
        } else {
            navbar.add(new AjaxLink<Page>("logout") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                protected void updateAjaxAttributes(final AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);

                    AjaxCallListener ajaxCallListener = new AjaxCallListener();
                    ajaxCallListener.onPrecondition("return confirm('" + getString("confirmGlobalLogout") + "');");
                    attributes.getAjaxCallListeners().add(ajaxCallListener);
                }

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    setResponsePage(beforeLogout);
                }
            });
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(new PriorityHeaderItem(META_IE_EDGE));
    }

    protected void addPageTitle(final String title) {
        contentWrapper.addOrReplace(new Label(EnduserConstants.PAGE_TITLE, new ResourceModel(title, title)));
    }

    protected void disableSidebarAndNavbar() {
        sidebar.setVisible(false);
        collapse.setVisible(false);
        navbar.setVisible(false);
        contentWrapper.add(new AttributeModifier("style", "margin-left: 0px"));
    }

    protected void setDomain(final PageParameters parameters) {
        if (parameters != null && !parameters.get("domain").isEmpty()) {
            BaseSession.class.cast(Session.get()).setDomain(parameters.get("domain").toString());
        }
    }
}
