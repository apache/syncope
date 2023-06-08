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
package org.apache.syncope.client.console.tasks;

import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.StartAtTogglePanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class TaskStartAtTogglePanel extends StartAtTogglePanel {

    private static final long serialVersionUID = -3195479265440591519L;

    @SpringBean
    protected TaskRestClient taskRestClient;

    public TaskStartAtTogglePanel(final WebMarkupContainer container, final PageReference pageRef) {
        super(container, pageRef);

        form.add(new AjaxSubmitLink("dryRun", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    taskRestClient.startExecution(key, startAtDateModel.getObject(), true);
                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    toggle(target, false);
                    target.add(container);
                } catch (SyncopeClientException e) {
                    SyncopeConsoleSession.get().onException(e);
                    LOG.error("While running task {}", key, e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }

    @Override
    protected TaskRestClient getRestClient() {
        return taskRestClient;
    }
}
