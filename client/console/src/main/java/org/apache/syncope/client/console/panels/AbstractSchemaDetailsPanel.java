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

import static org.apache.syncope.client.console.panels.AbstractModalPanel.FORM;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.PropertyModel;

public abstract class AbstractSchemaDetailsPanel extends Panel {

    private static final long serialVersionUID = -9096843774956370327L;

    /**
     * Schema rest client for create and update operations
     */
    protected final SchemaRestClient schemaRestClient = new SchemaRestClient();

    /**
     * Feedback panel specified by the caller.
     */
    protected final NotificationPanel feedbackPanel;

    /**
     * Schema form
     */
    protected final Form<AbstractSchemaTO> schemaForm;

    protected final AbstractSchemaTO schemaTO;
    
    private final AbstractBasePage page;
    
    public AbstractSchemaDetailsPanel(
            final String id,
            final PageReference pageReference,
            final BaseModal<AbstractSchemaTO> modal) {
        super(id);

        this.page = (AbstractBasePage) pageReference.getPage();
        this.feedbackPanel = page.getFeedbackPanel();
        this.schemaTO = modal.getFormModel();

        schemaForm = new Form<>(FORM);
        schemaForm.setModel(new CompoundPropertyModel<>(schemaTO));
        schemaForm.setOutputMarkupId(true);

        final AjaxTextFieldPanel name =
                new AjaxTextFieldPanel("key", getString("key"), new PropertyModel<String>(schemaTO, "key"));
        name.addRequiredLabel();
        name.setEnabled(schemaTO.getKey() == null || schemaTO.getKey().isEmpty());

        schemaForm.add(name);
        add(schemaForm);
    }

    public abstract void getOnSubmit(final AjaxRequestTarget target, final BaseModal<?> modal, final Form<?> form,
            final PageReference pageReference, final boolean createFlag);
}
