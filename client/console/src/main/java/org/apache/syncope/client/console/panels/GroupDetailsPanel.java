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

import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.JexlHelpUtil;
import org.apache.syncope.client.console.pages.GroupSelectModalPage;
import org.apache.syncope.client.console.pages.UserOwnerSelectModalPage;
import org.apache.syncope.client.console.panels.AttrTemplatesPanel.GroupAttrTemplatesChange;
import org.apache.syncope.client.console.panels.AttrTemplatesPanel.Type;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttributableType;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupDetailsPanel extends Panel {

    private static final long serialVersionUID = 855618618337931784L;

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(GroupDetailsPanel.class);

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private GroupRestClient groupRestClient;

    private final Fragment parentFragment;

    private final WebMarkupContainer ownerContainer;

    private final OwnerModel userOwnerModel;

    private final OwnerModel groupOwnerModel;

    private ParentModel parentModel;

    public GroupDetailsPanel(final String id, final GroupTO groupTO, final boolean templateMode) {
        super(id);

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
        final ModalWindow parentSelectWin = new ModalWindow("parentSelectWin");
        parentSelectWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        parentSelectWin.setCookieName("create-parentSelect-modal");
        this.add(parentSelectWin);

        if (templateMode) {
            parentFragment = new Fragment("parent", "parentFragment", this);

            parentModel = new ParentModel(groupTO);
            @SuppressWarnings("unchecked")
            final AjaxTextFieldPanel parent = new AjaxTextFieldPanel("parent", "parent", parentModel);
            parent.setReadOnly(true);
            parent.setOutputMarkupId(true);
            parentFragment.add(parent);
            final AjaxLink<Void> parentSelect = new IndicatingAjaxLink<Void>("parentSelect") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    parentSelectWin.setPageCreator(new ModalWindow.PageCreator() {

                        private static final long serialVersionUID = -7834632442532690940L;

                        @Override
                        public Page createPage() {
                            return new GroupSelectModalPage(getPage().getPageReference(), parentSelectWin,
                                    ParentSelectPayload.class);
                        }
                    });
                    parentSelectWin.show(target);
                }
            };
            parentFragment.add(parentSelect);
            final IndicatingAjaxLink<Void> parentReset = new IndicatingAjaxLink<Void>("parentReset") {

                private static final long serialVersionUID = -7978723352517770644L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    parentModel.setObject(null);
                    target.add(parent);
                }
            };
            parentFragment.add(parentReset);
        } else {
            parentFragment = new Fragment("parent", "emptyFragment", this);
        }
        parentFragment.setOutputMarkupId(true);
        this.add(parentFragment);

        final AjaxTextFieldPanel name =
                new AjaxTextFieldPanel("name", "name", new PropertyModel<String>(groupTO, "key"));

        final WebMarkupContainer jexlHelp = JexlHelpUtil.getJexlHelpWebContainer("jexlHelp");

        final AjaxLink<Void> questionMarkJexlHelp = JexlHelpUtil.getAjaxLink(jexlHelp, "questionMarkJexlHelp");
        this.add(questionMarkJexlHelp);
        questionMarkJexlHelp.add(jexlHelp);

        if (!templateMode) {
            name.addRequiredLabel();
            questionMarkJexlHelp.setVisible(false);
        }
        this.add(name);

        userOwnerModel = new OwnerModel(groupTO, AttributableType.USER);
        @SuppressWarnings("unchecked")
        final AjaxTextFieldPanel userOwner = new AjaxTextFieldPanel("userOwner", "userOwner", userOwnerModel);
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
                        return new UserOwnerSelectModalPage(getPage().getPageReference(), userOwnerSelectWin);
                    }
                });
                userOwnerSelectWin.show(target);
            }
        };
        ownerContainer.add(userOwnerSelect);
        final IndicatingAjaxLink<Void> userOwnerReset = new IndicatingAjaxLink<Void>("userOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                userOwnerModel.setObject(null);
                target.add(userOwner);
            }
        };
        ownerContainer.add(userOwnerReset);

        groupOwnerModel = new OwnerModel(groupTO, AttributableType.GROUP);
        @SuppressWarnings("unchecked")
        final AjaxTextFieldPanel groupOwner = new AjaxTextFieldPanel("groupOwner", "groupOwner", groupOwnerModel);
        groupOwner.setReadOnly(true);
        groupOwner.setOutputMarkupId(true);
        ownerContainer.add(groupOwner);
        final AjaxLink<Void> groupOwnerSelect = new IndicatingAjaxLink<Void>("groupOwnerSelect") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                parentSelectWin.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        return new GroupSelectModalPage(getPage().getPageReference(), parentSelectWin,
                                GroupOwnerSelectPayload.class);
                    }
                });
                parentSelectWin.show(target);
            }
        };
        ownerContainer.add(groupOwnerSelect);
        final IndicatingAjaxLink<Void> groupOwnerReset = new IndicatingAjaxLink<Void>("groupOwnerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                groupOwnerModel.setObject(null);
                target.add(groupOwner);
            }
        };
        ownerContainer.add(groupOwnerReset);

        final AjaxCheckBoxPanel inhOwner = new AjaxCheckBoxPanel("inheritOwner", "inheritOwner",
                new PropertyModel<Boolean>(groupTO, "inheritOwner"));
        this.add(inhOwner);

        final AjaxCheckBoxPanel inhTemplates = new AjaxCheckBoxPanel("inheritTemplates", "inheritTemplates",
                new PropertyModel<Boolean>(groupTO, "inheritTemplates"));
        inhTemplates.getField().add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                send(getPage(), Broadcast.BREADTH, new GroupAttrTemplatesChange(Type.gPlainAttrTemplates, target));
                send(getPage(), Broadcast.BREADTH, new GroupAttrTemplatesChange(Type.gDerAttrTemplates, target));
                send(getPage(), Broadcast.BREADTH, new GroupAttrTemplatesChange(Type.gVirAttrTemplates, target));
            }
        });
        this.add(inhTemplates);
    }

    /**
     * This is waiting for events from opened modal windows: first to get the selected user / group, then to update the
     * respective text panel.
     *
     * {@inheritDoc }
     */
    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);

        if (event.getPayload() instanceof ParentSelectPayload) {
            parentModel.setObject(((ParentSelectPayload) event.getPayload()).getGroupId());
        }
        if (event.getPayload() instanceof UserOwnerSelectPayload) {
            userOwnerModel.setObject(((UserOwnerSelectPayload) event.getPayload()).getUserId());
        }
        if (event.getPayload() instanceof GroupOwnerSelectPayload) {
            groupOwnerModel.setObject(((GroupOwnerSelectPayload) event.getPayload()).getGroupId());
        }

        if (event.getPayload() instanceof AjaxRequestTarget) {
            ((AjaxRequestTarget) event.getPayload()).add(parentFragment);
            ((AjaxRequestTarget) event.getPayload()).add(ownerContainer);
        }
    }

    private class OwnerModel implements IModel {

        private static final long serialVersionUID = -3865621970810102714L;

        private final GroupTO groupTO;

        private final AttributableType type;

        public OwnerModel(final GroupTO groupTO, final AttributableType type) {
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

                case MEMBERSHIP:
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

                case MEMBERSHIP:
                default:
            }
        }

        @Override
        public void detach() {
            // ignore
        }
    }

    private class ParentModel implements IModel {

        private static final long serialVersionUID = 1006546156848990721L;

        private final GroupTO groupTO;

        public ParentModel(final GroupTO groupTO) {
            this.groupTO = groupTO;
        }

        @Override
        public Object getObject() {
            Object object = null;
            if (groupTO.getParent() != 0) {
                GroupTO parent = null;
                try {
                    parent = groupRestClient.read(groupTO.getParent());
                } catch (Exception e) {
                    LOG.warn("Could not find group with id {}, ignoring", groupTO.getParent(), e);
                }
                if (parent == null) {
                    groupTO.setParent(0);
                } else {
                    object = parent.getDisplayName();
                }
            }
            return object;
        }

        @Override
        public void setObject(final Object object) {
            groupTO.setParent((object instanceof Long) ? ((Long) object) : 0);
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

    public static class ParentSelectPayload {

        private final Long groupId;

        public ParentSelectPayload(final Long groupId) {
            this.groupId = groupId;
        }

        public Long getGroupId() {
            return groupId;
        }
    }
}
