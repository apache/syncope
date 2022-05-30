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
import org.apache.syncope.client.ui.commons.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SCIMConfGeneralPanel extends SCIMConfTabPanel {

    private static final long serialVersionUID = 2765863608539154422L;

    private static final Logger LOG = LoggerFactory.getLogger(SCIMConfGeneralPanel.class);

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

        AjaxTextFieldPanel bulkMaxOperationsPanel =
                new AjaxTextFieldPanel("bulkMaxOperations", "bulkMaxOperations",
                        new PropertyModel<>("bulkMaxOperations", "bulkMaxOperations") {

                    private static final long serialVersionUID = -6427731218492117883L;

                    @Override
                    public String getObject() {
                        return String.valueOf(scimGeneralConf.getBulkMaxOperations());
                    }

                    @Override
                    public void setObject(final String object) {
                        try {
                            scimGeneralConf.setBulkMaxOperations(Integer.parseInt(object));
                        } catch (NumberFormatException e) {
                            LOG.error("Invalid value provided for 'bulkMaxOperations': {}", object, e);
                        }
                    }
                });
        bulkMaxOperationsPanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel bulkMaxMaxPayloadSizePanel =
                new AjaxTextFieldPanel("bulkMaxMaxPayloadSize", "bulkMaxMaxPayloadSize",
                        new PropertyModel<>("bulkMaxMaxPayloadSize", "bulkMaxMaxPayloadSize") {

                    private static final long serialVersionUID = -6427731218492117883L;

                    @Override
                    public String getObject() {
                        return String.valueOf(scimGeneralConf.getBulkMaxPayloadSize());
                    }

                    @Override
                    public void setObject(final String object) {
                        try {
                            scimGeneralConf.setBulkMaxPayloadSize(Integer.parseInt(object));
                        } catch (NumberFormatException e) {
                            LOG.error("Invalid value provided for 'bulkMaxPayloadSize': {}", object, e);
                        }
                    }
                });
        bulkMaxMaxPayloadSizePanel.setChoices(plainSchemaNames);

        AjaxTextFieldPanel filterMaxResultsPanel =
                new AjaxTextFieldPanel("filterMaxResults", "filterMaxResults",
                        new PropertyModel<>("filterMaxResults", "filterMaxResults") {

                    private static final long serialVersionUID = -6427731218492117883L;

                    @Override
                    public String getObject() {
                        return String.valueOf(scimGeneralConf.getFilterMaxResults());
                    }

                    @Override
                    public void setObject(final String object) {
                        try {
                            scimGeneralConf.setFilterMaxResults(Integer.parseInt(object));
                        } catch (NumberFormatException e) {
                            LOG.error("Invalid value provided for 'filterMaxResults': {}", object, e);
                        }
                    }
                });
        filterMaxResultsPanel.setChoices(plainSchemaNames);

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
        add(bulkMaxMaxPayloadSizePanel);
        add(filterMaxResultsPanel);
        add(eTagValuePanel);
    }
}
