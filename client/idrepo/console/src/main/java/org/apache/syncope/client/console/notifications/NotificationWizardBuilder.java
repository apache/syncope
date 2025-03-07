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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.events.EventCategory;
import org.apache.syncope.client.console.events.EventCategoryPanel;
import org.apache.syncope.client.console.panels.search.AbstractSearchPanel;
import org.apache.syncope.client.console.panels.search.AnyObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ImplementationRestClient;
import org.apache.syncope.client.console.rest.NotificationRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiPanel;
import org.apache.syncope.client.console.wizards.BaseAjaxWizardBuilder;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.MailTemplateTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.wizard.WizardModel;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.validation.validator.EmailAddressValidator;

public class NotificationWizardBuilder extends BaseAjaxWizardBuilder<NotificationWrapper> {

    private static final long serialVersionUID = -1975312550059578553L;

    protected final NotificationRestClient notificationRestClient;

    protected final AnyTypeRestClient anyTypeRestClient;

    protected final ImplementationRestClient implementationRestClient;

    protected final SchemaRestClient schemaRestClient;

    protected final IModel<List<EventCategory>> eventCategories;

    public NotificationWizardBuilder(
            final NotificationTO notificationTO,
            final NotificationRestClient notificationRestClient,
            final AnyTypeRestClient anyTypeRestClient,
            final ImplementationRestClient implementationRestClient,
            final SchemaRestClient schemaRestClient,
            final IModel<List<EventCategory>> eventCategories,
            final PageReference pageRef) {

        super(new NotificationWrapper(notificationTO), pageRef);

        this.notificationRestClient = notificationRestClient;
        this.anyTypeRestClient = anyTypeRestClient;
        this.implementationRestClient = implementationRestClient;
        this.schemaRestClient = schemaRestClient;
        this.eventCategories = eventCategories;
    }

    @Override
    protected Serializable onApplyInternal(final NotificationWrapper modelObject) {
        modelObject.fillRecipientConditions();
        modelObject.fillAboutConditions();

        final boolean createFlag = modelObject.getInnerObject().getKey() == null;
        if (createFlag) {
            notificationRestClient.create(modelObject.getInnerObject());
        } else {
            notificationRestClient.update(modelObject.getInnerObject());
        }

        return null;
    }

    @Override
    protected WizardModel buildModelSteps(final NotificationWrapper modelObject, final WizardModel wizardModel) {
        wizardModel.add(new Details(modelObject));
        wizardModel.add(new Recipients(modelObject));
        wizardModel.add(new Events(modelObject));
        wizardModel.add(new Abouts(modelObject));
        return wizardModel;
    }

    public class Details extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        public Details(final NotificationWrapper modelObject) {
            NotificationTO notificationTO = modelObject.getInnerObject();
            boolean createFlag = notificationTO.getKey() == null;

            AjaxTextFieldPanel sender = new AjaxTextFieldPanel("sender", getString("sender"),
                    new PropertyModel<>(notificationTO, "sender"));
            sender.addRequiredLabel();
            sender.addValidator(EmailAddressValidator.getInstance());
            add(sender);

            AjaxTextFieldPanel subject = new AjaxTextFieldPanel("subject", getString("subject"),
                    new PropertyModel<>(notificationTO, "subject"));
            subject.addRequiredLabel();
            add(subject);

            AjaxDropDownChoicePanel<String> template = new AjaxDropDownChoicePanel<>(
                    "template", getString("template"),
                    new PropertyModel<>(notificationTO, "template"));
            template.setChoices(notificationRestClient.listTemplates().stream().
                    map(MailTemplateTO::getKey).collect(Collectors.toList()));

            template.addRequiredLabel();
            add(template);

            AjaxDropDownChoicePanel<TraceLevel> traceLevel = new AjaxDropDownChoicePanel<>(
                    "traceLevel", getString("traceLevel"),
                    new PropertyModel<>(notificationTO, "traceLevel"));
            traceLevel.setChoices(List.of(TraceLevel.values()));
            traceLevel.addRequiredLabel();
            add(traceLevel);

            final AjaxCheckBoxPanel isActive = new AjaxCheckBoxPanel("isActive",
                    getString("isActive"), new PropertyModel<>(notificationTO, "active"));
            if (createFlag) {
                isActive.getField().setDefaultModelObject(Boolean.TRUE);
            }
            add(isActive);
        }

    }

    public class Events extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        public Events(final NotificationWrapper modelObject) {
            setTitleModel(new ResourceModel("events"));

            add(new EventCategoryPanel(
                    "eventSelection",
                    eventCategories.getObject(),
                    new PropertyModel<>(modelObject.getInnerObject(), "events")) {

                private static final long serialVersionUID = 6429053774964787735L;

                @Override
                protected List<String> getListAuthRoles() {
                    return List.of();
                }

                @Override
                protected List<String> getChangeAuthRoles() {
                    return List.of();
                }
            });
        }
    }

    public class About extends Panel {

        private static final long serialVersionUID = -9149543787708482882L;

        public About(final String id, final IModel<Pair<String, List<SearchClause>>> model) {
            super(id, model);
            setOutputMarkupId(true);

            AjaxDropDownChoicePanel<String> type =
                    new AjaxDropDownChoicePanel<>("about", "anyType", new Model<>() {

                        private static final long serialVersionUID = -2350296434572623272L;

                        @Override
                        public String getObject() {
                            return model.getObject().getLeft();
                        }

                        @Override
                        public void setObject(final String object) {
                            model.setObject(Pair.of(object, model.getObject().getRight()));
                        }

                    });
            type.setChoices(anyTypeRestClient.list());
            type.addRequiredLabel();
            add(type);

            ListModel<SearchClause> clauseModel = new ListModel<>() {

                private static final long serialVersionUID = 3769540249683319782L;

                @Override
                public List<SearchClause> getObject() {
                    return model.getObject().getRight();
                }

                @Override
                public void setObject(final List<SearchClause> object) {
                    model.getObject().setValue(object);
                }
            };

            WebMarkupContainer searchContainer = new WebMarkupContainer("search");
            add(searchContainer.setOutputMarkupId(true));

            searchContainer.add(getClauseBuilder(model.getObject().getLeft(), clauseModel).build("clauses"));

            type.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                private static final long serialVersionUID = -1107858522700306810L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    clauseModel.getObject().clear();
                    searchContainer.addOrReplace(getClauseBuilder(type.getModelObject(), clauseModel).
                            build("clauses").setRenderBodyOnly(true));
                    target.add(searchContainer);
                }
            });
        }

        private AbstractSearchPanel.Builder<?> getClauseBuilder(
                final String type, final ListModel<SearchClause> clauseModel) {

            AbstractSearchPanel.Builder<?> clause;

            switch (type) {
                case "USER":
                    clause = new UserSearchPanel.Builder(clauseModel, pageRef);
                    break;

                case "GROUP":
                    clause = new GroupSearchPanel.Builder(clauseModel, pageRef);
                    break;

                default:
                    clause = new AnyObjectSearchPanel.Builder(type, clauseModel, pageRef);
            }

            return clause;
        }
    }

    public class Abouts extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        public Abouts(final NotificationWrapper modelObject) {
            setTitleModel(new ResourceModel("about"));

            WebMarkupContainer aboutContainer = new WebMarkupContainer("about");
            aboutContainer.setOutputMarkupId(true);
            add(aboutContainer);

            IModel<List<Pair<String, List<SearchClause>>>> model = new PropertyModel<>(modelObject, "aboutClauses");

            aboutContainer.add(new MultiPanel<>("abouts", "abouts", model) {

                private static final long serialVersionUID = -2481579077338205547L;

                @Override
                protected Pair<String, List<SearchClause>> newModelObject() {
                    return Pair.of(AnyTypeKind.USER.name(), new ArrayList<>());
                }

                @Override
                protected About getItemPanel(final ListItem<Pair<String, List<SearchClause>>> item) {

                    return new About("panel", new Model<>() {

                        private static final long serialVersionUID = 6799404673615637845L;

                        @Override
                        public Pair<String, List<SearchClause>> getObject() {
                            return item.getModelObject();
                        }

                        @Override
                        public void setObject(final Pair<String, List<SearchClause>> object) {
                            item.setModelObject(object);
                        }

                        @Override
                        public void detach() {
                            // no detach
                        }
                    });
                }

                @Override
                protected void sendError(final String message) {
                    SyncopeConsoleSession.get().error(getString(Constants.OPERATION_ERROR));
                }
            }.hideLabel());
        }
    }

    public class Recipients extends WizardStep {

        private static final long serialVersionUID = -7709805590497687958L;

        private final IModel<List<String>> recipientProviders = new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 5275935387613157447L;

            @Override
            protected List<String> load() {
                return implementationRestClient.list(IdRepoImplementationType.RECIPIENTS_PROVIDER).stream().
                        map(ImplementationTO::getKey).sorted().collect(Collectors.toList());
            }
        };

        public Recipients(final NotificationWrapper modelObject) {
            setTitleModel(new ResourceModel("recipients"));

            NotificationTO notificationTO = modelObject.getInnerObject();

            AjaxTextFieldPanel recipientAttrName = new AjaxTextFieldPanel(
                    "recipientAttrName", new ResourceModel("recipientAttrName", "recipientAttrName").getObject(),
                    new PropertyModel<>(notificationTO, "recipientAttrName"));
            recipientAttrName.setChoices(getSchemas());
            recipientAttrName.addRequiredLabel();
            recipientAttrName.setTitle(getString("intAttrNameInfo.help")
                    + "<code>groups[groupName].attribute</code>, "
                    + "<code>users[userName].attribute</code>, "
                    + "<code>anyObjects[anyObjectName].attribute</code>, "
                    + "<code>relationships[relationshipType][anyType].attribute</code> or "
                    + "<code>memberships[groupName].attribute</code>", true);
            add(recipientAttrName);

            AjaxTextFieldPanel staticRecipientsFieldPanel =
                    new AjaxTextFieldPanel("panel", "staticRecipients", new Model<>());
            staticRecipientsFieldPanel.addValidator(EmailAddressValidator.getInstance());
            add(new MultiFieldPanel.Builder<>(
                    new PropertyModel<List<String>>(notificationTO, "staticRecipients")).
                    build("staticRecipients", "staticRecipients", staticRecipientsFieldPanel).hideLabel());

            add(new UserSearchPanel.Builder(
                    new PropertyModel<>(modelObject, "recipientClauses"), pageRef).
                    required(false).build("recipients"));

            AjaxDropDownChoicePanel<String> recipientsProvider = new AjaxDropDownChoicePanel<>(
                    "recipientsProvider", "recipientsProvider",
                    new PropertyModel<>(notificationTO, "recipientsProvider"), false);
            recipientsProvider.setChoices(recipientProviders.getObject());
            add(recipientsProvider);

            AjaxCheckBoxPanel selfAsRecipient = new AjaxCheckBoxPanel("selfAsRecipient",
                    getString("selfAsRecipient"), new PropertyModel<>(notificationTO, "selfAsRecipient"));
            if (notificationTO.getKey() == null) {
                selfAsRecipient.getField().setDefaultModelObject(Boolean.FALSE);
            }
            add(selfAsRecipient);
        }
    }

    protected List<String> getSchemas() {
        AnyTypeTO type = null;
        try {
            type = anyTypeRestClient.read(AnyTypeKind.USER.name());
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any types", e);
        }

        String[] anyTypeClasses = Optional.ofNullable(type).
                map(anyTypeTO -> anyTypeTO.getClasses().toArray(String[]::new)).orElseGet(() -> new String[0]);

        List<String> result = new ArrayList<>();
        result.add(Constants.USERNAME_FIELD_NAME);

        result.addAll(schemaRestClient.<PlainSchemaTO>getSchemas(SchemaType.PLAIN, null, anyTypeClasses).
                stream().map(PlainSchemaTO::getKey).toList());
        result.addAll(schemaRestClient.<DerSchemaTO>getSchemas(SchemaType.DERIVED, null, anyTypeClasses).
                stream().map(DerSchemaTO::getKey).toList());
        result.addAll(schemaRestClient.<VirSchemaTO>getSchemas(SchemaType.VIRTUAL, null, anyTypeClasses).
                stream().map(VirSchemaTO::getKey).toList());

        Collections.sort(result);
        return result;
    }
}
