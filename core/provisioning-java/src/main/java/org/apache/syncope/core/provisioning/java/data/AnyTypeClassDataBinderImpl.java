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
package org.apache.syncope.core.provisioning.java.data;

import java.util.Collections;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnyTypeClassDataBinderImpl implements AnyTypeClassDataBinder {

    private static final Logger LOG = LoggerFactory.getLogger(AnyTypeClassDataBinder.class);

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private EntityFactory entityFactory;

    @Override
    public AnyTypeClass create(final AnyTypeClassTO anyTypeClassTO) {
        AnyTypeClass anyTypeClass = entityFactory.newEntity(AnyTypeClass.class);
        update(anyTypeClass, anyTypeClassTO);
        return anyTypeClass;
    }

    @Override
    public void update(final AnyTypeClass anyTypeClass, final AnyTypeClassTO anyTypeClassTO) {
        if (anyTypeClass.getKey() == null) {
            anyTypeClass.setKey(anyTypeClassTO.getKey());
        }

        for (PlainSchema schema : plainSchemaDAO.findByAnyTypeClasses(Collections.singletonList(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }

        anyTypeClass.getPlainSchemas().clear();
        for (String schemaName : anyTypeClassTO.getPlainSchemas()) {
            PlainSchema schema = plainSchemaDAO.find(schemaName);
            if (schema == null || schema.getAnyTypeClass() != null) {
                LOG.debug("Invalid or already in use" + PlainSchema.class.getSimpleName()
                        + "{}, ignoring...", schemaName);
            } else {
                anyTypeClass.add(schema);
            }
        }

        for (DerSchema schema : derSchemaDAO.findByAnyTypeClasses(Collections.singletonList(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }

        anyTypeClass.getDerSchemas().clear();
        for (String schemaName : anyTypeClassTO.getDerSchemas()) {
            DerSchema schema = derSchemaDAO.find(schemaName);
            if (schema == null || schema.getAnyTypeClass() != null) {
                LOG.debug("Invalid or already in use" + DerSchema.class.getSimpleName()
                        + "{}, ignoring...", schemaName);
            } else {
                anyTypeClass.add(schema);
            }
        }

        for (VirSchema schema : virSchemaDAO.findByAnyTypeClasses(Collections.singletonList(anyTypeClass))) {
            schema.setAnyTypeClass(null);
        }

        anyTypeClass.getVirSchemas().clear();
        for (String schemaName : anyTypeClassTO.getVirSchemas()) {
            VirSchema schema = virSchemaDAO.find(schemaName);
            if (schema == null || schema.getAnyTypeClass() != null) {
                LOG.debug("Invalid or already in use" + VirSchema.class.getSimpleName()
                        + "{}, ignoring...", schemaName);
            } else {
                anyTypeClass.add(schema);
            }
        }
    }

    @Override
    public AnyTypeClassTO getAnyTypeClassTO(final AnyTypeClass anyTypeClass) {
        AnyTypeClassTO anyTypeClassTO = new AnyTypeClassTO();

        anyTypeClassTO.setKey(anyTypeClass.getKey());

        for (AnyType anyType : anyTypeDAO.findByTypeClass(anyTypeClass)) {
            anyTypeClassTO.getInUseByTypes().add(anyType.getKey());
        }

        for (PlainSchema schema : anyTypeClass.getPlainSchemas()) {
            anyTypeClassTO.getPlainSchemas().add(schema.getKey());
        }
        for (DerSchema schema : anyTypeClass.getDerSchemas()) {
            anyTypeClassTO.getDerSchemas().add(schema.getKey());
        }
        for (VirSchema schema : anyTypeClass.getVirSchemas()) {
            anyTypeClassTO.getVirSchemas().add(schema.getKey());
        }

        return anyTypeClassTO;
    }

}
