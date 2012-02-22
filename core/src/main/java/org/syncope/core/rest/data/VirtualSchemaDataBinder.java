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
package org.syncope.core.rest.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.VirtualSchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.persistence.beans.AbstractVirSchema;
import org.syncope.core.persistence.beans.AbstractSchema;

@Component
public class VirtualSchemaDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            VirtualSchemaDataBinder.class);

    private static final String[] ignoreVirtualSchemaProperties = {
        "schemas", "virtualAttributes"};

    private <T extends AbstractSchema> AbstractVirSchema populate(
            AbstractVirSchema virtualSchema,
            final VirtualSchemaTO virtualSchemaTO,
            final Class<T> reference,
            final SyncopeClientCompositeErrorException scce)
            throws SyncopeClientCompositeErrorException {

        BeanUtils.copyProperties(virtualSchemaTO, virtualSchema,
                ignoreVirtualSchemaProperties);

        return virtualSchema;
    }

    public <T extends AbstractSchema> AbstractVirSchema create(
            final VirtualSchemaTO virtualSchemaTO,
            AbstractVirSchema virtualSchema,
            final Class<T> reference) {

        return populate(virtualSchema, virtualSchemaTO, reference,
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST));
    }

    public <K extends AbstractSchema> AbstractVirSchema update(
            final VirtualSchemaTO virtualSchemaTO,
            AbstractVirSchema virtualSchema,
            final Class<K> reference) {

        return populate(virtualSchema, virtualSchemaTO, reference,
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST));
    }

    public <T extends AbstractVirSchema> VirtualSchemaTO getVirtualSchemaTO(
            final T virtualSchema) {

        VirtualSchemaTO virtualSchemaTO = new VirtualSchemaTO();
        BeanUtils.copyProperties(virtualSchema, virtualSchemaTO,
                ignoreVirtualSchemaProperties);

        return virtualSchemaTO;
    }
}
