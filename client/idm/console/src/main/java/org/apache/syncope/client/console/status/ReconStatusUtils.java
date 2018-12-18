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
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.client.console.rest.ReconciliationRestClient;
import org.apache.syncope.common.lib.to.ReconStatus;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReconStatusUtils implements Serializable {

    private static final long serialVersionUID = -5411720003057109354L;

    private static final Logger LOG = LoggerFactory.getLogger(ReconStatusUtils.class);

    private static final ReconciliationRestClient RECONCILIATION_REST_CLIENT = new ReconciliationRestClient();

    public static List<Pair<String, ReconStatus>> getReconStatuses(
            final AnyTypeKind anyTypeKind, final String anyKey, final Collection<String> resources) {

        return resources.stream().map(resource -> {
            try {
                return Pair.of(resource, RECONCILIATION_REST_CLIENT.status(anyTypeKind, anyKey, resource));
            } catch (Exception e) {
                LOG.warn("Unexpected error for {} {} on {}", anyTypeKind, anyKey, resource, e);
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private ReconStatusUtils() {
        // private constructor for static utility class
    }
}
