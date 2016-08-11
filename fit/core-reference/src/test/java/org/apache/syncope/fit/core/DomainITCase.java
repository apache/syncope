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
package org.apache.syncope.fit.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.AccessControlException;
import java.util.List;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.DomainTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.Test;

public class DomainITCase extends AbstractITCase {

    @Test
    public void list() {
        List<DomainTO> domains = domainService.list();
        assertNotNull(domains);
        assertFalse(domains.isEmpty());
        for (DomainTO domain : domains) {
            assertNotNull(domain);
        }
    }

    @Test
    public void create() {
        DomainTO domain = new DomainTO();
        domain.setKey("last");
        domain.setAdminCipherAlgorithm(CipherAlgorithm.SSHA512);
        domain.setAdminPwd("password");

        try {
            domainService.create(domain);
            fail();
        } catch (SyncopeClientException e) {
            assertEquals(ClientExceptionType.NotFound, e.getType());
        }
    }

    private void restoreTwo() {
        DomainTO two = new DomainTO();
        two.setKey("Two");
        two.setAdminCipherAlgorithm(CipherAlgorithm.SHA);
        two.setAdminPwd("password2");
        domainService.create(two);
    }

    @Test
    public void update() {
        DomainTO two = domainService.read("Two");
        assertNotNull(two);

        try {
            // 1. change admin pwd for domain Two
            two.setAdminCipherAlgorithm(CipherAlgorithm.AES);
            two.setAdminPwd("password3");
            domainService.update(two);

            // 2. attempt to access with old pwd -> fail
            try {
                new SyncopeClientFactoryBean().
                        setAddress(ADDRESS).setDomain("Two").setContentType(clientFactory.getContentType()).
                        create(ADMIN_UNAME, "password2").self();
            } catch (AccessControlException e) {
                assertNotNull(e);
            }

            // 3. access with new pwd -> succeed
            new SyncopeClientFactoryBean().
                    setAddress(ADDRESS).setDomain("Two").setContentType(clientFactory.getContentType()).
                    create(ADMIN_UNAME, "password3").self();
        } finally {
            restoreTwo();
        }
    }

    @Test
    public void delete() {
        DomainTO two = domainService.read("Two");
        assertNotNull(two);

        try {
            domainService.delete(two.getKey());

            try {
                domainService.read(two.getKey());
                fail();
            } catch (SyncopeClientException e) {
                assertEquals(ClientExceptionType.NotFound, e.getType());
            }
        } finally {
            restoreTwo();
        }
    }
}
