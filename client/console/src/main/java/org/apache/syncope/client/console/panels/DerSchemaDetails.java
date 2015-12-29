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
import org.apache.syncope.client.console.commons.JexlHelpUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;

public class DerSchemaDetails extends AbstractSchemaDetailsPanel {

    private static final long serialVersionUID = 6668789770131753386L;

    public DerSchemaDetails(final String id,
            final PageReference pageReference,
            final BaseModal<AbstractSchemaTO> modal) {
        super(id, pageReference, modal);

        final AjaxTextFieldPanel expression = new AjaxTextFieldPanel("expression", getString("expression"),
                new PropertyModel<String>(schemaTO, "expression"));
        expression.addRequiredLabel();
        schemaForm.add(expression);

        final WebMarkupContainer jexlHelp = JexlHelpUtils.getJexlHelpWebContainer("jexlHelp");

        final AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtils.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
        schemaForm.add(questionMarkJexlHelp);
        questionMarkJexlHelp.add(jexlHelp);
    }

    @Override
    public void getOnSubmit(final AjaxRequestTarget target,
            final BaseModal<?> modal, final Form<?> form, final PageReference pageReference, final boolean createFlag) {

        try {
            final DerSchemaTO updatedDerSchemaTO = DerSchemaTO.class.cast(form.getModelObject());
            if (createFlag) {
                schemaRestClient.createDerSchema(updatedDerSchemaTO);
            } else {
                schemaRestClient.updateDerSchema(updatedDerSchemaTO);
            }

            info(getString(Constants.OPERATION_SUCCEEDED));
            modal.close(target);
        } catch (Exception e) {
            LOG.error("While creating or updating derived schema", e);
            error(getString(Constants.ERROR) + ": " + e.getMessage());
            modal.getNotificationPanel().refresh(target);
        }
    }
}
