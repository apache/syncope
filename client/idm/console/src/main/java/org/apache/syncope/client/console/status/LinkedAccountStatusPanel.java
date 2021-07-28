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

import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.panels.RemoteObjectPanel;
import org.apache.syncope.client.console.wizards.any.ConnObjectPanel;
import org.apache.syncope.client.ui.commons.Constants;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;

public class LinkedAccountStatusPanel extends RemoteObjectPanel {

    private static final long serialVersionUID = 7662852503618434902L;

    private final String resource;

    private final String anyTypeKey;

    private final String connObjectKeyValue;

    public LinkedAccountStatusPanel(
            final String resource,
            final String anyTypeKey,
            final String connObjectKeyValue) {

        this.resource = resource;
        this.anyTypeKey = anyTypeKey;
        this.connObjectKeyValue = connObjectKeyValue;

        add(new ConnObjectPanel(
                REMOTE_OBJECT_PANEL_ID,
                Pair.<IModel<?>, IModel<?>>of(Model.of(Constants.SYNCOPE), new ResourceModel("resource")),
                getConnObjectTOs(),
                false));
    }

    @Override
    protected Pair<ConnObjectTO, ConnObjectTO> getConnObjectTOs() {
        Optional<ReconStatus> status = ReconStatusUtils.getReconStatus(anyTypeKey, connObjectKeyValue, resource);

        return status.map(reconStatus -> Pair.of(reconStatus.getOnSyncope(), reconStatus.getOnResource())).orElse(null);
    }
}
