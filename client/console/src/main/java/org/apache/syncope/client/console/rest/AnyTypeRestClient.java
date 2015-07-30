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
import java.util.List;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.common.lib.to.AnyTypeTO;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.springframework.stereotype.Component;

/**
 * Console client for invoking Rest any type class services.
 */
@Component
public class AnyTypeRestClient extends BaseRestClient {

    private static final long serialVersionUID = -8874495991295283249L;

    public List<AnyTypeTO> list() {
        return getService(AnyTypeService.class).list();
    }

    public List<AnyTypeClassTO> getAnyTypeClass(final List<String> anyTypeClassNames) {
        List<AnyTypeClassTO> anyTypeClassTOs = new ArrayList<AnyTypeClassTO>();
        for (String anyTypeClass : anyTypeClassNames) {
            anyTypeClassTOs.add(getService(AnyTypeClassService.class).read(anyTypeClass));
        }
        return anyTypeClassTOs;
    }

}
