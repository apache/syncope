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
package org.apache.syncope.console.pages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.client.to.NotificationTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.console.pages.panels.UserSearchPanel;
import org.apache.syncope.console.rest.NotificationRestClient;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.types.AttributableType;
import org.apache.syncope.types.IntMappingType;
import org.apache.syncope.types.TraceLevel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.EmailAddressValidator;

class NotificationModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1975312550059578553L;

    /**
     * OnChange event name.
     */
    private static final String ON_CHANGE = "onchange";

    @SpringBean
    private NotificationRestClient restClient;

    @SpringBean
    private SchemaRestClient schemaRestClient;

    public NotificationModalPage(final PageReference pageRef, final ModalWindow window,
            final NotificationTO notificationTO, final boolean createFlag) {

        Form form = new Form("form", new CompoundPropertyModel(notificationTO));
        form.setModel(new CompoundPropertyModel(notificationTO));

        final AjaxTextFieldPanel sender = new AjaxTextFieldPanel("sender", getString("sender"),
                new PropertyModel<String>(notificationTO, "sender"));
        sender.addRequiredLabel();
        sender.addValidator(EmailAddressValidator.getInstance());
        form.add(sender);

        final AjaxTextFieldPanel subject = new AjaxTextFieldPanel("subject", getString("subject"),
                new PropertyModel<String>(notificationTO, "subject"));
        subject.addRequiredLabel();
        form.add(subject);

        final AjaxDropDownChoicePanel<String> template = new AjaxDropDownChoicePanel<String>("template",
                getString("template"), new PropertyModel(notificationTO, "template"));
        template.setChoices(restClient.getMailTemplates());
        template.addRequiredLabel();
        form.add(template);

        final AjaxDropDownChoicePanel<TraceLevel> traceLevel = new AjaxDropDownChoicePanel<TraceLevel>("traceLevel",
                getString("traceLevel"), new PropertyModel(notificationTO, "traceLevel"));
        traceLevel.setChoices(Arrays.asList(TraceLevel.values()));
        traceLevel.addRequiredLabel();
        form.add(traceLevel);

        final UserSearchPanel about = new UserSearchPanel("about", notificationTO.getAbout(), pageRef);
        form.add(about);

        final AjaxDropDownChoicePanel<IntMappingType> recipientAttrType = new AjaxDropDownChoicePanel<IntMappingType>(
                "recipientAttrType",
                new ResourceModel("recipientAttrType", "recipientAttrType").getObject(),
                new PropertyModel<IntMappingType>(notificationTO, "recipientAttrType"));

        final Set<IntMappingType> toBeFiltered = new HashSet<IntMappingType>();
        toBeFiltered.add(IntMappingType.SyncopeUserId);
        toBeFiltered.add(IntMappingType.Password);

        recipientAttrType.setChoices(new ArrayList<IntMappingType>(
                (Set<IntMappingType>) IntMappingType.getAttributeTypes(AttributableType.USER, toBeFiltered)));

        recipientAttrType.setRequired(true);
        form.add(recipientAttrType);

        final AjaxDropDownChoicePanel<String> recipientAttrName = new AjaxDropDownChoicePanel<String>(
                "recipientAttrName",
                new ResourceModel("recipientAttrName", "recipientAttrName").getObject(),
                new PropertyModel(notificationTO, "recipientAttrName"));

        recipientAttrName.setChoices(getSchemaNames(notificationTO.getRecipientAttrType()));
        recipientAttrName.setRequired(true);
        form.add(recipientAttrName);

        recipientAttrType.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                recipientAttrName.setChoices(getSchemaNames(notificationTO.getRecipientAttrType()));
                target.add(recipientAttrName);
            }
        });

        final AjaxPalettePanel events = new AjaxPalettePanel("events", new PropertyModel(notificationTO, "events"),
                new ListModel<String>(restClient.getEvents()));
        form.add(events);

        final WebMarkupContainer recipientsContainer = new WebMarkupContainer("recipientsContainer");
        recipientsContainer.setOutputMarkupId(true);

        form.add(recipientsContainer);

        final AjaxCheckBoxPanel selfAsRecipient = new AjaxCheckBoxPanel("selfAsRecipient",
                getString("selfAsRecipient"), new PropertyModel(notificationTO, "selfAsRecipient"));
        form.add(selfAsRecipient);

        if (createFlag) {
            selfAsRecipient.getField().setDefaultModelObject(Boolean.TRUE);
        }

        final AjaxCheckBoxPanel checkRecipients =
                new AjaxCheckBoxPanel("checkRecipients", "checkRecipients",
                new Model<Boolean>(notificationTO.getRecipients() == null ? false : true));
        recipientsContainer.add(checkRecipients);

        final UserSearchPanel recipients =
                new UserSearchPanel("recipients",
                notificationTO.getRecipients() == null ? null : notificationTO.getRecipients(),
                pageRef);
        recipientsContainer.add(recipients);
        recipients.setEnabled(checkRecipients.getModelObject());

        selfAsRecipient.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (!Boolean.valueOf(selfAsRecipient.getField().getValue())) {
                    checkRecipients.getField().setDefaultModelObject(Boolean.TRUE);
                    target.add(checkRecipients);
                    recipients.setEnabled(checkRecipients.getModelObject());
                    target.add(recipients);
                    target.add(recipientsContainer);
                }
            }
        });

        checkRecipients.getField().add(new AjaxFormComponentUpdatingBehavior(ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                if (!checkRecipients.getModelObject()) {
                    selfAsRecipient.getField().setDefaultModelObject(Boolean.TRUE);
                    target.add(selfAsRecipient);
                }
                recipients.setEnabled(checkRecipients.getModelObject());
                target.add(recipients);
                target.add(recipientsContainer);
            }
        });

        AjaxButton submit = new ClearIndicatingAjaxButton("apply", new Model<String>(getString("submit")), pageRef) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                notificationTO.setAbout(about.buildSearchCond());
                notificationTO.setRecipients(checkRecipients.getModelObject() ? recipients.buildSearchCond() : null);

                try {
                    if (createFlag) {
                        restClient.createNotification(notificationTO);
                    } else {
                        restClient.updateNotification(notificationTO);
                    }
                    info(getString("operation_succeded"));

                    Configuration callerPage = (Configuration) pageRef.getPage();
                    callerPage.setModalResult(true);

                    window.close(target);
                } catch (SyncopeClientCompositeErrorException scee) {
                    error(getString("error") + ":" + scee.getMessage());
                    target.add(feedbackPanel);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {

                target.add(feedbackPanel);
            }
        };

        String allowedRoles = createFlag
                ? xmlRolesReader.getAllAllowedRoles("Notification", "create")
                : xmlRolesReader.getAllAllowedRoles("Notification", "update");
        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, allowedRoles);

        form.add(submit);

        add(form);
    }

    private List<String> getSchemaNames(final IntMappingType type) {
        if (type == null) {
            return Collections.<String>emptyList();
        }

        switch (type) {
            case UserSchema:
                return schemaRestClient.getSchemaNames(AttributableType.USER);

            case UserDerivedSchema:
                return schemaRestClient.getDerivedSchemaNames(AttributableType.USER);

            case UserVirtualSchema:
                return schemaRestClient.getVirtualSchemaNames(AttributableType.USER);

            case Username:
                return Collections.singletonList("Username");

            default:
                return Collections.<String>emptyList();
        }
    }
}
