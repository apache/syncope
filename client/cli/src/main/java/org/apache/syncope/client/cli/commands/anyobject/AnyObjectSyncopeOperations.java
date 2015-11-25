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
package org.apache.syncope.client.cli.commands.anyobject;

import java.util.List;
import java.util.Set;
import org.apache.syncope.client.cli.SyncopeServices;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;

public class AnyObjectSyncopeOperations {

    private final AnyObjectService anyObjectService = SyncopeServices.get(AnyObjectService.class);

    public List<AnyObjectTO> list(final String type) {
        return anyObjectService.list(type, new AnyListQuery()).getResult();
    }

    public AnyObjectTO read(final String anyId) {
        return anyObjectService.read(Long.valueOf(anyId));
    }

    public Set<AttrTO> readAttributes(final String anyId, final String schemaType) {
        return anyObjectService.read(Long.valueOf(anyId), SchemaType.valueOf(schemaType));
    }

    public AttrTO readAttribute(final String anyId, final String schemaType, final String schema) {
        return anyObjectService.read(Long.valueOf(anyId), SchemaType.valueOf(schemaType), schema);
    }

    public void delete(final String anyId) {
        anyObjectService.delete(Long.valueOf(anyId));
    }
}
