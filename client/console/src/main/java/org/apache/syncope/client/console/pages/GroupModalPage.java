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
package org.apache.syncope.client.console.pages;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.client.console.panels.GroupPanel;
import org.apache.syncope.common.lib.AttributableOperations;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Modal window with Group form.
 */
public class GroupModalPage extends BaseModalPage {

    private static final long serialVersionUID = -1732493223434085205L;

    protected final PageReference pageRef;

    protected final ModalWindow window;

    protected final Mode mode;

    protected final boolean createFlag;

    protected final GroupPanel groupPanel;

    protected GroupTO originalGroupTO;

    public GroupModalPage(final PageReference pageRef, final ModalWindow window, final GroupTO groupTO) {
        this(pageRef, window, groupTO, Mode.ADMIN);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public GroupModalPage(
            final PageReference pageRef, final ModalWindow window, final GroupTO groupTO, final Mode mode) {
        
        super();

        this.pageRef = pageRef;
        this.window = window;
        this.mode = mode;

        this.createFlag = groupTO.getKey() == 0;
        if (!createFlag) {
            originalGroupTO = SerializationUtils.clone(groupTO);
        }

        final Form<GroupTO> form = new Form<GroupTO>("groupForm");
        form.setMultiPart(true);

        add(new Label("displayName", groupTO.getKey() == 0 ? "" : groupTO.getDisplayName()));

        form.setModel(new CompoundPropertyModel<GroupTO>(groupTO));

        this.groupPanel = new GroupPanel.Builder("groupPanel").
                form(form).groupTO(groupTO).groupModalPageMode(mode).pageRef(getPageReference()).build();
        form.add(groupPanel);

        final AjaxButton submit = new IndicatingAjaxButton(SUBMIT, new ResourceModel(SUBMIT)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                try {
                    submitAction(target, form);

                    if (pageRef.getPage() instanceof BasePage) {
                        ((BasePage) pageRef.getPage()).setModalResult(true);
                    }

                    closeAction(target, form);
                } catch (Exception e) {
                    error(getString(Constants.ERROR) + ": " + e.getMessage());
                    feedbackPanel.refresh(target);
                }
            }

            @Override
            protected void onError(final AjaxRequestTarget target, final Form<?> form) {
                feedbackPanel.refresh(target);
            }
        };
        form.add(submit);
        form.setDefaultButton(submit);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                closeAction(target, form);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);

        MetaDataRoleAuthorizationStrategy.authorize(submit, ENABLE, xmlRolesReader.getEntitlement("Groups",
                createFlag
                        ? "create"
                        : "update"));

        add(form);
    }

    protected void submitAction(final AjaxRequestTarget target, final Form<?> form) {
        final GroupTO groupTO = (GroupTO) form.getDefaultModelObject();
        final List<String> entitlementList = new ArrayList<String>(groupPanel.getSelectedEntitlements());
        groupTO.getEntitlements().clear();
        groupTO.getEntitlements().addAll(entitlementList);

        GroupTO result;
        if (createFlag) {
            result = groupRestClient.create(groupTO);
        } else {
            GroupMod groupMod = AttributableOperations.diff(groupTO, originalGroupTO);

            // update group just if it is changed
            if (groupMod.isEmpty()) {
                result = groupTO;
            } else {
                result = groupRestClient.update(originalGroupTO.getETagValue(), groupMod);
            }
        }

        setResponsePage(new ResultStatusModalPage.Builder(window, result).build());
    }

    protected void closeAction(final AjaxRequestTarget target, final Form<?> form) {
        window.close(target);
    }
}
