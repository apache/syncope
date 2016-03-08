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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import org.apache.syncope.common.lib.to.CamelMetrics;
import org.apache.syncope.common.lib.to.CamelRouteTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.service.CamelRouteService;
import org.apache.syncope.core.logic.CamelRouteLogic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CamelRouteServiceImpl extends AbstractServiceImpl implements CamelRouteService {

    @Autowired
    private CamelRouteLogic logic;

    @Override
    public List<CamelRouteTO> list(final AnyTypeKind anyTypeKind) {
        return logic.list(anyTypeKind);
    }

    @Override
    public CamelRouteTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public void update(final CamelRouteTO route) {
        logic.update(route);
    }

    @Override
    public void restartContext() {
        logic.restartContext();
    }

    @Override
    public CamelMetrics metrics() {
        return logic.metrics();
    }

}
