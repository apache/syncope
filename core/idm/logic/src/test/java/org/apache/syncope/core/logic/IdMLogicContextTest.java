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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.apache.syncope.core.logic.init.IdMEntitlementLoader;
import org.apache.syncope.core.logic.init.IdMImplementationTypeLoader;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.data.RemediationDataBinder;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.InboundMatcher;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

public class IdMLogicContextTest {

    private final IdMLogicContext context = new IdMLogicContext();

    @Test
    void idmEntitlementLoader() {
        assertInstanceOf(IdMEntitlementLoader.class, context.idmEntitlementLoader());
    }

    @Test
    void idmImplementationTypeLoader() {
        assertInstanceOf(IdMImplementationTypeLoader.class, context.idmImplementationTypeLoader());
    }

    @Test
    void connectorLogic() {
        assertInstanceOf(ConnectorLogic.class, context.connectorLogic(
                mock(ConnIdBundleManager.class),
                mock(ExternalResourceDAO.class),
                mock(ConnInstanceDAO.class),
                mock(ConnInstanceDataBinder.class),
                mock(ConnectorManager.class)));
    }

    @Test
    void reconciliationLogic() {
        assertInstanceOf(ReconciliationLogic.class, context.reconciliationLogic(
                mock(AnyUtilsFactory.class),
                mock(AnyTypeDAO.class),
                mock(ExternalResourceDAO.class),
                mock(RealmSearchDAO.class),
                mock(PlainSchemaDAO.class),
                mock(DerSchemaDAO.class),
                mock(AnySearchDAO.class),
                mock(ConnectorManager.class),
                mock(InboundMatcher.class),
                mock(OutboundMatcher.class),
                mock(MappingManager.class),
                mock(ConfigurableApplicationContext.class)));
    }

    @Test
    void remediationLogic() {
        assertInstanceOf(RemediationLogic.class, context.remediationLogic(
                mock(UserLogicOp.class),
                mock(GroupLogicOp.class),
                mock(AnyObjectLogicOp.class),
                mock(RemediationDataBinder.class),
                mock(RemediationDAO.class)));
    }

    @Test
    void resourceLogic() {
        assertInstanceOf(ResourceLogic.class, context.resourceLogic(
                mock(ResourceDataBinder.class),
                mock(AnyUtilsFactory.class),
                mock(AnyTypeDAO.class),
                mock(ExternalResourceDAO.class),
                mock(ConnInstanceDAO.class),
                mock(ConnectorManager.class),
                mock(OutboundMatcher.class),
                mock(MappingManager.class)));
    }
}
