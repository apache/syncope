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

import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.DerSchemaTO;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.PropertyModel;

public class DerSchemaDetails extends AbstractSchemaDetailsPanel {

    private static final long serialVersionUID = 6668789770131753386L;

    public DerSchemaDetails(final String id, final DerSchemaTO schemaTO) {
        super(id, schemaTO);

        TextField<String> expression = new TextField<>("expression", new PropertyModel<>(schemaTO, "expression"));
        expression.setRequired(true);
        add(expression);

        add(Constants.getJEXLPopover(this, TooltipConfig.Placement.right));
    }
}
