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

import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGroupConf;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMConfGroupPanel extends SCIMConfTabPanel {

    protected static final Logger LOG = LoggerFactory.getLogger(SCIMConfGroupPanel.class);

    private static final long serialVersionUID = 8747864142447220523L;

    private final SCIMGroupConf scimGroupConf;

    public SCIMConfGroupPanel(final String id, final SCIMConf scimConf) {
        super(id);

        if (scimConf.getGroupConf() == null) {
            scimConf.setGroupConf(new SCIMGroupConf());
        }
        scimGroupConf = scimConf.getGroupConf();

        AjaxTextFieldPanel externalIdPanel = new AjaxTextFieldPanel(
                "externalId", "externalId", new PropertyModel<>("externalId", "externalId") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimGroupConf.getExternalId();
            }

            @Override
            public void setObject(final String object) {
                scimGroupConf.setExternalId(object);
            }
        });
        externalIdPanel.setChoices(groupPlainSchemas.getObject());

        add(externalIdPanel);
    }
}
