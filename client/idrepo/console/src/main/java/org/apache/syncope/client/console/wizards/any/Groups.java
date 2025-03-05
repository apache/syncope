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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.SyncopeWebApplication;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractGroups;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractGroupsModel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.DynRealmTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class Groups extends AbstractGroups {

    private static final long serialVersionUID = 552437609667518888L;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected SyncopeRestClient syncopeRestClient;

    protected final List<DynRealmTO> allDynRealms = new ArrayList<>();

    protected final boolean templateMode;

    protected final ConsoleGroupsModel groupsModel;

    protected AjaxPalettePanel.Builder<MembershipTO> groups;

    public <T extends AnyTO> Groups(final AnyWrapper<T> modelObject, final boolean templateMode) {
        super(modelObject);
        this.templateMode = templateMode;
        this.groupsModel = new ConsoleGroupsModel();

        // -----------------------------------------------------------------
        // Pre-Authorizations
        // -----------------------------------------------------------------
        ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        permissions.authorizeAll(RENDER);
        // -----------------------------------------------------------------

        addDynamicGroupsContainer();
        addGroupsPanel();
        addDynamicRealmsContainer();
    }

    protected List<GroupTO> searchAssignable(final String term) {
        return syncopeRestClient.searchAssignableGroups(
                SyncopeConstants.ROOT_REALM, term, 1, Constants.MAX_GROUP_LIST_SIZE);
    }

    @Override
    protected void addDynamicRealmsContainer() {
        dynrealmsContainer = new WebMarkupContainer("dynrealmsContainer");
        dynrealmsContainer.setOutputMarkupId(true);
        dynrealmsContainer.setOutputMarkupPlaceholderTag(true);
        dynrealmsContainer.add(new AjaxPalettePanel.Builder<>().build("dynrealms",
                new PropertyModel<>(anyTO, "dynRealms"),
                new ListModel<>(allDynRealms.stream().map(DynRealmTO::getKey).collect(Collectors.toList()))).
                hideLabel().setEnabled(false).setOutputMarkupId(true));
        add(dynrealmsContainer);
    }

    @Override
    protected void addGroupsPanel() {
        if (anyTO instanceof GroupTO) {
            groupsContainer.add(new Label("groups").setVisible(false));
            groupsContainer.setVisible(false);
            dyngroupsContainer.add(new Label("dyngroups").setVisible(false));
            dyngroupsContainer.setVisible(false);
        } else {
            groups = new AjaxPalettePanel.Builder<MembershipTO>().setRenderer(new IChoiceRenderer<>() {

                private static final long serialVersionUID = -3086661086073628855L;

                @Override
                public Object getDisplayValue(final MembershipTO object) {
                    return object.getGroupName();
                }

                @Override
                public String getIdValue(final MembershipTO object, final int index) {
                    return object.getGroupName();
                }

                @Override
                public MembershipTO getObject(
                        final String id, final IModel<? extends List<? extends MembershipTO>> choices) {

                    return choices.getObject().stream().
                            filter(object -> id.equalsIgnoreCase(object.getGroupName())).findAny().orElse(null);
                }
            });

            groupsContainer.add(groups.setAllowOrder(true).withFilter("*").build("groups", new ListModel<>() {

                private static final long serialVersionUID = -2583290457773357445L;

                @Override
                public List<MembershipTO> getObject() {
                    return Groups.this.groupsModel.getMemberships();
                }
            }, new AjaxPalettePanel.Builder.Query<>() {

                private static final long serialVersionUID = -7223078772249308813L;

                @Override
                public List<MembershipTO> execute(final String filter) {
                    return StringUtils.isEmpty(filter)
                            ? List.of()
                            : ("*".equals(filter)
                                    ? groupsModel.getObject()
                                    : searchAssignable(filter)).stream().
                                    map(group -> new MembershipTO.Builder(group.getKey()).
                                    groupName(group.getName()).build()).
                                    collect(Collectors.toList());
                }
            }).hideLabel().setOutputMarkupId(true));

            dyngroupsContainer.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build("dyngroups",
                    new ListModel<>() {

                private static final long serialVersionUID = -2583290457773357445L;

                @Override
                public List<String> getObject() {
                    return Groups.this.groupsModel.getDynMemberships();
                }
            }, new ListModel<>(groupsModel.getObject().stream().
                            map(GroupTO::getName).collect(Collectors.toList()))).
                    hideLabel().setEnabled(false).setOutputMarkupId(true));
        }
    }

    @Override
    protected void addDynamicGroupsContainer() {
        dyngroupsContainer = new WebMarkupContainer("dyngroupsContainer");
        dyngroupsContainer.setOutputMarkupId(true);
        dyngroupsContainer.setOutputMarkupPlaceholderTag(true);
        add(dyngroupsContainer);
    }

    @Override
    public boolean evaluate() {
        return (anyTO instanceof GroupTO
                ? !allDynRealms.isEmpty()
                : !allDynRealms.isEmpty() || !groupsModel.getObject().isEmpty())
                && SyncopeWebApplication.get().getSecuritySettings().getAuthorizationStrategy().
                        isActionAuthorized(this, RENDER);
    }

    public class ConsoleGroupsModel extends AbstractGroupsModel {

        private static final long serialVersionUID = -4541954630939063927L;

        protected List<String> dynMemberships;

        /**
         * Retrieve the first MAX_GROUP_LIST_SIZE assignable.
         */
        @Override
        protected void reloadObject() {
            groups = searchAssignable(null);
        }

        /**
         * Retrieve group memberships.
         */
        @Override
        protected void reloadMemberships() {
            // this is to be sure to have group names (required to see membership details in approval page)
            Map<String, String> assignedGroups = new HashMap<>();

            int total = GroupableRelatableTO.class.cast(anyTO).getMemberships().size();
            int pages = (total / Constants.MAX_GROUP_LIST_SIZE) + 1;
            SortParam<String> sort = new SortParam<>(Constants.NAME_FIELD_NAME, true);
            for (int page = 1; page <= pages; page++) {
                GroupFiqlSearchConditionBuilder builder = SyncopeClient.getGroupSearchConditionBuilder();

                List<CompleteCondition> conditions = GroupableRelatableTO.class.cast(anyTO).getMemberships().
                        stream().
                        skip((page - 1L) * Constants.MAX_GROUP_LIST_SIZE).
                        limit(Constants.MAX_GROUP_LIST_SIZE).
                        map(m -> builder.is(Constants.KEY_FIELD_NAME).equalTo(m.getGroupKey()).wrap()).
                        collect(Collectors.toList());

                if (!conditions.isEmpty()) {
                    assignedGroups.putAll(groupRestClient.search(
                            SyncopeConstants.ROOT_REALM,
                            builder.or(conditions).query(),
                            1,
                            Constants.MAX_GROUP_LIST_SIZE,
                            sort,
                            null).stream().collect(Collectors.toMap(GroupTO::getKey, GroupTO::getName)));
                }
            }

            // set group names in membership TOs and remove membership not assignable
            GroupableRelatableTO.class.cast(anyTO).getMemberships().stream().
                    filter(m -> m.getGroupName() == null && assignedGroups.containsKey(m.getGroupKey())).
                    forEach(m -> m.setGroupName(assignedGroups.get(m.getGroupKey())));
            GroupableRelatableTO.class.cast(anyTO).getMemberships().removeIf(m -> m.getGroupName() == null);

            memberships = GroupableRelatableTO.class.cast(anyTO).getMemberships();
            memberships.sort(Comparator.comparing(MembershipTO::getGroupName));
        }

        @Override
        protected void reloadDynMemberships() {
            GroupFiqlSearchConditionBuilder builder = SyncopeClient.getGroupSearchConditionBuilder();

            List<CompleteCondition> conditions = GroupableRelatableTO.class.cast(anyTO).getDynMemberships().
                    stream().map(membership -> builder.is(Constants.KEY_FIELD_NAME).
                    equalTo(membership.getGroupKey()).wrap()).
                    collect(Collectors.toList());

            dynMemberships = new ArrayList<>();
            if (SyncopeConsoleSession.get().owns(IdRepoEntitlement.GROUP_SEARCH) && !conditions.isEmpty()) {
                dynMemberships.addAll(groupRestClient.search(
                        SyncopeConstants.ROOT_REALM,
                        builder.or(conditions).query(),
                        -1,
                        -1,
                        new SortParam<>(Constants.NAME_FIELD_NAME, true),
                        null).stream().map(GroupTO::getName).toList());
            }
        }

        @Override
        public List<String> getDynMemberships() {
            if (dynMemberships == null) {
                reloadDynMemberships();
            }
            return dynMemberships;
        }
    }
}
