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
package org.apache.syncope.client.console.pages;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.panels.AbstractModalPanel;
import org.apache.syncope.client.console.rest.BaseRestClient;
import org.apache.syncope.client.console.wicket.markup.html.bootstrap.dialog.BaseModal;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.springframework.beans.BeanUtils;

public class BulkActionModalPage<T extends Serializable, S> extends AbstractModalPanel<T> {

    private static final long serialVersionUID = 4114026480146090962L;

    private final String pageId = "Any";

    public BulkActionModalPage(
            final BaseModal<T> modal,
            final PageReference pageRef,
            final Collection<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final BaseRestClient bulkActionExecutor,
            final String keyFieldName) {

        super(modal, pageRef);

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

        add(new AjaxFallbackDefaultDataTable<>(
                "selectedObjects",
                new ArrayList<>(columns.subList(1, columns.size() - 1)),
                dataProvider,
                Integer.MAX_VALUE).setVisible(items != null && !items.isEmpty()));

        @SuppressWarnings("rawtypes")
        final ActionLinksPanel<Serializable> actionPanel = ActionLinksPanel.builder(pageRef).build("actions");
        add(actionPanel);

        for (ActionLink.ActionType action : actions) {
            final BulkAction bulkAction = new BulkAction();
            for (T item : items) {
                try {
                    bulkAction.getTargets().add(getTargetId(item, keyFieldName).toString());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.error("Error retrieving item id {}", keyFieldName, e);
                }
            }

            switch (action) {
                case DELETE:
                    bulkAction.setType(BulkAction.Type.DELETE);
                    break;
                case SUSPEND:
                    bulkAction.setType(BulkAction.Type.SUSPEND);
                    break;
                case REACTIVATE:
                    bulkAction.setType(BulkAction.Type.REACTIVATE);
                    break;
                case EXECUTE:
                    bulkAction.setType(BulkAction.Type.EXECUTE);
                    break;
                case DRYRUN:
                    bulkAction.setType(BulkAction.Type.DRYRUN);
                    break;
                default:
                    LOG.error("Bulk action type not supported");
            }

            actionPanel.add(new ActionLink<Serializable>() {

                private static final long serialVersionUID = -3722207913631435501L;

                @Override
                public void onClick(final AjaxRequestTarget target, final Serializable ignore) {
                    try {
                        final BulkActionResult res = (BulkActionResult) bulkActionExecutor.getClass().
                                getMethod("bulkAction", BulkAction.class).invoke(bulkActionExecutor, bulkAction);

                        modal.setContent(new BulkActionResultModalPage<>(
                                modal, pageRef, items, columns, res, keyFieldName));
                        target.add(modal);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException 
                            | IllegalArgumentException | InvocationTargetException e) {
                        error(getString(Constants.ERROR)
                                + ": Operation " + bulkAction.getType() + " not supported");
                        modal.getNotificationPanel().refresh(target);
                    }

                }
            }, action, pageId, !items.isEmpty());
        }

        final Form<Void> form = new Form<>(FORM);
        add(form);

        final AjaxButton cancel = new IndicatingAjaxButton(CANCEL, new ResourceModel(CANCEL)) {

            private static final long serialVersionUID = -958724007591692537L;

            @Override
            protected void onSubmit(final AjaxRequestTarget target, final Form<?> form) {
                modal.close(target);
            }

        };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }

    private Object getTargetId(final Object target, final String idFieldName)
            throws IllegalAccessException, InvocationTargetException {

        return BeanUtils.getPropertyDescriptor(target.getClass(), idFieldName).
                getReadMethod().invoke(target, new Object[0]);
    }
}
