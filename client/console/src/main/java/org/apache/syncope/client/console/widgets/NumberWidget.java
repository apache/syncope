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
package org.apache.syncope.client.console.widgets;

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Comparator;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.layout.AnyObjectFormLayoutInfo;
import org.apache.syncope.client.console.layout.FormLayoutInfoUtils;
import org.apache.syncope.client.console.layout.GroupFormLayoutInfo;
import org.apache.syncope.client.console.layout.UserFormLayoutInfo;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.client.console.rest.AnyTypeRestClient;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.syncope.client.console.pages.Realms;
import org.apache.syncope.client.console.pages.Roles;
import org.apache.syncope.client.console.topology.Topology;
import org.apache.wicket.request.mapper.parameter.PageParameters;

public class NumberWidget extends BaseWidget {

    private static final long serialVersionUID = -816175678514035085L;

    private int number;

    private final Label numberLabel;

    public NumberWidget(final String id, final String bg, final int number, final String label, final String icon) {
        super(id);
        this.number = number;
        setOutputMarkupId(true);

        WebMarkupContainer box = new WebMarkupContainer("box");
        box.add(new AttributeAppender("class", " " + bg));
        box.add(new AjaxEventBehavior("onmousedown") {
            @Override 
            protected void onEvent(final AjaxRequestTarget target) {
                List<AnyTypeTO> anyTypeTOs = new AnyTypeRestClient().list();
                PageParameters pageParameters = new PageParameters();
                if (id.equals("totalUsers")) {
                    pageParameters.add("selectedIndex", 1);
                    setResponsePage(Realms.class, pageParameters);
                } else if (id.equals("totalGroups")) {
                    pageParameters.add("selectedIndex", 2);
                    setResponsePage(Realms.class, pageParameters);
                } else if (id.equals("totalAny1OrRoles")) {
                    if (icon.equals("ion ion-gear-a")) {
                        final Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>> 
                                formLayoutInfo = FormLayoutInfoUtils.fetch(anyTypeTOs);
                        Collections.sort(anyTypeTOs, new AnyTypeComparator());
                        int selectedIndex = 1;
                        for (final AnyTypeTO anyTypeTO : anyTypeTOs) {
                            if (anyTypeTO.getKey().equals(label)) {
                                pageParameters.add("selectedIndex", selectedIndex);
                                break;
                            }
                            selectedIndex++;
                        }
                        setResponsePage(Realms.class, pageParameters);
                    } else {
                        setResponsePage(Roles.class);
                    }
                } else if (id.equals("totalAny2OrResources")) {
                    if (icon.equals("ion ion-gear-a")) {
                        final Triple<UserFormLayoutInfo, GroupFormLayoutInfo, Map<String, AnyObjectFormLayoutInfo>>
                                formLayoutInfo = FormLayoutInfoUtils.fetch(anyTypeTOs);
                        Collections.sort(anyTypeTOs, new AnyTypeComparator());
                        int selectedIndex = 1;
                        for (final AnyTypeTO anyTypeTO : anyTypeTOs) {
                            if (anyTypeTO.getKey().equals(label)) {
                                pageParameters.add("selectedIndex", selectedIndex);
                                break;
                            }
                            selectedIndex++;
                        }
                        setResponsePage(Realms.class, pageParameters);
                    } else {
                        setResponsePage(Topology.class);
                    }
                } else {
                    pageParameters.add("selectedIndex", 0);
                    setResponsePage(Realms.class, pageParameters);
                }
            }
        });
        add(box);

        numberLabel = new Label("number", number);
        numberLabel.setOutputMarkupId(true);
        box.add(numberLabel);
        box.add(new Label("label", label));

        Label iconLabel = new Label("icon");
        iconLabel.add(new AttributeAppender("class", icon));
        box.add(iconLabel);
    }

    public boolean refresh(final int number) {
        if (this.number != number) {
            this.number = number;
            numberLabel.setDefaultModelObject(number);
            return true;
        }
        return false;
    }

    private static class AnyTypeComparator implements Comparator<AnyTypeTO> {

        @Override
        public int compare(final AnyTypeTO o1, final AnyTypeTO o2) {
            if (o1.getKind() == AnyTypeKind.USER) {
                return -1;
            }
            if (o2.getKind() == AnyTypeKind.USER) {
                return 1;
            }

            if (o1.getKind() == AnyTypeKind.GROUP) {
                return -1;
            }
            if (o2.getKind() == AnyTypeKind.GROUP) {
                return 1;
            }

            return ComparatorUtils.<String>naturalComparator().compare(o1.getKey(), o2.getKey());
        }

    }
}
