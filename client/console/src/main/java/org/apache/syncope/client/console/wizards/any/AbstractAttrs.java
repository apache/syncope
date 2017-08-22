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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.cxf.common.util.StringUtils;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.extensions.wizard.WizardModel.ICondition;
import org.apache.wicket.extensions.wizard.WizardStep;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public abstract class AbstractAttrs<S extends AbstractSchemaTO> extends WizardStep implements ICondition {

    private static final long serialVersionUID = -5387344116983102292L;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final AnyTypeClassRestClient anyTypeClassRestClient = new AnyTypeClassRestClient();

    private final GroupRestClient groupRestClient = new GroupRestClient();

    protected final AnyTO anyTO;

    private final List<String> whichAttrs;

    protected final Map<String, S> schemas = new LinkedHashMap<>();

    protected final Map<String, Map<String, S>> membershipSchemas = new LinkedHashMap<>();

    protected final IModel<List<AttrTO>> attrTOs;

    protected final IModel<List<MembershipTO>> membershipTOs;

    private final List<String> anyTypeClasses;

    public AbstractAttrs(
            final AnyWrapper<?> modelObject,
            final List<String> anyTypeClasses,
            final List<String> whichAttrs) {
        super();
        this.anyTypeClasses = anyTypeClasses;
        this.attrTOs = new ListModel<>(Collections.<AttrTO>emptyList());
        this.membershipTOs = new ListModel<>(Collections.<MembershipTO>emptyList());

        this.setOutputMarkupId(true);

        this.anyTO = modelObject.getInnerObject();
        this.whichAttrs = whichAttrs;
    }

    private List<AttrTO> loadAttrTOs() {
        List<String> classes = new ArrayList<>(anyTypeClasses);
        classes.addAll(anyTypeClassRestClient.list(anyTO.getAuxClasses()).stream().
                map(EntityTO::getKey).collect(Collectors.toList()));
        setSchemas(classes);
        setAttrs();
        return AbstractAttrs.this.getAttrsFromTO();
    }

    @SuppressWarnings("unchecked")
    private List<MembershipTO> loadMembershipAttrTOs() {
        List<MembershipTO> memberships = new ArrayList<>();
        try {
            membershipSchemas.clear();

            for (MembershipTO membership : (List<MembershipTO>) PropertyResolver.getPropertyField(
                    "memberships", anyTO).get(anyTO)) {
                setSchemas(membership.getGroupKey(),
                        anyTypeClassRestClient.list(getMembershipAuxClasses(membership, anyTO.getType())).
                                stream().map(EntityTO::getKey).collect(Collectors.toList()));
                setAttrs(membership);

                if (AbstractAttrs.this instanceof PlainAttrs && !membership.getPlainAttrs().isEmpty()) {
                    memberships.add(membership);
                } else if (AbstractAttrs.this instanceof DerAttrs && !membership.getDerAttrs().isEmpty()) {
                    memberships.add(membership);
                } else if (AbstractAttrs.this instanceof VirAttrs && !membership.getVirAttrs().isEmpty()) {
                    memberships.add(membership);
                }
            }
        } catch (WicketRuntimeException | IllegalArgumentException | IllegalAccessException ex) {
            // ignore
        }

        return memberships;
    }

    protected boolean reoderSchemas() {
        return !whichAttrs.isEmpty();
    }

    protected abstract SchemaType getSchemaType();

    private void setSchemas(final String membership, final List<String> anyTypeClasses) {
        final Map<String, S> mscs;

        if (membershipSchemas.containsKey(membership)) {
            mscs = membershipSchemas.get(membership);
        } else {
            mscs = new LinkedHashMap<>();
            membershipSchemas.put(membership, mscs);
        }
        setSchemas(anyTypeClasses, mscs);
    }

    private void setSchemas(final List<String> anyTypeClasses) {
        setSchemas(anyTypeClasses, schemas);
    }

    private void setSchemas(final List<String> anyTypeClasses, final Map<String, S> scs) {
        final List<S> allSchemas;
        if (anyTypeClasses.isEmpty()) {
            allSchemas = Collections.emptyList();
        } else {
            allSchemas = schemaRestClient.getSchemas(getSchemaType(), anyTypeClasses.toArray(new String[] {}));
        }

        scs.clear();

        if (reoderSchemas()) {
            // 1. remove attributes not selected for display
            allSchemas.removeAll(allSchemas.stream().
                    filter(schemaTO -> !whichAttrs.contains(schemaTO.getKey())).collect(Collectors.toSet()));

            // 2. sort remainig attributes according to configuration, e.g. attrLayout
            final Map<String, Integer> attrLayoutMap = new HashMap<>(whichAttrs.size());
            for (int i = 0; i < whichAttrs.size(); i++) {
                attrLayoutMap.put(whichAttrs.get(i), i);
            }
            Collections.sort(allSchemas, (schema1, schema2) -> {
                int value = 0;

                if (attrLayoutMap.get(schema1.getKey()) > attrLayoutMap.get(schema2.getKey())) {
                    value = 1;
                } else if (attrLayoutMap.get(schema1.getKey()) < attrLayoutMap.get(schema2.getKey())) {
                    value = -1;
                }

                return value;
            });
        }
        allSchemas.forEach(schemaTO -> {
            scs.put(schemaTO.getKey(), schemaTO);
        });
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (org.apache.cxf.common.util.CollectionUtils.isEmpty(attrTOs.getObject())
                && org.apache.cxf.common.util.CollectionUtils.isEmpty(membershipTOs.getObject())) {
            response.render(OnDomReadyHeaderItem.forScript(
                    String.format("$('#emptyPlaceholder').append(\"%s\"); $('#attributes').hide();",
                            getString("attribute.empty.list"))));
        }
    }

    protected abstract void setAttrs();

    protected abstract void setAttrs(final MembershipTO membershipTO);

    protected abstract List<AttrTO> getAttrsFromTO();

    protected abstract List<AttrTO> getAttrsFromTO(final MembershipTO membershipTO);

    protected List<String> getMembershipAuxClasses(final MembershipTO membershipTO, final String anyType) {
        try {
            final GroupTO groupTO = groupRestClient.read(membershipTO.getRightKey());
            return groupTO.getTypeExtension(anyType).get().getAuxClasses();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean evaluate() {
        this.attrTOs.setObject(loadAttrTOs());
        this.membershipTOs.setObject(loadMembershipAttrTOs());
        return !attrTOs.getObject().isEmpty() || !membershipTOs.getObject().isEmpty();
    }

    protected static class AttrComparator implements Comparator<AttrTO>, Serializable {

        private static final long serialVersionUID = -5105030477767941060L;

        @Override
        public int compare(final AttrTO left, final AttrTO right) {
            if (left == null || StringUtils.isEmpty(left.getSchema())) {
                return -1;
            }
            if (right == null || StringUtils.isEmpty(right.getSchema())) {
                return 1;
            } else {
                return left.getSchema().compareTo(right.getSchema());
            }
        }
    }

    public class Schemas extends Panel {

        private static final long serialVersionUID = -2447602429647965090L;

        public Schemas(final String id) {
            super(id);
        }
    }
}
