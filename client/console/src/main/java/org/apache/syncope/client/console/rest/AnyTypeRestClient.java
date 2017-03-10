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

import java.util.Collections;
import java.util.List;
import org.apache.syncope.client.console.commons.AnyTypeComparator;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.rest.api.service.AnyTypeService;

public class AnyTypeRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2211371717449597247L;

    public AnyTypeTO read(final String key) {
        AnyTypeTO type = null;

        try {
            type = getService(AnyTypeService.class).read(key);
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any types", e);
        }

        return type;
    }

    public List<AnyTypeTO> list() {
        List<AnyTypeTO> types = Collections.emptyList();

        try {
            types = getService(AnyTypeService.class).list();
            Collections.sort(types, new AnyTypeComparator());
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any types", e);
        }

        return types;
    }

    public void create(final AnyTypeTO anyTypeTO) {
        getService(AnyTypeService.class).create(anyTypeTO);
    }

    public void update(final AnyTypeTO anyTypeTO) {
        getService(AnyTypeService.class).update(anyTypeTO);
    }

    public void delete(final String key) {
        getService(AnyTypeService.class).delete(key);
    }
}
