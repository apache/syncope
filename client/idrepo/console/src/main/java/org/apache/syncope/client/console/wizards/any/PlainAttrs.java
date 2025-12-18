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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AttributableTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.to.RelationshipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public class PlainAttrs extends AbstractAttrs<PlainSchemaTO> {

    private static final long serialVersionUID = 552437609667518888L;

    public <T extends AttributableTO> PlainAttrs(
            final AnyWrapper<T> modelObject,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichPlainAttrs) throws IllegalArgumentException {

        super(modelObject, mode, anyTypeClasses, whichPlainAttrs);

        fileKey = modelObject.getInnerObject() instanceof UserTO userTO
                ? userTO.getUsername()
                : modelObject.getInnerObject() instanceof GroupTO groupTO
                ? groupTO.getName()
                : modelObject.getInnerObject() instanceof AnyObjectTO anyObjectTO
                ? anyObjectTO.getName()
                : null;

        previousObject = modelObject instanceof UserWrapper uw ? uw.getPreviousUserTO() : null;

        setTitleModel(new ResourceModel("attributes.plain"));

        add(new Accordion("plainSchemas", List.of(new AbstractTab(
                new ResourceModel("attributes.accordion", "Plain Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new PlainSchemasOwn(panelId, schemas, attrs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<>("membershipsPlainSchemas", memberships) {

            private static final long serialVersionUID = 6741044372185745296L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                MembershipTO membershipTO = item.getModelObject();
                item.add(new Accordion("membershipPlainSchemas", List.of(new AbstractTab(
                        new StringResourceModel(
                                "attributes.membership.accordion",
                                PlainAttrs.this,
                                Model.of(membershipTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new PlainSchemasAttributable(
                                panelId,
                                membershipSchemas.get(membershipTO.getGroupKey()),
                                new LoadableDetachableModel<>() {

                            private static final long serialVersionUID = 526768546610546553L;

                            @Override
                            protected AttributableTO load() {
                                return membershipTO;
                            }
                        });
                    }
                }), Model.of(-1)).setOutputMarkupId(true));
            }
        });

        add(new ListView<>("relationshipsPlainSchemas", relationships) {

            private static final long serialVersionUID = 6741044372185745296L;

            @Override
            protected void populateItem(final ListItem<RelationshipTO> item) {
                RelationshipTO relationshipTO = item.getModelObject();
                item.add(new Accordion("relationshipPlainSchemas", List.of(new AbstractTab(
                        new StringResourceModel(
                                "attributes.relationship.accordion",
                                PlainAttrs.this,
                                Model.of(relationshipTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new PlainSchemasAttributable(
                                panelId,
                                relationshipSchemas.get(Pair.of(
                                        relationshipTO.getType(), relationshipTO.getOtherEndKey())),
                                new LoadableDetachableModel<>() {

                            private static final long serialVersionUID = 526768546610546553L;

                            @Override
                            protected AttributableTO load() {
                                return relationshipTO;
                            }
                        });
                    }
                }), Model.of(-1)).setOutputMarkupId(true));
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.PLAIN;
    }

    @Override
    protected boolean reoderSchemas() {
        return super.reoderSchemas() && mode != AjaxWizard.Mode.TEMPLATE;
    }

    @Override
    protected List<Attr> getAttrsFromTO() {
        return attributable.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<Attr> getAttrsFromTO(final MembershipTO membershipTO) {
        return membershipTO.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<Attr> getAttrsFromTO(final RelationshipTO relationshipTO) {
        return relationshipTO.getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected void setAttrs() {
        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(attributable.getPlainAttrs());

        List<Attr> plainAttrs = schemas.values().stream().map(schema -> {
            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                if (schema.getType() != AttrSchemaType.Dropdown || !schema.isMultivalue()) {
                    attr.getValues().add(StringUtils.EMPTY);
                }
            } else {
                attr = attrMap.get(schema.getKey());
            }
            return attr;
        }).toList();

        attributable.getPlainAttrs().clear();
        attributable.getPlainAttrs().addAll(plainAttrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        Map<String, Attr> attrMap = GroupableRelatableTO.class.cast(attributable).
                getMembership(membershipTO.getGroupKey()).
                map(gr -> EntityTOUtils.buildAttrMap(gr.getPlainAttrs())).
                orElseGet(HashMap::new);

        List<Attr> plainAttrs = membershipSchemas.get(membershipTO.getGroupKey()).values().stream().map(schema -> {
            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                if (schema.getType() != AttrSchemaType.Dropdown || !schema.isMultivalue()) {
                    attr.getValues().add(StringUtils.EMPTY);
                }
            } else {
                attr.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            return attr;
        }).toList();

        membershipTO.getPlainAttrs().clear();
        membershipTO.getPlainAttrs().addAll(plainAttrs);
    }

    @Override
    protected void setAttrs(final RelationshipTO relationshipTO) {
        Map<String, Attr> attrMap = GroupableRelatableTO.class.cast(attributable).
                getRelationship(relationshipTO.getType(), relationshipTO.getOtherEndKey()).
                map(gr -> EntityTOUtils.buildAttrMap(gr.getPlainAttrs())).
                orElseGet(HashMap::new);

        List<Attr> plainAttrs = relationshipSchemas.get(
                Pair.of(relationshipTO.getType(), relationshipTO.getOtherEndKey())).values().stream().map(schema -> {

            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                if (schema.getType() != AttrSchemaType.Dropdown || !schema.isMultivalue()) {
                    attr.getValues().add(StringUtils.EMPTY);
                }
            } else {
                attr.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            return attr;
        }).toList();

        relationshipTO.getPlainAttrs().clear();
        relationshipTO.getPlainAttrs().addAll(plainAttrs);
    }

    protected class PlainSchemasOwn extends PlainSchemas<List<Attr>> {

        private static final long serialVersionUID = -4730563859116024676L;

        public PlainSchemasOwn(
                final String id,
                final Map<String, PlainSchemaTO> schemas,
                final IModel<List<Attr>> attrTOs) {

            super(id);

            add(new ListView<>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                protected void populateItem(final ListItem<Attr> item) {
                    PlainSchemaTO schema = schemas.get(item.getModelObject().getSchema());
                    setPanel(schemas, item, schema == null ? false : schema.isReadonly());
                }
            });
        }
    }

    protected class PlainSchemasAttributable extends PlainSchemas<AttributableTO> {

        private static final long serialVersionUID = 456754923340249215L;

        public PlainSchemasAttributable(
                final String id,
                final Map<String, PlainSchemaTO> schemas,
                final IModel<AttributableTO> attributableTO) {

            super(id);

            add(new ListView<>("schemas", new ListModel<>(attributableTO.getObject().
                    getPlainAttrs().stream().sorted(attrComparator).collect(Collectors.toList()))) {

                private static final long serialVersionUID = 5306618783986001008L;

                @Override
                protected void populateItem(final ListItem<Attr> item) {
                    PlainSchemaTO schema = schemas.get(item.getModelObject().getSchema());
                    setPanel(schemas, item, schema == null ? false : schema.isReadonly());
                }
            });
        }
    }
}
