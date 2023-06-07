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

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.rest.api.service.ClientAppService;

public class ClientAppRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    protected static final Comparator<ClientAppTO> COMPARATOR = Comparator.comparing(ClientAppTO::getName);

    public <T extends ClientAppTO> T read(final ClientAppType type, final String key) {
        T policy = null;
        try {
            policy = getService(ClientAppService.class).read(type, key);
        } catch (Exception e) {
            LOG.warn("No client app found for type {} and key {}", type, key, e);
        }
        return policy;
    }

    @SuppressWarnings("unchecked")
    public <T extends ClientAppTO> List<T> list(final ClientAppType type) {
        try {
            return getService(ClientAppService.class).<T>list(type).stream().
                    sorted(COMPARATOR).
                    collect(Collectors.toList());
        } catch (Exception ignore) {
            LOG.debug("No client app found", ignore);
            return List.of();
        }
    }

    public <T extends ClientAppTO> void create(final ClientAppType type, final T policy) {
        getService(ClientAppService.class).create(type, policy);
    }

    public <T extends ClientAppTO> void update(final ClientAppType type, final T policy) {
        getService(ClientAppService.class).update(type, policy);
    }

    public void delete(final ClientAppType type, final String key) {
        getService(ClientAppService.class).delete(type, key);
    }
}
