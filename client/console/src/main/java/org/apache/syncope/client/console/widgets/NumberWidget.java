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

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;

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
}
