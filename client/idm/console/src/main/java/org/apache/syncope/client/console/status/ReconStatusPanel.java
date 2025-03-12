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
package org.apache.syncope.client.console.status;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.RemoteObjectPanel;
import org.apache.syncope.client.console.wizards.any.ConnObjectPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

public class ReconStatusPanel extends RemoteObjectPanel {

    private static final long serialVersionUID = 8000309881812037770L;

    @SpringBean
    protected ReconStatusUtils reconStatusUtils;

    protected final String resource;

    protected final String anyTypeKey;

    protected final String anyKey;

    public ReconStatusPanel(
            final String resource,
            final String anyTypeKey,
            final String anyKey) {

        this.resource = resource;
        this.anyTypeKey = anyTypeKey;
        this.anyKey = anyKey;

        add(new ConnObjectPanel(
                REMOTE_OBJECT_PANEL_ID,
                Pair.of(Model.of(Constants.SYNCOPE), new ResourceModel("resource")),
                getConnObjectTOs(),
                false));
    }

    @Override
    protected Pair<ConnObject, ConnObject> getConnObjectTOs() {
        List<Pair<String, ReconStatus>> statuses =
                reconStatusUtils.getReconStatuses(anyTypeKey, anyKey, List.of(resource));

        return statuses.isEmpty()
                ? null
                : Pair.of(statuses.getFirst().getRight().getOnSyncope(),
                          statuses.getFirst().getRight().getOnResource());
    }
}
