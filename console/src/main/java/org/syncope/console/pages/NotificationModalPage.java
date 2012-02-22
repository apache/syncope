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

import java.util.Arrays;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.syncope.client.to.NotificationTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.console.pages.panels.UserSearchPanel;
import org.syncope.console.rest.NotificationRestClient;
import org.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.syncope.types.TraceLevel;

class NotificationModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1975312550059578553L;

    @SpringBean
    private NotificationRestClient restClient;

    public NotificationModalPage(final PageReference callPageRef,
            final ModalWindow window,
            final NotificationTO notificationTO,
            final boolean createFlag) {

        Form form = new Form("form", new CompoundPropertyModel(
                notificationTO));
        form.setModel(new CompoundPropertyModel(notificationTO));

        final AjaxTextFieldPanel sender = new AjaxTextFieldPanel(
                "sender", getString("sender"),
                new PropertyModel<String>(notificationTO, "sender"), false);
        sender.addRequiredLabel();
        sender.addValidator(EmailAddressValidator.getInstance());
        form.add(sender);

        final AjaxTextFieldPanel subject = new AjaxTextFieldPanel(
                "subject", getString("subject"),
                new PropertyModel<String>(notificationTO, "subject"), false);
        subject.addRequiredLabel();
        form.add(subject);

        final AjaxDropDownChoicePanel<String> template =
                new AjaxDropDownChoicePanel<String>("template",
                getString("template"),
                new PropertyModel(notificationTO, "template"),
                false);
        template.setChoices(restClient.getMailTemplates());
        template.addRequiredLabel();
        form.add(template);

        final AjaxDropDownChoicePanel<TraceLevel> traceLevel =
                new AjaxDropDownChoicePanel<TraceLevel>("traceLevel",
                getString("traceLevel"),
                new PropertyModel(notificationTO, "traceLevel"),
                false);
        traceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        traceLevel.addRequiredLabel();
        form.add(traceLevel);

        final UserSearchPanel about = new UserSearchPanel("about",
                notificationTO.getAbout());
        form.add(about);

        final AjaxPalettePanel events = new AjaxPalettePanel(
                "events", new PropertyModel(notificationTO, "events"),
                new ListModel<String>(restClient.getEvents()));
        form.add(events);

        final UserSearchPanel recipients = new UserSearchPanel("recipients",
                notificationTO.getRecipients());
        form.add(recipients);

        final AjaxCheckBoxPanel selfAsRecipient = new AjaxCheckBoxPanel(
                "selfAsRecipient", getString("selfAsRecipient"),
                new PropertyModel(notificationTO, "selfAsRecipient"), false);
        form.add(selfAsRecipient);

        AjaxButton submit = new IndicatingAjaxButton(
                "apply", new Model<String>(getString("submit"))) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target,
                    final Form form) {

                notificationTO.setAbout(about.buildSearchCond());
                notificationTO.setRecipients(recipients.buildSearchCond());

                try {
                    if (createFlag) {
                        restClient.createNotification(notificationTO);
                    } else {
                        restClient.updateNotification(notificationTO);
                    }
                    info(getString("operation_succeded"));

                    Configuration callerPage =
                            (Configuration) callPageRef.getPage();
                    callerPage.setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException scee) {
                    error(getString("error") + ":" + scee.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target,
                    final Form form) {

                target.add(feedbackPanel);
            }
        };

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Notification", "create")
                : xmlRolesReader.getAllAllowedRoles("Notification", "update");
        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE,
                allowedRoles);

        form.add(submit);

        add(form);
    }
}
