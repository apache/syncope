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

import static org.apache.syncope.client.console.panels.AbstractModalPanel.LOG;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SearchableDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SchemaTypePanel.SchemaProvider;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;

public class SchemaTypePanel extends AbstractTypesPanel<AbstractSchemaTO, SchemaProvider> {

    private static final long serialVersionUID = 3905038169553185171L;

    private static final Map<SchemaType, List<String>> COL_NAMES = new HashMap<SchemaType, List<String>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(SchemaType.PLAIN, Arrays.asList(new String[] { "key", "type",
                "mandatoryCondition", "uniqueConstraint", "multivalue", "readonly" }));
            put(SchemaType.DERIVED, Arrays.asList(new String[] { "key", "expression" }));
            put(SchemaType.VIRTUAL, Arrays.asList(new String[] { "key", "provision", "extAttrName", "readonly" }));
        }
    };

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final SchemaType schemaType;

    public SchemaTypePanel(final String id, final SchemaType schemaType,
            final AbstractSearchResultPanel.Builder<AbstractSchemaTO, AbstractSchemaTO, BaseRestClient> builder) {
        super(id, builder);

        this.schemaType = schemaType;
    }

    public SchemaTypePanel(final String id, final SchemaType schemaType, final PageReference pageRef) {
        super(id, new AbstractSearchResultPanel.Builder<AbstractSchemaTO, AbstractSchemaTO, BaseRestClient>(null,
                pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<AbstractSchemaTO> newInstance(final String id) {
                return new SchemaTypePanel(id, schemaType, this);
            }
        });

        this.schemaType = schemaType;

        try {
            this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<AbstractSchemaTO>(
                    BaseModal.CONTENT_ID, schemaType.getToClass().newInstance(), pageRef) {

                private static final long serialVersionUID = -6388405037134399367L;

                @Override
                public ModalPanel<AbstractSchemaTO> build(final int index, final boolean edit) {
                    final AbstractSchemaTO modelObject = newModelObject();
                    return new SchemaModalPanel(modal, modelObject, pageRef) {

                        private static final long serialVersionUID = -6227956682141146095L;

                        @Override
                        public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                            try {
                                if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
                                    SyncopeConsoleSession.get().getService(
                                            SchemaService.class).create(schemaType, modelObject);
                                } else {
                                    SyncopeConsoleSession.get().getService(
                                            SchemaService.class).update(schemaType, modelObject);
                                }
                                info(getString(Constants.OPERATION_SUCCEEDED));
                                modal.close(target);
                            } catch (Exception e) {
                                LOG.error("While creating or updating schema", e);
                                error(getString(Constants.ERROR) + ": " + e.getMessage());
                            }
                            modal.getNotificationPanel().refresh(target);
                        }
                    };
                }

                @Override
                protected void onCancelInternal(final AbstractSchemaTO modelObject) {
                }

                @Override
                protected Serializable onApplyInternal(final AbstractSchemaTO modelObject) {
                    return null;
                }
            }, true);

            initResultTable();
            MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.SCHEMA_LIST);
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
                if (clazzField.getType().equals(Boolean.class
                ) || clazzField.getType().equals(boolean.class
                )) {
                    columns.add(new AbstractColumn<AbstractSchemaTO, String>(new ResourceModel(field)) {

                        private static final long serialVersionUID = 8263694778917279290L;

                        @Override
                        public void populateItem(final Item<ICellPopulator<AbstractSchemaTO>> item,
                                final String componentId, final IModel<AbstractSchemaTO> model) {

                            BeanWrapper bwi = new BeanWrapperImpl(model.getObject());
                            Object obj = bwi.getPropertyValue(field);

                            item.add(new Label(componentId, StringUtils.EMPTY));
                            if (Boolean.valueOf(obj.toString())) {
                                item.add(new AttributeModifier("class", "glyphicon glyphicon-ok"));
                                item.add(new AttributeModifier("style", "display: table-cell; text-align: center;"));
                            }
                        }

                        @Override
                        public String getCssClass() {
                            return "short_fixedsize";
                        }
                    });
                } else {
                    final IColumn<AbstractSchemaTO, String> column = new PropertyColumn<AbstractSchemaTO, String>(
                            new ResourceModel(field), field, field) {

                        private static final long serialVersionUID = 3282547854226892169L;

                        @Override
                        public String getCssClass() {
                            String css = super.getCssClass();
                            if ("key".equals(field)) {
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
        }

        columns.add(new AbstractColumn<AbstractSchemaTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<AbstractSchemaTO>> item, final String componentId,
                    final IModel<AbstractSchemaTO> model) {

                final AbstractSchemaTO schemaTO = model.getObject();

                final ActionLinksPanel.Builder<Serializable> actionLinks = ActionLinksPanel.builder(page.
                        getPageReference());
                actionLinks.setDisableIndicator(true);
                actionLinks.setDisableIndicator(true);
                actionLinks.addWithRoles(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        send(SchemaTypePanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.SCHEMA_UPDATE).addWithRoles(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        try {
                            switch (schemaType) {
                                case DERIVED:
                                    schemaRestClient.deleteDerSchema(schemaTO.getKey());
                                    break;

                                case VIRTUAL:
                                    schemaRestClient.deleteVirSchema(schemaTO.getKey());
                                    break;

                                default:
                                    schemaRestClient.deletePlainSchema(schemaTO.getKey());
                                    break;
                            }

                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                        }
                        ((BasePage) getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.SCHEMA_DELETE);

                item.add(actionLinks.build(componentId));
            }
        });

        return columns;
    }

    private String getEnumValuesAsString(final List<String> enumerationValues) {
        final StringBuilder builder = new StringBuilder();

        for (String str : enumerationValues) {
            if (StringUtils.isNotBlank(str)) {
                if (builder.length() > 0) {
                    builder.append(SyncopeConstants.ENUM_VALUES_SEPARATOR);
                }

                builder.append(str.trim());
            }
        }

        return builder.toString();
    }

    protected final class SchemaProvider extends SearchableDataProvider<AbstractSchemaTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AbstractSchemaTO> comparator;

        private final SchemaType schemaType;

        private SchemaProvider(final int paginatorRows, final SchemaType schemaType) {
            super(paginatorRows);
            this.schemaType = schemaType;

            // Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AbstractSchemaTO> iterator(final long first, final long count) {
            final List<AbstractSchemaTO> list = schemaRestClient.getSchemas(this.schemaType);
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return schemaRestClient.getSchemas(this.schemaType).size();
        }

        @Override
        public IModel<AbstractSchemaTO> model(final AbstractSchemaTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
