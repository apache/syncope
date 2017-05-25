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
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.search.client.CompleteCondition;
import org.apache.syncope.client.console.SyncopeConsoleApplication;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.search.GroupFiqlSearchConditionBuilder;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.util.lang.Args;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.ActionPermissions;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;

public class Groups extends WizardStep implements ICondition {

    private static final long serialVersionUID = 552437609667518888L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    private final List<GroupTO> allGroups;

    private static final int MAX_GROUP_LIST_CARDINALITY = 30;

    public <T extends AnyTO> Groups(final T anyTO, final boolean templateMode) {
        super();

        final String realm = templateMode ? "/" : anyTO.getRealm();

        // -----------------------------------------------------------------
        // Pre-Authorizations
        // -----------------------------------------------------------------
        final ActionPermissions permissions = new ActionPermissions();
        setMetaData(MetaDataRoleAuthorizationStrategy.ACTION_PERMISSIONS, permissions);
        permissions.authorizeAll(RENDER);
        // -----------------------------------------------------------------

        setOutputMarkupId(true);

        Args.isTrue((anyTO instanceof UserTO) || (anyTO instanceof AnyObjectTO), "Expected user or anyObject");

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

                        return IterableUtils.find(choices.getObject(), new Predicate<MembershipTO>() {

                            @Override
                            public boolean evaluate(final MembershipTO object) {
                                return id.equalsIgnoreCase(object.getGroupName());
                            }
                        });
                    }
                });

        allGroups = groupRestClient.search(
                realm,
                SyncopeClient.getGroupSearchConditionBuilder().isAssignable().query(),
                1,
                MAX_GROUP_LIST_CARDINALITY,
                new SortParam<>("name", true),
                null);

        // ---------------------------------
        // Retrieve group memberships
        // ---------------------------------
        add(builder.setAllowOrder(true).withFilter().build("groups",
                new ListModel<>(GroupableRelatableTO.class.cast(anyTO).getMemberships()),
                new AjaxPalettePanel.Builder.Query<MembershipTO>() {

            private static final long serialVersionUID = -7223078772249308813L;

            @Override
            public List<MembershipTO> execute(final String filter) {
                return CollectionUtils.collect(
                        StringUtils.isEmpty(filter) || "*".equals(filter)
                        ? allGroups
                        : groupRestClient.search(
                                realm,
                                SyncopeClient.getGroupSearchConditionBuilder().
                                isAssignable().and().is("name").equalTo(filter).query(),
                                1, MAX_GROUP_LIST_CARDINALITY,
                                new SortParam<>("name", true),
                                null),
                        new Transformer<GroupTO, MembershipTO>() {

                    @Override
                    public MembershipTO transform(final GroupTO input) {
                        return new MembershipTO.Builder().
                                group(input.getKey(), input.getName()).
                                build();
                    }
                }, new ArrayList<MembershipTO>());
            }
        }).hideLabel().setOutputMarkupId(true));
        // ---------------------------------

        // ---------------------------------
        // Retrieve dyn group memberships
        // ---------------------------------
        final GroupFiqlSearchConditionBuilder searchConditionBuilder = SyncopeClient.getGroupSearchConditionBuilder();

        List<CompleteCondition> conditions = new ArrayList<>();
        for (String groupKey : GroupableRelatableTO.class.cast(anyTO).getDynGroups()) {
            conditions.add(searchConditionBuilder.is("key").equalTo(groupKey).wrap());
        }

        final List<GroupTO> dynGroups = new ArrayList<>();
        if (!conditions.isEmpty()) {
            dynGroups.addAll(groupRestClient.search(
                    "/",
                    searchConditionBuilder.or(conditions).query(),
                    -1,
                    -1,
                    new SortParam<>("name", true),
                    null));
        }

        add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build("dyngroups",
                new ListModel<>(CollectionUtils.collect(dynGroups,
                        new Transformer<GroupTO, String>() {

                    @Override
                    public String transform(final GroupTO input) {
                        return input.getName();
                    }
                }, new ArrayList<String>())),
                new ListModel<>(CollectionUtils.collect(allGroups, new Transformer<GroupTO, String>() {

                    @Override
                    public String transform(final GroupTO input) {
                        return input.getName();
                    }
                }, new ArrayList<String>()))).
                hideLabel().setEnabled(false).setOutputMarkupId(true));

        // ---------------------------------
    }

    @Override
    public boolean evaluate() {
        return CollectionUtils.isNotEmpty(allGroups)
                && SyncopeConsoleApplication.get().getSecuritySettings().getAuthorizationStrategy().
                isActionAuthorized(this, RENDER);
    }
}
