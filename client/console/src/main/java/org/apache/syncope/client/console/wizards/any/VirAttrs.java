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
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public class VirAttrs extends AbstractAttrs<VirSchemaTO> {

    private static final long serialVersionUID = -7982691107029848579L;

    private final AjaxWizard.Mode mode;

    public <T extends AnyTO> VirAttrs(
            final T anyTO,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichVirAttrs) {

        super(anyTO, anyTypeClasses, whichVirAttrs);
        this.mode = mode;

        setTitleModel(new ResourceModel("attributes.virtual"));

        add(new Accordion("virSchemas", Collections.<ITab>singletonList(new AbstractTab(
                new ResourceModel("attributes.accordion", "Virtual Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new VirAttrs.VirSchemas(panelId, attrTOs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<MembershipTO>("membershipsVirSchemas", membershipTOs) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                final MembershipTO membershipTO = item.getModelObject();
                item.add(new Accordion("membershipVirSchemas",
                        Collections.<ITab>singletonList(new AbstractTab(new StringResourceModel(
                                "attributes.membership.accordion", VirAttrs.this, Model.of(membershipTO))) {

                            private static final long serialVersionUID = 1037272333056449378L;

                            @Override
                            public WebMarkupContainer getPanel(final String panelId) {
                                return new VirAttrs.VirSchemas(panelId, new ListModel<>(getAttrsFromTO(membershipTO)));
                            }
                        }), Model.of(-1)).setOutputMarkupId(true));
            }
        });
    }

    @Override
    protected SchemaType getSchemaType() {
        return SchemaType.VIRTUAL;
    }

    @Override
    protected List<AttrTO> getAttrsFromTO() {
        final List<AttrTO> res = new ArrayList<>(anyTO.getVirAttrs());
        Collections.sort(res, new AttrComparator());
        return res;
    }

    @Override
    protected List<AttrTO> getAttrsFromTO(final MembershipTO membershipTO) {
        final List<AttrTO> res = new ArrayList<>(membershipTO.getVirAttrs());
        Collections.sort(res, new AttrComparator());
        return res;
    }

    @Override
    protected void setAttrs() {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = anyTO.getVirAttrMap();

        for (VirSchemaTO schema : schemas.values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            } else {
                attrTO.getValues().add(StringUtils.EMPTY);
            }

            attrs.add(attrTO);
        }

        anyTO.getVirAttrs().clear();
        anyTO.getVirAttrs().addAll(attrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = membershipTO.getVirAttrMap();

        for (VirSchemaTO schema : membershipSchemas.get(membershipTO.getGroupKey()).values()) {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            } else {
                attrTO.getValues().add(StringUtils.EMPTY);
            }

            attrs.add(attrTO);
        }

        membershipTO.getVirAttrs().clear();
        membershipTO.getVirAttrs().addAll(attrs);
    }

    public class VirSchemas extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public VirSchemas(final String id, final IModel<List<AttrTO>> attrTOs) {
            super(id);

            add(new ListView<AttrTO>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                @SuppressWarnings("unchecked")
                protected void populateItem(final ListItem<AttrTO> item) {
                    AttrTO attrTO = item.getModelObject();

                    AjaxTextFieldPanel panel =
                            new AjaxTextFieldPanel("panel", attrTO.getSchema(), new Model<String>(), false);

                    boolean readonly = attrTO.getSchemaInfo() == null
                            ? false
                            : VirSchemaTO.class.cast(attrTO.getSchemaInfo()).isReadonly();

                    if (mode == AjaxWizard.Mode.TEMPLATE) {
                        item.add(panel.enableJexlHelp().setEnabled(!readonly));
                    } else {
                        item.add(new MultiFieldPanel.Builder<>(
                                new PropertyModel<List<String>>(attrTO, "values")).build(
                                "panel",
                                attrTO.getSchema(),
                                panel).setEnabled(!readonly));
                    }
                }
            });
        }
    }
}
