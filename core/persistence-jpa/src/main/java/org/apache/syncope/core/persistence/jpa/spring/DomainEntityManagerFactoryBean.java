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
package org.apache.syncope.core.persistence.jpa.spring;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.syncope.core.persistence.jpa.openjpa.ConnectorManagerRemoteCommitListener;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 * Extension of {@link LocalContainerEntityManagerFactoryBean} relying on {@link CommonEntityManagerFactoryConf} for
 * common configuration options.
 */
public class DomainEntityManagerFactoryBean extends LocalContainerEntityManagerFactoryBean {

    private static final long serialVersionUID = 49152547930966545L;

    protected ConnectorManagerRemoteCommitListener connectorManagerRemoteCommitListener;

    public void setCommonEntityManagerFactoryConf(final CommonEntityManagerFactoryConf commonEMFConf) {
        super.setJpaPropertyMap(commonEMFConf.getJpaPropertyMap());

        if (commonEMFConf.getPackagesToScan() != null) {
            super.setPackagesToScan(commonEMFConf.getPackagesToScan());
        }

        super.setValidationMode(commonEMFConf.getValidationMode());

        if (commonEMFConf.getPersistenceUnitPostProcessors() != null) {
            super.setPersistenceUnitPostProcessors(commonEMFConf.getPersistenceUnitPostProcessors());
        }
    }

    public void setConnectorManagerRemoteCommitListener(
            final ConnectorManagerRemoteCommitListener connectorManagerRemoteCommitListener) {

        this.connectorManagerRemoteCommitListener = connectorManagerRemoteCommitListener;
    }

    @Override
    protected void postProcessEntityManagerFactory(final EntityManagerFactory emf, final PersistenceUnitInfo pui) {
        super.postProcessEntityManagerFactory(emf, pui);

        OpenJPAEntityManagerFactorySPI emfspi = emf.unwrap(OpenJPAEntityManagerFactorySPI.class);
        emfspi.getConfiguration().getRemoteCommitEventManager().addListener(connectorManagerRemoteCommitListener);
    }
}
