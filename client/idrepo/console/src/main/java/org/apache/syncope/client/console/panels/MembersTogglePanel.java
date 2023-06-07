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

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDropDownChoicePanel;
import org.apache.syncope.client.ui.commons.markup.html.form.FieldPanel;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class MembersTogglePanel extends TogglePanel<Serializable> {

    private static final long serialVersionUID = -3195479265440591519L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    protected GroupTO groupTO;

    protected final Form<?> form;

    protected final Model<String> typeModel = new Model<>();

    private final LoadableDetachableModel<List<String>> anyTypes = new LoadableDetachableModel<>() {

        private static final long serialVersionUID = 5275935387613157437L;

        @Override
        protected List<String> load() {
            return anyTypeRestClient.list().stream().
                    filter(anyType -> !AnyTypeKind.GROUP.name().equals(anyType)).collect(Collectors.toList());
        }
    };

    public MembersTogglePanel(final PageReference pageRef) {
        super(Constants.OUTER, "groupMembers", pageRef);

        form = new Form<>("membersForm");
        addInnerObject(form);

        FieldPanel<String> type = new AjaxDropDownChoicePanel<>("type", "type", typeModel, false).
                setChoices(anyTypes).
                setChoiceRenderer(new IChoiceRenderer<String>() {

                    private static final long serialVersionUID = -200150326532439794L;

                    @Override
                    public Object getDisplayValue(final String anyType) {
                        return new ResourceModel("anyType." + anyType, anyType).getObject();
                    }
                }).
                setStyleSheet("form-control").
                setRequired(true);

        type.hideLabel();
        form.add(type);

        form.add(new AjaxSubmitLink("changeit", form) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target) {
                try {
                    onApplyInternal(groupTO, typeModel.getObject(), target);
                    toggle(target, false);
                } catch (SyncopeClientException e) {
                    LOG.error("While inspecting group memebers of type {}", typeModel.getObject(), e);
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

    protected abstract Serializable onApplyInternal(
            GroupTO groupTO, String type, AjaxRequestTarget target);

    public void setTargetObject(final GroupTO groupTO) {
        this.groupTO = groupTO;
    }
}
