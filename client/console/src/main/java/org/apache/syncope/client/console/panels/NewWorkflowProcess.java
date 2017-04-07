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

import java.io.Serializable;
import javax.ws.rs.core.MediaType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.util.Charsets;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.WorkflowRestClient;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;

public class NewWorkflowProcess extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -4886361549305302161L;

    private final WorkflowRestClient restClient = new WorkflowRestClient();

    private final Form<?> form;

    public NewWorkflowProcess(final String id, final WebMarkupContainer container, final PageReference pageRef) {
        super(id, pageRef);

        form = new Form<>("form");
        addInnerObject(form);

        final TextField<String> key = new TextField<>("key", new Model<String>());
        key.setRequired(true);
        form.add(key);

        form.add(new AjaxSubmitLink("submit", form) {

            private static final long serialVersionUID = 4947613489823025052L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    restClient.setDefinition(MediaType.APPLICATION_XML_TYPE, key.getModelObject(),
                            IOUtils.toString(
                                    getClass().getResourceAsStream("empty.bpmn20.xml"),
                                    Charsets.UTF_8.name()).replaceAll("%KEY%", key.getModelObject()));

                    key.getModel().setObject(null);
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While creating new workflow process", e);
                    SyncopeConsoleSession.get().error(
                            StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }
}
