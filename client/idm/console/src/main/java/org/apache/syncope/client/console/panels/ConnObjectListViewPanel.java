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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.panels.search.AbstractSearchPanel;
import org.apache.syncope.client.console.panels.search.ConnObjectSearchPanel;
import org.apache.syncope.client.console.panels.search.SearchClause;
import org.apache.syncope.client.console.panels.search.SearchClausePanel;
import org.apache.syncope.client.console.panels.search.SearchUtils;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.client.ui.commons.wicket.markup.html.bootstrap.tabs.Accordion;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOQuery;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
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
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConnObjectListViewPanel extends Panel {

    private static final long serialVersionUID = 4986172040062752781L;

    protected static final Logger LOG = LoggerFactory.getLogger(ConnObjectListViewPanel.class);

    protected static final int SIZE = 10;

    protected static final String STATUS = "Status";

    @SpringBean
    protected ReconciliationRestClient reconciliationRestClient;

    @SpringBean
    protected ResourceRestClient resourceRestClient;

    @SpringBean
    protected AnyTypeRestClient anyTypeRestClient;

    protected String nextPageCookie;

    protected AbstractSearchPanel searchPanel;

    protected WebMarkupContainer arrows;

    protected String anyType;

    protected ResourceTO resource;

    protected final PageReference pageRef;

    protected ConnObjectListViewPanel(
            final String id,
            final ResourceTO resource,
            final String anyType,
            final PageReference pageRef) {

        super(id);

        this.anyType = anyType;
        this.resource = resource;
        this.pageRef = pageRef;

        final Model<Integer> model = Model.of(-1);
        final StringResourceModel res = new StringResourceModel("search.result", this, new Model<>(anyType));

        final Accordion accordion = new Accordion("accordionPanel", List.of(new AbstractTab(res) {

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

        List<ConnObject> listOfItems = reloadItems(resource.getKey(), anyType, null, null);

        ListViewPanel.Builder<ConnObject> builder = new ListViewPanel.Builder<>(
                ConnObject.class, pageRef) {

            private static final long serialVersionUID = -8251750413385566738L;

            @Override
            protected Component getValueComponent(final String key, final ConnObject bean) {
                if (StringUtils.equals(key, STATUS)) {
                    ReconStatus status;
                    try {
                        status = reconciliationRestClient.status(
                                new ReconQuery.Builder(anyType, resource.getKey()).fiql(bean.getFiql()).build());
                    } catch (Exception e) {
                        LOG.error("While requesting for reconciliation status of {} {} with FIQL '{}'",
                                anyType, resource.getKey(), bean.getFiql(), e);

                        status = new ReconStatus();
                    }

                    return status.getOnSyncope() == null
                            ? StatusUtils.getLabel("field", "notfound icon", "Not found", Constants.NOT_FOUND_ICON)
                            : new Label("field", Model.of()).add(new PopoverBehavior(
                                    Model.of(),
                                    Model.of(status.getAnyKey()),
                                    new PopoverConfig().
                                            withTitle(status.getMatchType() == MatchType.LINKED_ACCOUNT
                                                    ? MatchType.LINKED_ACCOUNT.name() + ", " + AnyTypeKind.USER
                                                    : status.getAnyTypeKind().name()).
                                            withPlacement(TooltipConfig.Placement.left)) {

                                private static final long serialVersionUID = -7867802555691605021L;

                                @Override
                                protected String createRelAttribute() {
                                    return "field";
                                }

                                @Override
                                public void onComponentTag(final Component component, final ComponentTag tag) {
                                    super.onComponentTag(component, tag);
                                    tag.put("class", Constants.ACTIVE_ICON);
                                }
                            });
                } else {
                    Optional<Attr> attr = bean.getAttrs().stream().
                            filter(object -> object.getSchema().equals(key)).findAny();
                    return attr.filter(a -> !a.getValues().isEmpty()).
                            map(a -> (Component) new CollectionPanel("field", a.getValues())).
                            orElseGet(() -> new Label("field", StringUtils.EMPTY));
                }
            }

        };

        builder.setReuseItem(false);
        builder.addAction(new ActionLink<>() {

            private static final long serialVersionUID = 7511002881490248598L;

            @Override
            public void onClick(final AjaxRequestTarget target, final ConnObject modelObject) {
                viewConnObject(modelObject, target);
            }
        }, ActionLink.ActionType.VIEW, IdMEntitlement.RESOURCE_GET_CONNOBJECT).
                setItems(listOfItems).
                includes(ConnIdSpecialName.UID,
                        ConnIdSpecialName.NAME,
                        ConnIdSpecialName.ENABLE).
                withChecks(ListViewPanel.CheckAvailability.NONE).
                setReuseItem(false);

        if (!StringUtils.equals(anyType, SyncopeConstants.REALM_ANYTYPE)) {
            builder.addAction(new ActionLink<>() {

                private static final long serialVersionUID = 6377238742125L;

                @Override
                public void onClick(final AjaxRequestTarget target, final ConnObject modelObject) {
                    try {
                        ReconStatus status = reconciliationRestClient.status(
                                new ReconQuery.Builder(anyType, resource.getKey()).fiql(modelObject.getFiql()).build());

                        pullConnObject(
                                modelObject.getFiql(),
                                target,
                                resource.getKey(),
                                anyType,
                                status.getRealm(),
                                StringUtils.isNotBlank(status.getAnyKey()),
                                pageRef);
                    } catch (Exception e) {
                        LOG.error("While puling single object {} {} with FIQL '{}'",
                                anyType, resource.getKey(), modelObject.getFiql(), e);

                        SyncopeConsoleSession.get().onException(e);
                        ((BasePage) pageRef.getPage()).getNotificationPanel().refresh(target);
                    }
                }
            }, ActionLink.ActionType.RECONCILIATION_PULL, IdRepoEntitlement.TASK_EXECUTE);

            builder.includes(STATUS);
        }

        add(builder.build("objs"));

        arrows = new WebMarkupContainer("arrows");
        add(arrows.setOutputMarkupId(true));

        arrows.add(new AjaxLink<Serializable>("next") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                List<ConnObject> listOfItems = reloadItems(resource.getKey(), anyType, nextPageCookie, getFiql());
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
            AjaxRequestTarget target = SearchClausePanel.SearchEvent.class.cast(event.getPayload()).getTarget();
            List<ConnObject> listOfItems = reloadItems(resource.getKey(), anyType, null, getFiql());
            target.add(arrows);
            send(ConnObjectListViewPanel.this, Broadcast.DEPTH, new ListViewReload<>(listOfItems, target));
        } else {
            super.onEvent(event);
        }
    }

    protected abstract void viewConnObject(ConnObject connObjectTO, AjaxRequestTarget target);

    protected abstract void pullConnObject(
            String fiql,
            AjaxRequestTarget target,
            String resource,
            String anyType,
            String realm,
            boolean isOnSyncope,
            PageReference pageRef);

    protected List<ConnObject> reloadItems(
            final String resource,
            final String anyType,
            final String cookie,
            final String fiql) {

        Pair<String, List<ConnObject>> items = resourceRestClient.searchConnObjects(
                resource,
                anyType,
                new ConnObjectTOQuery.Builder().
                        size(SIZE).
                        pagedResultsCookie(cookie).
                        fiql(fiql),
                new SortParam<>(ConnIdSpecialName.UID, true));

        nextPageCookie = items.getLeft();
        return items.getRight();
    }

    protected AbstractSearchPanel getSearchPanel(final String id, final String anyType) {
        final List<SearchClause> clauses = new ArrayList<>();
        final SearchClause clause = new SearchClause();
        clauses.add(clause);

        clause.setComparator(SearchClause.Comparator.EQUALS);
        clause.setType(SearchClause.Type.ATTRIBUTE);
        clause.setProperty("");

        AnyTypeKind anyTypeKind =
                StringUtils.equals(anyType, SyncopeConstants.REALM_ANYTYPE) || StringUtils.isEmpty(anyType)
                ? AnyTypeKind.ANY_OBJECT
                : anyTypeRestClient.read(anyType).getKind();

        return new ConnObjectSearchPanel.Builder(resource, anyTypeKind, anyType,
                new ListModel<>(clauses), pageRef).required(true).enableSearch().build(id);
    }

    protected String getFiql() {
        return SearchUtils.buildFIQL(
                searchPanel.getModel().getObject(),
                SyncopeClient.getConnObjectTOFiqlSearchConditionBuilder(),
                searchPanel.getAvailableSchemaTypes(),
                SearchUtils.NO_CUSTOM_CONDITION);
    }
}
