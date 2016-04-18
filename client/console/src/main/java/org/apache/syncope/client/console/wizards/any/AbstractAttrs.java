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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.wicket.extensions.wizard.WizardStep;

public abstract class AbstractAttrs extends WizardStep {

    private static final long serialVersionUID = -5387344116983102292L;

    protected final SchemaRestClient schemaRestClient = new SchemaRestClient();

    protected final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final GroupRestClient groupRestClient = new GroupRestClient();

    protected final AnyTO entityTO;

    public AbstractAttrs(final AnyTO entityTO) {
        this.entityTO = entityTO;
    }

    protected Set<String> getAllAuxClasses() {
        final List<MembershipTO> memberships;
        final List<String> dyngroups;
        if (entityTO instanceof UserTO) {
            memberships = UserTO.class.cast(entityTO).getMemberships();
            dyngroups = UserTO.class.cast(entityTO).getDynGroups();
        } else if (entityTO instanceof AnyObjectTO) {
            memberships = AnyObjectTO.class.cast(entityTO).getMemberships();
            dyngroups = AnyObjectTO.class.cast(entityTO).getDynGroups();
        } else {
            memberships = Collections.<MembershipTO>emptyList();
            dyngroups = Collections.<String>emptyList();
        }

        final List<GroupTO> groups = new ArrayList<>();
        CollectionUtils.collect(memberships, new Transformer<MembershipTO, GroupTO>() {

            @Override
            public GroupTO transform(final MembershipTO input) {
                dyngroups.remove(input.getRightKey());
                return groupRestClient.read(input.getRightKey());
            }
        }, groups);

        CollectionUtils.collect(dyngroups, new Transformer<String, GroupTO>() {

            @Override
            public GroupTO transform(final String input) {
                return groupRestClient.read(input);
            }
        }, groups);

        final Set<String> auxClasses = new HashSet<>(entityTO.getAuxClasses());
        for (GroupTO groupTO : groups) {
            auxClasses.addAll(groupTO.getAuxClasses());
        }

        return auxClasses;
    }
}
