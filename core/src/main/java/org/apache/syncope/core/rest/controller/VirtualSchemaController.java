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
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.apache.syncope.client.to.VirtualSchemaTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.AbstractVirSchema;
import org.apache.syncope.core.persistence.dao.VirSchemaDAO;
import org.apache.syncope.core.rest.data.VirtualSchemaDataBinder;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.SchemaSubCategory;

@Controller
@RequestMapping("/virtualSchema")
public class VirtualSchemaController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private VirSchemaDAO virtualSchemaDAO;

    @Autowired
    private VirtualSchemaDataBinder virtualSchemaDataBinder;

    @PreAuthorize("hasRole('SCHEMA_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public VirtualSchemaTO create(final HttpServletResponse response,
            @RequestBody final VirtualSchemaTO virtualSchemaTO, @PathVariable("kind") final String kind)
            throws SyncopeClientCompositeErrorException {

        AbstractVirSchema virtualSchema = virtualSchemaDAO.save(virtualSchemaDataBinder.create(
                virtualSchemaTO, getAttributableUtil(kind).newVirtualSchema(),
                getAttributableUtil(kind).schemaClass()));

        auditManager.audit(Category.schema, SchemaSubCategory.createVirtual, Result.success,
                "Successfully created virtual schema: " + kind + "/" + virtualSchema.getName());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return virtualSchemaDataBinder.getVirtualSchemaTO(virtualSchema);
    }

    @PreAuthorize("hasRole('SCHEMA_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/delete/{schema}")
    public VirtualSchemaTO delete(@PathVariable("kind") final String kind,
            @PathVariable("schema") final String virtualSchemaName) throws NotFoundException {

        Class reference = getAttributableUtil(kind).virtualSchemaClass();
        AbstractVirSchema virtualSchema = virtualSchemaDAO.find(virtualSchemaName, reference);
        if (virtualSchema == null) {
            throw new NotFoundException("Virtual schema '" + virtualSchemaName + "'");
        }

        VirtualSchemaTO schemaToDelete = virtualSchemaDataBinder.getVirtualSchemaTO(virtualSchema);
        virtualSchemaDAO.delete(virtualSchemaName, getAttributableUtil(kind));

        auditManager.audit(Category.schema, SchemaSubCategory.deleteVirtual, Result.success,
                "Successfully deleted virtual schema: " + kind + "/" + virtualSchema.getName());
        return schemaToDelete;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<VirtualSchemaTO> list(@PathVariable("kind") final String kind) {
        Class reference = getAttributableUtil(kind).virtualSchemaClass();
        List<AbstractVirSchema> virtualAttributeSchemas = virtualSchemaDAO.findAll(reference);

        List<VirtualSchemaTO> virtualSchemaTOs = new ArrayList<VirtualSchemaTO>(virtualAttributeSchemas.size());
        for (AbstractVirSchema virtualSchema : virtualAttributeSchemas) {
            virtualSchemaTOs.add(virtualSchemaDataBinder.getVirtualSchemaTO(virtualSchema));
        }

        auditManager.audit(Category.schema, SchemaSubCategory.listVirtual, Result.success,
                "Successfully listed all virtual schemas: " + kind + "/" + virtualSchemaTOs.size());

        return virtualSchemaTOs;
    }

    @PreAuthorize("hasRole('SCHEMA_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/read/{virtualSchema}")
    public VirtualSchemaTO read(@PathVariable("kind") final String kind,
            @PathVariable("virtualSchema") final String virtualSchemaName) throws NotFoundException {

        Class reference = getAttributableUtil(kind).virtualSchemaClass();
        AbstractVirSchema virtualSchema = virtualSchemaDAO.find(virtualSchemaName, reference);
        if (virtualSchema == null) {
            throw new NotFoundException("Virtual schema '" + virtualSchemaName + "'");
        }

        auditManager.audit(Category.schema, SchemaSubCategory.readVirtual, Result.success,
                "Successfully read virtual schema: " + kind + "/" + virtualSchema.getName());

        return virtualSchemaDataBinder.getVirtualSchemaTO(virtualSchema);
    }

    @PreAuthorize("hasRole('SCHEMA_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public VirtualSchemaTO update(@RequestBody final VirtualSchemaTO virtualSchemaTO,
            @PathVariable("kind") final String kind) throws SyncopeClientCompositeErrorException, NotFoundException {

        Class reference = getAttributableUtil(kind).virtualSchemaClass();
        AbstractVirSchema virtualSchema = virtualSchemaDAO.find(virtualSchemaTO.getName(), reference);
        if (virtualSchema == null) {
            throw new NotFoundException("Virtual schema is null");
        }

        virtualSchema = virtualSchemaDataBinder.update(virtualSchemaTO, virtualSchema,
                getAttributableUtil(kind).schemaClass());
        virtualSchema = virtualSchemaDAO.save(virtualSchema);

        auditManager.audit(Category.schema, SchemaSubCategory.updateVirtual, Result.success,
                "Successfully updated virtual schema: " + kind + "/" + virtualSchema.getName());

        return virtualSchemaDataBinder.getVirtualSchemaTO(virtualSchema);
    }
}
