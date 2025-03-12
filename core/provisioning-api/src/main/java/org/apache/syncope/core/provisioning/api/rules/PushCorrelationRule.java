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

import java.util.function.BiFunction;
import org.apache.syncope.common.lib.policy.PushCorrelationRuleConf;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;

/**
 * Interface for correlation rule to be evaluated during PushJob execution.
 */
@FunctionalInterface
public interface PushCorrelationRule {

    /**
     * Default FIQL builder using __UID__.
     */
    BiFunction<ConnectorObject, Provision, String> DEFAULT_FIQL_BUILDER =
            (connectorObject, provision) -> Uid.NAME + "==" + connectorObject.getUid().getUidValue();

    default void setConf(PushCorrelationRuleConf conf) {
    }

    /**
     * Returns a filter to match the given any with a connector object on the external resource identified by
     * the given provision.
     *
     * @param any user, group or any object
     * @param resource external resource
     * @param provision resource provision
     * @return filter.
     */
    Filter getFilter(Any any, ExternalResource resource, Provision provision);

    /**
     * Returns a FIQL string to match the given connector object when searching into the external resource identified by
     * the given provision.
     *
     * @param connectorObject connector object
     * @param provision resource provision
     * @return fiql
     */
    default String getFIQL(ConnectorObject connectorObject, Provision provision) {
        return DEFAULT_FIQL_BUILDER.apply(connectorObject, provision);
    }
}
