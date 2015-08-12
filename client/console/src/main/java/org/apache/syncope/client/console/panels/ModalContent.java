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

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modal window with Resource form.
 */
public class ModalContent extends Panel {

    private static final long serialVersionUID = 8611724965544132636L;

    protected static final Logger LOG = LoggerFactory.getLogger(ModalContent.class);

    protected static final String CANCEL = "cancel";

    protected static final String SUBMIT = "submit";

    protected static final String APPLY = "apply";

    protected static final String FORM = "form";

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    protected NotificationPanel feedbackPanel;

    protected final PageReference pageRef;

    protected final ModalWindow window;

    public ModalContent(final ModalWindow window, final PageReference pageRef) {
        super(window.getContentId());
        this.pageRef = pageRef;
        this.window = window;

        feedbackPanel = new NotificationPanel(Constants.FEEDBACK);
        feedbackPanel.setOutputMarkupId(true);
        add(feedbackPanel);
    }

    public NotificationPanel getFeedbackPanel() {
        return feedbackPanel;
    }

    /**
     * Generic modal event.
     */
    public static class ModalEvent {

        /**
         * Request target.
         */
        private final AjaxRequestTarget target;

        /**
         * Constructor.
         *
         * @param target request target.
         */
        public ModalEvent(final AjaxRequestTarget target) {
            this.target = target;
        }

        /**
         * Target getter.
         *
         * @return request target.
         */
        public AjaxRequestTarget getTarget() {
            return target;
        }
    }
}
