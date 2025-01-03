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
package org.apache.syncope.client.console.wizards.any;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.SyncopeConsoleSession;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class AnnotatedBeanPanel extends Panel {

    private static final long serialVersionUID = 4228064224811390809L;

    public <T extends AnyTO> AnnotatedBeanPanel(final String id, final T anyTO) {
        super(id);

        // ------------------------
        // Creation date
        // ------------------------
        add(new Label("creationDate", new Model<>(anyTO.getCreationDate() == null
                ? StringUtils.EMPTY
                : SyncopeConsoleSession.get().getDateFormat().format(anyTO.getCreationDate()))));
        // ------------------------

        // ------------------------
        // Last change date
        // ------------------------
        add(new Label("lastChangeDate", new Model<>(anyTO.getLastChangeDate() == null
                ? StringUtils.EMPTY
                : SyncopeConsoleSession.get().getDateFormat().format(anyTO.getLastChangeDate()))));
        // ------------------------

        // ------------------------
        // Creator
        // ------------------------
        add(new Label("creator", new Model<>(anyTO.getCreator() == null
                ? StringUtils.EMPTY : anyTO.getCreator())));
        // ------------------------

        // ------------------------
        // Last modifier
        // ------------------------
        add(new Label("lastModifier", new Model<>(anyTO.getLastModifier() == null
                ? StringUtils.EMPTY : anyTO.getLastModifier())));
        // ------------------------

        // ------------------------
        // Creator
        // ------------------------
        add(new Label("creationContext", new Model<>(anyTO.getCreationContext() == null
                ? StringUtils.EMPTY : anyTO.getCreationContext())));
        // ------------------------

        // ------------------------
        // Last modifier
        // ------------------------
        add(new Label("lastChangeContext", new Model<>(anyTO.getLastChangeContext() == null
                ? StringUtils.EMPTY : anyTO.getLastChangeContext())));
        // ------------------------
    }
}
