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
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.syncope.client.to.AttributeSchemaTO;
import org.syncope.client.to.DerivedAttributeSchemaTO;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractDerivedSchema;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.DerivedSchemaDAO;

@Controller
@RequestMapping("/schema")
public class SchemaController {

    private static final Logger log = LoggerFactory.getLogger(
            SchemaController.class);

    private Class getReference(String kind) throws IOException {
        Class result = null;

        if ("user".equals(kind)) {
            result = UserSchema.class;
        } else if ("role".equals(kind)) {
            result = RoleSchema.class;
        } else {// TODO: throw exception in REST style
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/attribute/{kind}/list")
    public List<AttributeSchemaTO> attributeList(HttpServletRequest request,
            @PathVariable("kind") String kind) throws IOException {

        Class reference = getReference(kind);

        WebApplicationContext webApplicationContext =
                RequestContextUtils.getWebApplicationContext(request);

        SchemaDAO schemaDAO =
                (SchemaDAO) webApplicationContext.getBean("schemaDAOImpl");

        List<AbstractSchema> attributeSchemas = schemaDAO.findAll(reference);

        // TODO: change TO?
        List<AttributeSchemaTO> result = new ArrayList<AttributeSchemaTO>(
                attributeSchemas.size());
        AttributeSchemaTO attributeSchemaTO = null;
        String[] ignoreProperties = {"derivedAttributeSchemas"};
        for (AbstractSchema attributeSchema : attributeSchemas) {
            attributeSchemaTO = new AttributeSchemaTO();
            BeanUtils.copyProperties(attributeSchema, attributeSchemaTO,
                    ignoreProperties);

            for (AbstractDerivedSchema derivedAttributeSchema :
                    attributeSchema.getDerivedSchemas()) {

                attributeSchemaTO.addDerivedAttributeSchema(
                        derivedAttributeSchema.getName());
            }

            result.add(attributeSchemaTO);
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/derivedAttribute/{kind}/list")
    public List<DerivedAttributeSchemaTO> derivedAttributeList(
            HttpServletRequest request, @PathVariable("kind") String kind)
            throws IOException {

        Class reference = getReference(kind);

        WebApplicationContext webApplicationContext =
                RequestContextUtils.getWebApplicationContext(request);

        DerivedSchemaDAO derivedAttributeSchemaDAO =
                (DerivedSchemaDAO) webApplicationContext.getBean(
                "derivedSchemaDAOImpl");

        List<AbstractDerivedSchema> derivedAttributeSchemas =
                derivedAttributeSchemaDAO.findAll(reference);
        // TODO: change TO?
        List<DerivedAttributeSchemaTO> result =
                new ArrayList<DerivedAttributeSchemaTO>(
                derivedAttributeSchemas.size());
        DerivedAttributeSchemaTO derivedAttributeSchemaTO = null;
        String[] ignoreProperties = {"attributeSchemas"};
        for (AbstractDerivedSchema derivedAttributeSchema :
                derivedAttributeSchemas) {

            derivedAttributeSchemaTO = new DerivedAttributeSchemaTO();
            BeanUtils.copyProperties(derivedAttributeSchema,
                    derivedAttributeSchemaTO, ignoreProperties);

            for (AbstractSchema attributeSchema :
                    derivedAttributeSchema.getSchemas()) {

                derivedAttributeSchemaTO.addAttributeSchema(
                        attributeSchema.getName());
            }

            result.add(derivedAttributeSchemaTO);
        }

        return result;
    }
}
