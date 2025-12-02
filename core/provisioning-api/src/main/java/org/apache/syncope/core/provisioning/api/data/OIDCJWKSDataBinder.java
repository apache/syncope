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
package org.apache.syncope.core.provisioning.api.data;

import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;

public interface OIDCJWKSDataBinder {

    String PARAMETER_STATE = "state";

    enum JsonWebKeyLifecycleState {
        /**
         * The key state is active and current and is used for crypto operations as necessary.
         * Per the rotation schedule, the key with this status would be replaced and rotated by the future key.
         */
        CURRENT(0),
        /**
         * The key state is one for the future and will take the place of the current key per the rotation schedule.
         */
        FUTURE(1),
        /**
         * Previous key prior to the current key.
         * This key continues to remain valid and available, and is a candidate to be removed from the keystore
         * per the revocation schedule.
         */
        PREVIOUS(2);

        private final long state;

        private JsonWebKeyLifecycleState(final long state) {
            this.state = state;
        }

        public long getState() {
            return state;
        }
    }

    OIDCJWKSTO getOIDCJWKSTO(OIDCJWKS jwks);

    OIDCJWKS create(String jwksKeyId, String jwksType, int jwksKeySize);
}
