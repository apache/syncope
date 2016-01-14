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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.SearchableDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.AnyTypePanel.AnyTypeProvider;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.client.console.wizards.AbstractModalPanelBuilder;
import org.apache.syncope.client.console.wizards.AjaxWizard;
import org.apache.syncope.client.console.wizards.WizardMgtPanel;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.metadata.MetaDataRoleAuthorizationStrategy;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public class AnyTypePanel extends AbstractTypesPanel<AnyTypeTO, AnyTypeProvider> {

    private static final long serialVersionUID = 3905038169553185171L;

    public AnyTypePanel(final String id, final Builder<AnyTypeTO, AnyTypeTO, BaseRestClient> builder) {
        super(id, builder);
    }

    public AnyTypePanel(final String id, final PageReference pageRef) {
        super(id, new Builder<AnyTypeTO, AnyTypeTO, BaseRestClient>(null, pageRef) {

            private static final long serialVersionUID = 8769126634538601689L;

            @Override
            protected WizardMgtPanel<AnyTypeTO> newInstance(final String id) {
                return new AnyTypePanel(id, this);
            }
        }.disableCheckBoxes());

        this.addNewItemPanelBuilder(new AbstractModalPanelBuilder<AnyTypeTO>(
                BaseModal.CONTENT_ID, new AnyTypeTO(), pageRef) {

            private static final long serialVersionUID = -6388405037134399367L;

            @Override
            public ModalPanel<AnyTypeTO> build(final int index, final boolean edit) {
                final AnyTypeTO modelObject = newModelObject();
                return new AnyTypeModalPanel(modal, modelObject, pageRef) {

                    private static final long serialVersionUID = -6227956682141146095L;

                    @Override
                    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                        try {
                            if (getOriginalItem() == null || StringUtils.isBlank(getOriginalItem().getKey())) {
                                SyncopeConsoleSession.get().getService(AnyTypeService.class).create(modelObject);
                            } else {
                                SyncopeConsoleSession.get().getService(AnyTypeService.class).update(modelObject);
                            }
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            modal.close(target);
                        } catch (Exception e) {
                            LOG.error("While creating or updating AnyTypeTO", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                        }
                        modal.getNotificationPanel().refresh(target);
                    }
                };
            }

            @Override
            protected void onCancelInternal(final AnyTypeTO modelObject) {
            }

            @Override
            protected Serializable onApplyInternal(final AnyTypeTO modelObject) {
                // do nothing
                return null;
            }
        }, true);

        initResultTable();
        MetaDataRoleAuthorizationStrategy.authorize(addAjaxLink, ENABLE, StandardEntitlement.ANYTYPE_CREATE);
    }

    @Override
    protected AnyTypeProvider dataProvider() {
        return new AnyTypeProvider(rows);
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
    protected List<IColumn<AnyTypeTO, String>> getColumns() {

        final List<IColumn<AnyTypeTO, String>> columns = new ArrayList<>();

        for (Field field : AnyTypeTO.class.getDeclaredFields()) {

            if (field != null && !Modifier.isStatic(field.getModifiers())) {
                final String fieldName = field.getName();
                if (field.getType().isArray()) {
                    final IColumn<AnyTypeTO, String> column = new PropertyColumn<AnyTypeTO, String>(
                            new ResourceModel(field.getName()), field.getName()) {

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

                } else {
                    final IColumn<AnyTypeTO, String> column = new PropertyColumn<AnyTypeTO, String>(
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
        }

        columns.add(new AbstractColumn<AnyTypeTO, String>(new ResourceModel("actions", "")) {

            private static final long serialVersionUID = 2054811145491901166L;

            @Override
            public String getCssClass() {
                return "action";
            }

            @Override
            public void populateItem(final Item<ICellPopulator<AnyTypeTO>> item, final String componentId,
                    final IModel<AnyTypeTO> model) {

                final ActionLinksPanel.Builder<Serializable> actionLinks =
                        ActionLinksPanel.builder(page.getPageReference());
                actionLinks.setDisableIndicator(true);
                actionLinks.addWithRoles(new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        send(AnyTypePanel.this, Broadcast.EXACT,
                                new AjaxWizard.EditItemActionEvent<>(model.getObject(), target));
                    }
                }, ActionLink.ActionType.EDIT, StandardEntitlement.ANYTYPE_UPDATE).addWithRoles(
                        new ActionLink<Serializable>() {

                    private static final long serialVersionUID = -3722207913631435501L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                        try {
                            SyncopeConsoleSession.get().
                                    getService(AnyTypeService.class).delete(model.getObject().getKey());
                            info(getString(Constants.OPERATION_SUCCEEDED));
                            target.add(container);
                        } catch (Exception e) {
                            LOG.error("While deleting AnyTypeTO", e);
                            error(getString(Constants.ERROR) + ": " + e.getMessage());
                        }
                        ((BasePage) getPage()).getNotificationPanel().refresh(target);
                    }
                }, ActionLink.ActionType.DELETE, StandardEntitlement.ANYTYPE_DELETE);

                item.add(actionLinks.build(componentId));
            }
        });

        return columns;

    }

    protected final class AnyTypeProvider extends SearchableDataProvider<AnyTypeTO> {

        private static final long serialVersionUID = -185944053385660794L;

        private final SortableDataProviderComparator<AnyTypeTO> comparator;

        private AnyTypeProvider(final int paginatorRows) {
            super(paginatorRows);
            comparator = new SortableDataProviderComparator<>(this);
        }

        @Override
        public Iterator<AnyTypeTO> iterator(final long first, final long count) {
            final List<AnyTypeTO> list = SyncopeConsoleSession.get().getService(AnyTypeService.class).list();
            Collections.sort(list, comparator);
            return list.subList((int) first, (int) first + (int) count).iterator();
        }

        @Override
        public long size() {
            return SyncopeConsoleSession.get().getService(AnyTypeService.class).list().size();
        }

        @Override
        public IModel<AnyTypeTO> model(final AnyTypeTO object) {
            return new CompoundPropertyModel<>(object);
        }
    }
}
