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
package org.apache.syncope.client.console.commons;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.client.console.panels.ListViewPanel;
import org.apache.syncope.client.console.status.ReconStatusUtils;
import org.apache.syncope.client.console.wicket.markup.html.form.ActionLink;
import org.apache.syncope.client.ui.commons.status.ConnObjectWrapper;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.types.IdMEntitlement;

public class IdMStatusProvider implements StatusProvider {

    private static final long serialVersionUID = 1875374599950896631L;

    protected final ReconStatusUtils reconStatusUtils;

    public IdMStatusProvider(final ReconStatusUtils reconStatusUtils) {
        this.reconStatusUtils = reconStatusUtils;
    }

    @Override
    public Optional<Pair<ConnObject, ConnObject>> get(
            final String anyTypeKey, final String connObjectKeyValue, final String resource) {

        return reconStatusUtils.getReconStatus(anyTypeKey, connObjectKeyValue, resource).
                map(status -> Pair.of(status.getOnSyncope(), status.getOnResource()));
    }

    @Override
    public List<Triple<ConnObject, ConnObjectWrapper, String>> get(
            final AnyTO any, final Collection<String> resources) {

        return reconStatusUtils.getReconStatuses(
                any.getType(), any.getKey(), any.getResources()).stream().
                map(status -> Triple.<ConnObject, ConnObjectWrapper, String>of(
                status.getRight().getOnSyncope(),
                new ConnObjectWrapper(any, status.getLeft(), status.getRight().getOnResource()),
                null)).
                collect(Collectors.toList());
    }

    @Override
    public <T extends Serializable> void addConnObjectLink(
            final ListViewPanel.Builder<T> builder,
            final ActionLink<T> connObjectLink) {

        builder.addAction(connObjectLink, ActionLink.ActionType.VIEW, IdMEntitlement.RESOURCE_GET_CONNOBJECT);
    }
}
