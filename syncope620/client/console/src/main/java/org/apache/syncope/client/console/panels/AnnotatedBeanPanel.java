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

import org.apache.syncope.client.console.SyncopeSession;
import org.apache.syncope.common.lib.to.AbstractAnnotatedBean;
import org.apache.syncope.common.lib.to.AbstractAttributableTO;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

public class AnnotatedBeanPanel extends Panel {

    private static final long serialVersionUID = 4228064224811390809L;

    public <T extends AbstractAttributableTO> AnnotatedBeanPanel(
            final String id, final AbstractAnnotatedBean sysInfoTO) {

        super(id);

        // ------------------------
        // Creation date
        // ------------------------
        add(new Label("creationDate", new Model<String>(sysInfoTO.getCreationDate() != null
                ? SyncopeSession.get().getDateFormat().format(sysInfoTO.getCreationDate()) : "")));
        // ------------------------

        // ------------------------
        // Last change date
        // ------------------------
        add(new Label("lastChangeDate", new Model<String>(sysInfoTO.getLastChangeDate() != null
                ? SyncopeSession.get().getDateFormat().format(sysInfoTO.getCreationDate()) : "")));
        // ------------------------

        // ------------------------
        // Creator
        // ------------------------
        add(new Label("creator", new Model<String>(sysInfoTO.getCreator() != null
                ? sysInfoTO.getCreator() : "")));
        // ------------------------

        // ------------------------
        // Last modifier
        // ------------------------
        add(new Label("lastModifier", new Model<String>(sysInfoTO.getLastModifier() != null
                ? sysInfoTO.getLastModifier() : "")));
        // ------------------------

    }
}
