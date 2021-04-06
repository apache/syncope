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

package org.apache.syncope.fit.core.wa;

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.wa.ImpersonationAccount;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ImpersonationITCase extends AbstractITCase {
    @Test
    public void createAndFind() {
        ImpersonationAccount account = new ImpersonationAccount.Builder()
            .owner(getUUIDString())
            .key(getUUIDString())
            .build();

        Response response = impersonationService.create(account);
        assertNotNull(response);

        assertFalse(impersonationService.findByOwner(account.getOwner()).isEmpty());
        account = impersonationService.find(account.getOwner(), account.getKey());
        assertNotNull(account);

        impersonationService.update(account);
        account = impersonationService.find(account.getOwner(), account.getKey());
        assertNotNull(account);

        response = impersonationService.delete(account.getOwner(), account.getKey());
        assertNotNull(response);
        
        try {
            impersonationService.find(account.getOwner(), account.getKey());
            fail("Should not happen");
        } catch (final SyncopeClientException e) {
           assertEquals(ClientExceptionType.DelegatedAdministration, e.getType());
        }
    }
}
