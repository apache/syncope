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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SchemaTypePanel.SchemaProvider;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public class SchemaTypePanel extends TypesDirectoryPanel<SchemaTO, SchemaProvider, SchemaRestClient> {

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

    private String keyword;

    public SchemaTypePanel(final String id, final SchemaType schemaType, final PageReference pageRef) {
        super(id, true, pageRef);
        this.schemaType = schemaType;

        disableCheckBoxes();

        try {
            addNewItemPanelBuilder(new SchemaTypeWizardBuilder(
                    schemaType.getToClass().getDeclaredConstructor().newInstance(), pageRef), true);
        } catch (Exception e) {
            LOG.error("Error creating instance of {}", schemaType, e);
        }

        this.restClient = new SchemaRestClient();

        initResultTable();
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.SCHEMA_CREATE);
    }

    @Override
    protected void setWindowClosedReloadCallback(final BaseModal<?> modal) {
        modal.setWindowClosedCallback(target -> {
            target.add(SchemaTypePanel.this);
            modal.show(false);
        });
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
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.<ActionLink.ActionType>emptyList();
    }

    @Override
    protected List<IColumn<SchemaTO, String>> getColumns() {
        final List<IColumn<SchemaTO, String>> columns = new ArrayList<>();

        for (String field : COL_NAMES.get(schemaType)) {
            Field clazzField = ReflectionUtils.findField(schemaType.getToClass(), field);

            if (clazzField != null && !clazzField.isSynthetic()) {
                if (clazzField.getType().equals(Boolean.class) || clazzField.getType().equals(boolean.class)) {
                    columns.add(new BooleanPropertyColumn<>(new ResourceModel(field), field, field));
                } else {
                    IColumn<SchemaTO, String> column = new PropertyColumn<SchemaTO, String>(
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

        return columns;
    }

    @Override
    public ActionsPanel<SchemaTO> getActions(final IModel<SchemaTO> model) {
        final ActionsPanel<SchemaTO> panel = super.getActions(model);
        panel.add(new ActionLink<SchemaTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SchemaTO ignore) {
                send(SchemaTypePanel.this, Broadcast.EXACT,
                        new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.SCHEMA_UPDATE);
        panel.add(new ActionLink<SchemaTO>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SchemaTO ignore) {
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
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.SCHEMA_DELETE, true);

        return panel;
    }

    protected final class SchemaProvider extends DirectoryDataProvider<SchemaTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<SchemaTO> comparator;

        private final SchemaType schemaType;

        private final ConfRestClient confRestClient = new ConfRestClient();

        private SchemaProvider(final int paginatorRows, final SchemaType schemaType) {
            super(paginatorRows);

            this.schemaType = schemaType;
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        private List<SchemaTO> getSchemas() {
            List<SchemaTO> schemas = restClient.getSchemas(this.schemaType, keyword);

            if (SchemaType.PLAIN == this.schemaType) {
                List<String> configurations = confRestClient.list().stream().
                        map(Attr::getSchema).collect(Collectors.toList());

                schemas.removeIf(schema -> configurations.contains(schema.getKey()));
            }

            return schemas;
        }

        @Override
        public Iterator<SchemaTO> iterator(final long first, final long count) {
            List<SchemaTO> schemas = getSchemas();
            Collections.sort(schemas, comparator);

            return schemas.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return getSchemas().size();
        }

        @Override
        public IModel<SchemaTO> model(final SchemaTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SchemaSearchEvent) {
            SchemaSearchEvent payload = SchemaSearchEvent.class.cast(event.getPayload());
            AjaxRequestTarget target = payload.getTarget();

            keyword = payload.getKeyword();
            if (!keyword.startsWith("*")) {
                keyword = "*" + keyword;
            }
            if (!keyword.endsWith("*")) {
                keyword = keyword + "*";
            }

            updateResultTable(target);
        } else {
            super.onEvent(event);
        }
    }

    public static class SchemaSearchEvent implements Serializable {

        private static final long serialVersionUID = -282052400565266028L;

        private final AjaxRequestTarget target;

        private final String keyword;

        SchemaSearchEvent(final AjaxRequestTarget target, final String keyword) {
            this.target = target;
            this.keyword = keyword;
        }

        public AjaxRequestTarget getTarget() {
            return target;
        }

        public String getKeyword() {
            return keyword;
        }
    }
}
