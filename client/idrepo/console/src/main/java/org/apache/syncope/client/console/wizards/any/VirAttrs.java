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
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.ui.commons.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.ui.commons.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.ui.commons.wizards.any.AnyWrapper;
import org.apache.syncope.client.ui.commons.wizards.any.UserWrapper;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.VirSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
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

    private final AnyWrapper<?> modelObject;

    public <T extends AnyTO> VirAttrs(
            final AnyWrapper<T> modelObject,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichVirAttrs) {

        super(modelObject, mode, anyTypeClasses, whichVirAttrs);
        this.modelObject = modelObject;

        setTitleModel(new ResourceModel("attributes.virtual"));

        add(new Accordion("virSchemas", List.of(new AbstractTab(
                new ResourceModel("attributes.accordion", "Virtual Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new VirAttrs.VirSchemas(panelId, schemas, attrs);
            }
        }), Model.of(0)).setOutputMarkupId(true));

        add(new ListView<>("membershipsVirSchemas", memberships) {

            private static final long serialVersionUID = 9101744072914090143L;

            @Override
            protected void populateItem(final ListItem<MembershipTO> item) {
                MembershipTO membTO = item.getModelObject();
                item.add(new Accordion("membershipVirSchemas", List.of(new AbstractTab(
                    new StringResourceModel("attributes.membership.accordion", VirAttrs.this, Model.of(membTO))) {

                    private static final long serialVersionUID = 1037272333056449378L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new VirAttrs.VirSchemas(
                            panelId,
                            membershipSchemas.get(membTO.getGroupKey()),
                            new ListModel<>(getAttrsFromTO(membTO)));
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
    protected List<Attr> getAttrsFromTO() {
        return anyTO.getVirAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<Attr> getAttrsFromTO(final MembershipTO membershipTO) {
        return membershipTO.getVirAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected void setAttrs() {
        List<Attr> virAttrs = new ArrayList<>();

        Map<String, Attr> attrMap = EntityTOUtils.buildAttrMap(anyTO.getVirAttrs());

        virAttrs.addAll(schemas.values().stream().map(schema -> {
            Attr attrTO = new Attr();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            } else {
                attrTO.getValues().add(StringUtils.EMPTY);
            }
            return attrTO;
        }).toList());

        anyTO.getVirAttrs().clear();
        anyTO.getVirAttrs().addAll(virAttrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        Map<String, Attr> attrMap = GroupableRelatableTO.class.cast(anyTO).getMembership(membershipTO.getGroupKey()).
                map(gr -> EntityTOUtils.buildAttrMap(gr.getVirAttrs())).
                orElseGet(HashMap::new);

        List<Attr> virAttrs = membershipSchemas.get(membershipTO.getGroupKey()).values().stream().map(schema -> {
            Attr attr = new Attr();
            attr.setSchema(schema.getKey());
            if (attrMap.get(schema.getKey()) == null || attrMap.get(schema.getKey()).getValues().isEmpty()) {
                attr.getValues().add(StringUtils.EMPTY);
            } else {
                attr.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            }
            return attr;
        }).toList();

        membershipTO.getVirAttrs().clear();
        membershipTO.getVirAttrs().addAll(virAttrs);
    }

    public class VirSchemas extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public VirSchemas(
                final String id,
                final Map<String, VirSchemaTO> schemas,
                final IModel<List<Attr>> attrTOs) {
            super(id);

            add(new ListView<>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                @SuppressWarnings("unchecked")
                protected void populateItem(final ListItem<Attr> item) {
                    Attr attrTO = item.getModelObject();
                    VirSchemaTO virSchemaTO = schemas.get(attrTO.getSchema());

                    AbstractFieldPanel<?> panel = new AjaxTextFieldPanel(
                        "panel",
                        virSchemaTO.getLabel(SyncopeConsoleSession.get().getLocale()),
                        new Model<>(),
                        false);

                    if (mode == AjaxWizard.Mode.TEMPLATE) {
                        AjaxTextFieldPanel.class.cast(panel).enableJexlHelp().setEnabled(!virSchemaTO.isReadonly());
                    } else {
                        panel = new MultiFieldPanel.Builder<>(
                            new PropertyModel<List<String>>(attrTO, "values")).build(
                            "panel",
                            virSchemaTO.getLabel(SyncopeConsoleSession.get().getLocale()),
                            AjaxTextFieldPanel.class.cast(panel));
                        panel.setEnabled(!virSchemaTO.isReadonly());
                    }

                    item.add(panel);

                    if (!attrTO.getValues().isEmpty()
                        && VirAttrs.this.modelObject instanceof UserWrapper
                        && UserWrapper.class.cast(VirAttrs.this.modelObject).getPreviousUserTO() != null) {

                        panel.showExternAction(new LabelInfo("externalAction", StringUtils.EMPTY));
                    }
                }
            });
        }
    }
}
