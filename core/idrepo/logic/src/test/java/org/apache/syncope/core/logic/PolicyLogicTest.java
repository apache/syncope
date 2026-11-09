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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccountPolicyTO;
import org.apache.syncope.common.lib.policy.PasswordPolicyTO;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PolicyLogicTest {

    private PolicyDAO policyDAO;

    private PolicyDataBinder binder;

    private PolicyUtilsFactory policyUtilsFactory;

    private PolicyLogic logic;

    @BeforeEach
    void setUp() {
        policyDAO = mock(PolicyDAO.class);
        binder = mock(PolicyDataBinder.class);
        policyUtilsFactory = new PolicyUtilsFactory();
        logic = new PolicyLogic(policyDAO, binder, policyUtilsFactory);
    }

    @Test
    void createWithWrongType() {
        assertThrows(SyncopeClientException.class,
                () -> logic.create(PolicyType.ACCOUNT, new PasswordPolicyTO()));
    }

    @Test
    void create() {
        AccountPolicyTO to = new AccountPolicyTO();
        to.setKey("key");
        AccountPolicy policy = mock(AccountPolicy.class);

        when(binder.create(to)).thenReturn(policy);
        when(policyDAO.save(policy)).thenReturn(policy);
        when(binder.getPolicyTO(policy)).thenReturn(to);

        assertSame(to, logic.create(PolicyType.ACCOUNT, to));
    }

    @Test
    void updateMissing() {
        AccountPolicyTO to = new AccountPolicyTO();
        to.setKey("missing");

        doReturn(Optional.empty()).when(policyDAO).findById("missing", AccountPolicy.class);

        assertThrows(NotFoundException.class, () -> logic.update(PolicyType.ACCOUNT, to));
    }

    @Test
    void updateWithWrongType() {
        PasswordPolicyTO to = new PasswordPolicyTO();
        to.setKey("key");
        AccountPolicy policy = mock(AccountPolicy.class);
        doReturn(Optional.of(policy)).when(policyDAO).findById("key", AccountPolicy.class);

        assertThrows(SyncopeClientException.class, () -> logic.update(PolicyType.ACCOUNT, to));
    }

    @Test
    void update() {
        AccountPolicyTO to = new AccountPolicyTO();
        to.setKey("key");
        AccountPolicy policy = mock(AccountPolicy.class);
        AccountPolicy updated = mock(AccountPolicy.class);

        doReturn(Optional.of(policy)).when(policyDAO).findById("key", AccountPolicy.class);
        when(binder.update(policy, to)).thenReturn(updated);
        when(policyDAO.save(updated)).thenReturn(updated);
        when(binder.getPolicyTO(updated)).thenReturn(to);

        assertSame(to, logic.update(PolicyType.ACCOUNT, to));
    }

    @Test
    void list() {
        AccountPolicy policy = mock(AccountPolicy.class);
        AccountPolicyTO to = new AccountPolicyTO();
        to.setKey("key");

        doReturn(List.of(policy)).when(policyDAO).findAll(AccountPolicy.class);
        when(binder.getPolicyTO(policy)).thenReturn(to);

        assertEquals(List.of(to), logic.list(PolicyType.ACCOUNT));
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(policyDAO).findById("missing", AccountPolicy.class);

        assertThrows(NotFoundException.class, () -> logic.read(PolicyType.ACCOUNT, "missing"));
    }

    @Test
    void read() {
        AccountPolicy policy = mock(AccountPolicy.class);
        AccountPolicyTO to = new AccountPolicyTO();
        to.setKey("key");

        doReturn(Optional.of(policy)).when(policyDAO).findById("key", AccountPolicy.class);
        when(binder.getPolicyTO(policy)).thenReturn(to);

        assertSame(to, logic.read(PolicyType.ACCOUNT, "key"));
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(policyDAO).findById("missing", AccountPolicy.class);

        assertThrows(NotFoundException.class, () -> logic.delete(PolicyType.ACCOUNT, "missing"));
    }

    @Test
    void delete() {
        AccountPolicy policy = mock(AccountPolicy.class);
        AccountPolicyTO to = new AccountPolicyTO();
        to.setKey("key");

        doReturn(Optional.of(policy)).when(policyDAO).findById("key", AccountPolicy.class);
        when(binder.getPolicyTO(policy)).thenReturn(to);

        assertSame(to, logic.delete(PolicyType.ACCOUNT, "key"));
        verify(policyDAO).delete(policy);
    }

    @Test
    void resolveReference() throws Exception {
        PolicyTO to = new AccountPolicyTO();
        to.setKey("key");
        AccountPolicy policy = mock(AccountPolicy.class);

        doReturn(Optional.of(policy)).when(policyDAO).findById("key", AccountPolicy.class);
        when(binder.getPolicyTO(policy)).thenReturn(to);

        assertSame(to, logic.resolveBeanReference(
                PolicyLogic.class.getMethod("create", PolicyType.class, PolicyTO.class), PolicyType.ACCOUNT, "key"));
    }
}
