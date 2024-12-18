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
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.Set;
import org.apache.syncope.common.lib.to.OrgUnit;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.core.provisioning.api.pushpull.InboundActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;

public class KafkaInboundActions implements InboundActions {

    /**
     * These are attributes added by the ConnId Kafka connector's livesync() implementation
     */
    protected static final Set<String> KAFKA_CONNECTOR_ATTRS = Set.of(
            "record.timestamp", "record.headers", "record.value");

    @Override
    public Set<String> moreAttrsToGet(final ProvisioningProfile<?, ?> profile, final Provision provision) {
        return KAFKA_CONNECTOR_ATTRS;
    }

    @Override
    public Set<String> moreAttrsToGet(final ProvisioningProfile<?, ?> profile, final OrgUnit orgUnit) {
        return KAFKA_CONNECTOR_ATTRS;
    }
}
