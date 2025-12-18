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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.console.rest.RelationshipTypeRestClient;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.RelatableTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public abstract class AbstractAttrs<S extends SchemaTO> extends AbstractAttrsWizardStep<S> {

    private static final long serialVersionUID = -5387344116983102292L;

    @SpringBean
    protected GroupRestClient groupRestClient;

    @SpringBean
    protected RelationshipTypeRestClient relationshipTypeRestClient;

    protected final IModel<List<MembershipTO>> memberships;

    protected final IModel<List<RelationshipTO>> relationships;

    protected final Map<String, Map<String, S>> membershipSchemas = new LinkedHashMap<>();

    protected final Map<Pair<String, String>, Map<String, S>> relationshipSchemas = new LinkedHashMap<>();

    public AbstractAttrs(
            final AnyWrapper<?> modelObject,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichAttrs) {

        super(modelObject.getInnerObject(), mode, anyTypeClasses, whichAttrs);

        this.memberships = new ListModel<>(List.of());
        this.relationships = new ListModel<>(List.of());

        this.setOutputMarkupId(true);
    }

    private List<MembershipTO> loadMemberships() {
        if (attributable instanceof GroupableRelatableTO anyTO) {
            membershipSchemas.clear();

            List<MembershipTO> membs = new ArrayList<>();
            anyTO.getMemberships().forEach(memb -> {
                setSchemas(memb.getGroupKey(),
                        anyTypeClassRestClient.list(getMembershipAuxClasses(
                                memb.getGroupKey(), ((AnyTO) anyTO).getType())).stream().
                                map(AnyTypeClassTO::getKey).collect(Collectors.toList()));
                setAttrs(memb);

                if (this instanceof PlainAttrs && !memb.getPlainAttrs().isEmpty()) {
                    membs.add(memb);
                } else if (this instanceof DerAttrs && !memb.getDerAttrs().isEmpty()) {
                    membs.add(memb);
                }
            });

            return membs;
        }

        return List.of();
    }

    private List<RelationshipTO> loadRelationships() {
        if (attributable instanceof RelatableTO anyTO) {
            relationshipSchemas.clear();

            List<RelationshipTO> rels = new ArrayList<>();
            anyTO.getRelationships().forEach(rel -> {
                setSchemas(Pair.of(rel.getType(), rel.getOtherEndKey()),
                        anyTypeClassRestClient.list(getRelationshipAuxClasses(
                                rel.getType(), ((AnyTO) anyTO).getType())).stream().
                                map(AnyTypeClassTO::getKey).collect(Collectors.toList()));
                setAttrs(rel);

                if (this instanceof PlainAttrs && !rel.getPlainAttrs().isEmpty()) {
                    rels.add(rel);
                } else if (this instanceof DerAttrs && !rel.getDerAttrs().isEmpty()) {
                    rels.add(rel);
                }
            });

            return rels;
        }

        return List.of();
    }

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

    private void setSchemas(final Pair<String, String> relationship, final List<String> anyTypeClasses) {
        final Map<String, S> mscs;

        if (relationshipSchemas.containsKey(relationship)) {
            mscs = relationshipSchemas.get(relationship);
        } else {
            mscs = new LinkedHashMap<>();
            relationshipSchemas.put(relationship, mscs);
        }
        setSchemas(anyTypeClasses, mscs);
    }

    protected List<String> getMembershipAuxClasses(final String group, final String anyType) {
        try {
            GroupTO groupTO = groupRestClient.read(group);
            return groupTO.getTypeExtension(anyType).map(TypeExtensionTO::getAuxClasses).orElseGet(List::of);
        } catch (Exception e) {
            return List.of();
        }
    }

    protected List<String> getRelationshipAuxClasses(final String relationshipType, final String anyType) {
        try {
            RelationshipTypeTO typeTO = relationshipTypeRestClient.read(relationshipType);
            return typeTO.getTypeExtension(anyType).map(TypeExtensionTO::getAuxClasses).orElseGet(List::of);
        } catch (Exception e) {
            return List.of();
        }
    }

    protected abstract void setAttrs(MembershipTO membershipTO);

    protected abstract void setAttrs(RelationshipTO relationshipTO);

    protected abstract List<Attr> getAttrsFromTO(MembershipTO membershipTO);

    protected abstract List<Attr> getAttrsFromTO(RelationshipTO relationshipTO);

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (CollectionUtils.isEmpty(attrs.getObject())
                && CollectionUtils.isEmpty(memberships.getObject())
                && CollectionUtils.isEmpty(relationships.getObject())) {

            response.render(OnDomReadyHeaderItem.forScript(
                    String.format("$('#emptyPlaceholder').append(\"%s\"); $('#attributes').hide();",
                            getString("attribute.empty.list"))));
        }
    }

    @Override
    public boolean evaluate() {
        this.attrs.setObject(loadAttrs());
        this.memberships.setObject(loadMemberships());
        this.relationships.setObject(loadRelationships());
        return !attrs.getObject().isEmpty()
                || !memberships.getObject().isEmpty()
                || !relationships.getObject().isEmpty();
    }
}
