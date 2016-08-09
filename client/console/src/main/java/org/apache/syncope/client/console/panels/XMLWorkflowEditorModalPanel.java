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

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.io.IOUtils;

public class XMLWorkflowEditorModalPanel extends AbstractModalPanel<String> {

    private static final long serialVersionUID = 1937773326401753564L;

    private final WorkflowRestClient wfRestClient;

    private final TextArea<String> workflowDefArea;

    private String wfDefinition;

    public XMLWorkflowEditorModalPanel(
            final BaseModal<String> modal,
            final WorkflowRestClient wfRestClient,
            final PageReference pageRef) {

        super(modal, pageRef);
        this.wfRestClient = wfRestClient;

        try {
            wfDefinition = IOUtils.toString(wfRestClient.getDefinition(MediaType.APPLICATION_XML_TYPE));
        } catch (IOException e) {
            LOG.error("Could not get workflow definition", e);
            wfDefinition = StringUtils.EMPTY;
        }

        workflowDefArea = new TextArea<>("workflowDefArea", new Model<>(wfDefinition));
        add(workflowDefArea);
    }

    @Override
    public String getItem() {
        return this.wfDefinition;
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        try {
            wfRestClient.updateDefinition(MediaType.APPLICATION_XML_TYPE, workflowDefArea.getModelObject());
            SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));

            modal.show(false);
            modal.close(target);
        } catch (SyncopeClientException e) {
            SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.
                    getMessage());
        }
        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnLoadHeaderItem.forScript(
                "CodeMirror.fromTextArea(document.getElementById('workflowDefArea'), {"
                + "  lineNumbers: true, "
                + "  lineWrapping: true, "
                + "  autoCloseTags: true, "
                + "  mode: 'text/html', "
                + "  autoRefresh: true"
                + "}).on('change', updateTextArea);"));
    }

}
