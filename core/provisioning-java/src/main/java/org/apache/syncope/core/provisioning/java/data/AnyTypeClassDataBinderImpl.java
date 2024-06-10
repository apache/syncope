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

import java.util.List;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnyTypeClassDataBinderImpl implements AnyTypeClassDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyTypeClassDataBinder.class);

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final AnyTypeDAO anyTypeDAO;

    protected final EntityFactory entityFactory;

    public AnyTypeClassDataBinderImpl(
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeDAO anyTypeDAO,
            final EntityFactory entityFactory) {

        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.anyTypeDAO = anyTypeDAO;
        this.entityFactory = entityFactory;
    }

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

        plainSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass)).forEach(schema -> {
            schema.setAnyTypeClass(null);
            plainSchemaDAO.save(schema);
        });

        anyTypeClass.getPlainSchemas().clear();
        anyTypeClassTO.getPlainSchemas().forEach(key -> {
            PlainSchema schema = plainSchemaDAO.findById(key).orElse(null);
            if (schema == null || schema.getAnyTypeClass() != null) {
                LOG.debug("Invalid or already in use: {} {}, ignoring...", PlainSchema.class.getSimpleName(), key);
            } else {
                anyTypeClass.add(schema);
                schema.setAnyTypeClass(anyTypeClass);
                plainSchemaDAO.save(schema);
            }
        });

        derSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass)).forEach(schema -> {
            schema.setAnyTypeClass(null);
            derSchemaDAO.save(schema);
        });

        anyTypeClass.getDerSchemas().clear();
        anyTypeClassTO.getDerSchemas().forEach(key -> {
            DerSchema schema = derSchemaDAO.findById(key).orElse(null);
            if (schema == null || schema.getAnyTypeClass() != null) {
                LOG.debug("Invalid or already in use: {} {}, ignoring...", DerSchema.class.getSimpleName(), key);
            } else {
                anyTypeClass.add(schema);
                schema.setAnyTypeClass(anyTypeClass);
                derSchemaDAO.save(schema);
            }
        });

        virSchemaDAO.findByAnyTypeClasses(List.of(anyTypeClass)).forEach(schema -> {
            schema.setAnyTypeClass(null);
            virSchemaDAO.save(schema);
        });

        anyTypeClass.getVirSchemas().clear();
        anyTypeClassTO.getVirSchemas().forEach(key -> {
            VirSchema schema = virSchemaDAO.findById(key).orElse(null);
            if (schema == null || schema.getAnyTypeClass() != null) {
                LOG.debug("Invalid or already in use: {} {}, ignoring...", VirSchema.class.getSimpleName(), key);
            } else {
                anyTypeClass.add(schema);
                schema.setAnyTypeClass(anyTypeClass);
                virSchemaDAO.save(schema);
            }
        });
    }

    @Override
    public AnyTypeClassTO getAnyTypeClassTO(final AnyTypeClass anyTypeClass) {
        AnyTypeClassTO anyTypeClassTO = new AnyTypeClassTO();

        anyTypeClassTO.setKey(anyTypeClass.getKey());

        anyTypeClassTO.getInUseByTypes().addAll(
                anyTypeDAO.findByClassesContaining(anyTypeClass).stream().
                        map(AnyType::getKey).toList());

        anyTypeClassTO.getPlainSchemas().addAll(
                anyTypeClass.getPlainSchemas().stream().map(PlainSchema::getKey).toList());
        anyTypeClassTO.getDerSchemas().addAll(
                anyTypeClass.getDerSchemas().stream().map(DerSchema::getKey).toList());
        anyTypeClassTO.getVirSchemas().addAll(
                anyTypeClass.getVirSchemas().stream().map(VirSchema::getKey).toList());

        return anyTypeClassTO;
    }
}
