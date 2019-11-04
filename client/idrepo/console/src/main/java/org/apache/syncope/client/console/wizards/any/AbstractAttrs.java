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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.syncope.client.console.rest.AnyTypeClassRestClient;
import org.apache.syncope.client.console.rest.GroupRestClient;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.core.util.lang.PropertyResolver;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

public abstract class AbstractAttrs<S extends SchemaTO> extends AbstractAttrsWizardStep<S> {

    private static final long serialVersionUID = -5387344116983102292L;

    private final GroupRestClient groupRestClient = new GroupRestClient();

    protected final IModel<List<MembershipTO>> membershipTOs;

    protected final Map<String, Map<String, S>> membershipSchemas = new LinkedHashMap<>();

    public AbstractAttrs(
            final AnyWrapper<?> modelObject,
            final List<String> anyTypeClasses,
            final List<String> whichAttrs) {

        super(modelObject.getInnerObject(), AjaxWizard.Mode.CREATE, anyTypeClasses, whichAttrs, null);

        this.membershipTOs = new ListModel<>(Collections.<MembershipTO>emptyList());

        this.setOutputMarkupId(true);
    }

    @SuppressWarnings("unchecked")
    private List<MembershipTO> loadMembershipAttrs() {
        List<MembershipTO> memberships = new ArrayList<>();
        try {
            membershipSchemas.clear();

            for (MembershipTO membership : (List<MembershipTO>) PropertyResolver.getPropertyField(
                    "memberships", anyTO).get(anyTO)) {
                setSchemas(membership.getGroupKey(),
                        AnyTypeClassRestClient.list(getMembershipAuxClasses(membership, anyTO.getType())).
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

    protected List<String> getMembershipAuxClasses(final MembershipTO membershipTO, final String anyType) {
        try {
            final GroupTO groupTO = groupRestClient.read(membershipTO.getGroupKey());
            return groupTO.getTypeExtension(anyType).get().getAuxClasses();
        } catch (Exception e) {
            return List.of();
        }
    }

    protected abstract void setAttrs(MembershipTO membershipTO);

    protected abstract List<Attr> getAttrsFromTO(MembershipTO membershipTO);

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        if (CollectionUtils.isEmpty(attrs.getObject())
                && CollectionUtils.isEmpty(membershipTOs.getObject())) {
            response.render(OnDomReadyHeaderItem.forScript(
                    String.format("$('#emptyPlaceholder').append(\"%s\"); $('#attributes').hide();",
                            getString("attribute.empty.list"))));
        }
    }

    @Override
    public boolean evaluate() {
        this.attrs.setObject(loadAttrs());
        this.membershipTOs.setObject(loadMembershipAttrs());
        return !attrs.getObject().isEmpty() || !membershipTOs.getObject().isEmpty();
    }

}
