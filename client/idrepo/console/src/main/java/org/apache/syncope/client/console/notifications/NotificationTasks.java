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
package org.apache.syncope.client.console.notifications;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.TaskRestClient;
import org.apache.syncope.client.console.tasks.NotificationMailBodyDetails;
import org.apache.syncope.client.console.tasks.NotificationTaskDirectoryPanel;
import org.apache.syncope.client.console.tasks.TaskExecutionDetails;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.ui.commons.panels.ModalPanel;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class NotificationTasks extends Panel implements ModalPanel {

    private static final long serialVersionUID = 1066124171682570083L;

    @SpringBean
    protected TaskRestClient taskRestClient;

    public NotificationTasks(
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final PageReference pageReference) {

        this(null, anyTypeKind, entityKey, pageReference);
    }

    public NotificationTasks(
            final String notification,
            final PageReference pageReference) {
        this(notification, null, null, pageReference);
    }

    private NotificationTasks(
            final String notification,
            final AnyTypeKind anyTypeKind,
            final String entityKey,
            final PageReference pageRef) {
        super(BaseModal.CONTENT_ID);

        final MultilevelPanel mlp = new MultilevelPanel("tasks");
        add(mlp);

        mlp.setFirstLevel(
                new NotificationTaskDirectoryPanel(taskRestClient, notification, anyTypeKind, entityKey, mlp, pageRef) {

            private static final long serialVersionUID = -2195387360323687302L;

            @Override
            protected void viewTaskExecs(final NotificationTaskTO taskTO, final AjaxRequestTarget target) {
                mlp.next(
                        new StringResourceModel("task.view", this, new Model<>(Pair.of(null, taskTO))).getObject(),
                        new TaskExecutionDetails<>(taskTO, pageRef), target);
            }

            @Override
            protected void viewMailBody(
                    final MailTemplateFormat format, final String content, final AjaxRequestTarget target) {

                mlp.next(
                        new StringResourceModel("content", this).setParameters(format.name()).getObject(),
                        new NotificationMailBodyDetails(content), target);
            }
        });
    }
}
