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

import jakarta.ws.rs.core.MediaType;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.rest.BpmnProcessRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class NewBpmnProcess extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -4886361549305302161L;

    @SpringBean
    protected BpmnProcessRestClient bpmnProcessRestClient;

    public NewBpmnProcess(final String id, final WebMarkupContainer container, final PageReference pageRef) {
        super(id, pageRef);

        Form<?> form = new Form<>("form");
        addInnerObject(form);

        TextField<String> key = new TextField<>("key", new Model<>());
        key.setRequired(true);
        form.add(key);

        form.add(new AjaxSubmitLink("submit", form) {

            private static final long serialVersionUID = 4947613489823025052L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    bpmnProcessRestClient.setDefinition(MediaType.APPLICATION_XML_TYPE, key.getModelObject(),
                            IOUtils.toString(
                                    NewBpmnProcess.class.getResourceAsStream("empty.bpmn20.xml"),
                                    StandardCharsets.UTF_8).replaceAll("%KEY%", key.getModelObject()));

                    key.getModel().setObject(null);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While creating new BPMN process", e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }
}
