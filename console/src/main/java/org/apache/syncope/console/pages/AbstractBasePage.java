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

import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.XMLRolesReader;
import org.apache.syncope.markup.head.MetaHeaderItem;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractBasePage extends WebPage {

    private static final long serialVersionUID = 8611724965544132636L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractBasePage.class);

    protected final HeaderItem meta = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    @SpringBean
    protected XMLRolesReader xmlRolesReader;

    protected FeedbackPanel feedbackPanel;

    /**
     * Response flag set by the Modal Window after the operation is completed.
     */
    protected boolean modalResult = false;

    public AbstractBasePage() {
        super();
        setupFeedbackPanel();
    }

    public AbstractBasePage(final PageParameters parameters) {
        super(parameters);
        setupFeedbackPanel();
    }

    protected final void setupFeedbackPanel() {
        feedbackPanel = new FeedbackPanel(Constants.FEEDBACK);
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);
    }

    public FeedbackPanel getFeedbackPanel() {
        return feedbackPanel;
    }

    public boolean isModalResult() {
        return modalResult;
    }

    public void setModalResult(final boolean operationResult) {
        this.modalResult = operationResult;
    }
}
