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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.SchemaTO;
import org.syncope.client.to.SchemaTOs;
import org.syncope.core.persistence.validation.UniqueValueException;
import org.syncope.core.rest.data.SchemaDataBinder;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.SchemaDAO;

@Controller
@RequestMapping("/schema")
public class SchemaController extends AbstractController {

    @Autowired
    private SchemaDAO schemaDAO;
    @Autowired
    private SchemaDataBinder schemaDataBinder;

    @Transactional
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public SchemaTO create(HttpServletResponse response,
            @RequestBody SchemaTO schemaTO, @PathVariable("kind") String kind)
            throws InstantiationException, IllegalAccessException,
            UniqueValueException {

        Class reference = getAttributable(kind).getSchemaClass();
        AbstractSchema schema = schemaDataBinder.createSchema(schemaTO,
                reference, getAttributable(kind).getDerivedSchemaClass());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return schemaDataBinder.getSchemaTO(schema);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{kind}/delete/{schema}")
    public void delete(HttpServletResponse response,
            @PathVariable("kind") String kind,
            @PathVariable("schema") String schemaName) throws IOException {

        Class reference = getAttributable(kind).getSchemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            log.error("Could not find schema '" + schemaName + "'");
            throwNotFoundException(schemaName, response);
        } else {
            schemaDAO.delete(schemaName, reference);
            schemaDAO.getEntityManager().flush();
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public SchemaTOs list(@PathVariable("kind") String kind) {

        Class reference = getAttributable(kind).getSchemaClass();
        List<AbstractSchema> schemas = schemaDAO.findAll(reference);

        List<SchemaTO> schemaTOs = new ArrayList<SchemaTO>(schemas.size());
        for (AbstractSchema schema : schemas) {
            schemaTOs.add(schemaDataBinder.getSchemaTO(schema));
        }

        SchemaTOs result = new SchemaTOs();
        result.setSchemas(schemaTOs);
        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/read/{schema}")
    public SchemaTO read(HttpServletResponse response,
            @PathVariable("kind") String kind,
            @PathVariable("schema") String schemaName) throws IOException {

        Class reference = getAttributable(kind).getSchemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            log.error("Could not find schema '" + schemaName + "'");
            return throwNotFoundException(schemaName, response);
        }

        return schemaDataBinder.getSchemaTO(schema);
    }

    // TODO: implement and verify if current attributes are affected by this update
    @Transactional
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public SchemaTO update(HttpServletResponse response,
            @RequestBody SchemaTO schemaTO, @PathVariable("kind") String kind)
            throws InstantiationException, IllegalAccessException,
            UniqueValueException {

        return create(response, schemaTO, kind);
    }
}
