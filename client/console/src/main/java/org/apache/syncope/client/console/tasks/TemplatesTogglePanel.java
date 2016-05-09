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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.client.console.panels.TogglePanel;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.any.AnyObjectWizardBuilder;
import org.apache.syncope.client.console.wizards.any.AnyWizardBuilder;
import org.apache.syncope.client.console.wizards.any.GroupWizardBuilder;
import org.apache.syncope.client.console.wizards.any.UserWizardBuilder;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

public class TemplatesTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    protected final Form<?> form;

    protected final Model<String> typeModel = new Model<>();

    private final LoadableDetachableModel<List<String>> anyTypes = new LoadableDetachableModel<List<String>>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            final List<String> res = new ArrayList<>();
            CollectionUtils.collect(new AnyTypeRestClient().list(), EntityTOUtils.<AnyTypeTO>keyTransformer(), res);
            return res;
        }
    };

    public TemplatesTogglePanel(final SchedTaskDirectoryPanel<?> container, final PageReference pageRef) {
        super("schedTaskTemplates");
        this.pageRef = pageRef;

        form = new Form<>("templatesForm");
        addInnerObject(form);

        final FieldPanel<String> type = new AjaxDropDownChoicePanel<>("type", "type", typeModel, false).
                setChoices(anyTypes).
                setStyleSheet("form-control").
                setRequired(true);

        type.hideLabel();
        form.add(type);

        form.add(new AjaxSubmitLink("changeit", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    final AjaxWizard.NewItemActionEvent<UserTO> payload
                            = new AjaxWizard.NewItemActionEvent<>(null, target);

                    final AnyWizardBuilder<?> builder;

                    switch (typeModel.getObject()) {
                        case "USER":
                            builder = new UserWizardBuilder(
                                    new UserTO(),
                                    Collections.<String>emptyList(),
                                    new UserFormLayoutInfo(),
                                    pageRef);
                            break;
                        case "GROUP":
                            builder = new GroupWizardBuilder(
                                    new GroupTO(),
                                    Collections.<String>emptyList(),
                                    new GroupFormLayoutInfo(),
                                    pageRef);
                            break;
                        default:
                            AnyObjectTO anyObjectTO = new AnyObjectTO();
                            anyObjectTO.setType(typeModel.getObject());
                            builder = new AnyObjectWizardBuilder(
                                    anyObjectTO,
                                    Collections.<String>emptyList(),
                                    new AnyObjectFormLayoutInfo(),
                                    pageRef);
                    }
                    payload.forceModalPanel(builder.build(
                            container.getActualId(), payload.getIndex(), AjaxWizard.Mode.TEMPLATE));

                    send(container, Broadcast.EXACT, payload);
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    error(StringUtils.isBlank(e.getMessage()) ? e.getClass().getName() : e.getMessage());
                    LOG.error("While editing template for {}", typeModel.getObject(), e);
                }
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                SyncopeConsoleSession.get().getNotificationPanel().refresh(target);
            }
        });
    }
}
