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
package org.apache.syncope.client.console.rest;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.RelationshipTypeTO;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;

public class RelationshipTypeRestClient extends BaseRestClient {

    private static final long serialVersionUID = -5400007385180229980L;

    public void create(final RelationshipTypeTO relationshipType) {
        getService(RelationshipTypeService.class).create(relationshipType);
    }

    public void update(final RelationshipTypeTO relationshipType) {
        getService(RelationshipTypeService.class).update(relationshipType);
    }

    public void delete(final String key) {
        getService(RelationshipTypeService.class).delete(key);
    }

    public RelationshipTypeTO read(final String key) {
        return getService(RelationshipTypeService.class).read(key);
    }

    public List<RelationshipTypeTO> list() {
        List<RelationshipTypeTO> types = List.of();

        try {
            types = getService(RelationshipTypeService.class).list();
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any type classes", e);
        }

        return types;
    }
}
