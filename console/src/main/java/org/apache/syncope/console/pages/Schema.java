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
package org.apache.syncope.console.pages;

import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.console.commons.Constants;
import org.apache.syncope.console.commons.PreferenceManager;
import org.apache.syncope.console.commons.SchemaModalPageFactory;
import org.apache.syncope.console.commons.SelectChoiceRenderer;
import org.apache.syncope.console.commons.SortableDataProviderComparator;
import org.apache.syncope.console.pages.panels.JQueryUITabbedPanel;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.util.ReflectionUtils;

/**
 * Schema WebPage.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class Schema extends BasePage {

    private static final long serialVersionUID = 8091922398776299403L;

    private static final Map<SchemaType, List<String>> COL_NAMES = new HashMap<SchemaType, List<String>>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(SchemaType.NORMAL, Arrays.asList(new String[] {"name", "type",
                "mandatoryCondition", "uniqueConstraint", "multivalue", "readonly"}));
            put(SchemaType.DERIVED, Arrays.asList(new String[] {"name", "expression"}));
            put(SchemaType.VIRTUAL, Arrays.asList(new String[] {"name", "readonly"}));
        }
    };

    private static final Map<Map.Entry<AttributableType, SchemaType>, String> PAGINATOR_ROWS_KEYS =
            new HashMap<Map.Entry<AttributableType, SchemaType>, String>() {

        private static final long serialVersionUID = 3109256773218160485L;

        {
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.USER, SchemaType.NORMAL),
                    Constants.PREF_USER_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.USER, SchemaType.DERIVED),
                    Constants.PREF_USER_DER_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.USER, SchemaType.VIRTUAL),
                    Constants.PREF_USER_VIR_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.MEMBERSHIP, SchemaType.NORMAL),
                    Constants.PREF_MEMBERSHIP_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.MEMBERSHIP,
                    SchemaType.DERIVED),
                    Constants.PREF_MEMBERSHIP_DER_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.MEMBERSHIP,
                    SchemaType.VIRTUAL),
                    Constants.PREF_MEMBERSHIP_VIR_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.ROLE, SchemaType.NORMAL),
                    Constants.PREF_ROLE_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.ROLE, SchemaType.DERIVED),
                    Constants.PREF_ROLE_DER_SCHEMA_PAGINATOR_ROWS);
            put(new SimpleEntry<AttributableType, SchemaType>(AttributableType.ROLE, SchemaType.VIRTUAL),
                    Constants.PREF_ROLE_VIR_SCHEMA_PAGINATOR_ROWS);
        }
    };

    private static final int WIN_WIDTH = 600;

    private static final int WIN_HEIGHT = 200;

    private static final int NORMAL_WIN_HEIGHT = 500;

    @SpringBean
    private SchemaRestClient restClient;

    @SpringBean
    private PreferenceManager prefMan;

    private final String allowedCreateRoles = xmlRolesReader.getAllAllowedRoles("Schema", "create");

    private final String allowedReadRoles = xmlRolesReader.getAllAllowedRoles("Schema", "read");

    private final String allowedDeleteRoles = xmlRolesReader.getAllAllowedRoles("Schema", "delete");

    public Schema() {
        super();

        for (final AttributableType attrType : AttributableType.values()) {
            final String attrTypeAsString = attrType.name().toLowerCase();

            List<ITab> tabs = new ArrayList<ITab>();

            for (final SchemaType schemaType : SchemaType.values()) {
                final String schemaTypeAsString = schemaType.name().toLowerCase();

                tabs.add(new AbstractTab(new Model<String>(getString(schemaTypeAsString))) {

                    private static final long serialVersionUID = -5861786415855103549L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        return new SchemaTypePanel(panelId, attrType, schemaType);
                    }
                });
            }

            add(new JQueryUITabbedPanel(attrTypeAsString + "Tabs", tabs));
        }
    }

    private <T extends AbstractSchemaModalPage> List<IColumn> getColumns(
            final WebMarkupContainer webContainer, final ModalWindow modalWindow,
            final AttributableType attributableType, final SchemaType schemaType,
            final Collection<String> fields) {

        List<IColumn> columns = new ArrayList<IColumn>();

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

                            item.add(new Label(componentId, ""));
                            item.add(new AttributeModifier("class", new Model<String>(obj.toString())));
                        }

                        @Override
                        public String getCssClass() {
                            return "small_fixedsize";
                        }
                    });
                } else {
                    IColumn column = new PropertyColumn(new ResourceModel(field), field, field) {

                        private static final long serialVersionUID = 3282547854226892169L;

                        @Override
                        public String getCssClass() {
                            String css = super.getCssClass();
                            if ("name".equals(field)) {
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

                final ActionLinksPanel panel = new ActionLinksPanel(componentId, model, getPageReference());

                panel.addWithRoles(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        modalWindow.setPageCreator(new ModalWindow.PageCreator() {

                            private static final long serialVersionUID = -7834632442532690940L;

                            @Override
                            public Page createPage() {
                                AbstractSchemaModalPage page = SchemaModalPageFactory.getSchemaModalPage(
                                        attributableType, schemaType);

                                page.setSchemaModalPage(Schema.this.getPageReference(), modalWindow, schemaTO, false);

                                return page;
                            }
                        });

                        modalWindow.show(target);
                    }
                }, ActionType.EDIT, allowedReadRoles);

                panel.addWithRoles(new ActionLink() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {

                        switch (schemaType) {
                            case DERIVED:
                                restClient.deleteDerivedSchema(attributableType, schemaTO.getName());
                                break;

                            case VIRTUAL:
                                restClient.deleteVirtualSchema(attributableType, schemaTO.getName());
                                break;

                            default:
                                restClient.deleteSchema(attributableType, schemaTO.getName());
                                break;
                        }

                        info(getString(Constants.OPERATION_SUCCEEDED));
                        target.add(feedbackPanel);

                        target.add(webContainer);
                    }
                }, ActionType.DELETE, allowedDeleteRoles);

                item.add(panel);
            }
        });

        return columns;
    }

    private Form<Void> getPaginatorForm(final WebMarkupContainer webContainer,
            final AjaxFallbackDefaultDataTable dataTable,
            final String formname, final SchemaTypePanel schemaTypePanel, final String rowsPerPagePrefName) {

        Form<Void> form = new Form<Void>(formname);

        final DropDownChoice<Integer> rowChooser = new DropDownChoice<Integer>("rowsChooser",
                new PropertyModel<Integer>(schemaTypePanel, "pageRows"), prefMan.getPaginatorChoices(),
                new SelectChoiceRenderer<Integer>());

        rowChooser.add(new AjaxFormComponentUpdatingBehavior(SyncopeConstants.ON_CHANGE) {

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

    private <T extends AbstractSchemaModalPage> AjaxLink<Void> getCreateSchemaLink(final ModalWindow modalWindow,
            final AttributableType attrType, final SchemaType schemaType, final String winLinkName) {

        AjaxLink<Void> link = new ClearIndicatingAjaxLink<Void>(winLinkName, getPageReference()) {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            protected void onClickInternal(final AjaxRequestTarget target) {
                modalWindow.setPageCreator(new ModalWindow.PageCreator() {

                    private static final long serialVersionUID = -7834632442532690940L;

                    @Override
                    public Page createPage() {
                        T page = SchemaModalPageFactory.getSchemaModalPage(attrType, schemaType);
                        page.setSchemaModalPage(Schema.this.getPageReference(), modalWindow, null, true);

                        return page;
                    }
                });

                modalWindow.show(target);
            }
        };

        MetaDataRoleAuthorizationStrategy.authorize(link, ENABLE, allowedCreateRoles);

        return link;
    }

    private class SchemaProvider extends SortableDataProvider<AbstractSchemaTO, String> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AbstractSchemaTO> comparator;

        private final AttributableType attrType;

        private final SchemaType schemaType;

        public SchemaProvider(final AttributableType attrType, final SchemaType schemaType) {
            super();

            this.attrType = attrType;
            this.schemaType = schemaType;

            // Default sorting
            setSort("name", SortOrder.ASCENDING);

            comparator = new SortableDataProviderComparator<AbstractSchemaTO>(this);
        }

        @Override
        public Iterator<AbstractSchemaTO> iterator(final long first, final long count) {
            @SuppressWarnings("unchecked")
            List<AbstractSchemaTO> list =
                    (List<AbstractSchemaTO>) restClient.getSchemas(this.attrType, this.schemaType);

            Collections.sort(list, comparator);

            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.getSchemas(this.attrType, this.schemaType).size();
        }

        @Override
        public IModel<AbstractSchemaTO> model(final AbstractSchemaTO object) {
            return new CompoundPropertyModel<AbstractSchemaTO>(object);
        }
    }

    private class SchemaTypePanel extends Panel {

        private static final long serialVersionUID = 2854050613688773575L;

        private int pageRows;

        private final AttributableType attrType;

        private final SchemaType schemaType;

        public SchemaTypePanel(final String id, final AttributableType attrType, final SchemaType schemaType) {
            super(id);

            this.attrType = attrType;
            this.schemaType = schemaType;

            setup();
        }

        private void setup() {
            ModalWindow editSchemaWin = new ModalWindow("editSchemaWin");
            editSchemaWin.setCssClassName(ModalWindow.CSS_CLASS_GRAY);
            editSchemaWin.setInitialWidth(WIN_WIDTH);
            if (schemaType == SchemaType.NORMAL) {
                editSchemaWin.setInitialHeight(NORMAL_WIN_HEIGHT);
            } else {
                editSchemaWin.setInitialHeight(WIN_HEIGHT);
            }
            editSchemaWin.setCookieName("editSchemaWin");
            editSchemaWin.setMarkupId("editSchemaWin");
            add(editSchemaWin);

            WebMarkupContainer schemaWrapContainer = new WebMarkupContainer("schemaWrapContainer");
            schemaWrapContainer.setOutputMarkupId(true);
            if (schemaType != SchemaType.VIRTUAL) {
                schemaWrapContainer.add(new AttributeModifier("style", "width:auto;"));
            }
            add(schemaWrapContainer);

            WebMarkupContainer schemaContainer = new WebMarkupContainer("schemaContainer");
            schemaContainer.setOutputMarkupId(true);
            schemaWrapContainer.add(schemaContainer);
            setWindowClosedCallback(editSchemaWin, schemaContainer);

            final String paginatorRowsKey = PAGINATOR_ROWS_KEYS.get(
                    new SimpleEntry<AttributableType, SchemaType>(attrType, schemaType));
            pageRows = prefMan.getPaginatorRows(getRequest(), paginatorRowsKey);

            List<IColumn> tableCols = getColumns(schemaContainer, editSchemaWin, attrType,
                    schemaType, COL_NAMES.get(schemaType));
            final AjaxFallbackDefaultDataTable table = new AjaxFallbackDefaultDataTable("datatable", tableCols,
                    new SchemaProvider(attrType, schemaType), pageRows);
            table.setOutputMarkupId(true);
            schemaContainer.add(table);

            schemaWrapContainer.add(getPaginatorForm(schemaContainer, table, "paginatorForm", this, paginatorRowsKey));

            add(getCreateSchemaLink(editSchemaWin, attrType, schemaType, "createSchemaLink"));
        }
    }
}
