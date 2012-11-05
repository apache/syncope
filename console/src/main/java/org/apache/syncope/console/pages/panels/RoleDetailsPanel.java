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
package org.apache.syncope.console.pages.panels;

import org.apache.syncope.console.pages.RoleOwnerSelectModalPage;
import org.apache.syncope.console.pages.UserOwnerSelectModalPage;
import org.apache.syncope.console.rest.RoleRestClient;
import org.apache.syncope.console.rest.UserRestClient;
import org.apache.syncope.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.to.RoleTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.AttributableType;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class RoleDetailsPanel extends Panel {

    private static final long serialVersionUID = 855618618337931784L;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private RoleRestClient roleRestClient;

    private final WebMarkupContainer ownerContainer;

    private final OwnerModel userOwnerModel;

    private final OwnerModel roleOwnerModel;

    public RoleDetailsPanel(final String id, final RoleTO roleTO, final Form form) {
        super(id);

        ownerContainer = new WebMarkupContainer("ownerContainer");
        ownerContainer.setOutputMarkupId(true);
        this.add(ownerContainer);

        final ModalWindow userOwnerSelectWin = new ModalWindow("userOwnerSelectWin");
        userOwnerSelectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        userOwnerSelectWin.setCookieName("create-userOwnerSelect-modal");
        this.add(userOwnerSelectWin);
        final ModalWindow roleOwnerSelectWin = new ModalWindow("roleOwnerSelectWin");
        roleOwnerSelectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        roleOwnerSelectWin.setCookieName("create-roleOwnerSelect-modal");
        this.add(roleOwnerSelectWin);

        final AjaxTextFieldPanel name =
                new AjaxTextFieldPanel("name", "name", new PropertyModel<String>(roleTO, "name"));
        name.addRequiredLabel();
        this.add(name);

        userOwnerModel = new OwnerModel(roleTO, AttributableType.USER);
        final AjaxTextFieldPanel userOwner = new AjaxTextFieldPanel("userOwner", "userOwner", userOwnerModel);
        userOwner.setReadOnly(true);
        userOwner.setOutputMarkupId(true);
        ownerContainer.add(userOwner);
        final IndicatingAjaxLink userOwnerSelect = new IndicatingAjaxLink("userOwnerSelect") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userOwnerSelectWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new UserOwnerSelectModalPage(getPage().getPageReference(), userOwnerSelectWin);
                    }
                });
                userOwnerSelectWin.show(target);
            }
        };
        ownerContainer.add(userOwnerSelect);
        final IndicatingAjaxLink userOwnerReset = new IndicatingAjaxLink("userOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userOwnerModel.setObject(null);
                target.add(userOwner);
            }
        };
        ownerContainer.add(userOwnerReset);

        roleOwnerModel = new OwnerModel(roleTO, AttributableType.ROLE);
        final AjaxTextFieldPanel roleOwner = new AjaxTextFieldPanel("roleOwner", "roleOwner", roleOwnerModel);
        roleOwner.setReadOnly(true);
        roleOwner.setOutputMarkupId(true);
        ownerContainer.add(roleOwner);
        final IndicatingAjaxLink roleOwnerSelect = new IndicatingAjaxLink("roleOwnerSelect") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                roleOwnerSelectWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new RoleOwnerSelectModalPage(getPage().getPageReference(), roleOwnerSelectWin);
                    }
                });
                roleOwnerSelectWin.show(target);
            }
        };
        ownerContainer.add(roleOwnerSelect);
        final IndicatingAjaxLink roleOwnerReset = new IndicatingAjaxLink("roleOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                roleOwnerModel.setObject(null);
                target.add(roleOwner);
            }
        };
        ownerContainer.add(roleOwnerReset);

        final AjaxCheckBoxPanel inhOwner = new AjaxCheckBoxPanel("inheritOwner", "inheritOwner",
                new PropertyModel<Boolean>(roleTO, "inheritOwner"));
        this.add(inhOwner);
    }

    /**
     * This is waiting for events from opened modal windows: first to get the selected user / role, then to update the
     * respective text panel.
     *
     * {@inheritDoc }
     */
    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof UserOwnerSelectPayload) {
            userOwnerModel.setObject(((UserOwnerSelectPayload) event.getPayload()).getUserId());
        }
        if (event.getPayload() instanceof RoleOwnerSelectPayload) {
            roleOwnerModel.setObject(((RoleOwnerSelectPayload) event.getPayload()).getRoleId());
        }
        if (event.getPayload() instanceof AjaxRequestTarget) {
            ((AjaxRequestTarget) event.getPayload()).add(ownerContainer);
        }
    }

    private class OwnerModel implements IModel {

        private static final long serialVersionUID = -3865621970810102714L;

        private final RoleTO roleTO;

        private final AttributableType type;

        public OwnerModel(final RoleTO roleTO, final AttributableType type) {
            this.roleTO = roleTO;
            this.type = type;
        }

        @Override
        public Object getObject() {
            String object = null;

            switch (type) {
                case USER:
                    if (roleTO.getUserOwner() != null) {
                        UserTO user = userRestClient.read(roleTO.getUserOwner());
                        if (user == null) {
                            object = String.valueOf(roleTO.getUserOwner());
                        } else {
                            object = user.getId() + " " + user.getUsername();
                        }
                    }
                    break;

                case ROLE:
                    if (roleTO.getRoleOwner() != null) {
                        RoleTO role = roleRestClient.read(roleTO.getRoleOwner());
                        if (role == null) {
                            object = String.valueOf(roleTO.getRoleOwner());
                        } else {
                            object = role.getDisplayName();
                        }
                    }
                    break;

                case MEMBERSHIP:
                default:
            }

            return object;
        }

        @Override
        public void setObject(final Object object) {
            switch (type) {
                case USER:
                    roleTO.setUserOwner((Long) object);
                    break;

                case ROLE:
                    roleTO.setRoleOwner((Long) object);
                    break;

                case MEMBERSHIP:
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

    public static class RoleOwnerSelectPayload {

        private final Long roleId;

        public RoleOwnerSelectPayload(final Long roleId) {
            this.roleId = roleId;
        }

        public Long getRoleId() {
            return roleId;
        }
    }
}
