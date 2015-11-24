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
package org.apache.syncope.client.console.panels;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RelationshipTypePanel extends AbstractTypesPanel<RelationshipTypeTO> {

    private static final long serialVersionUID = -3731778000138547357L;

    private static final Logger LOG = LoggerFactory.getLogger(RelationshipTypePanel.class);

    private static final String PAGINATOR_ROWS_KEYS = Constants.PREF_RELATIONSHIPTYPE_PAGINATOR_ROWS;

    private final BaseModal<RelationshipTypeTO> modal;

    public RelationshipTypePanel(
            final String panelId,
            final PageReference pageReference,
            final BaseModal<RelationshipTypeTO> modal) {
        super(panelId, pageReference);

        this.pageRows = prefMan.getPaginatorRows(getRequest(), PAGINATOR_ROWS_KEYS);
        this.modal = modal;

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        buildDataTable(container, getColumns(container, pageReference),
                new RelationshipTypePanel.RelationshipTypeProvider(), PAGINATOR_ROWS_KEYS);
    }

    private List<IColumn<RelationshipTypeTO, String>> getColumns(
            final WebMarkupContainer webContainer, final PageReference pageReference) {

        final List<IColumn<RelationshipTypeTO, String>> columns = new ArrayList<>();

        for (Field field : RelationshipTypeTO.class.getDeclaredFields()) {

            if (field != null && !Modifier.isStatic(field.getModifiers())) {
                final String fieldName = field.getName();

                final IColumn<RelationshipTypeTO, String> column =
                        new PropertyColumn<RelationshipTypeTO, String>(
                                new ResourceModel(field.getName()), field.getName(), field.getName()) {

                    private static final long serialVersionUID = 3282547854226892169L;

                    @Override
                    public String getCssClass() {
                        String css = super.getCssClass();
                        if ("key".equals(fieldName)) {
                            css = StringUtils.isBlank(css)
                                    ? "medium_fixedsize"
                                    : css + " medium_fixedsize";
                        }
                        return css;
                    }
                };
                columns.add(column);

            }
        }

        columns.add(new AbstractColumn<RelationshipTypeTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<RelationshipTypeTO>> item, final String componentId,
                    final IModel<RelationshipTypeTO> model) {

                final RelationshipTypeTO relationshipTypeTO = model.getObject();

                final ActionLinksPanel.Builder<Serializable> actionLinks = ActionLinksPanel.builder(pageReference);
                actionLinks.setDisableIndicator(true);
                actionLinks.addWithRoles(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        modal.header(Model.of(relationshipTypeTO.getKey()));
                        modal.setFormModel(relationshipTypeTO);
                        target.add(modal.setContent(new RelationshipTypeModalPanel(modal, pageReference, false)));
                        modal.addSumbitButton();
                        modal.show(true);
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.RELATIONSHIPTYPE_UPDATE).addWithRoles(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        try {
                            SyncopeConsoleSession.get().getService(
                                    RelationshipTypeService.class).delete(relationshipTypeTO.getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            feedbackPanel.refresh(target);
                            target.add(webContainer);
                        } catch (Exception e) {
                            LOG.error("While deleting RelationshipType", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                            feedbackPanel.refresh(target);
                        }
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.RELATIONSHIPTYPE_DELETE);

                item.add(actionLinks.build(componentId));
            }
        });

        return columns;

    }

    private final class RelationshipTypeProvider extends SortableDataProvider<RelationshipTypeTO, String> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<RelationshipTypeTO> comparator;

        private RelationshipTypeProvider() {
            super();
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<RelationshipTypeTO> iterator(final long first, final long count) {
            final List<RelationshipTypeTO> list =
                    SyncopeConsoleSession.get().getService(RelationshipTypeService.class).list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return SyncopeConsoleSession.get().getService(RelationshipTypeService.class).list().size();
        }

        @Override
        public IModel<RelationshipTypeTO> model(final RelationshipTypeTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

}
