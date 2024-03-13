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
package org.apache.syncope.common.keymaster.client.self;

import java.util.List;
import java.util.Map;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.keymaster.rest.api.service.DomainService;
import org.apache.syncope.common.lib.types.CipherAlgorithm;

public class SelfKeymasterDomainOps extends SelfKeymasterOps implements DomainOps {

    public SelfKeymasterDomainOps(final JAXRSClientFactoryBean clientFactory) {
        super(clientFactory);
    }

    @Override
    public List<Domain> list() {
        return client(DomainService.class, Map.of()).list();
    }

    @Override
    public Domain read(final String key) {
        try {
            return client(DomainService.class, Map.of()).read(key);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void create(final Domain domain) {
        try {
            client(DomainService.class, Map.of()).create(domain);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void deployed(final String key) {
        try {
            client(DomainService.class, Map.of()).deployed(key);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void changeAdminPassword(final String key, final String password, final CipherAlgorithm cipherAlgorithm) {
        try {
            client(DomainService.class, Map.of()).changeAdminPassword(key, password, cipherAlgorithm);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void adjustPoolSize(final String key, final int maxPoolSize, final int minIdle) {
        try {
            client(DomainService.class, Map.of()).adjustPoolSize(key, maxPoolSize, minIdle);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public void delete(final String key) {
        try {
            client(DomainService.class, Map.of()).delete(key);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }
}
