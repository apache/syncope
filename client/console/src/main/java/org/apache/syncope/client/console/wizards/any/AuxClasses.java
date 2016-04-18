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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class AuxClasses extends WizardStep {

    private static final long serialVersionUID = 552437609667518888L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    public <T extends AnyTO> AuxClasses(final T entityTO, final String... anyTypeClass) {
        this.setOutputMarkupId(true);

        final Fragment fragment;
        if (entityTO instanceof GroupTO) {
            fragment = new Fragment("groups", "emptyFragment", this);
        } else {
            fragment = new Fragment("groups", "groupsFragment", this);

            final List<MembershipTO> memberships;
            final List<Long> dyngroups;

            if (entityTO instanceof GroupableTO) {
                memberships = GroupableTO.class.cast(entityTO).getMemberships();
                dyngroups = GroupableTO.class.cast(entityTO).getDynGroups();
            } else {
                memberships = Collections.<MembershipTO>emptyList();
                dyngroups = Collections.<Long>emptyList();
            }

            final AjaxPalettePanel.Builder<MembershipTO> builder = new AjaxPalettePanel.Builder<MembershipTO>().
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
                            for (MembershipTO membershipTO : choices.getObject()) {
                                if (id.equalsIgnoreCase(membershipTO.getGroupName())) {
                                    return membershipTO;
                                }
                            }
                            return null;
                        }
                    });

            fragment.add(builder.setAllowOrder(true).withFilter().build(
                    "groups", new ListModel<>(memberships),
                    new AjaxPalettePanel.Builder.Query<MembershipTO>() {

                private static final long serialVersionUID = 1L;

                @Override
                public List<MembershipTO> execute(final String filter) {
                    return CollectionUtils.collect(
                            groupRestClient.search(
                                    entityTO.getRealm(),
                                    SyncopeClient.getGroupSearchConditionBuilder().
                                    isAssignable().and().is("name").equalTo(filter).query(),
                                    -1, -1,
                                    new SortParam<>("name", true),
                                    null),
                            new Transformer<GroupTO, MembershipTO>() {

                        @Override
                        public MembershipTO transform(final GroupTO input) {
                            final MembershipTO membershipTO = new MembershipTO();
                            membershipTO.setGroupName(input.getName());
                            membershipTO.setRightKey(input.getKey() == null ? 0L : input.getKey());
                            membershipTO.setRightType(input.getType());
                            membershipTO.setLeftKey(entityTO.getKey() == null ? 0L : entityTO.getKey());
                            membershipTO.setLeftType(entityTO.getType());
                            return membershipTO;
                        }
                    }, new ArrayList<MembershipTO>());
                }
            }).hideLabel().setOutputMarkupId(true));

            final ArrayList<String> dynamics = CollectionUtils.collect(dyngroups, new Transformer<Long, String>() {

                @Override
                public String transform(final Long input) {
                    final GroupTO groupTO = groupRestClient.read(input);
                    return String.format("[%d] %s", groupTO.getKey(), groupTO.getName());
                }
            }, new ArrayList<String>());

            fragment.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build(
                    "dyngroups",
                    new ListModel<>(dynamics),
                    new ListModel<>(dynamics)).hideLabel().setEnabled(false).setOutputMarkupId(true));
        }
        add(fragment);

        final List<String> current = Arrays.asList(anyTypeClass);

        final List<String> choices = new ArrayList<>();
        for (AnyTypeClassTO aux : new AnyTypeClassRestClient().list()) {
            if (!current.contains(aux.getKey())) {
                choices.add(aux.getKey());
            }
        }

        add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build("auxClasses",
                new PropertyModel<List<String>>(entityTO, "auxClasses"),
                new ListModel<>(choices)).hideLabel().setOutputMarkupId(true));
    }
}
