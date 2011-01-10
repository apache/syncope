/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.console.pages;

import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.SyncopeSession;
import org.syncope.console.SyncopeUser;
import org.syncope.console.commons.XMLRolesReader;

/**
 * Syncope Wicket base-page.
 */
public class BasePage extends WebPage {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            BasePage.class);

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    protected FeedbackPanel feedbackPanel;

    public BasePage() {
        pageSetup();
    }

    /**
     * Constructor that is invoked when page is invoked without a
     * sessadd(new BookmarkablePageLink("roles", Roles.class));ion.
     *
     * @param PageParameters parameters
     */
    public BasePage(final PageParameters parameters) {
        super(parameters);

        pageSetup();
    }

    private void pageSetup() {
        ((SyncopeApplication) getApplication()).setupNavigationPane(
                this, xmlRolesReader);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final String kind = getClass().getSimpleName().toLowerCase();
        Component kindIcon = get(kind);
        kindIcon.add(new AbstractBehavior() {

            @Override
            public void onComponentTag(final Component component,
                    final ComponentTag tag) {

                tag.put("class", kind);
            }
        });
        add(kindIcon);
    }

    /**
     * @return the current SyncopeSession
     */
    public SyncopeSession getSyncopeSession() {
        return (SyncopeSession) getSession();
    }

    /**
     * @return the current SyncopeUser logged-in from the session
     */
    public SyncopeUser getSyncopeUser() {
        return (SyncopeUser) getSyncopeSession().getUser();

    }
}
