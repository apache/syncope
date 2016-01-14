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
package org.apache.syncope.client.console.pages;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.io.IOUtils;

public class XMLEditorPopupPage extends BasePopupPage {

    private static final long serialVersionUID = 5816041644635271734L;

    private final WorkflowRestClient wfRestClient = new WorkflowRestClient();

    public XMLEditorPopupPage() {
        Form<?> wfForm = new Form<>("workflowDefForm");

        String definition;
        try {
            definition = IOUtils.toString(wfRestClient.getDefinition(MediaType.APPLICATION_XML_TYPE));
        } catch (IOException e) {
            LOG.error("Could not get workflow definition", e);
            definition = StringUtils.EMPTY;
        }
        final TextArea<String> workflowDefArea = new TextArea<>("workflowDefArea", new Model<>(definition));
        wfForm.add(workflowDefArea);

        AjaxButton submit = new IndicatingAjaxButton(APPLY, new Model<>(getString(SUBMIT))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    wfRestClient.updateDefinition(MediaType.APPLICATION_XML_TYPE, workflowDefArea.getModelObject());
                    info(getString(Constants.OPERATION_SUCCEEDED));
                } catch (SyncopeClientException scee) {
                    error(getString(Constants.ERROR) + ": " + scee.getMessage());
                }
                notificationPanel.refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                notificationPanel.refresh(target);
            }
        };

        final Button close = new Button("closePage", new Model<>(getString(CANCEL)));

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, StandardEntitlement.WORKFLOW_DEF_UPDATE);
        wfForm.add(submit);
        wfForm.add(close);
        this.add(wfForm);
    }

}
