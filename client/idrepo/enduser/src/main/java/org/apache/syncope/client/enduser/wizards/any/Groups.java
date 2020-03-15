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
package org.apache.syncope.client.enduser.wizards.any;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.rest.GroupRestClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractGroups;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractGroupsModel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public class Groups extends AbstractGroups {

    private static final long serialVersionUID = 552437609667518888L;

    private final EnduserGroupsModel groupsModel;

    public <T extends AnyTO> Groups(final AnyWrapper<T> modelObject, final boolean templateMode) {
        super(modelObject);
        this.groupsModel = new EnduserGroupsModel();

        setOutputMarkupId(true);

        addDynamicGroupsContainer();
        addGroupsPanel();
        addDynamicRealmsContainer();
    }

    @Override
    protected void addGroupsPanel() {
        if (anyTO instanceof GroupTO) {
            groupsContainer.add(new Label("groups").setVisible(false));
            groupsContainer.setVisible(false);
        } else {
            AjaxPalettePanel.Builder<MembershipTO> builder = new AjaxPalettePanel.Builder<MembershipTO>().
                    setRenderer(new IChoiceRenderer<MembershipTO>() {

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

            groupsContainer.add(builder.setAllowOrder(true).withFilter().build("groups",
                    new ListModel<MembershipTO>() {

                private static final long serialVersionUID = -2583290457773357445L;

                @Override
                public List<MembershipTO> getObject() {
                    return Groups.this.groupsModel.getMemberships();
                }

            }, new AjaxPalettePanel.Builder.Query<MembershipTO>() {

                private static final long serialVersionUID = -7223078772249308813L;

                @Override
                public List<MembershipTO> execute(final String filter) {
                    return (StringUtils.isEmpty(filter) || "*".equals(filter)
                            ? groupsModel.getObject()
                            : GroupRestClient.searchAssignableGroups(
                                    anyTO.getRealm(),
                                    SyncopeClient.getGroupSearchConditionBuilder().
                                            isAssignable().and().is("name").equalTo(filter).query(),
                                    1, MAX_GROUP_LIST_CARDINALITY)).stream()
                            .map(input -> new MembershipTO.Builder(input.getKey())
                            .groupName(input.getName()).build()).collect(Collectors.toList());
                }
            }).hideLabel().setOutputMarkupId(true));
            // ---------------------------------
        }
    }

    @Override
    protected void addDynamicRealmsContainer() {
    }

    @Override
    protected void addDynamicGroupsContainer() {
    }

    protected class EnduserGroupsModel extends AbstractGroupsModel {

        private static final long serialVersionUID = -4541954630939063927L;

        private List<GroupTO> groups;

        private List<MembershipTO> memberships;

        private String realm;

        @Override
        public List<GroupTO> getObject() {
            reload();
            return groups;
        }

        /**
         * Retrieve the first MAX_GROUP_LIST_CARDINALITY assignable.
         */
        @Override
        protected void reloadObject() {
            groups = GroupRestClient.searchAssignableGroups(
                    realm,
                    null,
                    1,
                    MAX_GROUP_LIST_CARDINALITY);
        }

        @Override
        public List<MembershipTO> getMemberships() {
            reload();
            return memberships;
        }

        /**
         * Retrieve group memberships.
         */
        @Override
        protected void reloadMemberships() {
            memberships = GroupableRelatableTO.class.cast(anyTO).getMemberships();
        }

        @Override
        public List<String> getDynMemberships() {
            return List.of();
        }

        /**
         * Retrieve dyn group memberships.
         */
        @Override
        protected void reloadDynMemberships() {
            // DO NOTHING
        }

        /**
         * Reload data if the realm changes (see SYNCOPE-1135).
         */
        @Override
        protected void reload() {
            boolean reload = Groups.this.anyTO.getRealm() != null
                    && !Groups.this.anyTO.getRealm().equalsIgnoreCase(realm);
            realm = Groups.this.anyTO.getRealm();

            if (reload) {
                reloadObject();
                reloadMemberships();
            }
        }
    }
}
