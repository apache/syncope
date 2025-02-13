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
package org.apache.syncope.core.provisioning.api.rules;

import java.util.Optional;
import org.apache.syncope.common.lib.policy.InboundCorrelationRuleConf;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.identityconnectors.framework.common.objects.LiveSyncDelta;

/**
 * Interface for correlation rule to be evaluated during inbound task execution.
 */
@FunctionalInterface
public interface InboundCorrelationRule {

    InboundMatch NO_MATCH = new InboundMatch(MatchType.ANY, null);

    default void setConf(InboundCorrelationRuleConf conf) {
    }

    /**
     * Return a search condition.
     *
     * @param syncDelta change operation, including external attributes
     * @param provision resource provision
     * @return search condition.
     */
    SearchCond getSearchCond(LiveSyncDelta syncDelta, Provision provision);

    /**
     * Create matching information for the given Any, found matching for the given
     * {@link LiveSyncDelta} and {@link Provision}.
     * For users, this might end with creating / updating / deleting a
     * {@link org.apache.syncope.core.persistence.api.entity.user.LinkedAccount}.
     *
     * @param any any
     * @param syncDelta change operation, including external attributes
     * @param provision resource provision
     * @return matching information
     */
    default InboundMatch matching(Any any, LiveSyncDelta syncDelta, Provision provision) {
        return new InboundMatch(MatchType.ANY, any);
    }

    /**
     * Optionally create matching information in case no matching Any was found for the given
     * {@link LiveSyncDelta} and {@link Provision}.
     * For users, this might end with creating a
     * {@link org.apache.syncope.core.persistence.api.entity.user.LinkedAccount}.
     *
     * @param syncDelta change operation, including external attributes
     * @param provision resource provision
     * @return matching information
     */
    default Optional<InboundMatch> unmatching(LiveSyncDelta syncDelta, Provision provision) {
        return Optional.of(NO_MATCH);
    }
}
