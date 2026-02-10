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

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.panels.search.AnySelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.GroupSearchPanel;
import org.apache.syncope.client.console.panels.search.GroupSelectionDirectoryPanel;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.panels.search.UserSearchPanel;
import org.apache.syncope.client.console.panels.search.UserSelectionDirectoryPanel;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.ajax.form.IndicatorAjaxFormComponentUpdatingBehavior;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.Roles;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxLink;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Management extends WizardStep implements ICondition {

    private static final long serialVersionUID = 855618618337931784L;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    @SpringBean
    protected AnyTypeClassRestClient anyTypeClassRestClient;

    @SpringBean
    private UserRestClient userRestClient;

    @SpringBean
    private GroupRestClient groupRestClient;

    private final Pattern manager = Pattern.compile("\\[\\(\\d+\\)\\] .*");

    private final AnyWrapper<? extends AnyTO> wrapper;

    private final WebMarkupContainer managerContainer;

    private final GroupSearchPanel groupSearchPanel;

    private final Fragment groupSearchFragment;

    private final GroupSelectionDirectoryPanel groupDirectoryPanel;

    private final UserSearchPanel userSearchPanel;

    private final Fragment userSearchFragment;

    private final UserSelectionDirectoryPanel userDirectoryPanel;

    private final Model<Boolean> isGManager;

    public Management(final AnyWrapper<? extends AnyTO> anyWrapper, final PageReference pageRef) {
        super();

        // -----------------------------------------------------------------
        // Pre-Authorizations
        // -----------------------------------------------------------------
        ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        permissions.authorize(RENDER, new Roles(IdRepoEntitlement.USER_SEARCH));
        // -----------------------------------------------------------------

        setTitleModel(new ResourceModel("manager"));
        this.wrapper = anyWrapper;

        isGManager = Model.of(anyWrapper.getInnerObject().getGManager() != null);

        BootstrapToggleConfig config = new BootstrapToggleConfig().
                withOnStyle(BootstrapToggleConfig.Style.info).
                withOffStyle(BootstrapToggleConfig.Style.warning).
                withSize(BootstrapToggleConfig.Size.mini);

        add(new BootstrapToggle("management", new Model<>() {

            private static final long serialVersionUID = 6062041315055645807L;

            @Override
            public Boolean getObject() {
                return isGManager.getObject();
            }
        }, config) {

            private static final long serialVersionUID = 2969634208049189343L;

            @Override
            protected IModel<String> getOffLabel() {
                return Model.of(AnyTypeKind.USER.name());
            }

            @Override
            protected IModel<String> getOnLabel() {
                return Model.of(AnyTypeKind.GROUP.name());
            }

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new IndicatorAjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

                    private static final long serialVersionUID = 18235445704320L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                        isGManager.setObject(!isGManager.getObject());
                        if (isGManager.getObject()) {
                            managerContainer.addOrReplace(groupSearchFragment);
                            groupDirectoryPanel.search(null, target);
                        } else {
                            managerContainer.addOrReplace(userSearchFragment);
                            userDirectoryPanel.search(null, target);
                        }
                        target.add(managerContainer);
                    }
                });
                return checkBox;
            }
        });

        managerContainer = new WebMarkupContainer("managerContainer");
        managerContainer.setOutputMarkupId(true);
        add(managerContainer);

        groupSearchFragment = new Fragment("search", "groupSearchFragment", this);
        groupSearchPanel = new GroupSearchPanel.Builder(
                new ListModel<>(new ArrayList<>()), pageRef).required(false).enableSearch(Management.this).
                build("groupsearch");
        groupSearchFragment.add(groupSearchPanel.setRenderBodyOnly(true));

        AnyTypeTO anyTypeTO = anyTypeRestClient.read(AnyTypeKind.GROUP.name());
        groupDirectoryPanel = GroupSelectionDirectoryPanel.class.cast(new GroupSelectionDirectoryPanel.Builder(
                anyTypeClassRestClient.list(anyTypeTO.getClasses()),
                groupRestClient,
                anyTypeTO.getKey(),
                pageRef).build("searchResult"));
        groupSearchFragment.add(groupDirectoryPanel);

        userSearchFragment = new Fragment("search", "userSearchFragment", this);
        userSearchPanel = UserSearchPanel.class.cast(new UserSearchPanel.Builder(
                new ListModel<>(new ArrayList<>()), pageRef).required(false).enableSearch(Management.this).
                build("usersearch"));
        userSearchFragment.add(userSearchPanel.setRenderBodyOnly(true));

        anyTypeTO = anyTypeRestClient.read(AnyTypeKind.USER.name());
        userDirectoryPanel = UserSelectionDirectoryPanel.class.cast(new UserSelectionDirectoryPanel.Builder(
                anyTypeClassRestClient.list(anyTypeTO.getClasses()), userRestClient, anyTypeTO.getKey(), pageRef).
                build("searchResult"));
        userSearchFragment.add(userDirectoryPanel);

        if (isGManager.getObject()) {
            managerContainer.add(groupSearchFragment);
        } else {
            managerContainer.add(userSearchFragment);
        }

        AjaxTextFieldPanel uManager = new AjaxTextFieldPanel(
                "uManager", "uManager", new PropertyModel<>(anyWrapper.getInnerObject(), "uManager") {

            private static final long serialVersionUID = -3743432456095828573L;

            @Override
            public String getObject() {
                if (anyWrapper.getInnerObject().getUManager() == null) {
                    return StringUtils.EMPTY;
                }

                UserTO userTO = userRestClient.read(anyWrapper.getInnerObject().getUManager());
                if (userTO == null) {
                    return StringUtils.EMPTY;
                }

                return String.format("[%s] %s", userTO.getKey(), userTO.getUsername());
            }

            @Override
            public void setObject(final String object) {
                if (StringUtils.isBlank(object)) {
                    anyWrapper.getInnerObject().setUManager(null);
                } else {
                    Matcher matcher = manager.matcher(object);
                    if (matcher.matches()) {
                        anyWrapper.getInnerObject().setUManager(matcher.group(1));
                    }
                }
            }
        }, false);
        uManager.setPlaceholder("uManager");
        uManager.hideLabel();
        uManager.setReadOnly(true).setOutputMarkupId(true);
        userSearchFragment.add(uManager);

        IndicatingAjaxLink<Void> uManagerReset = new IndicatingAjaxLink<>("uManagerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(Management.this, Broadcast.EXACT, new GroupSelectionDirectoryPanel.ItemSelection<>(target, null));
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        userSearchFragment.add(uManagerReset);

        AjaxTextFieldPanel gManager = new AjaxTextFieldPanel(
                "gManager", "gManager", new PropertyModel<>(anyWrapper.getInnerObject(), "gManager") {

            private static final long serialVersionUID = -3743432456095828573L;

            @Override
            public String getObject() {
                if (anyWrapper.getInnerObject().getGManager() == null) {
                    return StringUtils.EMPTY;
                } else {
                    GroupTO groupTO = groupRestClient.read(anyWrapper.getInnerObject().getGManager());
                    if (groupTO == null) {
                        return StringUtils.EMPTY;
                    } else {
                        return String.format("[%s] %s", groupTO.getKey(), groupTO.getName());
                    }
                }
            }

            @Override
            public void setObject(final String object) {
                if (StringUtils.isBlank(object)) {
                    anyWrapper.getInnerObject().setGManager(null);
                } else {
                    final Matcher matcher = manager.matcher(object);
                    if (matcher.matches()) {
                        anyWrapper.getInnerObject().setGManager(matcher.group(1));
                    }
                }
            }
        }, false);
        gManager.setPlaceholder("gManager");
        gManager.hideLabel();
        gManager.setReadOnly(true).setOutputMarkupId(true);
        groupSearchFragment.add(gManager);

        IndicatingAjaxLink<Void> gManagerReset = new IndicatingAjaxLink<>("gManagerReset") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                send(Management.this, Broadcast.EXACT, new GroupSelectionDirectoryPanel.ItemSelection<>(target, null));
            }

            @Override
            public String getAjaxIndicatorMarkupId() {
                return Constants.VEIL_INDICATOR_MARKUP_ID;
            }

        };
        groupSearchFragment.add(gManagerReset);
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent payload) {
            AjaxRequestTarget target = payload.getTarget();
            if (Management.this.isGManager.getObject()) {
                String fiql = SearchUtils.buildFIQL(
                        groupSearchPanel.getModel().getObject(), SyncopeClient.getGroupSearchConditionBuilder());
                groupDirectoryPanel.search(fiql, target);
            } else {
                String fiql = SearchUtils.buildFIQL(
                        userSearchPanel.getModel().getObject(), SyncopeClient.getUserSearchConditionBuilder());
                userDirectoryPanel.search(fiql, target);
            }
        } else if (event.getPayload() instanceof final AnySelectionDirectoryPanel.ItemSelection<?> itemSelection) {
            AnyTO sel = itemSelection.getSelection();
            if (sel == null) {
                wrapper.getInnerObject().setUManager(null);
                wrapper.getInnerObject().setGManager(null);
            } else if (sel instanceof UserTO) {
                wrapper.getInnerObject().setUManager(sel.getKey());
                wrapper.getInnerObject().setGManager(null);
            } else if (sel instanceof GroupTO) {
                wrapper.getInnerObject().setUManager(null);
                wrapper.getInnerObject().setGManager(sel.getKey());
            }
            itemSelection.getTarget().add(managerContainer);
        } else {
            super.onEvent(event);
        }
    }

    @Override
    public boolean evaluate() {
        return SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                isActionAuthorized(this, RENDER);
    }
}
