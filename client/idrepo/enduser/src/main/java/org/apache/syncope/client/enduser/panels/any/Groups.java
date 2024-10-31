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
package org.apache.syncope.client.enduser.panels.any;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.enduser.rest.GroupRestClient;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractGroupsModel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.danekja.java.util.function.serializable.SerializableFunction;

public class Groups extends Panel {

    private static final long serialVersionUID = 552437609667518888L;

    protected static final int MAX_GROUP_LIST_CARDINALITY = 30;

    @SpringBean
    protected GroupRestClient groupRestClient;

    protected final EnduserGroupsModel groupsModel;

    protected final AnyTO anyTO;

    protected WebMarkupContainer dyngroupsContainer;

    protected WebMarkupContainer dynrealmsContainer;

    protected WebMarkupContainer groupsContainer;

    public <T extends AnyTO> Groups(final String id, final AnyWrapper<T> modelObject) {
        super(id);
        this.anyTO = modelObject.getInnerObject();

        setOutputMarkupId(true);

        groupsContainer = new WebMarkupContainer("groupsContainer");
        groupsContainer.setOutputMarkupId(true);
        groupsContainer.setOutputMarkupPlaceholderTag(true);
        add(groupsContainer);

        // ------------------
        // insert changed label if needed
        // ------------------
        if (modelObject instanceof UserWrapper
                && UserWrapper.class.cast(modelObject).getPreviousUserTO() != null
                && !ListUtils.isEqualList(
                        UserWrapper.class.cast(modelObject).getInnerObject().getMemberships(),
                        UserWrapper.class.cast(modelObject).getPreviousUserTO().getMemberships())) {
            groupsContainer.add(new LabelInfo("changed", StringUtils.EMPTY));
        } else {
            groupsContainer.add(new Label("changed", StringUtils.EMPTY));
        }
        // ------------------

        this.groupsModel = new EnduserGroupsModel();

        setOutputMarkupId(true);

        addDynamicGroupsContainer();
        addGroupsPanel();
        addDynamicRealmsContainer();
    }

    protected SerializableFunction<AjaxRequestTarget, Boolean> getEventFunction() {
        return target -> {
            send(Groups.this.getPage(), Broadcast.BREADTH,
                    new AjaxPalettePanel.UpdateActionEvent((UserTO) anyTO, target));
            return true;
        };
    }

    protected void addGroupsPanel() {
        if (anyTO instanceof GroupTO) {
            groupsContainer.add(new Label("groups").setVisible(false));
            groupsContainer.setVisible(false);
        } else {
            AjaxPalettePanel.Builder<MembershipTO> builder = new AjaxPalettePanel.Builder<MembershipTO>().
                    setRenderer(new IChoiceRenderer<>() {

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
                    }).event(getEventFunction());

            groupsContainer.add(builder.setAllowOrder(true).withFilter().build("groups",
                    new ListModel<>() {

                private static final long serialVersionUID = -2583290457773357445L;

                @Override
                public List<MembershipTO> getObject() {
                    return Groups.this.groupsModel.getMemberships();
                }

            }, new AjaxPalettePanel.Builder.Query<>() {

                private static final long serialVersionUID = -7223078772249308813L;

                @Override
                public List<MembershipTO> execute(final String filter) {
                    return (StringUtils.isEmpty(filter) || "*".equals(filter)
                            ? groupsModel.getObject()
                            : groupRestClient.searchAssignableGroups(
                                    anyTO.getRealm(),
                                    filter,
                                    1, MAX_GROUP_LIST_CARDINALITY)).stream()
                            .map(input -> new MembershipTO.Builder(input.getKey())
                            .groupName(input.getName()).build()).collect(Collectors.toList());
                }
            }).hideLabel().setOutputMarkupId(true));
            // ---------------------------------
        }
    }

    protected void addDynamicRealmsContainer() {
    }

    protected void addDynamicGroupsContainer() {
    }

    protected class EnduserGroupsModel extends AbstractGroupsModel {

        private static final long serialVersionUID = -4541954630939063927L;

        /**
         * Retrieve the first MAX_GROUP_LIST_CARDINALITY assignable.
         */
        @Override
        protected void reloadObject() {
            groups = groupRestClient.searchAssignableGroups(
                    SyncopeConstants.ROOT_REALM,
                    null,
                    1,
                    MAX_GROUP_LIST_CARDINALITY);
        }

        /**
         * Retrieve group memberships.
         */
        @Override
        protected void reloadMemberships() {
            memberships = GroupableRelatableTO.class.cast(anyTO).getMemberships();
        }

        @Override
        protected void reloadDynMemberships() {
            // DO NOTHING
        }

        @Override
        public List<String> getDynMemberships() {
            return List.of();
        }
    }
}
