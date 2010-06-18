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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.syncope.client.to.AttributeSchemaTO;
import org.syncope.client.to.DerivedAttributeSchemaTO;
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.beans.DerivedAttributeSchema;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;
import org.syncope.core.persistence.dao.DerivedAttributeSchemaDAO;

@Controller
@RequestMapping("/schema")
public class SchemaController {

    private static final Logger log = LoggerFactory.getLogger(
            SchemaController.class);

    @RequestMapping(method = RequestMethod.GET, value = "/attribute/list")
    public List<AttributeSchemaTO> attributeList(HttpServletRequest request)
            throws IOException {

        WebApplicationContext webApplicationContext =
                RequestContextUtils.getWebApplicationContext(request);

        AttributeSchemaDAO attributeSchemaDAO =
                (AttributeSchemaDAO) webApplicationContext.getBean(
                "attributeSchemaDAOImpl");

        List<AttributeSchema> attributeSchemas = attributeSchemaDAO.findAll();
        List<AttributeSchemaTO> result = new ArrayList<AttributeSchemaTO>(
                attributeSchemas.size());
        AttributeSchemaTO attributeSchemaTO = null;
        String[] ignoreProperties = {"derivedAttributeSchemas"};
        for (AttributeSchema attributeSchema : attributeSchemas) {
            attributeSchemaTO = new AttributeSchemaTO();
            BeanUtils.copyProperties(attributeSchema, attributeSchemaTO,
                    ignoreProperties);

            for (DerivedAttributeSchema derivedAttributeSchema :
                    attributeSchema.getDerivedAttributeSchemas()) {

                attributeSchemaTO.addDerivedAttributeSchema(
                        derivedAttributeSchema.getName());
            }

            result.add(attributeSchemaTO);
        }

        return result;
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/derivedAttribute/list")
    public List<DerivedAttributeSchemaTO> derivedAttributeList(
            HttpServletRequest request) throws IOException {

        WebApplicationContext webApplicationContext =
                RequestContextUtils.getWebApplicationContext(request);

        DerivedAttributeSchemaDAO derivedAttributeSchemaDAO =
                (DerivedAttributeSchemaDAO) webApplicationContext.getBean(
                "derivedAttributeSchemaDAOImpl");

        List<DerivedAttributeSchema> derivedAttributeSchemas =
                derivedAttributeSchemaDAO.findAll();
        List<DerivedAttributeSchemaTO> result =
                new ArrayList<DerivedAttributeSchemaTO>(
                derivedAttributeSchemas.size());
        DerivedAttributeSchemaTO derivedAttributeSchemaTO = null;
        String[] ignoreProperties = {"attributeSchemas"};
        for (DerivedAttributeSchema derivedAttributeSchema :
                derivedAttributeSchemas) {

            derivedAttributeSchemaTO = new DerivedAttributeSchemaTO();
            BeanUtils.copyProperties(derivedAttributeSchema,
                    derivedAttributeSchemaTO, ignoreProperties);

            for (AttributeSchema attributeSchema :
                    derivedAttributeSchema.getAttributeSchemas()) {

                derivedAttributeSchemaTO.addAttributeSchema(
                        attributeSchema.getName());
            }

            result.add(derivedAttributeSchemaTO);
        }

        return result;
    }
}
