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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.model.LoadableDetachableModel;

public abstract class AbstractAttrs<S extends AbstractSchemaTO> extends WizardStep {

    private static final long serialVersionUID = -5387344116983102292L;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final GroupRestClient groupRestClient = new GroupRestClient();

    protected final AnyTO anyTO;

    private final List<String> whichAttrs;

    protected final Map<String, S> schemas = new LinkedHashMap<>();

    protected final LoadableDetachableModel<List<AttrTO>> attrTOs;

    public AbstractAttrs(final AnyTO anyTO, final List<String> anyTypeClasses, final List<String> whichAttrs) {
        super();
        this.setOutputMarkupId(true);

        this.anyTO = anyTO;
        this.whichAttrs = whichAttrs;

        this.attrTOs = new LoadableDetachableModel<List<AttrTO>>() {

            private static final long serialVersionUID = 5275935387613157437L;

            @Override
            protected List<AttrTO> load() {
                setSchemas(CollectionUtils.collect(anyTypeClassRestClient.list(getAllAuxClasses()),
                        EntityTOUtils.<AnyTypeClassTO>keyTransformer(), new ArrayList<>(anyTypeClasses)));
                setAttrs();
                return new ArrayList<>(getAttrsFromAnyTO());
            }
        };
    }

    protected boolean reoderSchemas() {
        return !whichAttrs.isEmpty();
    }

    protected abstract SchemaType getSchemaType();

    private void setSchemas(final List<String> anyTypeClasses) {
        List<S> allSchemas = Collections.emptyList();
        if (!anyTypeClasses.isEmpty()) {
            allSchemas = schemaRestClient.getSchemas(getSchemaType(), anyTypeClasses.toArray(new String[] {}));
        }

        schemas.clear();

        if (reoderSchemas()) {
            // 1. remove attributes not selected for display
            CollectionUtils.filter(allSchemas, new Predicate<S>() {

                @Override
                public boolean evaluate(final S schemaTO) {
                    return whichAttrs.contains(schemaTO.getKey());
                }
            });

            // 2. sort remainig attributes according to configuration, e.g. attrLayout
            final Map<String, Integer> attrLayoutMap = new HashMap<>(whichAttrs.size());
            for (int i = 0; i < whichAttrs.size(); i++) {
                attrLayoutMap.put(whichAttrs.get(i), i);
            }
            Collections.sort(allSchemas, new Comparator<S>() {

                @Override
                public int compare(final S schema1, final S schema2) {
                    int value = 0;

                    if (attrLayoutMap.get(schema1.getKey()) > attrLayoutMap.get(schema2.getKey())) {
                        value = 1;
                    } else if (attrLayoutMap.get(schema1.getKey()) < attrLayoutMap.get(schema2.getKey())) {
                        value = -1;
                    }

                    return value;
                }
            });
        }
        for (S schemaTO : allSchemas) {
            schemas.put(schemaTO.getKey(), schemaTO);
        }
    }

    protected abstract void setAttrs();

    protected abstract Set<AttrTO> getAttrsFromAnyTO();

    protected Set<String> getAllAuxClasses() {
        final List<MembershipTO> memberships;
        final List<String> dyngroups;
        if (anyTO instanceof UserTO) {
            memberships = UserTO.class.cast(anyTO).getMemberships();
            dyngroups = UserTO.class.cast(anyTO).getDynGroups();
        } else if (anyTO instanceof AnyObjectTO) {
            memberships = AnyObjectTO.class.cast(anyTO).getMemberships();
            dyngroups = AnyObjectTO.class.cast(anyTO).getDynGroups();
        } else {
            memberships = Collections.<MembershipTO>emptyList();
            dyngroups = Collections.<String>emptyList();
        }

        List<GroupTO> groups = new ArrayList<>();
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

        Set<String> auxClasses = new HashSet<>(anyTO.getAuxClasses());
        for (GroupTO groupTO : groups) {
            auxClasses.addAll(groupTO.getAuxClasses());
        }

        return auxClasses;
    }
}
