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
package org.apache.syncope.client.console.wizards.any;

import java.util.List;
import org.apache.syncope.client.console.commons.JexlHelpUtils;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupDetails extends Details {

    private static final long serialVersionUID = 855618618337931784L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(GroupDetails.class);

    private final UserRestClient userRestClient = new UserRestClient();

    private final GroupRestClient groupRestClient = new GroupRestClient();

    private final WebMarkupContainer ownerContainer;

    private final OwnerModel userOwnerModel;

    private final OwnerModel groupOwnerModel;

    public GroupDetails(
            final GroupTO groupTO,
            final IModel<List<StatusBean>> statusModel,
            final boolean templateMode,
            final PageReference pageRef,
            final boolean includeStatusPanel) {
        super(groupTO, statusModel, pageRef, includeStatusPanel);

        ownerContainer = new WebMarkupContainer("ownerContainer");
        ownerContainer.setOutputMarkupId(true);
        this.add(ownerContainer);

        final ModalWindow userOwnerSelectWin = new ModalWindow("userOwnerSelectWin");
        userOwnerSelectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        userOwnerSelectWin.setCookieName("create-userOwnerSelect-modal");
        this.add(userOwnerSelectWin);
        final ModalWindow groupOwnerSelectWin = new ModalWindow("groupOwnerSelectWin");
        groupOwnerSelectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        groupOwnerSelectWin.setCookieName("create-groupOwnerSelect-modal");
        this.add(groupOwnerSelectWin);

        final AjaxTextFieldPanel name
                = new AjaxTextFieldPanel("name", "name", new PropertyModel<String>(groupTO, "name"), false);

        final WebMarkupContainer jexlHelp = JexlHelpUtils.getJexlHelpWebContainer("jexlHelp");

        final AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtils.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
        this.add(questionMarkJexlHelp);
        questionMarkJexlHelp.add(jexlHelp);

        if (!templateMode) {
            name.addRequiredLabel();
            questionMarkJexlHelp.setVisible(false);
        }
        this.add(name);

        userOwnerModel = new OwnerModel(groupTO, AnyTypeKind.USER);
        @SuppressWarnings("unchecked")
        final AjaxTextFieldPanel userOwner = new AjaxTextFieldPanel("userOwner", "userOwner", userOwnerModel, false);
        userOwner.setReadOnly(true);
        userOwner.setOutputMarkupId(true);
        ownerContainer.add(userOwner);
        final AjaxLink<Void> userOwnerSelect = new IndicatingAjaxLink<Void>("userOwnerSelect") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userOwnerSelectWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
//                        return new UserOwnerSelectModalPage(getPage().getPageReference(), userOwnerSelectWin);
                        return null;
                    }
                });
                userOwnerSelectWin.show(target);
            }
        };
        ownerContainer.add(userOwnerSelect.setEnabled(false));
        final IndicatingAjaxLink<Void> userOwnerReset = new IndicatingAjaxLink<Void>("userOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userOwnerModel.setObject(null);
                target.add(userOwner);
            }
        };
        ownerContainer.add(userOwnerReset.setEnabled(false));

        groupOwnerModel = new OwnerModel(groupTO, AnyTypeKind.GROUP);
        @SuppressWarnings("unchecked")
        final AjaxTextFieldPanel groupOwner
                = new AjaxTextFieldPanel("groupOwner", "groupOwner", groupOwnerModel, false);
        groupOwner.setReadOnly(true);
        groupOwner.setOutputMarkupId(true);
        ownerContainer.add(groupOwner);
        final AjaxLink<Void> groupOwnerSelect = new IndicatingAjaxLink<Void>("groupOwnerSelect") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userOwnerSelectWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
//                        return new GroupSelectModalPage(getPage().getPageReference(), userOwnerSelectWin,
//                                GroupOwnerSelectPayload.class);
                        return null;
                    }
                });
                userOwnerSelectWin.show(target);
            }
        };
        ownerContainer.add(groupOwnerSelect.setEnabled(false));
        final IndicatingAjaxLink<Void> groupOwnerReset = new IndicatingAjaxLink<Void>("groupOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                groupOwnerModel.setObject(null);
                target.add(groupOwner);
            }
        };
        ownerContainer.add(groupOwnerReset.setEnabled(false));
    }

    /**
     * This is waiting for events from opened modal windows: first to get the selected user / group, then to update the
     * respective text panel.
     *
     * {@inheritDoc }
     *
     * @param event
     */
    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof UserOwnerSelectPayload) {
            userOwnerModel.setObject(((UserOwnerSelectPayload) event.getPayload()).getUserId());
        }
        if (event.getPayload() instanceof GroupOwnerSelectPayload) {
            groupOwnerModel.setObject(((GroupOwnerSelectPayload) event.getPayload()).getGroupId());
        }

        if (event.getPayload() instanceof AjaxRequestTarget) {
            ((AjaxRequestTarget) event.getPayload()).add(ownerContainer);
        }
    }

    private class OwnerModel implements IModel {

        private static final long serialVersionUID = -3865621970810102714L;

        private final GroupTO groupTO;

        private final AnyTypeKind type;

        OwnerModel(final GroupTO groupTO, final AnyTypeKind type) {
            this.groupTO = groupTO;
            this.type = type;
        }

        @Override
        public Object getObject() {
            String object = null;

            switch (type) {
                case USER:
                    if (groupTO.getUserOwner() != null) {
                        UserTO user = null;
                        try {
                            user = userRestClient.read(groupTO.getUserOwner());
                        } catch (Exception e) {
                            LOG.warn("Could not find user with id {}, ignoring", groupTO.getUserOwner(), e);
                        }
                        if (user == null) {
                            groupTO.setUserOwner(null);
                        } else {
                            object = user.getKey() + " " + user.getUsername();
                        }
                    }
                    break;

                case GROUP:
                    GroupTO group = null;
                    if (groupTO.getGroupOwner() != null) {
                        try {
                            group = groupRestClient.read(groupTO.getGroupOwner());
                        } catch (Exception e) {
                            LOG.warn("Could not find group with id {}, ignoring", groupTO.getGroupOwner(), e);
                        }
                        if (group == null) {
                            groupTO.setGroupOwner(null);
                        } else {
                            object = group.getDisplayName();
                        }
                    }
                    break;

                default:
            }

            return object;
        }

        @Override
        public void setObject(final Object object) {
            switch (type) {
                case USER:
                    groupTO.setUserOwner((Long) object);
                    break;

                case GROUP:
                    groupTO.setGroupOwner((Long) object);
                    break;

                default:
            }
        }

        @Override
        public void detach() {
            // ignore
        }
    }

    public static class UserOwnerSelectPayload {

        private final Long userId;

        public UserOwnerSelectPayload(final Long userId) {
            this.userId = userId;
        }

        public Long getUserId() {
            return userId;
        }
    }

    public static class GroupOwnerSelectPayload {

        private final Long groupId;

        public GroupOwnerSelectPayload(final Long groupId) {
            this.groupId = groupId;
        }

        public Long getGroupId() {
            return groupId;
        }
    }
}
