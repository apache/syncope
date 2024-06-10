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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.enduser.layout.CustomizationOption;
import org.apache.syncope.client.enduser.rest.SchemaRestClient;
import org.apache.syncope.client.enduser.rest.SyncopeRestClient;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxPalettePanel;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class AbstractAttrs<S extends SchemaTO> extends Panel {

    private static final long serialVersionUID = -5387344116983102292L;

    protected static final String FORM_SUFFIX = "form_";

    @SpringBean
    protected SchemaRestClient schemaRestClient;

    @SpringBean
    protected SyncopeRestClient syncopeRestClient;

    protected final Comparator<Attr> attrComparator = new AttrComparator();

    protected final UserTO userTO;

    protected final Map<String, CustomizationOption> whichAttrs;

    protected final Map<String, S> schemas = new LinkedHashMap<>();

    protected final Map<String, Map<String, S>> membershipSchemas = new LinkedHashMap<>();

    protected final IModel<List<Attr>> attrs;

    protected final IModel<List<MembershipTO>> membershipTOs;

    private final List<String> anyTypeClasses;

    public AbstractAttrs(
            final String id,
            final AnyWrapper<UserTO> modelObject,
            final List<String> anyTypeClasses,
            final Map<String, CustomizationOption> whichAttrs) {

        super(id);
        this.anyTypeClasses = anyTypeClasses;
        this.attrs = new ListModel<>(List.of());
        this.membershipTOs = new ListModel<>(List.of());

        this.setOutputMarkupId(true);

        this.userTO = modelObject.getInnerObject();
        this.whichAttrs = whichAttrs;

        evaluate();
    }

    protected List<Attr> loadAttrs() {
        List<String> classes = new ArrayList<>(anyTypeClasses);
        // just add keys
        classes.addAll(userTO.getAuxClasses());
        setSchemas(classes);
        setAttrs();
        return AbstractAttrs.this.getAttrsFromTO();
    }

    @SuppressWarnings({ "unchecked" })
    protected List<MembershipTO> loadMembershipAttrs() {
        List<MembershipTO> memberships = new ArrayList<>();

        membershipSchemas.clear();

        for (MembershipTO membership : userTO.getMemberships()) {
            setSchemas(Pair.of(
                    membership.getGroupKey(), membership.getGroupName()),
                    getMembershipAuxClasses(membership));
            setAttrs(membership);

            if (AbstractAttrs.this instanceof PlainAttrs && !membership.getPlainAttrs().isEmpty()) {
                memberships.add(membership);
            } else if (AbstractAttrs.this instanceof DerAttrs && !membership.getDerAttrs().isEmpty()) {
                memberships.add(membership);
            } else if (AbstractAttrs.this instanceof VirAttrs && !membership.getVirAttrs().isEmpty()) {
                memberships.add(membership);
            }
        }

        return memberships;
    }

    protected boolean filterSchemas() {
        return !whichAttrs.isEmpty();
    }

    protected boolean renderAsReadonly(final String schema, final String groupName) {
        // whether to render the attribute as readonly or not, without considering schema readonly property
        String schemaName = (StringUtils.isBlank(groupName)
                ? StringUtils.EMPTY
                : groupName + '#')
                + schema;
        return whichAttrs.get(schemaName) == null ? false : whichAttrs.get(schemaName).isReadonly();
    }

    protected List<String> getDefaultValues(final String schema) {
        return getDefaultValues(schema, null);
    }

    protected List<String> getDefaultValues(final String schema, final String groupName) {
        String schemaName = (StringUtils.isBlank(groupName)
                ? StringUtils.EMPTY
                : groupName + '#')
                + schema;
        return whichAttrs.get(schemaName) == null
                ? List.of()
                : whichAttrs.get(schemaName).getDefaultValues();
    }

    protected abstract SchemaType getSchemaType();

    protected void setSchemas(final Pair<String, String> membership, final List<String> anyTypeClasses) {
        final Map<String, S> mscs;

        if (membershipSchemas.containsKey(membership.getKey())) {
            mscs = membershipSchemas.get(membership.getKey());
        } else {
            mscs = new LinkedHashMap<>();
            membershipSchemas.put(membership.getKey(), mscs);
        }
        setSchemas(anyTypeClasses, membership.getValue(), mscs);
    }

    protected void setSchemas(final List<String> anyTypeClasses) {
        setSchemas(anyTypeClasses, null, schemas);
    }

    protected void setSchemas(final List<String> anyTypeClasses, final String groupName, final Map<String, S> scs) {
        final List<S> allSchemas;
        if (anyTypeClasses.isEmpty()) {
            allSchemas = new ArrayList<>();
        } else {
            allSchemas = schemaRestClient.getSchemas(getSchemaType(), null, anyTypeClasses.toArray(String[]::new));
        }

        scs.clear();

        if (filterSchemas()) {
            // 1. remove attributes not selected for display
            allSchemas.removeAll(allSchemas.stream().
                    filter(schemaTO -> StringUtils.isBlank(groupName)
                    ? !whichAttrs.containsKey(schemaTO.getKey())
                    : !whichAttrs.containsKey(groupName + '#' + schemaTO.getKey())).collect(Collectors.toSet()));
        }

        allSchemas.forEach(schemaTO -> scs.put(schemaTO.getKey(), schemaTO));
    }

    public boolean isPanelVisible() {
        return !attrs.getObject().isEmpty() || !membershipTOs.getObject().isEmpty();
    }

    protected abstract void setAttrs();

    protected abstract void setAttrs(MembershipTO membershipTO);

    protected abstract List<Attr> getAttrsFromTO();

    protected abstract List<Attr> getAttrsFromTO(MembershipTO membershipTO);

    protected List<String> getMembershipAuxClasses(final MembershipTO membershipTO) {
        try {
            return syncopeRestClient.searchUserTypeExtensions(membershipTO.getGroupName());
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    protected void onInitialize() {
        evaluate();
        super.onInitialize();
    }

    public boolean evaluate() {
        this.attrs.setObject(loadAttrs());
        this.membershipTOs.setObject(loadMembershipAttrs());
        return !attrs.getObject().isEmpty() || !membershipTOs.getObject().isEmpty();
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        super.onEvent(event);
        if (event.getPayload() instanceof AjaxPalettePanel.UpdateActionEvent updateActionEvent) {
            evaluate();
            AjaxPalettePanel.UpdateActionEvent updateEvent = updateActionEvent;
            updateEvent.getTarget().add(this);
        }
    }

    protected class AttrComparator implements Comparator<Attr>, Serializable {

        private static final long serialVersionUID = -5105030477767941060L;

        @Override
        public int compare(final Attr left, final Attr right) {
            if (left == null || StringUtils.isBlank(left.getSchema())) {
                return -1;
            }
            if (right == null || StringUtils.isBlank(right.getSchema())) {
                return 1;
            } else if (AbstractAttrs.this.filterSchemas()) {
                int leftIndex = new ArrayList<>(AbstractAttrs.this.whichAttrs.keySet()).indexOf(left.getSchema());
                int rightIndex = new ArrayList<>(AbstractAttrs.this.whichAttrs.keySet()).indexOf(right.getSchema());

                if (leftIndex > rightIndex) {
                    return 1;
                } else if (leftIndex < rightIndex) {
                    return -1;
                } else {
                    return 0;
                }
            } else {
                return left.getSchema().compareTo(right.getSchema());
            }
        }
    }

    protected static class Schemas extends Panel {

        private static final long serialVersionUID = -2447602429647965090L;

        public Schemas(final String id) {
            super(id);
        }
    }
}
