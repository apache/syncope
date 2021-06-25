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
package org.apache.syncope.client.ui.commons.panels;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.ResourceModel;

public final class CardPanel<T extends Component> extends Panel {

    private static final long serialVersionUID = -7906010415162945394L;

    private CardPanel(final String id, final Builder<T> builder) {
        super(id);
        this.setOutputMarkupId(true);
        this.setVisible(builder.visible);

        this.add(new Label("cardLabel", new ResourceModel(builder.name, builder.name)).setOutputMarkupId(true));
        this.add(builder.component);
    }

    public static class Builder<T extends Component> {

        protected String name;

        protected T component;

        protected boolean visible;

        public Builder<T> setName(final String name) {
            this.name = name;
            return this;
        }

        public Builder<T> setComponent(final T component) {
            this.component = component;
            return this;
        }

        public Builder<T> isVisible(final boolean visible) {
            this.visible = visible;
            return this;
        }

        public CardPanel<T> build(final String id) {
            return new CardPanel<>(id, this);
        }
    }
}
