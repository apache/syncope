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

    public static final String PULL_ACTIONS = "PULL_ACTIONS";

    public static final String PUSH_ACTIONS = "PUSH_ACTIONS";

    public static final String PULL_CORRELATION_RULE = "PULL_CORRELATION_RULE";

    public static final String PUSH_CORRELATION_RULE = "PUSH_CORRELATION_RULE";

    public static final String PROVISION_SORTER = "PROVISION_SORTER";

    private static final Map<String, String> VALUES = Map.ofEntries(
            Pair.of(RECON_FILTER_BUILDER, "org.apache.syncope.core.persistence.api.dao.Reportlet"),
            Pair.of(PROPAGATION_ACTIONS, "org.apache.syncope.core.provisioning.api.propagation.PropagationActions"),
            Pair.of(PULL_ACTIONS, "org.apache.syncope.core.provisioning.api.pushpull.PullActions"),
            Pair.of(PUSH_ACTIONS, "org.apache.syncope.core.provisioning.api.pushpull.PushActions"),
            Pair.of(PULL_CORRELATION_RULE, "org.apache.syncope.core.persistence.api.dao.PullCorrelationRule"),
            Pair.of(PUSH_CORRELATION_RULE, "org.apache.syncope.core.persistence.api.dao.PushCorrelationRule"),
            Pair.of(PROVISION_SORTER, "org.apache.syncope.core.provisioning.api.ProvisionSorter"));

    public static Map<String, String> values() {
        return VALUES;
    }

    private IdMImplementationType() {
        // private constructor for static utility class
    }
}
