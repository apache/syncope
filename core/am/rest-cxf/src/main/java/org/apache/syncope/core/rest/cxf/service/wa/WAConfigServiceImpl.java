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
package org.apache.syncope.core.rest.cxf.service.wa;

import java.util.List;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.core.logic.wa.WAConfigLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;

public class WAConfigServiceImpl extends AbstractService implements WAConfigService {

    protected final WAConfigLogic logic;

    public WAConfigServiceImpl(final WAConfigLogic logic) {
        this.logic = logic;
    }

    @Override
    public List<Attr> list() {
        return logic.list();
    }

    @Override
    public void delete(final String schema) {
        logic.delete(schema);
    }

    @Override
    public Attr get(final String schema) {
        return logic.get(schema);
    }

    @Override
    public void set(final Attr value) {
        logic.set(value);
    }

    @Override
    public void pushToWA(final PushSubject subject, final List<String> services) {
        logic.pushToWA(subject, services);
    }
}
