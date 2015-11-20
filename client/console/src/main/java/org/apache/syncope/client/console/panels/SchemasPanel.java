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

import de.agilecoders.wicket.core.markup.html.bootstrap.tabs.Collapsible;
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
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.AbstractBasePage;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SelectChoiceRenderer;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;

public class SchemasPanel extends Panel {

    private static final long serialVersionUID = -1140213992451232279L;

    private static final Map<SchemaType, String> PAGINATOR_ROWS_KEYS =
            new HashMap<SchemaType, String>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(SchemaType.PLAIN, Constants.PREF_CONF_SCHEMA_PAGINATOR_ROWS);
            put(SchemaType.DERIVED, Constants.PREF_CONF_SCHEMA_PAGINATOR_ROWS);
            put(SchemaType.VIRTUAL, Constants.PREF_CONF_SCHEMA_PAGINATOR_ROWS);
        }
    };

    private static final Map<SchemaType, List<String>> COL_NAMES = new HashMap<SchemaType, List<String>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(SchemaType.PLAIN, Arrays.asList(new String[] { "key", "type",
                "mandatoryCondition", "uniqueConstraint", "multivalue", "readonly" }));
            put(SchemaType.DERIVED, Arrays.asList(new String[] { "key", "expression" }));
            put(SchemaType.VIRTUAL, Arrays.asList(new String[] { "key", "provision", "extAttrName", "readonly" }));
        }
    };

    private final NotificationPanel feedbackPanel;

    private final AbstractBasePage page;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final PreferenceManager prefMan = new PreferenceManager();

    private final PageReference pageReference;

    private final BaseModal<AbstractSchemaTO> modal;

    public SchemasPanel(final String id, final PageReference pageReference, final BaseModal<AbstractSchemaTO> modal) {
        super(id);

        this.pageReference = pageReference;
        this.page = (AbstractBasePage) pageReference.getPage();
        this.feedbackPanel = page.getFeedbackPanel();
        this.modal = modal;

        final Collapsible collapsible = new Collapsible("collapsePanel", buildTabList());
        collapsible.setOutputMarkupId(true);
        add(collapsible);
    }

    private List<ITab> buildTabList() {

        final List<ITab> tabs = new ArrayList<>();

        for (final SchemaType schemaType : SchemaType.values()) {
            tabs.add(new AbstractTab(new Model<>(schemaType.name())) {

                private static final long serialVersionUID = 1037272333056449378L;

                @Override
                public Panel getPanel(final String panelId) {
                    return new SchemaTypePanel(panelId, schemaType);
                }
            });
        }
        return tabs;
    }

    private <T extends SchemaModalPanel> List<IColumn<AbstractSchemaTO, String>> getColumns(
            final WebMarkupContainer webContainer,
            final SchemaType schemaType,
            final Collection<String> fields) {

        final List<IColumn<AbstractSchemaTO, String>> columns = new ArrayList<>();

        for (final String field : fields) {
            final Field clazzField = ReflectionUtils.findField(schemaType.getToClass(), field);

            if (clazzField != null) {
                if (clazzField.getType().equals(Boolean.class) || clazzField.getType().equals(boolean.class)) {
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
                    final IColumn<AbstractSchemaTO, String> column =
                            new PropertyColumn<AbstractSchemaTO, String>(new ResourceModel(field), field, field) {

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

                final ActionLinksPanel.Builder<Serializable> actionLinks = ActionLinksPanel.builder(pageReference);
                actionLinks.setDisableIndicator(true);
                actionLinks.addWithRoles(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        modal.header(Model.of(schemaTO.getKey()));
                        modal.setFormModel(schemaTO);
                        modal.addSumbitButton();
                        modal.show(true);
                        target.add(modal.setContent(new SchemaModalPanel(modal, pageReference, false)));
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.SCHEMA_UPDATE).addWithRoles(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {

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
                        feedbackPanel.refresh(target);

                        target.add(webContainer);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.SCHEMA_DELETE);

                item.add(actionLinks.build(componentId));
            }
        });

        return columns;
    }

    private final class SchemaTypePanel extends Panel {

        private static final long serialVersionUID = 2854050613688773575L;

        private int pageRows;

        private SchemaTypePanel(final String id, final SchemaType schemaType) {
            super(id);
            setup(schemaType);
        }

        private void setup(final SchemaType schemaType) {

            final WebMarkupContainer schemaWrapContainer = new WebMarkupContainer("schemaWrapContainer");
            schemaWrapContainer.setOutputMarkupId(true);
            add(schemaWrapContainer);

            if (schemaType != SchemaType.VIRTUAL) {
                schemaWrapContainer.add(new AttributeModifier("style", "width:auto;"));
            }

            final WebMarkupContainer schemaContainer = new WebMarkupContainer("schemaContainer");
            schemaContainer.setOutputMarkupId(true);
            schemaWrapContainer.add(schemaContainer);

            final String paginatorRowsKey = PAGINATOR_ROWS_KEYS.get(schemaType);
            pageRows = prefMan.getPaginatorRows(getRequest(), paginatorRowsKey);

            final List<IColumn<AbstractSchemaTO, String>> tableCols = getColumns(schemaContainer,
                    schemaType, COL_NAMES.get(schemaType));

            final AjaxFallbackDataTable<AbstractSchemaTO, String> table =
                    new AjaxFallbackDataTable<>("datatable",
                            tableCols, new SchemaProvider(schemaType), pageRows, schemaContainer);
            table.setOutputMarkupId(true);
            schemaContainer.add(table);

            schemaWrapContainer.add(getPaginatorForm(schemaContainer, table, "paginator", this, paginatorRowsKey));
        }
    }

    private Form<Void> getPaginatorForm(final WebMarkupContainer webContainer,
            final AjaxFallbackDataTable<AbstractSchemaTO, String> dataTable,
            final String formname, final SchemaTypePanel schemaTypePanel, final String rowsPerPagePrefName) {

        final Form<Void> form = new Form<>(formname);

        final DropDownChoice<Integer> rowChooser = new DropDownChoice<>("rowsChooser",
                new PropertyModel<Integer>(schemaTypePanel, "pageRows"), prefMan.getPaginatorChoices(),
                new SelectChoiceRenderer<Integer>());

        rowChooser.add(new AjaxFormComponentUpdatingBehavior(Constants.ON_CHANGE) {

            private static final long serialVersionUID = -1107858522700306810L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                prefMan.set(getRequest(), getResponse(), rowsPerPagePrefName, rowChooser.getInput());
                dataTable.setItemsPerPage(rowChooser.getModelObject());

                target.add(webContainer);
            }
        });

        form.add(rowChooser);

        return form;
    }

    private final class SchemaProvider extends SortableDataProvider<AbstractSchemaTO, String> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AbstractSchemaTO> comparator;

        private final SchemaType schemaType;

        private SchemaProvider(final SchemaType schemaType) {
            super();

            this.schemaType = schemaType;

            // Default sorting
            setSort("key", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AbstractSchemaTO> iterator(final long first, final long count) {
            @SuppressWarnings("unchecked")
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
