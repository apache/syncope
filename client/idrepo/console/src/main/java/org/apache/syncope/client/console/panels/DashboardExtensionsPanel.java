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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.console.widgets.BaseExtWidget;
import org.apache.syncope.client.ui.commons.annotations.ExtWidget;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class DashboardExtensionsPanel extends Panel {

    private static final long serialVersionUID = 6381578992589664490L;

    protected static final Logger LOG = LoggerFactory.getLogger(DashboardExtensionsPanel.class);

    public DashboardExtensionsPanel(
            final String id, final List<Class<? extends BaseExtWidget>> extWidgetClasses, final PageReference pageRef) {

        super(id);

        List<BaseExtWidget> instances = new ArrayList<>();

        extWidgetClasses.forEach(clazz -> {
            Constructor<? extends BaseExtWidget> constructor =
                    ClassUtils.getConstructorIfAvailable(clazz, String.class, PageReference.class);
            if (constructor == null) {
                LOG.error("Could not find required constructor in {}, ignoring", clazz);
            } else {
                try {
                    instances.add(constructor.newInstance("widget", pageRef));
                } catch (Exception e) {
                    LOG.error("While creating instance of {}", clazz, e);
                }
            }
        });

        ListView<BaseExtWidget> widgets = new ListView<>("widgets", instances) {

            private static final long serialVersionUID = 4949588177564901031L;

            @Override
            protected void populateItem(final ListItem<BaseExtWidget> item) {
                WebMarkupContainer widgetContainer = new WebMarkupContainer("widgetContainer");
                widgetContainer.setOutputMarkupId(true);
                ExtWidget ann = item.getModelObject().getClass().getAnnotation(ExtWidget.class);
                if (ann != null) {
                    widgetContainer.add(new AttributeModifier("class", ann.cssClass()));
                }
                item.add(widgetContainer);

                item.getModelObject().setOutputMarkupId(true);
                widgetContainer.add(item.getModelObject());
            }
        };
        add(widgets);
    }
}
