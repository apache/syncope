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
import java.util.List;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.commons.ConnIdSpecialName;
import org.apache.syncope.client.console.panels.ListViewPanel.ListViewReload;
import org.apache.syncope.client.console.rest.ResourceRestClient;
import org.apache.syncope.client.console.wicket.extensions.markup.html.repeater.data.table.CollectionPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;

public abstract class ConnObjectListViewPanel extends Panel {

    private static final long serialVersionUID = 4986172040062752781L;

    private static final int SIZE = 10;

    private String nextPageCookie;

    protected ConnObjectListViewPanel(
            final String id,
            final String resource,
            final String anyType,
            final PageReference pageRef) {

        super(id);

        final List<ConnObjectTO> listOfItems = reloadItems(resource, anyType, null);

        final ListViewPanel.Builder<ConnObjectTO> builder = new ListViewPanel.Builder<ConnObjectTO>(
                ConnObjectTO.class, pageRef) {

            private static final long serialVersionUID = -8251750413385566738L;

            @Override
            protected Component getValueComponent(final String key, final ConnObjectTO bean) {
                final AttrTO attrTO = IterableUtils.find(bean.getAttrs(), new Predicate<AttrTO>() {

                    @Override
                    public boolean evaluate(final AttrTO object) {
                        return object.getSchema().equals(key);
                    }
                });

                return attrTO == null || CollectionUtils.isEmpty(attrTO.getValues())
                        ? new Label("field", StringUtils.EMPTY)
                        : new CollectionPanel("field", attrTO.getValues());
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

        final WebMarkupContainer arrows = new WebMarkupContainer("arrows");
        add(arrows.setOutputMarkupId(true));

        arrows.add(new AjaxLink<Serializable>("next") {

            private static final long serialVersionUID = -7978723352517770644L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                final List<ConnObjectTO> listOfItems = reloadItems(resource, anyType, nextPageCookie);
                target.add(arrows);
                send(ConnObjectListViewPanel.this, Broadcast.DEPTH,
                        new ListViewReload<ConnObjectTO>(listOfItems, target));
            }

            @Override
            public boolean isVisible() {
                return nextPageCookie != null;
            }
        });
    }

    protected abstract void viewConnObject(ConnObjectTO connObjectTO, AjaxRequestTarget target);

    private List<ConnObjectTO> reloadItems(
            final String resource,
            final String anyType,
            final String cookie) {

        final Pair<String, List<ConnObjectTO>> items = new ResourceRestClient().listConnObjects(resource,
                anyType,
                SIZE,
                cookie,
                new SortParam<String>(ConnIdSpecialName.UID, true));

        nextPageCookie = items.getLeft();
        return items.getRight();
    }
}
