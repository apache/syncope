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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

public class AuxClasses extends WizardStep {

    private static final long serialVersionUID = 552437609667518888L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("\\[(\\d*)\\]? (.*)");

    public <T extends AnyTO> AuxClasses(final T entityTO, final String... anyTypeClass) {
        this.setOutputMarkupId(true);

        final Fragment fragment;
        if (entityTO instanceof GroupTO) {
            fragment = new Fragment("groups", "emptyFragment", this);
        } else {
            fragment = new Fragment("groups", "groupsFragment", this);

            final ArrayList<String> available = CollectionUtils.collect(
                    groupRestClient.list(entityTO.getRealm(), -1, -1, new SortParam<>("name", true), null),
                    new Transformer<GroupTO, String>() {

                        @Override
                        public String transform(final GroupTO input) {
                            return String.format("[%d] %s", input.getKey(), input.getName());
                        }
                    }, new ArrayList<String>());

            final List<MembershipTO> memberships;
            final List<Long> dyngroups;

            if (entityTO instanceof UserTO) {
                memberships = UserTO.class.cast(entityTO).getMemberships();
                dyngroups = UserTO.class.cast(entityTO).getDynGroups();
            } else if (entityTO instanceof AnyObjectTO) {
                memberships = AnyObjectTO.class.cast(entityTO).getMemberships();
                dyngroups = AnyObjectTO.class.cast(entityTO).getDynGroups();
            } else {
                memberships = Collections.<MembershipTO>emptyList();
                dyngroups = Collections.<Long>emptyList();
            }

            fragment.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build(
                    "groups", new ListModel<String>(CollectionUtils.collect(memberships,
                                    new Transformer<MembershipTO, String>() {

                                        @Override
                                        public String transform(final MembershipTO input) {
                                            return String.format("[%d] %s", input.getRightKey(), input.getGroupName());
                                        }
                                    }, new ArrayList<String>())) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void setObject(final List<String> object) {
                            super.setObject(object);
                            memberships.clear();
                            CollectionUtils.collect(getObject(), new Transformer<String, MembershipTO>() {

                                @Override
                                public MembershipTO transform(final String input) {
                                    final Matcher m = GROUP_ID_PATTERN.matcher(input);
                                    final String name;
                                    final long key;
                                    if (m.matches()) {
                                        key = Long.parseLong(m.group(1));
                                        name = m.group(2);
                                    } else {
                                        key = -1L;
                                        name = input;
                                    }

                                    return new MembershipTO.Builder().
                                    left(entityTO.getType(), entityTO.getKey()).group(key, name).build();
                                }
                            }, memberships);
                        }
                    },
                    new ListModel<>(available)).setOutputMarkupId(true));

            fragment.add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build(
                    "dyngroups", new ListModel<String>(CollectionUtils.collect(dyngroups,
                                    new Transformer<Long, String>() {

                                        @Override
                                        public String transform(final Long input) {
                                            final GroupTO groupTO = groupRestClient.read(input);
                                            return String.format("[%d] %s", groupTO.getKey(), groupTO.getName());
                                        }
                                    }, new ArrayList<String>())),
                    new ListModel<>(available)).setEnabled(false).setOutputMarkupId(true));
        }
        add(fragment);

        final List<String> current = Arrays.asList(anyTypeClass);

        final List<String> choices = new ArrayList<>();
        for (AnyTypeClassTO aux : AnyTypeRestClient.getAllAnyTypeClass()) {
            if (!current.contains(aux.getKey())) {
                choices.add(aux.getKey());
            }
        }

        add(new AjaxPalettePanel.Builder<String>().setAllowOrder(true).build("auxClasses",
                new PropertyModel<List<String>>(entityTO, "auxClasses"),
                new ListModel<>(choices)).setOutputMarkupId(true));
    }
}
