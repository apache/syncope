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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RemediationTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

public class RemediationLogicTest {

    private UserLogicOp userLogic;

    private GroupLogicOp groupLogic;

    private AnyObjectLogicOp anyObjectLogic;

    private RemediationDataBinder binder;

    private RemediationDAO remediationDAO;

    private RemediationLogic logic;

    @BeforeEach
    void setUp() {
        userLogic = mock(UserLogicOp.class);
        groupLogic = mock(GroupLogicOp.class);
        anyObjectLogic = mock(AnyObjectLogicOp.class);
        binder = mock(RemediationDataBinder.class);
        remediationDAO = mock(RemediationDAO.class);

        logic = new RemediationLogic(
                userLogic, groupLogic, anyObjectLogic, binder, remediationDAO);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void readMissing() {
        when(remediationDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void deleteMissing() {
        when(remediationDAO.findById("missing")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void remedyUser() {
        LogicTestAuth.authenticate("admin", "Master",
                Map.of(IdMEntitlement.REMEDIATION_REMEDY, Set.of(SyncopeConstants.ROOT_REALM)));

        Remediation remediation = mock(Remediation.class);
        RemediationTO remediationTO = new RemediationTO();
        remediationTO.setKey("key");
        remediationTO.setAnyType(AnyTypeKind.USER.name());

        doReturn(Optional.of(remediation)).when(remediationDAO).findById("key");
        when(binder.getRemediationTO(remediation)).thenReturn(remediationTO);

        AnyCR anyCR = new UserCR.Builder(SyncopeConstants.ROOT_REALM, "user").build();
        ProvisioningResult<UserTO> result = mock(ProvisioningResult.class);

        when(userLogic.create((UserCR) anyCR, false)).thenReturn(result);

        assertSame(result, logic.remedy("key", anyCR, false));

        verify(remediationDAO).deleteById("key");
    }

    @Test
    void list() {
        when(remediationDAO.count(any(), any())).thenReturn(0L);
        when(remediationDAO.findAll(any(), any(), any(Pageable.class))).thenReturn(List.of());

        var result = logic.list(null, null, Pageable.unpaged());

        assertEquals(0L, result.getTotalElements());
    }
}
