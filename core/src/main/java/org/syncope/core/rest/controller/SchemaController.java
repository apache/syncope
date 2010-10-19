/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.rest.data.SchemaDataBinder;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.validation.MultiUniqueValueException;

@Controller
@RequestMapping("/schema")
public class SchemaController extends AbstractController {

    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private SchemaDataBinder schemaDataBinder;

    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public SchemaTO create(final HttpServletResponse response,
            @RequestBody final SchemaTO schemaTO,
            @PathVariable("kind") final String kind)
            throws MultiUniqueValueException,
            SyncopeClientCompositeErrorException {

        AbstractSchema schema = schemaDataBinder.create(schemaTO,
                getAttributableUtil(kind).newSchema(),
                getAttributableUtil(kind).getDerivedSchemaClass());

        schema = schemaDAO.save(schema);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return schemaDataBinder.getSchemaTO(schema);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{kind}/delete/{schema}")
    public void delete(@PathVariable("kind") final String kind,
            @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).getSchemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            LOG.error("Could not find schema '" + schemaName + "'");

            throw new NotFoundException(schemaName);
        } else {
            schemaDAO.delete(schemaName, reference);
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<SchemaTO> list(@PathVariable("kind") final String kind) {
        Class reference = getAttributableUtil(kind).getSchemaClass();
        List<AbstractSchema> schemas = schemaDAO.findAll(reference);

        List<SchemaTO> schemaTOs = new ArrayList<SchemaTO>(schemas.size());
        for (AbstractSchema schema : schemas) {
            schemaTOs.add(schemaDataBinder.getSchemaTO(schema));
        }

        return schemaTOs;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/read/{schema}")
    public SchemaTO read(@PathVariable("kind") final String kind,
            @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).getSchemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            LOG.error("Could not find schema '" + schemaName + "'");
            throw new NotFoundException("Schema '" + schemaName + "'");
        }

        return schemaDataBinder.getSchemaTO(schema);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public SchemaTO update(@RequestBody final SchemaTO schemaTO,
            @PathVariable("kind") final String kind)
            throws SyncopeClientCompositeErrorException,
            MultiUniqueValueException, NotFoundException {

        Class reference = getAttributableUtil(kind).getSchemaClass();
        AbstractSchema schema = schemaDAO.find(schemaTO.getName(), reference);
        if (schema == null) {
            LOG.error("Could not find schema '" + schemaTO.getName() + "'");
            throw new NotFoundException("Schema '" + schemaTO.getName() + "'");
        }

        schema = schemaDataBinder.update(schemaTO,
                schema, getAttributableUtil(kind).getDerivedSchemaClass());
        schema = schemaDAO.save(schema);

        return schemaDataBinder.getSchemaTO(schema);
    }
}
