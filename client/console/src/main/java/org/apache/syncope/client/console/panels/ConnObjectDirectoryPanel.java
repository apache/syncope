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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.commons.ConnIdSpecialAttributeName;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.DirectoryDataProvider;
import org.apache.syncope.client.console.commons.SortableDataProviderComparator;
import org.apache.syncope.client.console.panels.ConnObjectDirectoryPanel.ConnObjectDataProvider;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.ActionColumn;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.AttrColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

public abstract class ConnObjectDirectoryPanel
        extends DirectoryPanel<ConnObjectTO, ConnObjectTO, ConnObjectDataProvider, ResourceRestClient>
        implements ModalPanel<ConnObjectTO> {

    private static final long serialVersionUID = 4986172040062752781L;

    private final String resource;

    private final String anyType;

    protected final BaseModal<?> baseModal;

    private final MultilevelPanel multiLevelPanelRef;

    protected ConnObjectDirectoryPanel(
            final BaseModal<?> baseModal,
            final MultilevelPanel multiLevelPanelRef,
            final String resource,
            final String anyType,
            final PageReference pageRef) {

        super(MultilevelPanel.FIRST_LEVEL_ID, pageRef, false);
        this.resource = resource;
        this.anyType = anyType;
        this.baseModal = baseModal;
        this.multiLevelPanelRef = multiLevelPanelRef;
        restClient = new ResourceRestClient();
        setShowResultPage(false);
        disableCheckBoxes();
        initResultTable();
    }

    @Override
    protected void resultTableCustomChanges(final AjaxDataTablePanel.Builder<ConnObjectTO, String> resultTableBuilder) {
        resultTableBuilder.setMultiLevelPanel(baseModal, multiLevelPanelRef);
    }

    @Override
    public void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onError(final AjaxRequestTarget target, final Form<?> form) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ConnObjectTO getItem() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected ConnObjectDataProvider dataProvider() {
        return new ConnObjectDataProvider(rows);
    }

    @Override
    protected String paginatorRowsKey() {
        return Constants.PREF_CONNOBJECTS_PAGINATOR_ROWS;
    }

    @Override
    protected Collection<ActionLink.ActionType> getBulkActions() {
        return Collections.emptyList();
    }

    @Override
    protected List<IColumn<ConnObjectTO, String>> getColumns() {
        final List<IColumn<ConnObjectTO, String>> columns = new ArrayList<>();

        columns.add(new AttrColumn<>(ConnIdSpecialAttributeName.UID, SchemaType.PLAIN));
        columns.add(new AttrColumn<>(ConnIdSpecialAttributeName.NAME, SchemaType.PLAIN));
        columns.add(new AttrColumn<>(ConnIdSpecialAttributeName.ENABLE, SchemaType.PLAIN));

        columns.add(new ActionColumn<ConnObjectTO, String>(new ResourceModel("actions")) {

            private static final long serialVersionUID = 906457126287899096L;

            @Override
            public ActionLinksPanel<ConnObjectTO> getActions(
                    final String componentId, final IModel<ConnObjectTO> model) {

                final ConnObjectTO connObjectTO = model.getObject();

                final ActionLinksPanel<ConnObjectTO> panel = ActionLinksPanel.<ConnObjectTO>builder().
                        add(new ActionLink<ConnObjectTO>() {

                            private static final long serialVersionUID = -3722207913631435501L;

                            @Override
                            public void onClick(final AjaxRequestTarget target, final ConnObjectTO ignore) {
                                viewConnObject(connObjectTO, target);
                            }
                        }, ActionLink.ActionType.VIEW, StandardEntitlement.RESOURCE_GET_CONNOBJECT).
                        build(componentId);

                return panel;
            }

            @Override
            public ActionLinksPanel<ConnObjectTO> getHeader(final String componentId) {
                final ActionLinksPanel.Builder<ConnObjectTO> panel = ActionLinksPanel.builder();

                return panel.add(new ActionLink<ConnObjectTO>() {

                    private static final long serialVersionUID = 7511002881490248598L;

                    @Override
                    public void onClick(final AjaxRequestTarget target, final ConnObjectTO ignore) {
                        if (target != null) {
                            target.add(container);
                        }
                    }
                }, ActionLink.ActionType.RELOAD, StandardEntitlement.RESOURCE_LIST_CONNOBJECT).build(componentId);
            }
        });

        return columns;
    }

    protected abstract void viewConnObject(ConnObjectTO connObjectTO, AjaxRequestTarget target);

    protected class ConnObjectDataProvider extends DirectoryDataProvider<ConnObjectTO> {

        private static final long serialVersionUID = -20112718133295756L;

        private final SortableDataProviderComparator<ConnObjectTO> comparator;

        private final List<ConnObjectTO> connObjectTOs;

        public ConnObjectDataProvider(final int paginatorRows) {
            super(paginatorRows);

            setSort("lastChangeDate", SortOrder.DESCENDING);
            comparator = new SortableDataProviderComparator<>(this);
            connObjectTOs = restClient.listConnObjects(resource, anyType, getSort());
        }

        public SortableDataProviderComparator<ConnObjectTO> getComparator() {
            return comparator;
        }

        @Override
        public IModel<ConnObjectTO> model(final ConnObjectTO object) {
            return new CompoundPropertyModel<>(object);
        }

        @Override
        public long size() {
            return connObjectTOs.size();
        }

        @Override
        public Iterator<? extends ConnObjectTO> iterator(final long first, final long count) {
            List<ConnObjectTO> sublist = connObjectTOs.subList((int) first, (int) (first + count));

            Collections.sort(sublist, getComparator());
            return sublist.iterator();
        }
    }
}
