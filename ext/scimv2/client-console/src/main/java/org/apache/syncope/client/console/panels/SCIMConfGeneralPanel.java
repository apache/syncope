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
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxSpinnerFieldPanel;
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

public class SCIMConfGeneralPanel extends SCIMConfTabPanel {

    private static final long serialVersionUID = 2765863608539154422L;

    public SCIMConfGeneralPanel(final String id, final SCIMConf scimConf) {
        super(id);

        SCIMGeneralConf scimGeneralConf = scimConf.getGeneralConf();

        AjaxDateTimeFieldPanel creationDatePanel =
                new AjaxDateTimeFieldPanel("creationDate", "creationDate", new Model<>() {

                    private static final long serialVersionUID = 7075312408615929880L;

                    @Override
                    public Date getObject() {
                        return DateOps.convert(scimGeneralConf.getCreationDate());
                    }

                    @Override
                    public void setObject(final Date object) {
                        scimGeneralConf.setCreationDate(DateOps.convert(object));
                    }
                }, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
        creationDatePanel.setEnabled(false);

        AjaxDateTimeFieldPanel lastChangeDatePanel =
                new AjaxDateTimeFieldPanel("lastChangeDate", "lastChangeDate", new Model<>() {

                    private static final long serialVersionUID = 7075312408615929880L;

                    @Override
                    public Date getObject() {
                        return DateOps.convert(scimGeneralConf.getLastChangeDate());
                    }

                    @Override
                    public void setObject(final Date object) {
                        scimGeneralConf.setLastChangeDate(DateOps.convert(object));
                    }
                }, DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT);
        lastChangeDatePanel.setEnabled(false);

        AjaxSpinnerFieldPanel<Integer> bulkMaxOperationsPanel = new AjaxSpinnerFieldPanel.Builder<Integer>().
                build("bulkMaxOperations", "bulkMaxOperations", Integer.class,
                        new PropertyModel<>(scimGeneralConf, "bulkMaxOperations"));

        AjaxSpinnerFieldPanel<Integer> bulkMaxPayloadSizePanel = new AjaxSpinnerFieldPanel.Builder<Integer>().
                build("bulkMaxPayloadSize", "bulkMaxPayloadSize", Integer.class,
                        new PropertyModel<>(scimGeneralConf, "bulkMaxPayloadSize"));

        AjaxSpinnerFieldPanel<Integer> filterMaxResultsPanel = new AjaxSpinnerFieldPanel.Builder<Integer>().
                build("filterMaxResults", "filterMaxResults", Integer.class,
                        new PropertyModel<>(scimGeneralConf, "filterMaxResults"));

        AjaxTextFieldPanel eTagValuePanel = new AjaxTextFieldPanel("eTagValue", "eTagValue",
                new PropertyModel<>("eTagValue", "eTagValue") {

            private static final long serialVersionUID = -6427731218492117883L;

            @Override
            public String getObject() {
                return scimGeneralConf.getETagValue();
            }

            @Override
            public void setObject(final String object) {
                // nothing to do
            }
        });
        eTagValuePanel.setEnabled(false);

        add(creationDatePanel);
        add(lastChangeDatePanel);
        add(bulkMaxOperationsPanel);
        add(bulkMaxPayloadSizePanel);
        add(filterMaxResultsPanel);
        add(eTagValuePanel);
    }
}
