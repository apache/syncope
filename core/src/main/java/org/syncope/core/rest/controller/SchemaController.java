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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.SchemaTO;
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

    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/create")
    public SchemaTO create(HttpServletResponse response,
            @RequestBody SchemaTO schemaTO, @PathVariable("kind") String kind)
            throws IOException {

        Class reference = getAttributable(kind).getSchemaClass();
        AbstractSchema schema = null;
        try {
            schema = schemaDataBinder.createSchema(schemaTO, reference,
                    getAttributable(kind).getDerivedSchemaClass());
        } catch (Exception e) {
            log.error("Could not create for " + schemaTO, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new SchemaTO();
        }

        return schemaDataBinder.getSchemaTO(schema);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{kind}/delete/{schema}")
    public void delete(HttpServletResponse response,
            @PathVariable("kind") String kind,
            @PathVariable("schema") String schemaName) throws IOException {

        Class reference = getAttributable(kind).getSchemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            log.error("Could not find schema '" + schemaName + "'");

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
            try {
                schemaDAO.delete(schemaName, reference);
            } catch (Throwable t) {
                log.error("While deleting " + schemaName, t);
                response.sendError(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{kind}/list")
    public List<SchemaTO> list(@PathVariable("kind") String kind) {

        Class reference = getAttributable(kind).getSchemaClass();
        List<AbstractSchema> schemas = schemaDAO.findAll(reference);

        List<SchemaTO> result = new ArrayList<SchemaTO>(schemas.size());
        for (AbstractSchema schema : schemas) {
            result.add(schemaDataBinder.getSchemaTO(schema));
        }

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

            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return new SchemaTO();
        }

        return schemaDataBinder.getSchemaTO(schema);
    }

    // TODO: implement and verify if current attributes are affected by this update
    @RequestMapping(method = RequestMethod.POST, value = "/{kind}/update")
    public SchemaTO update(HttpServletResponse response,
            @RequestBody SchemaTO schemaTO, @PathVariable("kind") String kind)
            throws IOException {

        return create(response, schemaTO, kind);
    }
}
