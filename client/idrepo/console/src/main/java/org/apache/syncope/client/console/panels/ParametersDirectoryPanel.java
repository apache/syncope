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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.rest.ConfRestClient;
import org.apache.syncope.client.console.rest.SchemaRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.client.ui.commons.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.ui.commons.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.client.ui.commons.panels.WizardModalPanel;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow.WindowClosedCallback;
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

public class ParametersDirectoryPanel
        extends DirectoryPanel<Attr, Attr, ParametersDirectoryPanel.ParametersProvider, ConfRestClient> {

    private static final long serialVersionUID = 2765863608539154422L;

    private final SchemaRestClient schemaRestClient = new SchemaRestClient();

    private final BaseModal<Attr> modalDetails = new BaseModal<Attr>("modalDetails") {

        private static final long serialVersionUID = 389935548143327858L;

        @Override
        protected void onConfigure() {
            super.onConfigure();
            setFooterVisible(true);
        }
    };

    public ParametersDirectoryPanel(final String id, final PageReference pageRef) {
        super(id, new Builder<Attr, Attr, ConfRestClient>(new ConfRestClient(), pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<Attr> newInstance(final String id, final boolean wizardInModal) {
                throw new UnsupportedOperationException();
            }
        });

        itemKeyFieldName = "schema";
        disableCheckBoxes();

        modalDetails.setWindowClosedCallback(new WindowClosedCallback() {

            private static final long serialVersionUID = 8804221891699487139L;

            @Override
            public void onClose(final AjaxRequestTarget target) {
                modalDetails.show(false);
                target.add(container);
            }
        });
        modalDetails.size(Modal.Size.Medium);
        addInnerObject(modalDetails);

        this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<Attr>(new Attr(), pageRef) {

            private static final long serialVersionUID = 1995192603527154740L;

            @Override
            public WizardModalPanel<Attr> build(final String id, final int index, final AjaxWizard.Mode mode) {
                return new ParametersCreateModalPanel(modal, newModelObject(), pageRef);
            }
        }, true);
        modal.size(Modal.Size.Medium);
        initResultTable();

        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, RENDER, IdRepoEntitlement.CONFIGURATION_SET);
    }

    public ParametersDirectoryPanel(final String id, final Builder<Attr, Attr, ConfRestClient> builder) {
        super(id, builder);
    }

    @Override
    protected ParametersProvider dataProvider() {
        return new ParametersProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_PARAMETERS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBatches() {
        return Collections.<ActionLink.ActionType>singletonList(ActionLink.ActionType.DELETE);
    }

    @Override
    protected List<IColumn<Attr, String>> getColumns() {
        final List<IColumn<Attr, String>> columns = new ArrayList<>();
        columns.add(new PropertyColumn<>(new ResourceModel("schema"), "schema"));
        columns.add(new PropertyColumn<Attr, String>(new ResourceModel("values"), "values") {

            private static final long serialVersionUID = -1822504503325964706L;

            @Override
            public void populateItem(
                    final Item<ICellPopulator<Attr>> item,
                    final String componentId,
                    final IModel<Attr> rowModel) {

                PlainSchemaTO schema = null;
                try {
                    schema = schemaRestClient.read(SchemaType.PLAIN, rowModel.getObject().getSchema());
                } catch (Exception e) {
                    LOG.error("Could not read conf schema {}", rowModel.getObject().getSchema(), e);
                }
                AttrSchemaType type = schema == null ? null : schema.getType();
                if (type == AttrSchemaType.Binary || type == AttrSchemaType.Encrypted) {
                    item.add(new Label(componentId, type.name()).
                            add(new AttributeModifier("style", "font-style:italic")));
                } else {
                    super.populateItem(item, componentId, rowModel);
                }
            }
        });
        return columns;
    }

    @Override
    public ActionsPanel<Attr> getActions(final IModel<Attr> model) {
        final ActionsPanel<Attr> panel = super.getActions(model);

        panel.add(new ActionLink<Attr>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                target.add(modalDetails);
                modalDetails.addSubmitButton();
                modalDetails.header(new StringResourceModel("any.edit"));
                modalDetails.setContent(new ParametersEditModalPanel(modalDetails, model.getObject(), pageRef));
                modalDetails.show(true);
            }
        }, ActionLink.ActionType.EDIT, IdRepoEntitlement.CONFIGURATION_SET);

        panel.add(new ActionLink<Attr>() {

            private static final long serialVersionUID = -3722207913631435501L;

            @Override
            public void onClick(final AjaxRequestTarget target, final Attr ignore) {
                try {
                    restClient.delete(model.getObject().getSchema());
                    schemaRestClient.deletePlainSchema(model.getObject().getSchema());
                    SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    target.add(container);
                } catch (Exception e) {
                    LOG.error("While deleting {}", model.getObject(), e);
                    SyncopeConsoleSession.get().error(StringUtils.isBlank(e.getMessage())
                            ? e.getClass().getName() : e.getMessage());
                }
                ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
            }
        }, ActionLink.ActionType.DELETE, IdRepoEntitlement.CONFIGURATION_DELETE, true);

        return panel;
    }

    protected final class ParametersProvider extends DirectoryDataProvider<Attr> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<Attr> comparator;

        private ParametersProvider(final int paginatorRows) {
            super(paginatorRows);
            setSort("schema", SortOrder.ASCENDING);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<Attr> iterator(final long first, final long count) {
            final List<Attr> list = restClient.list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return restClient.list().size();
        }

        @Override
        public IModel<Attr> model(final Attr object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
