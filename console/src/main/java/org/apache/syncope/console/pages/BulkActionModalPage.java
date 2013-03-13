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

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.console.rest.BaseRestClient;
import org.apache.syncope.console.wicket.ajax.markup.html.ClearIndicatingAjaxButton;
import org.apache.syncope.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.console.wicket.markup.html.form.ActionLinksPanel;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.springframework.beans.BeanUtils;

public class BulkActionModalPage<T, S> extends BaseModalPage {

    private static final long serialVersionUID = 4114026480146090962L;

    public BulkActionModalPage(
            final PageReference pageRef,
            final ModalWindow window,
            final List<T> items,
            final List<IColumn<T, S>> columns,
            final Collection<ActionLink.ActionType> actions,
            final BaseRestClient bulkActionExecutor,
            final String idFieldName,
            final String pageId) {

        super();

        final SortableDataProvider<T, S> dataProvider = new SortableDataProvider<T, S>() {

            @Override
            public Iterator<? extends T> iterator(long first, long count) {
                return items.iterator();
            }

            @Override
            public long size() {
                return items.size();
            }

            @Override
            public IModel<T> model(T object) {
                return new CompoundPropertyModel<T>(object);
            }
        };

        add(new AjaxFallbackDefaultDataTable<T, S>(
                "selectedObjects",
                new ArrayList<IColumn<T, S>>(columns.subList(1, columns.size() - 1)),
                dataProvider,
                Integer.MAX_VALUE).setVisible(items != null && !items.isEmpty()));

        final ActionLinksPanel actionPanel = new ActionLinksPanel("actions", new Model(), getPageReference());
        add(actionPanel);

        for (ActionLink.ActionType action : actions) {
            final BulkAction bulkAction = new BulkAction();
            for (Object item : items) {
                try {
                    bulkAction.addTarget(getTargetId(item, idFieldName).toString());
                } catch (Exception e) {
                    LOG.error("Error retrieving item id {}", idFieldName, e);
                }
            }

            switch (action) {
                case DELETE:
                    bulkAction.setOperation(BulkAction.Type.DELETE);
                    break;
                case SUSPEND:
                    bulkAction.setOperation(BulkAction.Type.SUSPEND);
                    break;
                case REACTIVATE:
                    bulkAction.setOperation(BulkAction.Type.REACTIVATE);
                    break;
                case EXECUTE:
                    bulkAction.setOperation(BulkAction.Type.EXECUTE);
                    break;
                case DRYRUN:
                    bulkAction.setOperation(BulkAction.Type.DRYRUN);
                    break;
                default:
                    LOG.error("Bulk action type not supported");
            }

            actionPanel.add(new ActionLink() {

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    try {
                        final BulkActionRes res = (BulkActionRes) bulkActionExecutor.getClass().
                                getMethod("bulkAction", BulkAction.class).invoke(bulkActionExecutor, bulkAction);

                        setResponsePage(new BulkActionResultModalPage(pageRef, window, items, columns, res, idFieldName));
                    } catch (Exception e) {
                        LOG.error("Operation {} not supported", bulkAction.getOperation(), e);
                    }

                }
            }, action, pageId, !items.isEmpty());
        }

        final Form form = new Form("form");
        add(form);

        final AjaxButton cancel =
                new ClearIndicatingAjaxButton("cancel", new ResourceModel("cancel"), getPageReference()) {

                    private static final long serialVersionUID = -958724007591692537L;

                    @Override
                    protected void onSubmitInternal(final AjaxRequestTarget target, final Form<?> form) {
                        window.close(target);
                    }
                };

        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
    }

    private Object getTargetId(final Object target, final String idFieldName)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final PropertyDescriptor propDesc = BeanUtils.getPropertyDescriptor(target.getClass(), idFieldName);
        return propDesc.getReadMethod().invoke(target, new Object[0]);
    }
}
