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

import java.util.List;
import org.apache.syncope.client.console.PreferenceManager;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AjaxFallbackDataTable;
import org.apache.syncope.client.console.wicket.markup.html.form.SelectChoiceRenderer;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;

public class AbstractTypesPanel<T extends AbstractBaseBean> extends Panel {

    private static final long serialVersionUID = 7890071604330629259L;

    protected final PreferenceManager prefMan = new PreferenceManager();

    protected final PageReference pageRef;

    protected int pageRows;

    public AbstractTypesPanel(final String id, final PageReference pageRef) {
        super(id);
        this.pageRef = pageRef;
    }

    protected void buildDataTable(final WebMarkupContainer container, final List<IColumn<T, String>> tableCols,
            final SortableDataProvider<T, String> provider, final String paginatorKey) {

        final WebMarkupContainer tableContainer = new WebMarkupContainer("tableContainer");

        tableContainer.setOutputMarkupId(true);
        container.add(tableContainer);

        final AjaxFallbackDataTable<T, String> table =
                new AjaxFallbackDataTable<>("datatable",
                        tableCols, provider, pageRows, tableContainer);

        table.setOutputMarkupId(true);
        tableContainer.add(table);
        container.add(getPaginatorForm(tableContainer, table, "paginator", this, paginatorKey));
    }

    protected Form<Void> getPaginatorForm(final WebMarkupContainer webContainer,
            final AjaxFallbackDataTable<T, String> dataTable,
            final String formname, final Panel panel, final String rowsPerPagePrefName) {

        final Form<Void> form = new Form<>(formname);

        final DropDownChoice<Integer> rowChooser = new DropDownChoice<>("rowsChooser",
                new PropertyModel<Integer>(panel, "pageRows"), prefMan.getPaginatorChoices(),
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

    protected int getPageRows() {
        return pageRows;
    }
}

