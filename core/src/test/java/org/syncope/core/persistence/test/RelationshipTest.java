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
package org.syncope.core.persistence.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.transaction.annotation.Transactional;
import org.syncope.core.persistence.beans.Attribute;
import org.syncope.core.persistence.beans.AttributeSchema;
import org.syncope.core.persistence.beans.AttributeValue;
import org.syncope.core.persistence.beans.DerivedAttributeSchema;
import org.syncope.core.persistence.dao.AttributeDAO;
import org.syncope.core.persistence.dao.AttributeSchemaDAO;
import org.syncope.core.persistence.dao.AttributeValueDAO;
import org.syncope.core.persistence.dao.DerivedAttributeSchemaDAO;

@Transactional
public class RelationshipTest extends AbstractDAOTest {

    @Autowired
    AttributeSchemaDAO attributeSchemaDAO;
    @Autowired
    AttributeDAO attributeDAO;
    @Autowired
    AttributeValueDAO attributeValueDAO;
    @Autowired
    DerivedAttributeSchemaDAO derivedAttributeSchemaDAO;
    private Set<Long> loginDateAttributeIds;
    private Set<Long> allLoginDateAttributeValueIds;
    private Set<String> derivedAttributeSchemaNames;
    private Set<String> attributeSchemaNames;

    @Test
    @Rollback(false)
    public final void prepare() {

        // 1. AttributeSchema <-> Attribute <-> AttributeValue
        {
            AttributeSchema loginDateSchema =
                    attributeSchemaDAO.find("loginDate");
            assertNotNull(loginDateSchema);

            loginDateAttributeIds = new HashSet<Long>();
            allLoginDateAttributeValueIds = new HashSet<Long>();
            for (Attribute loginDateAttribute :
                    loginDateSchema.getAttributes()) {

                loginDateAttributeIds.add(loginDateAttribute.getId());

                for (AttributeValue loginDateAttributeValue :
                        loginDateAttribute.getValues()) {

                    allLoginDateAttributeValueIds.add(
                            loginDateAttributeValue.getId());
                }
            }

            attributeSchemaDAO.delete("loginDate");
        }

        // 2. AttributeSchema <-> DerivedAttributeSchema
        {
            AttributeSchema surnameSchema = attributeSchemaDAO.find("surname");
            assertNotNull(surnameSchema);

            derivedAttributeSchemaNames = new HashSet<String>();
            for (DerivedAttributeSchema derivedAttributeSchema :
                    surnameSchema.getDerivedAttributeSchemas()) {

                derivedAttributeSchemaNames.add(
                        derivedAttributeSchema.getName());
            }

            attributeSchemaDAO.delete("surname");
        }

        // 3. DerivedAttributeSchema <-> AttributeSchema
        /*{
            DerivedAttributeSchema alternativeCNSchema =
                    derivedAttributeSchemaDAO.find("icon2");
            assertNotNull(alternativeCNSchema);

            attributeSchemaNames = new HashSet<String>();
            for (AttributeSchema attributeSchema :
                    alternativeCNSchema.getAttributeSchemas()) {

                attributeSchemaNames.add(attributeSchema.getName());
            }

            derivedAttributeSchemaDAO.delete("icon2");
        }*/
    }

    @AfterTransaction
    public final void verify() {

        // 1. AttributeSchema <-> Attribute <-> AttributeValue
        {
            assertNotNull(loginDateAttributeIds);
            assertNotNull(allLoginDateAttributeValueIds);
            assertNull(attributeSchemaDAO.find("loginDate"));

            for (Long loginDateAttribute : loginDateAttributeIds) {
                assertNull(attributeDAO.find(loginDateAttribute));
            }
            for (Long attributeValue : allLoginDateAttributeValueIds) {
                assertNull(attributeValueDAO.find(attributeValue));
            }
        }

        // 2. AttributeSchema <-> DerivedAttributeSchema
        {
            assertNotNull(derivedAttributeSchemaNames);
            assertNull(attributeSchemaDAO.find("surname"));

            DerivedAttributeSchema derivedAttributeSchema = null;
            for (String derivedAttributeSchemaName :
                    derivedAttributeSchemaNames) {

                derivedAttributeSchema =
                        derivedAttributeSchemaDAO.find(
                        derivedAttributeSchemaName);

                for (AttributeSchema attributeSchema :
                        derivedAttributeSchema.getAttributeSchemas()) {

                    assertTrue(!"surname".equals(attributeSchema.getName()));
                }
            }
        }

        // 3. DerivedAttributeSchema <-> AttributeSchema
        /*{
            assertNotNull(attributeSchemaNames);
            assertNull(derivedAttributeSchemaDAO.find("icon2"));

            AttributeSchema attributeSchema = null;
            for (String attributeSchemaName : attributeSchemaNames) {
                attributeSchema = attributeSchemaDAO.find(attributeSchemaName);

                for (DerivedAttributeSchema derivedAttributeSchema :
                        attributeSchema.getDerivedAttributeSchemas()) {

                    assertTrue(!"icon2".equals(
                            derivedAttributeSchema.getName()));
                }
            }
        }*/
    }
}
