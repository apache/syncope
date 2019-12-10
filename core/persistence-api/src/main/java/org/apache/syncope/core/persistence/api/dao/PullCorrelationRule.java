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
package org.apache.syncope.core.persistence.api.dao;

import java.util.Optional;
import org.apache.syncope.common.lib.policy.PullCorrelationRuleConf;
import org.apache.syncope.common.lib.types.MatchType;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.identityconnectors.framework.common.objects.SyncDelta;

/**
 * Interface for correlation rule to be evaluated during PullJob execution.
 */
@FunctionalInterface
public interface PullCorrelationRule {

    PullMatch NO_MATCH = new PullMatch(MatchType.ANY, null);

    default void setConf(PullCorrelationRuleConf conf) {
    }

    /**
     * Return a search condition.
     *
     * @param syncDelta change operation, including external attributes
     * @param provision resource provision
     * @return search condition.
     */
    SearchCond getSearchCond(SyncDelta syncDelta, Provision provision);

    /**
     * Create matching information for the given Any, found matching for the given
     * {@link SyncDelta} and {@link Provision}.
     * For users, this might end with creating / updating / deleting a
     * {@link org.apache.syncope.core.persistence.api.entity.user.LinkedAccount}.
     *
     * @param any any
     * @param syncDelta change operation, including external attributes
     * @param provision resource provision
     * @return matching information
     */
    default PullMatch matching(Any<?> any, SyncDelta syncDelta, Provision provision) {
        return new PullMatch(MatchType.ANY, any);
    }

    /**
     * Optionally create matching information in case no matching Any was found for the given
     * {@link SyncDelta} and {@link Provision}.
     * For users, this might end with creating a
     * {@link org.apache.syncope.core.persistence.api.entity.user.LinkedAccount}.
     *
     * @param syncDelta change operation, including external attributes
     * @param provision resource provision
     * @return matching information
     */
    default Optional<PullMatch> unmatching(SyncDelta syncDelta, Provision provision) {
        return Optional.of(NO_MATCH);
    }
}
