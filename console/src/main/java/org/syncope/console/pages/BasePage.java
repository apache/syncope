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
import org.apache.wicket.ajax.IAjaxIndicatorAware;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syncope.console.SyncopeApplication;
import org.syncope.console.commons.XMLRolesReader;

/**
 * Syncope Wicket base-page.
 */
public class BasePage extends WebPage implements IAjaxIndicatorAware {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            BasePage.class);
    private static final long serialVersionUID = 1571997737305598502L;
    
    @SpringBean
    protected XMLRolesReader xmlRolesReader;
    
    @SpringBean(name = "version")
    private String version;
    
    protected FeedbackPanel feedbackPanel;
    /**
     * Response flag set by the Modal Window after the operation is completed.
     */
    protected boolean modalResult = false;

    public BasePage() {
        super();

        pageSetup();
    }

    /**
     * Constructor that is invoked when page is invoked without a
     * session.
     *
     * @param PageParameters parameters
     */
    public BasePage(final PageParameters parameters) {
        super(parameters);

        pageSetup();
    }

    private void pageSetup() {
        ((SyncopeApplication) getApplication()).setupNavigationPane(
                this, xmlRolesReader, version);

        feedbackPanel = new FeedbackPanel("feedback");
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);

        final String kind = getClass().getSimpleName().toLowerCase();
        final Component kindIcon = get(kind);
        if (kindIcon != null) {
            kindIcon.add(new Behavior() {

                @Override
                public void onComponentTag(final Component component,
                        final ComponentTag tag) {

                    tag.put("class", kind);
                }
            });
            add(kindIcon);
        }
    }

    @Override
    public String getAjaxIndicatorMarkupId() {
        return "veil";
    }

    public FeedbackPanel getFeedbackPanel() {
        return feedbackPanel;
    }

    public boolean isModalResult() {
        return modalResult;
    }

    public void setModalResult(boolean modalResult) {
        this.modalResult = modalResult;
    }
}
