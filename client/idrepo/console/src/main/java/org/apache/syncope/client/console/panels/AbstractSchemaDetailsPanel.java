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

import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSchemaDetailsPanel extends Panel {

    private static final long serialVersionUID = -9096843774956370327L;

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSchemaDetailsPanel.class);

    public AbstractSchemaDetailsPanel(final String id, final SchemaTO schemaTO) {
        super(id);

        AjaxTextFieldPanel key = new AjaxTextFieldPanel(
                Constants.KEY_FIELD_NAME,
                getString(Constants.KEY_FIELD_NAME),
                new PropertyModel<>(schemaTO, Constants.KEY_FIELD_NAME));
        key.addRequiredLabel();
        key.setEnabled(schemaTO == null || schemaTO.getKey() == null || schemaTO.getKey().isEmpty());
        add(key);
    }
}
