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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.wicket.ajax.markup.html.LabelInfo;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.console.wicket.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.GroupableRelatableTO;
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

    private final AnyWrapper<?> modelObject;

    public <T extends AnyTO> VirAttrs(
            final AnyWrapper<T> modelObject,
            final AjaxWizard.Mode mode,
            final List<String> anyTypeClasses,
            final List<String> whichVirAttrs) {

        super(modelObject, anyTypeClasses, whichVirAttrs);
        this.mode = mode;
        this.modelObject = modelObject;

        setTitleModel(new ResourceModel("attributes.virtual"));

        add(new Accordion("virSchemas", Collections.<ITab>singletonList(new AbstractTab(
                new ResourceModel("attributes.accordion", "Virtual Attributes")) {

            private static final long serialVersionUID = 1037272333056449378L;

            @Override
            public WebMarkupContainer getPanel(final String panelId) {
                return new VirAttrs.VirSchemas(panelId, schemas, attrTOs);
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
                                return new VirAttrs.VirSchemas(
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
        return SchemaType.VIRTUAL;
    }

    @Override
    protected List<AttrTO> getAttrsFromTO() {
        return anyTO.getVirAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected List<AttrTO> getAttrsFromTO(final MembershipTO membershipTO) {
        return membershipTO.getVirAttrs().stream().sorted(attrComparator).collect(Collectors.toList());
    }

    @Override
    protected void setAttrs() {
        List<AttrTO> attrs = new ArrayList<>();

        Map<String, AttrTO> attrMap = EntityTOUtils.buildAttrMap(anyTO.getVirAttrs());

        attrs.addAll(schemas.values().stream().map(schema -> {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            } else {
                attrTO.getValues().add(StringUtils.EMPTY);
            }
            return attrTO;
        }).collect(Collectors.toList()));

        anyTO.getVirAttrs().clear();
        anyTO.getVirAttrs().addAll(attrs);
    }

    @Override
    protected void setAttrs(final MembershipTO membershipTO) {
        List<AttrTO> attrs = new ArrayList<>();

        final Map<String, AttrTO> attrMap;
        if (GroupableRelatableTO.class.cast(anyTO).getMembership(membershipTO.getGroupKey()).isPresent()) {
            attrMap = EntityTOUtils.buildAttrMap(GroupableRelatableTO.class.cast(anyTO)
                    .getMembership(membershipTO.getGroupKey()).get().getVirAttrs());
        } else {
            attrMap = new HashMap<>();
        }

        attrs.addAll(membershipSchemas.get(membershipTO.getGroupKey()).values().stream().map(schema -> {
            AttrTO attrTO = new AttrTO();
            attrTO.setSchema(schema.getKey());
            if (attrMap.containsKey(schema.getKey())) {
                attrTO.getValues().addAll(attrMap.get(schema.getKey()).getValues());
            } else {
                attrTO.getValues().add(StringUtils.EMPTY);
            }
            return attrTO;
        }).collect(Collectors.toList()));

        membershipTO.getVirAttrs().clear();
        membershipTO.getVirAttrs().addAll(attrs);
    }

    public class VirSchemas extends Schemas {

        private static final long serialVersionUID = -4730563859116024676L;

        public VirSchemas(
                final String id,
                final Map<String, VirSchemaTO> schemas,
                final IModel<List<AttrTO>> attrTOs) {
            super(id);

            add(new ListView<AttrTO>("schemas", attrTOs) {

                private static final long serialVersionUID = 9101744072914090143L;

                @Override
                @SuppressWarnings("unchecked")
                protected void populateItem(final ListItem<AttrTO> item) {
                    AttrTO attrTO = item.getModelObject();

                    AbstractFieldPanel<?> panel = new AjaxTextFieldPanel(
                            "panel",
                            schemas.get(attrTO.getSchema()).getLabel(SyncopeConsoleSession.get().getLocale()),
                            new Model<>(),
                            false);

                    boolean readonly = attrTO.getSchemaInfo() == null
                            ? false
                            : VirSchemaTO.class.cast(attrTO.getSchemaInfo()).isReadonly();

                    if (mode == AjaxWizard.Mode.TEMPLATE) {
                        AjaxTextFieldPanel.class.cast(panel).enableJexlHelp().setEnabled(!readonly);
                    } else {
                        panel = new MultiFieldPanel.Builder<>(
                                new PropertyModel<List<String>>(attrTO, "values")).build(
                                "panel",
                                schemas.get(attrTO.getSchema()).getLabel(SyncopeConsoleSession.get().getLocale()),
                                AjaxTextFieldPanel.class.cast(panel));
                        panel.setEnabled(!readonly);
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
