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
package org.apache.syncope.common.lib.types;

import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

public final class IdMImplementationType {

    public static final String RECON_FILTER_BUILDER = "RECON_FILTER_BUILDER";

    public static final String PROPAGATION_ACTIONS = "PROPAGATION_ACTIONS";

    public static final String INBOUND_ACTIONS = "INBOUND_ACTIONS";

    public static final String PUSH_ACTIONS = "PUSH_ACTIONS";

    public static final String INBOUND_CORRELATION_RULE = "INBOUND_CORRELATION_RULE";

    public static final String PUSH_CORRELATION_RULE = "PUSH_CORRELATION_RULE";

    public static final String PROVISION_SORTER = "PROVISION_SORTER";

    public static final String LIVE_SYNC_DELTA_MAPPER = "LIVE_SYNC_DELTA_MAPPER";

    private static final Map<String, String> VALUES = Map.ofEntries(
            Pair.of(RECON_FILTER_BUILDER, "org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder"),
            Pair.of(PROPAGATION_ACTIONS, "org.apache.syncope.core.provisioning.api.propagation.PropagationActions"),
            Pair.of(INBOUND_ACTIONS, "org.apache.syncope.core.provisioning.api.pushpull.InboundActions"),
            Pair.of(PUSH_ACTIONS, "org.apache.syncope.core.provisioning.api.pushpull.PushActions"),
            Pair.of(INBOUND_CORRELATION_RULE, "org.apache.syncope.core.provisioning.api.rules.InboundCorrelationRule"),
            Pair.of(PUSH_CORRELATION_RULE, "org.apache.syncope.core.provisioning.api.rules.PushCorrelationRule"),
            Pair.of(PROVISION_SORTER, "org.apache.syncope.core.provisioning.api.ProvisionSorter"),
            Pair.of(LIVE_SYNC_DELTA_MAPPER, "org.apache.syncope.core.provisioning.api.pushpull.LiveSyncDeltaMapper"));

    public static Map<String, String> values() {
        return VALUES;
    }

    private IdMImplementationType() {
        // private constructor for static utility class
    }
}
