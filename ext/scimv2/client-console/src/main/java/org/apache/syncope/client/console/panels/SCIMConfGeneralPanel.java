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

import java.util.Date;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.syncope.client.ui.commons.DateOps;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxDateTimeFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxNumberFieldPanel;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class SCIMConfGeneralPanel extends SCIMConfTabPanel {

    private static final long serialVersionUID = 2765863608539154422L;

    public SCIMConfGeneralPanel(final String id, final SCIMConf scimConf) {
        super(id);

        SCIMGeneralConf scimGeneralConf = scimConf.getGeneralConf();
        add(new AjaxDateTimeFieldPanel("creationDate", "creationDate", new Model<>() {

            private static final long serialVersionUID = 7075312408615929880L;

            @Override
            public Date getObject() {
                return DateOps.convert(scimGeneralConf.getCreationDate());
            }

            @Override
            public void setObject(final Date object) {
                // nothing to do
            }
        }, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT).setEnabled(false));

        add(new AjaxDateTimeFieldPanel("lastChangeDate", "lastChangeDate", new Model<>() {

            private static final long serialVersionUID = 7075312408615929880L;

            @Override
            public Date getObject() {
                return DateOps.convert(scimGeneralConf.getLastChangeDate());
            }

            @Override
            public void setObject(final Date object) {
                // nothing to do
            }
        }, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT).setEnabled(false));

        add(new AjaxNumberFieldPanel.Builder<Integer>().enableOnChange().convertValuesToString(false).
                build("bulkMaxOperations", "bulkMaxOperations", Integer.class,
                        new PropertyModel<>(scimGeneralConf, "bulkMaxOperations")));

        add(new AjaxNumberFieldPanel.Builder<Integer>().enableOnChange().convertValuesToString(false).
                build("bulkMaxPayloadSize", "bulkMaxPayloadSize", Integer.class,
                        new PropertyModel<>(scimGeneralConf, "bulkMaxPayloadSize")));

        add(new AjaxNumberFieldPanel.Builder<Integer>().enableOnChange().convertValuesToString(false).
                build("filterMaxResults", "filterMaxResults", Integer.class,
                        new PropertyModel<>(scimGeneralConf, "filterMaxResults")));
    }
}
