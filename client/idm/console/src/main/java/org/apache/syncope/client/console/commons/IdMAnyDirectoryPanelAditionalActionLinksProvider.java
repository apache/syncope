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
package org.apache.syncope.client.console.commons;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.status.AnyStatusModal;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.Action;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyEntitlement;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class IdMAnyDirectoryPanelAditionalActionLinksProvider
        implements AnyDirectoryPanelAditionalActionLinksProvider {

    private static final long serialVersionUID = -1698293704237878993L;

    @Override
    public List<Action<UserTO>> get(
            final UserTO modelObject,
            final String realm,
            final BaseModal<AnyWrapper<UserTO>> modal,
            final String header,
            final PageReference pageRef) {

        List<Action<UserTO>> actions = new ArrayList<>();

        Action<UserTO> enable = new Action<>(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                IModel<AnyWrapper<UserTO>> formModel = new CompoundPropertyModel<>(
                        new AnyWrapper<>(modelObject));
                modal.setFormModel(formModel);

                target.add(modal.setContent(new AnyStatusModal<>(
                        modal,
                        pageRef,
                        formModel.getObject().getInnerObject(),
                        "resource",
                        true)));

                modal.header(new Model<>(header));

                modal.show(true);
            }
        }, ActionLink.ActionType.ENABLE);
        enable.setEntitlements(StandardEntitlement.USER_UPDATE);
        enable.setOnConfirm(false);
        enable.setRealms(realm, modelObject.getDynRealms());
        actions.add(enable);

        Action<UserTO> manageResources = new Action<>(new ActionLink<UserTO>() {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target, final UserTO ignore) {
                IModel<AnyWrapper<UserTO>> formModel = new CompoundPropertyModel<>(
                        new AnyWrapper<>(modelObject));
                modal.setFormModel(formModel);

                target.add(modal.setContent(new AnyStatusModal<>(
                        modal,
                        pageRef,
                        formModel.getObject().getInnerObject(),
                        "resource",
                        false)));

                modal.header(new Model<>(header));

                modal.show(true);
            }
        }, ActionLink.ActionType.MANAGE_RESOURCES);
        manageResources.setEntitlements(
                String.format("%s,%s", StandardEntitlement.USER_READ, StandardEntitlement.USER_UPDATE));
        manageResources.setOnConfirm(false);
        manageResources.setRealms(realm, modelObject.getDynRealms());
        actions.add(manageResources);

        return actions;
    }

    @Override
    public List<Action<GroupTO>> get(
            final GroupTO modelObject,
            final String realm,
            final BaseModal<AnyWrapper<GroupTO>> modal,
            final String header,
            final PageReference pageRef) {

        List<Action<GroupTO>> actions = new ArrayList<>();

        Action<GroupTO> manageResources = new Action<>(new ActionLink<GroupTO>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                IModel<AnyWrapper<GroupTO>> formModel = new CompoundPropertyModel<>(
                        new AnyWrapper<>(modelObject));
                modal.setFormModel(formModel);

                target.add(modal.setContent(new AnyStatusModal<>(
                        modal,
                        pageRef,
                        formModel.getObject().getInnerObject(),
                        "resource",
                        false)));

                modal.header(new Model<>(header));

                modal.show(true);
            }
        }, ActionLink.ActionType.MANAGE_RESOURCES);
        manageResources.setEntitlements(
                String.format("%s,%s", StandardEntitlement.GROUP_READ, StandardEntitlement.GROUP_UPDATE));
        manageResources.setOnConfirm(false);
        manageResources.setRealms(realm, modelObject.getDynRealms());
        actions.add(manageResources);

        return actions;
    }

    @Override
    public List<Action<AnyObjectTO>> get(
            final String type,
            final AnyObjectTO modelObject,
            final String realm,
            final BaseModal<AnyWrapper<AnyObjectTO>> modal,
            final String header,
            final PageReference pageRef) {

        List<Action<AnyObjectTO>> actions = new ArrayList<>();

        Action<AnyObjectTO> manageResources = new Action<>(new ActionLink<AnyObjectTO>() {

            private static final long serialVersionUID = -7978723352517770645L;

            @Override
            public void onClick(final AjaxRequestTarget target, final AnyObjectTO ignore) {
                final IModel<AnyWrapper<AnyObjectTO>> formModel = new CompoundPropertyModel<>(
                        new AnyWrapper<>(modelObject));
                modal.setFormModel(formModel);

                target.add(modal.setContent(new AnyStatusModal<>(
                        modal,
                        pageRef,
                        formModel.getObject().getInnerObject(),
                        "resource",
                        false)));

                modal.header(new Model<>(header));

                modal.show(true);
            }
        }, ActionLink.ActionType.MANAGE_RESOURCES);
        manageResources.setEntitlements(
                String.format("%s,%s", AnyEntitlement.READ.getFor(type), AnyEntitlement.UPDATE.getFor(type)));
        manageResources.setRealms(realm, modelObject.getDynRealms());
        actions.add(manageResources);
        return actions;
    }
}
