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
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
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
            final T anyTO,
            final List<String> anyTypeClasses,
            final List<String> whichDerAttrs) {

        super(anyTO, anyTypeClasses, whichDerAttrs);
        setTitleModel(new ResourceModel("attributes.derived"));

        add(new Accordion("derSchemas", Collections.<ITab>singletonList(new AbstractTab(
                new ResourceModel("attributes.accordion", "Derived Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new DerAttrs.DerSchemas(panelId, attrTOs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<MembershipTO>("membershipsDerSchemas", membershipTOs) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                final MembershipTO membershipTO = item.getModelObject();
                item.add(new Accordion("membershipDerSchemas", Collections.<ITab>singletonList(new AbstractTab(
                        new StringResourceModel(
                                "attributes.membership.accordion",
                                DerAttrs.this,
                                Model.of(membershipTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new DerAttrs.DerSchemas(panelId, new ListModel<AttrTO>(getAttrsFromTO(membershipTO)));
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
    protected List<AttrTO> getAttrsFromTO() {
        final List<AttrTO> res = new ArrayList<>(anyTO.getDerAttrs());
        Collections.sort(res, new AttrComparator());
        return res;
    }

    @Override
    protected List<AttrTO> getAttrsFromTO(final MembershipTO membershipTO) {
        final List<AttrTO> res = new ArrayList<>(membershipTO.getDerAttrs());
        Collections.sort(res, new AttrComparator());
        return res;
    }

    @Override
    protected void setAttrs() {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = anyTO.getDerAttrMap();

        for (DerSchemaTO schema : schemas.values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }

            attrs.add(attrTO);
        }

        anyTO.getDerAttrs().clear();
        anyTO.getDerAttrs().addAll(attrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = membershipTO.getDerAttrMap();

        for (DerSchemaTO schema : membershipSchemas.get(membershipTO.getGroupKey()).values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }

            attrs.add(attrTO);
        }

        membershipTO.getDerAttrs().clear();
        membershipTO.getDerAttrs().addAll(attrs);
    }

    public class DerSchemas extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public DerSchemas(final String id, final IModel<List<AttrTO>> attrTOs) {
            super(id);

            add(new ListView<AttrTO>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
                    super.onComponentTagBody(markupStream, openTag);
                    openTag.put("class", "empty");
                }

                @Override
                protected void populateItem(final ListItem<AttrTO> item) {
                    AttrTO attrTO = item.getModelObject();

                    IModel<String> model;
                    List<String> values = attrTO.getValues();
                    if (values == null || values.isEmpty()) {
                        model = new ResourceModel("derived.emptyvalue.message", StringUtils.EMPTY);
                    } else {
                        model = new Model<>(values.get(0));
                    }

                    AjaxTextFieldPanel panel = new AjaxTextFieldPanel("panel", attrTO.getSchema(), model, false);
                    panel.setEnabled(false);
                    panel.setRequired(true);
                    panel.setOutputMarkupId(true);
                    item.add(panel);
                }
            });
        }
    }
}
