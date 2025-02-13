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
package org.apache.syncope.core.provisioning.api;

import java.util.List;
import java.util.Map;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.Membership;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.identityconnectors.framework.common.objects.ConnectorObject;

public interface VirAttrHandler {

    String CACHE = "virAttrCache";

    /**
     * Updates cache with values from external resource.
     *
     * @param any any object
     * @param connObj connector object from external resource
     */
    void setValues(Any any, ConnectorObject connObj);

    /**
     * Query external resource (or cache, if configured) associated to the given any for values associated to the given
     * virtual schema, not related to any membership.
     *
     * @param any any object
     * @param schema virtual schema
     * @return virtual attribute values, either for local cache or external resource, if resource is owned by the given
     * any and associated to the given virtual schema; empty list otherwise.
     */
    List<String> getValues(Any any, VirSchema schema);

    /**
     * Query external resource (or cache, if configured) associated to the given any for values associated to the given
     * virtual schema, for the given membership.
     *
     * @param any any object
     * @param membership membership
     * @param schema virtual schema
     * @return virtual attribute values, either for local cache or external resource, if resource is owned by the given
     * any and associated to the given virtual schema; empty list otherwise.
     */
    List<String> getValues(Any any, Membership<?> membership, VirSchema schema);

    /**
     * Query external resources (or cache, if configured) associated to the given any for values associated to all
     * {@link VirSchema} instances in the {@link org.apache.syncope.core.persistence.api.entity.AnyTypeClass}
     * associated to the given any, with no membership.
     *
     * @param any any object
     * @return virtual attribute values, either for local cache or external resources
     */
    Map<VirSchema, List<String>> getValues(Any any);

    /**
     * Query external resources (or cache, if configured) associated to the given any for values associated to all
     * {@link VirSchema} instances in the {@link org.apache.syncope.core.persistence.api.entity.AnyTypeClass}
     * associated to the given any, for the given membership.
     *
     * @param any any object
     * @param membership membership
     * @return virtual attribute values, either for local cache or external resources
     */
    Map<VirSchema, List<String>> getValues(Any any, Membership<?> membership);

}
