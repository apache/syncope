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
package org.apache.syncope.core.workflow.java;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.Date;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.ConnectorRegistry;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration(proxyBeanMethods = false)
@ImportResource({
    "classpath:persistenceTest.xml",
    "classpath:workflowContext.xml",
    "classpath:workflowTest.xml" })
public class WorkflowTestContext {

    @Bean
    public UserDataBinder userDataBinder(final RealmDAO realmDAO) {
        UserDataBinder dataBinder = mock(UserDataBinder.class);

        doAnswer(ic -> {
            User user = ic.getArgument(0);
            UserTO userTO = ic.getArgument(1);

            user.setUsername(userTO.getUsername());
            user.setRealm(realmDAO.findByFullPath(userTO.getRealm()));
            user.setCreator("admin");
            user.setCreationDate(new Date());
            user.setCipherAlgorithm(CipherAlgorithm.SHA256);
            user.setPassword(userTO.getPassword());

            return null;
        }).when(dataBinder).create(any(User.class), any(UserTO.class), anyBoolean());

        return dataBinder;
    }

    @Bean
    public GroupDataBinder groupDataBinder() {
        GroupDataBinder dataBinder = mock(GroupDataBinder.class);
        return dataBinder;
    }

    @Bean
    public AnyObjectDataBinder anyObjectDataBinder() {
        AnyObjectDataBinder dataBinder = mock(AnyObjectDataBinder.class);
        return dataBinder;
    }

    @Bean
    public ConnectorRegistry connectorRegistry() {
        return new DummyConnectorRegistry();
    }

    @Bean
    public TestInitializer testInitializer() {
        return new TestInitializer();
    }

    @Bean
    public ImplementationLookup implementationLookup() {
        return new DummyImplementationLookup();
    }
}
