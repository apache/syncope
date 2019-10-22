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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.panels.search.AbstractSearchPanel;
import org.apache.syncope.client.console.panels.search.ConnObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

public abstract class ConnObjectListViewPanel extends Panel {

    private static final long serialVersionUID = 4986172040062752781L;

    private final AnyTypeRestClient anyTypeRestClient = new AnyTypeRestClient();

    private static final int SIZE = 10;

    private String nextPageCookie;

    private AbstractSearchPanel searchPanel;

    private WebMarkupContainer arrows;

    private String anyType;

    private ResourceTO resource;

    protected ConnObjectListViewPanel(
            final String id,
            final ResourceTO resource,
            final String anyType,
            final PageReference pageRef) {

        super(id);

        this.anyType = anyType;
        this.resource = resource;

        final Model<Integer> model = Model.of(-1);
        final StringResourceModel res = new StringResourceModel("search.result", this, new Model<>(anyType));

        final Accordion accordion = new Accordion("accordionPanel",
                Collections.<ITab>singletonList(new AbstractTab(res) {

                    private static final long serialVersionUID = 1037272333056449377L;

                    @Override
                    public WebMarkupContainer getPanel(final String panelId) {
                        searchPanel = getSearchPanel(panelId, anyType);
                        return searchPanel;
                    }

                }), model) {

            private static final long serialVersionUID = 6581261306163L;

            @Override
            protected Component newTitle(final String markupId, final ITab tab, final Accordion.State state) {
                return new AjaxLink<Integer>(markupId) {

                    private static final long serialVersionUID = 6584438659172L;

                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("style", "color: #337ab7");
                    }

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        model.setObject(model.getObject() == 0 ? -1 : 0);
                    }
                }.setBody(res);
            }
        };
        accordion.setOutputMarkupId(true);
        add(accordion.setEnabled(true).setVisible(true));

        final List<ConnObjectTO> listOfItems = reloadItems(resource.getKey(), anyType, null, null);

        final ListViewPanel.Builder<ConnObjectTO> builder = new ListViewPanel.Builder<ConnObjectTO>(
                ConnObjectTO.class, pageRef) {

            private static final long serialVersionUID = -8251750413385566738L;

            @Override
            protected Component getValueComponent(final String key, final ConnObjectTO bean) {
                Optional<AttrTO> attrTO =
                        bean.getAttrs().stream().filter(object -> object.getSchema().equals(key)).findAny();

                return !attrTO.isPresent() || attrTO.get().getValues().isEmpty()
                        ? new Label("field", StringUtils.EMPTY)
                        : new CollectionPanel("field", attrTO.get().getValues());
            }

        };

        builder.setReuseItem(false);
        builder.addAction(new ActionLink<ConnObjectTO>() {

            private static final long serialVersionUID = 7511002881490248598L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConnObjectTO modelObject) {
                viewConnObject(modelObject, target);
            }
        }, ActionLink.ActionType.VIEW, StandardEntitlement.RESOURCE_GET_CONNOBJECT).
                setItems(listOfItems).
                includes(ConnIdSpecialName.UID,
                        ConnIdSpecialName.NAME,
                        ConnIdSpecialName.ENABLE).
                withChecks(ListViewPanel.CheckAvailability.NONE).
                setReuseItem(false);

        add(builder.build("objs"));

        arrows = new WebMarkupContainer("arrows");
        add(arrows.setOutputMarkupId(true));

        arrows.add(new AjaxLink<Serializable>("next") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final List<ConnObjectTO> listOfItems = reloadItems(resource.getKey(), anyType, nextPageCookie, null);
                target.add(arrows);
                send(ConnObjectListViewPanel.this, Broadcast.DEPTH, new ListViewReload<>(listOfItems, target));
            }

            @Override
            public boolean isVisible() {
                return nextPageCookie != null;
            }
        });
    }

    @Override
    public void onEvent(final IEvent<?> event) {
        if (event.getPayload() instanceof SearchClausePanel.SearchEvent) {
            this.nextPageCookie = null;
            final AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();
            List<ConnObjectTO> listOfItems = reloadItems(resource.getKey(), anyType, null, SearchUtils.buildFIQL(
                    ConnObjectListViewPanel.this.searchPanel.getModel().getObject(),
                    SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder(),
                    ConnObjectListViewPanel.this.searchPanel.getAvailableSchemaTypes()));
            target.add(arrows);
            send(ConnObjectListViewPanel.this, Broadcast.DEPTH, new ListViewReload<>(listOfItems, target));
        } else {
            super.onEvent(event);
        }
    }

    protected abstract void viewConnObject(ConnObjectTO connObjectTO, AjaxRequestTarget target);

    private List<ConnObjectTO> reloadItems(
            final String resource,
            final String anyType,
            final String cookie,
            final String fiql) {

        Pair<String, List<ConnObjectTO>> items = new ResourceRestClient().listConnObjects(resource,
                anyType,
                SIZE,
                cookie,
                new SortParam<>(ConnIdSpecialName.UID, true),
                fiql);

        nextPageCookie = items.getLeft();
        return items.getRight();
    }

    private AbstractSearchPanel getSearchPanel(final String id, final String anyType) {
        final List<SearchClause> clauses = new ArrayList<>();
        final SearchClause clause = new SearchClause();
        clauses.add(clause);

        clause.setComparator(SearchClause.Comparator.EQUALS);
        clause.setType(SearchClause.Type.ATTRIBUTE);
        clause.setProperty("");

        AnyTypeKind anyTypeKind = anyType.equals(SyncopeConstants.REALM_ANYTYPE)
                ? AnyTypeKind.ANY_OBJECT
                : anyTypeRestClient.read(anyType).getKind();

        return new ConnObjectSearchPanel.Builder(resource, anyTypeKind, anyType,
                new ListModel<>(clauses)).required(true).enableSearch().build(id);
    }
}
