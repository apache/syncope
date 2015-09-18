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
import org.apache.syncope.client.console.commons.Mode;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

public final class GroupPanel extends Panel {

    private static final long serialVersionUID = 4216376097320768369L;

    public static class Builder implements Serializable {

        private static final long serialVersionUID = 8150440254654306070L;

        private String id;

        private Form form;

        private GroupTO groupTO;

        private Mode mode;

        private PageReference pageReference;

        public Builder(final String id) {
            this.id = id;
        }

        public Builder form(final Form form) {
            this.form = form;
            return this;
        }

        public Builder groupTO(final GroupTO groupTO) {
            this.groupTO = groupTO;
            return this;
        }

        public Builder groupModalPageMode(final Mode mode) {
            this.mode = mode;
            return this;
        }

        public GroupPanel build() {
            return new GroupPanel(this);
        }
    }

    private GroupPanel(final Builder builder) {
        super(builder.id);
        

    }
}
