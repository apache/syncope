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

import de.agilecoders.wicket.core.markup.html.bootstrap.dialog.Modal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.IdRepoConstants;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.ParametersDirectoryPanel.ParametersProvider;
import org.apache.syncope.client.console.rest.SyncopeRestClient;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.pages.BaseWebPage;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ParametersDirectoryPanel
        extends DirectoryPanel<ConfParam, ConfParam, ParametersProvider, SyncopeRestClient> {

    private static final long serialVersionUID = 2765863608539154422L;

    @SpringBean
    protected ConfParamOps confParamOps;

    public ParametersDirectoryPanel(final String id, final SyncopeRestClient restClient, final PageReference pageRef) {
        super(id, new Builder<>(restClient, pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<ConfParam> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        });

        itemKeyFieldName = "schema";
        disableCheckBoxes();

        this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<ConfParam>(new ConfParam(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public WizardModalPanel<ConfParam> build(final String id, final int index, final AjaxWizard.Mode mode) {
                return new ParametersModalPanel(modal, newModelObject(), confParamOps, mode, pageRef);
            }
        }, true);
        modal.size(Modal.Size.Default);
        initResultTable();
    }

    public ParametersDirectoryPanel(final String id, final Builder<ConfParam, ConfParam, SyncopeRestClient> builder) {
        super(id, builder);
    }

    @Override
    protected ParametersProvider dataProvider() {
        return new ParametersProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return IdRepoConstants.PREF_PARAMETERS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return List.of();
    }

    @Override
    protected List<IColumn<ConfParam, String>> getColumns() {
        final List<IColumn<ConfParam, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new ResourceModel("schema"), "schema"));
        columns.add(new PropertyColumn<>(new ResourceModel("values"), "values") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<ConfParam>> item,
                    final String componentId,
                    final IModel<ConfParam> rowModel) {

                if (rowModel.getObject().getValues().toString().length() > 96) {
                    item.add(new Label(componentId, getString("tooLong")).
                            add(new AttributeModifier("style", "font-style:italic")));
                } else {
                    super.populateItem(item, componentId, rowModel);
                }
            }
        });
        return columns;
    }

    @Override
    public ActionsPanel<ConfParam> getActions(final IModel<ConfParam> model) {
        final ActionsPanel<ConfParam> panel = super.getActions(model);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConfParam ignore) {
                target.add(modal);
                modal.header(new StringResourceModel("any.edit"));
                modal.setContent(new ParametersModalPanel(
                        modal, model.getObject(), confParamOps, AjaxWizard.Mode.EDIT, pageRef));
                modal.show(true);
            }
        }, ActionLink.ActionType.EDIT, null);

        panel.add(new ActionLink<>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConfParam ignore) {
                try {
                    confParamOps.remove(SyncopeConsoleSession.get().getDomain(), model.getObject().getSchema());

                    SyncopeConsoleSession.get().success(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject(), e);
                    SyncopeConsoleSession.get().onException(e);
                }
                ((BaseWebPage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, null, true);

        return panel;
    }

    protected final class ParametersProvider extends DirectoryDataProvider<ConfParam> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<ConfParam> comparator;

        private ParametersProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("schema", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<ConfParam> iterator(final long first, final long count) {
            List<ConfParam> list = confParamOps.list(SyncopeConsoleSession.get().getDomain()).entrySet().stream().
                    skip(first).limit(count).
                    map(entry -> {
                        ConfParam param = new ConfParam();
                        param.setSchema(entry.getKey());
                        param.setValues(entry.getValue());
                        return param;
                    }).collect(Collectors.toList());

            list.sort(comparator);
            return list.iterator();
        }

        @Override
        public long size() {
            return confParamOps.list(SyncopeConsoleSession.get().getDomain()).size();
        }

        @Override
        public IModel<ConfParam> model(final ConfParam object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
