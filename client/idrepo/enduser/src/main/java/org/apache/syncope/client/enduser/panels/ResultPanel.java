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
package org.apache.syncope.client.enduser.panels;

import java.util.ArrayList;
import java.util.List;
import org.apache.syncope.client.ui.commons.panels.SimpleListViewPanel;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.client.ui.commons.wizards.any.AbstractResultPanel;
import org.apache.wicket.Component;
import org.apache.wicket.PageReference;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultPanel extends AbstractResultPanel<String, List<StatusBean>> {

    private static final long serialVersionUID = -8995647450549098844L;

    protected static final Logger LOG = LoggerFactory.getLogger(ResultPanel.class);

    private final PageReference pageRef;

    public ResultPanel(final String item, final List<StatusBean> errors, final PageReference pageRef) {
        super(item, errors);
        this.pageRef = pageRef;
    }

    @Override
    protected Panel customResultBody(final String panelId, final String item, final List<StatusBean> errorBeans) {
        return new SimpleListViewPanel.Builder<>(StatusBean.class, pageRef) {

            private static final long serialVersionUID = -6809736686861678498L;

            @Override
            protected Component getValueComponent(final String key, final StatusBean bean) {
                if ("status".equalsIgnoreCase(key)) {
                    return StatusUtils.getWarningStatusPanel("field");
                } else {
                    return super.getValueComponent(key, bean);
                }
            }
        }.setModel(new ListModel<>(new ArrayList<>()))
                .setItems(errorBeans)
                .includes("resource", "status")
                .build(panelId);
    }

}
