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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.KeywordSearchEvent;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.SchemaTypePanel.SchemaProvider;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BooleanPropertyColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.util.ReflectionUtils;

public class SchemaTypePanel extends TypesDirectoryPanel<SchemaTO, SchemaProvider, SchemaRestClient> {

    private static final long serialVersionUID = 3905038169553185171L;

    protected static final Map<SchemaType, List<String>> COL_NAMES = Map.of(
            SchemaType.PLAIN,
            List.of(Constants.KEY_FIELD_NAME,
                    "type", "mandatoryCondition", "uniqueConstraint", "multivalue", "readonly"),
            SchemaType.DERIVED,
            List.of(Constants.KEY_FIELD_NAME, "expression"),
            SchemaType.VIRTUAL,
            List.of(Constants.KEY_FIELD_NAME, "resource", "anyType", "extAttrName", "readonly"));

    protected final SchemaType schemaType;

    protected String keyword;

    public SchemaTypePanel(
            final String id,
            final SchemaRestClient restClient,
            final SchemaType schemaType,
            final PageReference pageRef) {

        super(id, restClient, true, pageRef);
        this.schemaType = schemaType;

        disableCheckBoxes();

        try {
            addNewItemPanelBuilder(new SchemaTypeWizardBuilder(
                    schemaType.getToClass().getDeclaredConstructor().newInstance(), restClient, pageRef), true);
        } catch (Exception e) {
            LOG.error("Error creating instance of {}", schemaType, e);
        }

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
        return IdRepoConstants.PREF_ANYTYPE_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
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
                    IColumn<SchemaTO, String> column = new PropertyColumn<>(
                            new ResourceModel(field), field, field) {

                        private static final long serialVersionUID = 3282547854226892169L;

                        @Override
                        public String getCssClass() {
                            String css = super.getCssClass();
                            if (Constants.KEY_FIELD_NAME.equals(field)) {
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
        ActionsPanel<SchemaTO> panel = super.getActions(model);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SchemaTO ignore) {
                send(SchemaTypePanel.this, Broadcast.EXACT, new AjaxWizard.EditItemActionEvent<>(
                        restClient.read(schemaType, model.getObject().getKey()), target));
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.SCHEMA_UPDATE);
        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final SchemaTO ignore) {
                try {
                    restClient.delete(schemaType, model.getObject().getKey());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.SCHEMA_DELETE, true);

        return panel;
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof KeywordSearchEvent) {
            KeywordSearchEvent payload = KeywordSearchEvent.class.cast(event.getPayload());

            keyword = payload.getKeyword();
            if (StringUtils.isNotBlank(keyword)) {
                if (!StringUtils.startsWith(keyword, "*")) {
                    keyword = "*" + keyword;
                }
                if (!StringUtils.endsWith(keyword, "*")) {
                    keyword += "*";
                }
            }

            updateResultTable(payload.getTarget());
        } else {
            super.onEvent(event);
        }
    }

    protected final class SchemaProvider extends DirectoryDataProvider<SchemaTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<SchemaTO> comparator;

        private final SchemaType schemaType;

        private SchemaProvider(final int paginatorRows, final SchemaType schemaType) {
            super(paginatorRows);
            this.schemaType = schemaType;
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<SchemaTO> iterator(final long first, final long count) {
            List<SchemaTO> schemas = restClient.getSchemas(this.schemaType, keyword);
            schemas.sort(comparator);
            return schemas.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getSchemas(this.schemaType, keyword).size();
        }

        @Override
        public IModel<SchemaTO> model(final SchemaTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
