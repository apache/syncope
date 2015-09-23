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

import org.apache.syncope.client.console.init.MIMETypesLoader;
import org.apache.syncope.client.console.rest.ConfigurationRestClient;
import org.apache.syncope.client.console.rest.ConnectorRestClient;
import org.apache.syncope.client.console.rest.ReportRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.rest.UserSelfRestClient;
import org.apache.syncope.client.console.wicket.markup.head.MetaHeaderItem;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.HeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractModalPanel extends Panel {

    private static final long serialVersionUID = 8611724965544132636L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractModalPanel.class);

    protected final BaseModal<?> modal;

    protected static final String CANCEL = "cancel";

    protected static final String SUBMIT = "submit";

    protected static final String APPLY = "apply";

    protected static final String FORM = "form";

    protected final PageReference pageRef;

    protected final HeaderItem meta = new MetaHeaderItem("X-UA-Compatible", "IE=edge");

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected UserSelfRestClient userSelfRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected TaskRestClient taskRestClient;

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected ConnectorRestClient connectorRestClient;

    @SpringBean
    protected ReportRestClient reportRestClient;

    @SpringBean
    protected ConfigurationRestClient confRestClient;

    @SpringBean
    protected MIMETypesLoader mimeTypesInitializer;

    public AbstractModalPanel(final BaseModal<?> modal, final PageReference pageRef) {
        super(BaseModal.getContentId());
        this.pageRef = pageRef;
        this.modal = modal;
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(new PriorityHeaderItem(meta));
    }

    protected void closeAction(final AjaxRequestTarget target, final Form<?> form) {
        this.modal.close(target);
    }

    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        modal.getFeedbackPanel().refresh(target);
    }

    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        modal.getFeedbackPanel().refresh(target);
    }
}
