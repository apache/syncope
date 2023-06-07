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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.client.ui.commons.ConnIdSpecialName;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.rest.api.beans.ReconQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconStatusUtils implements Serializable {

    private static final long serialVersionUID = -5411720003057109354L;

    private static final Logger LOG = LoggerFactory.getLogger(ReconStatusUtils.class);

    protected final ReconciliationRestClient reconciliationRestClient;

    public ReconStatusUtils(final ReconciliationRestClient reconciliationRestClient) {
        this.reconciliationRestClient = reconciliationRestClient;
    }

    public Optional<ReconStatus> getReconStatus(
            final String anyTypeKey, final String connObjectKeyValue, final String resource) {

        ReconStatus result = null;
        try {
            result = reconciliationRestClient.status(new ReconQuery.Builder(anyTypeKey, resource).
                    fiql(ConnIdSpecialName.UID + "==" + connObjectKeyValue).build());
        } catch (Exception e) {
            LOG.warn("Unexpected error for {} {} on {}", anyTypeKey, connObjectKeyValue, resource, e);
        }
        return Optional.ofNullable(result);
    }

    public List<Pair<String, ReconStatus>> getReconStatuses(
            final String anyTypeKey, final String anyKey, final Collection<String> resources) {

        return resources.stream().map(resource -> {
            try {
                return Pair.of(resource, reconciliationRestClient.status(
                        new ReconQuery.Builder(anyTypeKey, resource).anyKey(anyKey).build()));
            } catch (Exception e) {
                LOG.warn("Unexpected error for {} {} on {}", anyTypeKey, anyKey, resource, e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
