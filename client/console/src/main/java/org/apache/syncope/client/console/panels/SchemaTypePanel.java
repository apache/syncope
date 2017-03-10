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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SchemaTypePanel.SchemaProvider;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public class SchemaTypePanel extends TypesDirectoryPanel<AbstractSchemaTO, SchemaProvider, SchemaRestClient> {

    private static final long serialVersionUID = 3905038169553185171L;

    private static final Map<SchemaType, List<String>> COL_NAMES = new HashMap<SchemaType, List<String>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(SchemaType.PLAIN, Arrays.asList(new String[] {
                "key", "type", "mandatoryCondition", "uniqueConstraint", "multivalue", "readonly" }));
            put(SchemaType.DERIVED, Arrays.asList(new String[] {
                "key", "expression" }));
            put(SchemaType.VIRTUAL, Arrays.asList(new String[] {
                "key", "resource", "anyType", "extAttrName", "readonly" }));
        }
    };

    private final SchemaType schemaType;

    public SchemaTypePanel(final String id, final SchemaType schemaType, final PageReference pageRef) {
        super(id, pageRef);
        this.restClient = new SchemaRestClient();
        disableCheckBoxes();

        this.schemaType = schemaType;

        try {
            this.addNewItemPanelBuilder(
                    new AbstractModalPanelBuilder<AbstractSchemaTO>(schemaType.getToClass().newInstance(), pageRef) {

                private static final long serialVersionUID = -6388405037134399367L;

                @Override
                public WizardModalPanel<AbstractSchemaTO> build(
                        final String id, final int index, final AjaxWizard.Mode mode) {

                    final AbstractSchemaTO modelObject = newModelObject();
                    return new SchemaModalPanel(modal, modelObject, pageRef) {

                        private static final long serialVersionUID = -6227956682141146095L;

                        @Override
                        public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                            try {
                                if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
                                    restClient.create(schemaType, modelObject);
                                } else {
                                    restClient.update(schemaType, modelObject);
                                }

                                SchemaTypePanel.this.updateResultTable(target);
                                SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                                modal.close(target);
                            } catch (Exception e) {
                                LOG.error("While creating or updating {}", modelObject, e);
                                SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage()) ? e.getClass().
                                        getName() : e.getMessage());
                            }
                            ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                        }
                    };
                }
            }, true);

            initResultTable();
            MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, StandardEntitlement.SCHEMA_LIST);
        } catch (InstantiationException | IllegalAccessException e) {
            LOG.error("Error create new schema", e);
        }
    }

    @Override
    protected SchemaProvider dataProvider() {
        return new SchemaProvider(rows, schemaType);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_ANYTYPE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    @Override
    protected List<IColumn<AbstractSchemaTO, String>> getColumns() {
        final List<IColumn<AbstractSchemaTO, String>> columns = new ArrayList<>();

        for (final String field : COL_NAMES.get(schemaType)) {
            final Field clazzField = ReflectionUtils.findField(schemaType.getToClass(), field);

            if (clazzField != null) {
                if (clazzField.getType().equals(Boolean.class) || clazzField.getType().equals(boolean.class)) {
                    columns.add(new BooleanPropertyColumn<AbstractSchemaTO>(new ResourceModel(field), field, field));
                } else {
                    final IColumn<AbstractSchemaTO, String> column = new PropertyColumn<AbstractSchemaTO, String>(
                            new ResourceModel(field), field, field) {

                        private static final long serialVersionUID = 3282547854226892169L;

                        @Override
                        public String getCssClass() {
                            String css = super.getCssClass();
                            if ("key".equals(field)) {
                                css = StringUtils.isBlank(css)
                                        ? "col-xs-1"
                                        : css + " col-xs-1";
                            }
                            return css;
                        }
                    };
                    columns.add(column);
                }
            }
        }

        columns.add(new ActionColumn<AbstractSchemaTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 906457126287899096L;

            @Override
            public ActionLinksPanel<AbstractSchemaTO> getActions(
                    final String componentId, final IModel<AbstractSchemaTO> model) {

                ActionLinksPanel<AbstractSchemaTO> panel = ActionLinksPanel.<AbstractSchemaTO>builder().
                        add(new ActionLink<AbstractSchemaTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AbstractSchemaTO ignore) {
                                send(SchemaTypePanel.this, Broadcast.EXACT,
                                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                            }
                        }, ActionLink.ActionType.EDIT, StandardEntitlement.SCHEMA_UPDATE).
                        add(new ActionLink<AbstractSchemaTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final AbstractSchemaTO ignore) {
                                try {
                                    switch (schemaType) {
                                        case DERIVED:
                                            restClient.deleteDerSchema(model.getObject().getKey());
                                            break;

                                        case VIRTUAL:
                                            restClient.deleteVirSchema(model.getObject().getKey());
                                            break;

                                        default:
                                            restClient.deletePlainSchema(model.getObject().getKey());
                                            break;
                                    }

                                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                                    target.add(container);
                                } catch (Exception e) {
                                    LOG.error("While deleting {}", model.getObject(), e);
                                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                                            ? e.getClass().getName() : e.getMessage());
                                }
                                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                            }
                        }, ActionLink.ActionType.DELETE, StandardEntitlement.SCHEMA_DELETE).
                        build(componentId);

                return panel;
            }

            @Override
            public ActionLinksPanel<AbstractSchemaTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<AbstractSchemaTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<AbstractSchemaTO>() {

                    private static final long serialVersionUID = -1140254463922516111L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final AbstractSchemaTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD).build(componentId);
            }
        });

        return columns;
    }

    protected final class SchemaProvider extends DirectoryDataProvider<AbstractSchemaTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AbstractSchemaTO> comparator;

        private final SchemaType schemaType;

        private final ConfRestClient confRestClient = new ConfRestClient();

        private SchemaProvider(final int paginatorRows, final SchemaType schemaType) {
            super(paginatorRows);

            this.schemaType = schemaType;
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AbstractSchemaTO> iterator(final long first, final long count) {
            final List<AbstractSchemaTO> list = restClient.getSchemas(this.schemaType);
            Collections.sort(list, comparator);

            if (SchemaType.PLAIN == this.schemaType) {
                final List<String> configurations = new ArrayList<>();

                CollectionUtils.collect(confRestClient.list(), new Transformer<AttrTO, String>() {

                    @Override
                    public String transform(final AttrTO attrTO) {
                        return attrTO.getSchema();
                    }
                }, configurations);

                final List<AbstractSchemaTO> res = new ArrayList<>();
                for (AbstractSchemaTO item : list) {
                    if (!configurations.contains(item.getKey())) {
                        res.add(item);
                    }
                }
                return res.subList((int) first, (int) first + (int) count).iterator();
            } else {
                return list.subList((int) first, (int) first + (int) count).iterator();
            }
        }

        @Override
        public long size() {
            int size = restClient.getSchemas(this.schemaType).size();
            return SchemaType.PLAIN == this.schemaType
                    ? size - confRestClient.list().size()
                    : size;
        }

        @Override
        public IModel<AbstractSchemaTO> model(final AbstractSchemaTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
