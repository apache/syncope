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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;

public class AnyTypeClassRestClient extends BaseRestClient {

    private static final long serialVersionUID = -2211371717449597247L;

    public void create(final AnyTypeClassTO anyTypeClass) {
        getService(AnyTypeClassService.class).create(anyTypeClass);
    }

    public void update(final AnyTypeClassTO anyTypeClass) {
        getService(AnyTypeClassService.class).update(anyTypeClass);
    }

    public void delete(final String key) {
        getService(AnyTypeClassService.class).delete(key);
    }

    public AnyTypeClassTO read(final String key) {
        return getService(AnyTypeClassService.class).read(key);
    }

    public List<AnyTypeClassTO> list() {
        List<AnyTypeClassTO> types = List.of();

        try {
            types = getService(AnyTypeClassService.class).list();
        } catch (SyncopeClientException e) {
            LOG.error("While reading all any type classes", e);
        }

        return types;
    }

    public List<AnyTypeClassTO> list(final Collection<String> anyTypeClassNames) {
        List<AnyTypeClassTO> anyTypeClassTOs = new ArrayList<>();
        for (String anyTypeClass : anyTypeClassNames) {
            anyTypeClassTOs.add(read(anyTypeClass));
        }
        return anyTypeClassTOs;
    }
}
