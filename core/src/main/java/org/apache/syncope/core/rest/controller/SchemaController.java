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
package org.apache.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityExistsException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.to.DerSchemaTO;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.to.VirSchemaTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.persistence.dao.VirSchemaDAO;
import org.apache.syncope.core.rest.data.SchemaDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SchemaController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private SchemaDataBinder binder;

    private boolean doesSchemaExist(final SchemaType schemaType, final String name, final AttributableUtil attrUtil) {
        boolean found;

        switch (schemaType) {
            case VIRTUAL:
                found = virSchemaDAO.find(name, attrUtil.virSchemaClass()) != null;
                break;

            case DERIVED:
                found = derSchemaDAO.find(name, attrUtil.derSchemaClass()) != null;
                break;

            case NORMAL:
                found = schemaDAO.find(name, attrUtil.schemaClass()) != null;
                break;

            default:
                found = false;
        }

        return found;
    }

    @PreAuthorize("hasRole('SCHEMA_CREATE')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractSchemaTO> T create(final AttributableType attrType, final SchemaType schemaType,
            final T schemaTO) {

        if (StringUtils.isBlank(schemaTO.getName())) {
            SyncopeClientCompositeErrorException sccee =
                    new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);
            SyncopeClientException sce = new SyncopeClientException(SyncopeClientExceptionType.RequiredValuesMissing);
            sce.addElement("Schema name");
            sccee.addException(sce);
            throw sccee;
        }

        final AttributableUtil attrUtil = AttributableUtil.getInstance(attrType);

        if (doesSchemaExist(schemaType, schemaTO.getName(), attrUtil)) {
            throw new EntityExistsException(schemaType + "/" + attrType + "/" + schemaTO.getName());
        }

        T created;
        switch (schemaType) {
            case VIRTUAL:
                AbstractVirSchema virSchema = attrUtil.newVirSchema();
                binder.create((VirSchemaTO) schemaTO, virSchema);
                virSchema = virSchemaDAO.save(virSchema);

                created = (T) binder.getVirSchemaTO(virSchema);
                break;

            case DERIVED:
                AbstractDerSchema derSchema = attrUtil.newDerSchema();
                binder.create((DerSchemaTO) schemaTO, derSchema);
                derSchema = derSchemaDAO.save(derSchema);

                created = (T) binder.getDerSchemaTO(derSchema);
                break;

            case NORMAL:
            default:
                AbstractSchema normalSchema = attrUtil.newSchema();
                binder.create((SchemaTO) schemaTO, normalSchema);
                normalSchema = schemaDAO.save(normalSchema);

                created = (T) binder.getSchemaTO(normalSchema, attrUtil);
        }

        auditManager.audit(AuditElements.Category.schema, AuditElements.SchemaSubCategory.create,
                AuditElements.Result.success,
                "Successfully created schema: " + schemaType + "/" + attrType + "/" + created.getName());

        return created;
    }

    @PreAuthorize("hasRole('SCHEMA_DELETE')")
    public void delete(final AttributableType attrType, final SchemaType schemaType, final String schemaName) {
        final AttributableUtil attrUtil = AttributableUtil.getInstance(attrType);

        if (!doesSchemaExist(schemaType, schemaName, attrUtil)) {
            throw new NotFoundException(schemaType + "/" + attrType + "/" + schemaName);
        }

        switch (schemaType) {
            case VIRTUAL:
                virSchemaDAO.delete(schemaName, attrUtil);
                break;

            case DERIVED:
                derSchemaDAO.delete(schemaName, attrUtil);
                break;

            case NORMAL:
            default:
                schemaDAO.delete(schemaName, attrUtil);
        }

        auditManager.audit(AuditElements.Category.schema, AuditElements.SchemaSubCategory.delete,
                AuditElements.Result.success,
                "Successfully deleted schema: " + schemaType + "/" + attrType + "/" + schemaName);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractSchemaTO> List<T> list(final AttributableType attrType, final SchemaType schemaType) {
        final AttributableUtil attrUtil = AttributableUtil.getInstance(attrType);

        List<T> result;
        switch (schemaType) {
            case VIRTUAL:
                List<AbstractVirSchema> virSchemas = virSchemaDAO.findAll(attrUtil.virSchemaClass());
                result = (List<T>) new ArrayList<VirSchemaTO>(virSchemas.size());
                for (AbstractVirSchema derSchema : virSchemas) {
                    result.add((T) binder.getVirSchemaTO(derSchema));
                }
                break;

            case DERIVED:
                List<AbstractDerSchema> derSchemas = derSchemaDAO.findAll(attrUtil.derSchemaClass());
                result = (List<T>) new ArrayList<DerSchemaTO>(derSchemas.size());
                for (AbstractDerSchema derSchema : derSchemas) {
                    result.add((T) binder.getDerSchemaTO(derSchema));
                }
                break;

            case NORMAL:
            default:
                List<AbstractSchema> schemas = schemaDAO.findAll(attrUtil.schemaClass());
                result = (List<T>) new ArrayList<SchemaTO>(schemas.size());
                for (AbstractSchema schema : schemas) {
                    result.add((T) binder.getSchemaTO(schema, attrUtil));
                }
        }

        auditManager.audit(AuditElements.Category.schema, AuditElements.SchemaSubCategory.list,
                AuditElements.Result.success,
                "Successfully listed schemas: " + schemaType + "/" + attrType + " " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('SCHEMA_READ')")
    @SuppressWarnings("unchecked")
    public <T extends AbstractSchemaTO> T read(final AttributableType attrType, final SchemaType schemaType,
            final String schemaName) {

        final AttributableUtil attrUtil = AttributableUtil.getInstance(attrType);

        T read;
        switch (schemaType) {
            case VIRTUAL:
                AbstractVirSchema virSchema = virSchemaDAO.find(schemaName, attrUtil.virSchemaClass());
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaName + "'");
                }

                read = (T) binder.getVirSchemaTO(virSchema);
                break;

            case DERIVED:
                AbstractDerSchema derSchema = derSchemaDAO.find(schemaName, attrUtil.derSchemaClass());
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaName + "'");
                }

                read = (T) binder.getDerSchemaTO(derSchema);
                break;

            case NORMAL:
            default:
                AbstractSchema schema = schemaDAO.find(schemaName, attrUtil.schemaClass());
                if (schema == null) {
                    throw new NotFoundException("Schema '" + schemaName + "'");
                }

                read = (T) binder.getSchemaTO(schema, attrUtil);
        }

        auditManager.audit(AuditElements.Category.schema, AuditElements.SchemaSubCategory.read,
                AuditElements.Result.success,
                "Successfully read schema: " + schemaType + "/" + attrType + "/" + schemaName);

        return read;
    }

    @PreAuthorize("hasRole('SCHEMA_UPDATE')")
    public <T extends AbstractSchemaTO> void update(final AttributableType attrType, final SchemaType schemaType,
            final String schemaName, final T schemaTO) {

        final AttributableUtil attrUtil = AttributableUtil.getInstance(attrType);

        if (!doesSchemaExist(schemaType, schemaName, attrUtil)) {
            throw new NotFoundException(schemaType + "/" + attrType + "/" + schemaName);
        }

        switch (schemaType) {
            case VIRTUAL:
                AbstractVirSchema virSchema = virSchemaDAO.find(schemaName, attrUtil.virSchemaClass());
                if (virSchema == null) {
                    throw new NotFoundException("Virtual Schema '" + schemaName + "'");
                }

                binder.update((VirSchemaTO) schemaTO, virSchema);
                virSchemaDAO.save(virSchema);
                break;

            case DERIVED:
                AbstractDerSchema derSchema = derSchemaDAO.find(schemaName, attrUtil.derSchemaClass());
                if (derSchema == null) {
                    throw new NotFoundException("Derived schema '" + schemaName + "'");
                }

                binder.update((DerSchemaTO) schemaTO, derSchema);
                derSchemaDAO.save(derSchema);
                break;

            case NORMAL:
            default:
                AbstractSchema schema = schemaDAO.find(schemaName, attrUtil.schemaClass());
                if (schema == null) {
                    throw new NotFoundException("Schema '" + schemaName + "'");
                }

                binder.update((SchemaTO) schemaTO, schema, attrUtil);
                schemaDAO.save(schema);
        }

        auditManager.audit(AuditElements.Category.schema, AuditElements.SchemaSubCategory.update,
                AuditElements.Result.success,
                "Successfully updated schema: " + schemaType + "/" + attrType + "/" + schemaName);
    }
}
