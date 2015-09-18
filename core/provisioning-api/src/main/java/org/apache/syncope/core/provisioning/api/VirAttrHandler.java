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

import java.util.Collection;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IntMappingType;
import org.apache.syncope.common.lib.types.PropagationByResource;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;

public interface VirAttrHandler {

    /**
     * Create and add virtual attributes to any.
     *
     * @param any any
     * @param vAttrs virtual attributes to be added.
     */
    void createVirtual(Any any, Collection<AttrTO> vAttrs);

    /**
     * Update virtual attributes to any.
     *
     * @param any
     * @param vAttrs virtual attributes to be updated.
     * @return operations to be performed on external resources for virtual attributes changes
     */
    PropagationByResource updateVirtual(Any any, Collection<AttrPatch> vAttrs);

    /**
     * Update virtual attributes to any identified by the given {@code key}.
     *
     * @param key any key
     * @param anyTypeKind type kind
     * @param vAttrs virtual attributes to be updated.
     * @return operations to be performed on external resources for virtual attributes changes
     */
    PropagationByResource updateVirtual(Long key, AnyTypeKind anyTypeKind, Collection<AttrPatch> vAttrs);

    VirSchema getVirSchema(String virSchemaName);

    /**
     * Query connected external resources for values to populated virtual attributes associated with the given owner.
     *
     * @param any any object
     */
    void retrieveVirAttrValues(Any<?, ?, ?> any);

    void updateOnResourcesIfMappingMatches(
            Any<?, ?, ?> any, String schemaKey, Iterable<? extends ExternalResource> resources,
            IntMappingType mappingType, PropagationByResource propByRes);

}
