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
package org.apache.syncope.client.enduser.rest;

import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.rest.api.service.AnyService;

public abstract class AbstractAnyRestClient<TO extends AnyTO> extends BaseRestClient {

    private static final long serialVersionUID = 1962529678091410544L;

    protected abstract Class<? extends AnyService<TO>> getAnyServiceClass();

    public abstract long count(String realm, String fiql, String type);

    public TO read(final String key) {
        return getService(getAnyServiceClass()).read(key);
    }
}
