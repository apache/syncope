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
package org.syncope.console.pages;

import java.util.ArrayList;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.PropertyModel;
import org.syncope.client.to.NotificationTaskTO;
import org.syncope.client.to.TaskTO;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;

public class NotificationTaskModalPage extends TaskModalPage {

    private static final long serialVersionUID = -4399606755452034216L;

    public NotificationTaskModalPage(final TaskTO taskTO) {
        super(taskTO);

        final AjaxTextFieldPanel sender = new AjaxTextFieldPanel(
                "sender", getString("sender"),
                new PropertyModel<String>(taskTO, "sender"), false);
        sender.setEnabled(false);
        profile.add(sender);

        final ListMultipleChoice<String> recipients =
                new ListMultipleChoice<String>("recipients",
                new ArrayList<String>(((NotificationTaskTO) taskTO).
                getRecipients()));
        recipients.setMaxRows(5);
        recipients.setEnabled(false);
        profile.add(recipients);

        final AjaxTextFieldPanel subject = new AjaxTextFieldPanel(
                "subject", getString("subject"),
                new PropertyModel<String>(taskTO, "subject"), false);
        subject.setEnabled(false);
        profile.add(subject);

        final TextArea<String> textBody = new TextArea<String>("textBody",
                new PropertyModel<String>(taskTO, "textBody"));
        textBody.setEnabled(false);
        profile.add(textBody);

        final TextArea<String> htmlBody = new TextArea<String>("htmlBody",
                new PropertyModel<String>(taskTO, "htmlBody"));
        htmlBody.setEnabled(false);
        profile.add(htmlBody);

        final AjaxTextFieldPanel traceLevel = new AjaxTextFieldPanel(
                "traceLevel", getString("traceLevel"),
                new PropertyModel<String>(taskTO, "traceLevel"), false);
        traceLevel.setEnabled(false);
        profile.add(traceLevel);
    }
}
