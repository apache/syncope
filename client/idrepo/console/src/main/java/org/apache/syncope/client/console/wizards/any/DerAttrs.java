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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public class DerAttrs extends AbstractAttrs<DerSchemaTO> {

    private static final long serialVersionUID = -5387344116983102292L;

    public <T extends AnyTO> DerAttrs(
            final AnyWrapper<T> modelObject,
            final List<String> anyTypeClasses,
            final List<String> whichDerAttrs) {

        super(modelObject, AjaxWizard.Mode.CREATE, anyTypeClasses, whichDerAttrs);
        setTitleModel(new ResourceModel("attributes.derived"));

        add(new Accordion("derSchemas", List.of(new AbstractTab(
                new ResourceModel("attributes.accordion", "Derived Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new DerAttrs.DerSchemas(panelId, schemas, attrs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<>("membershipsDerSchemas", memberships) {

            private static final long serialVersionUID = 6741044372185745296L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                final MembershipTO membershipTO = item.getModelObject();
                item.add(new Accordion("membershipDerSchemas", List.of(new AbstractTab(
                        new StringResourceModel(
                                "attributes.membership.accordion",
                                DerAttrs.this,
                                Model.of(membershipTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new DerAttrs.DerSchemas(
                                panelId,
                                membershipSchemas.get(membershipTO.getGroupKey()),
                                new ListModel<>(getAttrsFromTO(membershipTO)));
                    }
                }), Model.of(-1)).setOutputMarkupId(true));
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.DERIVED;
    }

    @Override
    protected List<Attr> getAttrsFromTO() {
        return anyTO.getDerAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<Attr> getAttrsFromTO(final MembershipTO membershipTO) {
        return membershipTO.getDerAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected void setAttrs() {
        List<Attr> derAttrs = new ArrayList<>();

        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(anyTO.getDerAttrs());

        schemas.values().forEach(schema -> {
            Attr attrTO = new Attr();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }

            derAttrs.add(attrTO);
        });

        anyTO.getDerAttrs().clear();
        anyTO.getDerAttrs().addAll(derAttrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        Map<String, Attr> attrMap = GroupableRelatableTO.class.cast(anyTO).getMembership(membershipTO.getGroupKey()).
                map(gr -> EntityTOUtils.buildAttrMap(gr.getDerAttrs())).
                orElseGet(HashMap::new);

        List<Attr> derAttrs = membershipSchemas.get(membershipTO.getGroupKey()).values().stream().map(schema -> {
            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attr.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }

            return attr;
        }).toList();

        membershipTO.getDerAttrs().clear();
        membershipTO.getDerAttrs().addAll(derAttrs);
    }

    public static class DerSchemas extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public DerSchemas(
                final String id,
                final Map<String, DerSchemaTO> schemas,
                final IModel<List<Attr>> attrTOs) {

            super(id);

            add(new ListView<>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                    super.onComponentTagBody(markupStream, openTag);
                    openTag.put("class", "empty");
                }

                @Override
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attrTO = item.getModelObject();

                    IModel<String> model;
                    List<String> values = attrTO.getValues();
                    if (values == null || values.isEmpty()) {
                        model = new ResourceModel("derived.emptyvalue.message", StringUtils.EMPTY);
                    } else {
                        model = new Model<>(values.getFirst());
                    }

                    AjaxTextFieldPanel panel = new AjaxTextFieldPanel(
                            "panel",
                            schemas.get(attrTO.getSchema()).getLabel(SyncopeConsoleSession.get().getLocale()),
                            model,
                            false);
                    panel.setEnabled(false);
                    panel.setRequired(true);
                    panel.setOutputMarkupId(true);
                    item.add(panel);
                }
            });
        }
    }
}
