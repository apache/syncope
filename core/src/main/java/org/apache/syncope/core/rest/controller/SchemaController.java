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
import javax.servlet.http.HttpServletResponse;
import org.apache.syncope.common.to.SchemaTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditElements.SchemaSubCategory;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.rest.data.SchemaDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/schema")
public class SchemaController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private SchemaDataBinder binder;

    @PreAuthorize("hasRole('SCHEMA_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public SchemaTO create(final HttpServletResponse response, @RequestBody final SchemaTO schemaTO,
            @PathVariable("kind") final String kind) {

        AbstractSchema schema = getAttributableUtil(kind).newSchema();
        binder.create(schemaTO, schema);
        schema = schemaDAO.save(schema);

        auditManager.audit(Category.schema, SchemaSubCategory.create, Result.success,
                "Successfully created schema: " + kind + "/" + schema.getName());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getSchemaTO(schema, getAttributableUtil(kind));
    }

    @PreAuthorize("hasRole('SCHEMA_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/delete/{schema}")
    public SchemaTO delete(@PathVariable("kind") final String kind, @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        Class<? extends AbstractSchema> reference = getAttributableUtil(kind).schemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            throw new NotFoundException("Schema '" + schemaName + "'");
        }

        SchemaTO schemaToDelete = binder.getSchemaTO(schema, getAttributableUtil(kind));

        schemaDAO.delete(schemaName, getAttributableUtil(kind));

        auditManager.audit(Category.schema, SchemaSubCategory.delete, Result.success,
                "Successfully deleted schema: " + kind + "/" + schema.getName());

        return schemaToDelete;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<SchemaTO> list(@PathVariable("kind") final String kind) {
        AttributableUtil attributableUtil = getAttributableUtil(kind);
        List<AbstractSchema> schemas = schemaDAO.findAll(attributableUtil.schemaClass());

        List<SchemaTO> schemaTOs = new ArrayList<SchemaTO>(schemas.size());
        for (AbstractSchema schema : schemas) {
            schemaTOs.add(binder.getSchemaTO(schema, attributableUtil));
        }

        auditManager.audit(Category.schema, SchemaSubCategory.list, Result.success,
                "Successfully listed all schemas: " + kind + "/" + schemaTOs.size());

        return schemaTOs;
    }

    @PreAuthorize("hasRole('SCHEMA_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/read/{schema}")
    public SchemaTO read(@PathVariable("kind") final String kind, @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        AttributableUtil attributableUtil = getAttributableUtil(kind);
        AbstractSchema schema = schemaDAO.find(schemaName, attributableUtil.schemaClass());
        if (schema == null) {
            throw new NotFoundException("Schema '" + schemaName + "'");
        }

        auditManager.audit(Category.schema, SchemaSubCategory.read, Result.success,
                "Successfully read schema: " + kind + "/" + schema.getName());

        return binder.getSchemaTO(schema, attributableUtil);
    }

    @PreAuthorize("hasRole('SCHEMA_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public SchemaTO update(@RequestBody final SchemaTO schemaTO, @PathVariable("kind") final String kind)
            throws NotFoundException {

        AttributableUtil attributableUtil = getAttributableUtil(kind);
        AbstractSchema schema = schemaDAO.find(schemaTO.getName(), attributableUtil.schemaClass());
        if (schema == null) {
            throw new NotFoundException("Schema '" + schemaTO.getName() + "'");
        }

        binder.update(schemaTO, schema, attributableUtil);
        schema = schemaDAO.save(schema);

        auditManager.audit(Category.schema, SchemaSubCategory.update, Result.success,
                "Successfully updated schema: " + kind + "/" + schema.getName());

        return binder.getSchemaTO(schema, attributableUtil);
    }
}
