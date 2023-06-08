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

import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.AnyObjectRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wizards.any.AnyObjectTemplateWizardBuilder;
import org.apache.syncope.client.console.wizards.any.AnyWizardBuilder;
import org.apache.syncope.client.console.wizards.any.GroupTemplateWizardBuilder;
import org.apache.syncope.client.console.wizards.any.TemplateWizardBuilder;
import org.apache.syncope.client.console.wizards.any.UserTemplateWizardBuilder;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.TemplatableTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class TemplatesTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected UserRestClient userRestClient;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected AnyObjectRestClient anyObjectRestClient;

    protected TemplatableTO targetObject;

    protected final Form<?> form;

    protected final Model<String> typeModel = new Model<>();

    protected final LoadableDetachableModel<List<String>> anyTypes = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return anyTypeRestClient.list();
        }
    };

    public TemplatesTogglePanel(final String targetId, final MarkupContainer container, final PageReference pageRef) {
        super("toggleTemplates", pageRef);

        form = new Form<>("templatesForm");
        addInnerObject(form);

        FieldPanel<String> type = new AjaxDropDownChoicePanel<>("type", "type", typeModel, false).
                setChoices(anyTypes).
                setStyleSheet("form-control").
                setRequired(true);

        type.hideLabel();
        form.add(type);

        form.add(new AjaxSubmitLink("changeit", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    AjaxWizard.NewItemActionEvent<AnyTO> payload = new AjaxWizard.NewItemActionEvent<>(null, target);

                    payload.setTitleModel(new StringResourceModel("inner.template.edit", container,
                            Model.of(Pair.of(typeModel.getObject(), targetObject))).setDefaultValue(
                            "Edit template"));

                    List<String> classes = anyTypeRestClient.read(typeModel.getObject()).getClasses();

                    TemplateWizardBuilder<?> builder;
                    switch (typeModel.getObject()) {
                        case "USER":
                            builder = new UserTemplateWizardBuilder(
                                    targetObject,
                                    classes,
                                    new UserFormLayoutInfo(),
                                    userRestClient,
                                    pageRef) {

                                private static final long serialVersionUID = -7978723352517770634L;

                                @Override
                                protected Serializable onApplyInternal(final AnyWrapper<UserTO> modelObject) {
                                    return TemplatesTogglePanel.this.onApplyInternal(
                                            targetObject, typeModel.getObject(), modelObject.getInnerObject());
                                }
                            };
                            break;

                        case "GROUP":
                            builder = new GroupTemplateWizardBuilder(
                                    targetObject,
                                    classes,
                                    new GroupFormLayoutInfo(),
                                    groupRestClient,
                                    pageRef) {

                                private static final long serialVersionUID = -7978723352517770634L;

                                @Override
                                protected Serializable onApplyInternal(final AnyWrapper<GroupTO> modelObject) {
                                    return TemplatesTogglePanel.this.onApplyInternal(
                                            targetObject, typeModel.getObject(), modelObject.getInnerObject());
                                }
                            };
                            break;

                        default:
                            builder = new AnyObjectTemplateWizardBuilder(
                                    targetObject,
                                    typeModel.getObject(),
                                    classes,
                                    new AnyObjectFormLayoutInfo(),
                                    anyObjectRestClient,
                                    pageRef) {

                                private static final long serialVersionUID = -7978723352517770634L;

                                @Override
                                protected Serializable onApplyInternal(final AnyWrapper<AnyObjectTO> modelObject) {
                                    return TemplatesTogglePanel.this.onApplyInternal(
                                            targetObject, typeModel.getObject(), modelObject.getInnerObject());
                                }
                            };
                    }
                    AnyWizardBuilder.class.cast(builder).setEventSink(container);
                    payload.forceModalPanel(builder.build(targetId));
                    send(container, Broadcast.EXACT, payload);
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While editing template for {}", typeModel.getObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target) {
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        });
    }

    protected abstract Serializable onApplyInternal(TemplatableTO targetObject, String type, AnyTO anyTO);

    public void setTargetObject(final TemplatableTO targetObject) {
        this.targetObject = targetObject;
    }
}
