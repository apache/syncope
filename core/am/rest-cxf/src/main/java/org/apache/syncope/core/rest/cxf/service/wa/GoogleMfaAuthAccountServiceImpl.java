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
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.wa.GoogleMfaAuthAccount;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthAccountService;
import org.apache.syncope.core.logic.wa.GoogleMfaAuthAccountLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractService;

public class GoogleMfaAuthAccountServiceImpl extends AbstractService implements GoogleMfaAuthAccountService {

    protected final GoogleMfaAuthAccountLogic logic;

    public GoogleMfaAuthAccountServiceImpl(final GoogleMfaAuthAccountLogic logic) {
        this.logic = logic;
    }

    @Override
    public void delete(final String owner) {
        logic.delete(owner);
    }

    @Override
    public void delete(final long id) {
        logic.delete(id);
    }

    @Override
    public void deleteAll() {
        logic.deleteAll();
    }

    @Override
    public void create(final String owner, final GoogleMfaAuthAccount acct) {
        logic.create(owner, acct);
    }

    @Override
    public void update(final String owner, final GoogleMfaAuthAccount acct) {
        logic.update(owner, acct);
    }

    private PagedResult<GoogleMfaAuthAccount> build(final List<GoogleMfaAuthAccount> read) {
        PagedResult<GoogleMfaAuthAccount> result = new PagedResult<>();
        result.setPage(1);
        result.setSize(read.size());
        result.setTotalCount(read.size());
        result.getResult().addAll(read);
        return result;
    }

    @Override
    public PagedResult<GoogleMfaAuthAccount> read(final String owner) {
        return build(logic.read(owner));
    }

    @Override
    public GoogleMfaAuthAccount read(final long id) {
        return logic.read(id);
    }

    @Override
    public PagedResult<GoogleMfaAuthAccount> list() {
        return build(logic.list());
    }
}
