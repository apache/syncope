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
package org.apache.syncope.client.console.panels.search;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.pages.GroupDisplayAttributesModalPage;
import org.apache.syncope.client.console.panels.GroupSearchResultPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.console.wizards.any.AnyHandler;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public final class GroupSelectionSearchResultPanel extends GroupSearchResultPanel {

    private static final long serialVersionUID = -1100228004207271271L;

    private GroupSelectionSearchResultPanel(final String id, final Builder builder) {
        super(id, builder);
    }

    @Override
    protected List<IColumn<GroupTO, String>> getColumns() {
        final List<IColumn<GroupTO, String>> columns = new ArrayList<>();

        for (String name : prefMan.getList(getRequest(), Constants.PREF_GROUP_DETAILS_VIEW)) {
            final Field field = ReflectionUtils.findField(GroupTO.class, name);

            if ("token".equalsIgnoreCase(name)) {
                columns.add(new PropertyColumn<GroupTO, String>(new ResourceModel(name, name), name, name));
            } else if (field != null && field.getType().equals(Date.class)) {
                columns.add(new PropertyColumn<GroupTO, String>(new ResourceModel(name, name), name, name));
            } else {
                columns.add(new PropertyColumn<GroupTO, String>(new ResourceModel(name, name), name, name));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_GROUP_ATTRIBUTES_VIEW)) {
            if (schemaNames.contains(name)) {
                columns.add(new AttrColumn<GroupTO>(name, SchemaType.PLAIN));
            }
        }

        for (String name : prefMan.getList(getRequest(), Constants.PREF_GROUP_DERIVED_ATTRIBUTES_VIEW)) {
            if (dSchemaNames.contains(name)) {
                columns.add(new AttrColumn<GroupTO>(name, SchemaType.DERIVED));
            }
        }

        // Add defaults in case of no selection
        if (columns.isEmpty()) {
            for (String name : GroupDisplayAttributesModalPage.GROUP_DEFAULT_SELECTION) {
                columns.add(new PropertyColumn<GroupTO, String>(new ResourceModel(name, name), name, name));
            }

            prefMan.setList(getRequest(), getResponse(), Constants.PREF_GROUP_DETAILS_VIEW,
                    Arrays.asList(GroupDisplayAttributesModalPage.GROUP_DEFAULT_SELECTION));
        }

        columns.add(new ActionColumn<GroupTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = -3503023501954863131L;

            @Override
            public ActionLinksPanel<GroupTO> getActions(final String componentId, final IModel<GroupTO> model) {
                final ActionLinksPanel.Builder<GroupTO> panel = ActionLinksPanel.builder(page.getPageReference());

                panel.add(new ActionLink<GroupTO>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final GroupTO ignore) {
                        send(GroupSelectionSearchResultPanel.this,
                                Broadcast.BUBBLE, new GroupSelection(target, model.getObject()));
                    }
                }, ActionLink.ActionType.SELECT, StandardEntitlement.GROUP_READ);

                return panel.build(componentId);
            }

            @Override
            public ActionLinksPanel<Serializable> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<Serializable> panel = ActionLinksPanel.builder(page.getPageReference());

                return panel.add(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -7978723352517770644L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.GROUP_SEARCH).build(componentId);
            }
        });

        return columns;
    }

    @Override
    protected <T extends AnyTO> Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    public static final class Builder extends GroupSearchResultPanel.Builder {

        private static final long serialVersionUID = 1L;

        public Builder(
                final List<AnyTypeClassTO> anyTypeClassTOs,
                final AbstractAnyRestClient<GroupTO> restClient,
                final String type,
                final PageReference pageRef) {

            super(anyTypeClassTOs, restClient, type, pageRef);
            this.filtered = true;
            this.checkBoxEnabled = false;
        }

        @Override
        protected WizardMgtPanel<AnyHandler<GroupTO>> newInstance(final String id) {
            return new GroupSelectionSearchResultPanel(id, this);
        }
    }

    public static class GroupSelection implements Serializable {

        private static final long serialVersionUID = 1242677935378149180L;

        private final AjaxRequestTarget target;

        private final GroupTO grp;

        public GroupSelection(final AjaxRequestTarget target, final GroupTO grp) {
            this.target = target;
            this.grp = grp;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public GroupTO getSelection() {
            return grp;
        }
    }
}
