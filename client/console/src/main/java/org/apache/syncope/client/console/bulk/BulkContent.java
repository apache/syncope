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
package org.apache.syncope.client.console.bulk;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.commons.status.StatusBean;
import org.apache.syncope.client.console.pages.BasePage;
import org.apache.syncope.client.console.panels.MultilevelPanel;
import org.apache.syncope.client.console.rest.AbstractAnyRestClient;
import org.apache.syncope.client.console.rest.RestClient;
import org.apache.syncope.client.console.rest.UserRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.BulkActionResultColumn;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink.ActionType;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionsPanel;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

public class BulkContent<T extends Serializable, S> extends MultilevelPanel.SecondLevel {

    private static final long serialVersionUID = 4114026480146090963L;

    protected static final Logger LOG = LoggerFactory.getLogger(BulkContent.class);

    public BulkContent(
            final BaseModal<?> modal,
            final Collection<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final RestClient bulkActionExecutor,
            final String keyFieldName) {

        this(MultilevelPanel.SECOND_LEVEL_ID, modal, items, columns, actions, bulkActionExecutor, keyFieldName);
    }

    public BulkContent(
            final String id,
            final BaseModal<?> modal,
            final Collection<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final RestClient bulkActionExecutor,
            final String keyFieldName) {

        super(id);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        final SortableDataProvider<T, S> dataProvider = new SortableDataProvider<T, S>() {

            private static final long serialVersionUID = 5291903859908641954L;

            @Override
            public Iterator<? extends T> iterator(final long first, final long count) {
                return items.iterator();
            }

            @Override
            public long size() {
                return items.size();
            }

            @Override
            public IModel<T> model(final T object) {
                return new CompoundPropertyModel<>(object);
            }
        };

        container.add(new AjaxFallbackDefaultDataTable<>(
                "selectedObjects",
                columns,
                dataProvider,
                Integer.MAX_VALUE).setMarkupId("selectedObjects").setVisible(items != null && !items.isEmpty()));

        final ActionsPanel<Serializable> actionPanel = new ActionsPanel<>("actions", null);
        container.add(actionPanel);

        for (ActionLink.ActionType action : actions) {
            final ActionType actionToBeAddresed = action;

            actionPanel.add(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                protected boolean statusCondition(final Serializable modelObject) {
                    return items != null && !items.isEmpty();
                }

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        if (items == null || items.isEmpty()) {
                            throw new IllegalArgumentException("Invalid items");
                        }

                        String fieldName = keyFieldName;

                        BulkActionResult res = null;
                        try {
                            if (items.iterator().next() instanceof StatusBean) {
                                throw new IllegalArgumentException("Invalid items");
                            }

                            final BulkAction bulkAction = new BulkAction();
                            bulkAction.setType(BulkAction.Type.valueOf(actionToBeAddresed.name()));
                            items.forEach(item -> {
                                try {
                                    bulkAction.getTargets().add(getTargetId(item, keyFieldName).toString());
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    LOG.error("Error retrieving item id {}", keyFieldName, e);
                                }
                            });
                            res = BulkActionResult.class.cast(
                                    bulkActionExecutor.getClass().getMethod("bulkAction", BulkAction.class).invoke(
                                            bulkActionExecutor, bulkAction));
                        } catch (IllegalArgumentException biae) {
                            if (!(items.iterator().next() instanceof StatusBean)) {
                                throw new IllegalArgumentException("Invalid items");
                            }

                            if (!(bulkActionExecutor instanceof AbstractAnyRestClient)) {
                                throw new IllegalArgumentException("Invalid bulk action executor");
                            }

                            final AbstractAnyRestClient<?, ?> anyRestClient = AbstractAnyRestClient.class.cast(
                                    bulkActionExecutor);

                            // Group bean information by anyKey
                            final Map<String, List<StatusBean>> beans = new HashMap<>();
                            items.stream().map(bean -> StatusBean.class.cast(bean)).
                                    forEachOrdered(sb -> {
                                        final List<StatusBean> sblist;
                                        if (beans.containsKey(sb.getKey())) {
                                            sblist = beans.get(sb.getKey());
                                        } else {
                                            sblist = new ArrayList<>();
                                            beans.put(sb.getKey(), sblist);
                                        }
                                        sblist.add(sb);
                                    });

                            for (Map.Entry<String, List<StatusBean>> entry : beans.entrySet()) {
                                final String etag = anyRestClient.read(entry.getKey()).getETagValue();
                                switch (actionToBeAddresed.name()) {
                                    case "DEPROVISION":
                                        res = anyRestClient.deprovision(etag, entry.getKey(), entry.getValue());
                                        break;
                                    case "UNASSIGN":
                                        res = anyRestClient.unassign(etag, entry.getKey(), entry.getValue());
                                        break;
                                    case "UNLINK":
                                        res = anyRestClient.unlink(etag, entry.getKey(), entry.getValue());
                                        break;
                                    case "ASSIGN":
                                        res = anyRestClient.assign(etag, entry.getKey(), entry.getValue());
                                        break;
                                    case "LINK":
                                        res = anyRestClient.link(etag, entry.getKey(), entry.getValue());
                                        break;
                                    case "PROVISION":
                                        res = anyRestClient.provision(etag, entry.getKey(), entry.getValue());
                                        break;
                                    case "REACTIVATE":
                                        res = ((UserRestClient) anyRestClient).
                                                reactivate(etag, entry.getKey(), entry.getValue());
                                        fieldName = "resource";
                                        break;
                                    case "SUSPEND":
                                        res = ((UserRestClient) anyRestClient).
                                                suspend(etag, entry.getKey(), entry.getValue());
                                        fieldName = "resource";
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }

                        if (modal != null) {
                            modal.changeCloseButtonLabel(getString("close", null, "Close"), target);
                        }

                        final List<IColumn<T, S>> newColumnList = new ArrayList<>(columns);
                        newColumnList.add(newColumnList.size(), new BulkActionResultColumn<>(res, fieldName));

                        container.addOrReplace(new AjaxFallbackDefaultDataTable<>(
                                "selectedObjects",
                                newColumnList,
                                dataProvider,
                                Integer.MAX_VALUE).setVisible(!items.isEmpty()));

                        actionPanel.setEnabled(false);
                        actionPanel.setVisible(false);
                        target.add(container);
                        target.add(actionPanel);

                        SyncopeConsoleSession.get().info(getString(Constants.OPERATION_SUCCEEDED));
                    } catch (IllegalArgumentException | NoSuchMethodException | SecurityException
                            | IllegalAccessException | InvocationTargetException e) {
                        LOG.error("Bulk action failure", e);
                        SyncopeConsoleSession.get().error("Operation " + actionToBeAddresed.getActionId()
                                + " not supported");
                    }
                    ((BasePage) getPage()).getNotificationPanel().refresh(target);
                }
            }, action, StandardEntitlement.CONFIGURATION_LIST).hideLabel();
        }
    }

    private Object getTargetId(final Object target, final String idFieldName)
            throws IllegalAccessException, InvocationTargetException {

        return BeanUtils.getPropertyDescriptor(target.getClass(), idFieldName).
                getReadMethod().invoke(target, new Object[0]);
    }
}
