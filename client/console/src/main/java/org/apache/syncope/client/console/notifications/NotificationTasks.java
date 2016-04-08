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

import java.io.Serializable;
import org.apache.syncope.client.console.panels.ModalPanel;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.tasks.NotificationTaskDirectoryPanel;
import org.apache.syncope.client.console.tasks.TaskExecutionDetails;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.NotificationTaskTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

public class NotificationTasks extends Panel implements ModalPanel<Serializable> {

    private static final long serialVersionUID = 1066124171682570083L;

    public NotificationTasks(
            final BaseModal<?> baseModal, final PageReference pageReference, final NotificationTO notificationTO) {
        super(BaseModal.CONTENT_ID);

        final MultilevelPanel mlp = new MultilevelPanel("tasks");
        add(mlp);

        mlp.setFirstLevel(new NotificationTaskDirectoryPanel(null, mlp, pageReference) {

            private static final long serialVersionUID = -2195387360323687302L;

            @Override
            protected void viewTask(final NotificationTaskTO taskTO, final AjaxRequestTarget target) {
                mlp.next("task.view", new TaskExecutionDetails<>(null, taskTO, pageReference), target);
            }
        });
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NotificationTO getItem() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
