/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.enduser.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class UserRequestValidatorTest {

    private AttrTO attrTO(String schemaKey, String... values) {
        return new AttrTO.Builder().schema(schemaKey).values(values).build();
    }

    @Test
    public void testCompliant() throws IOException {
        UserTO userTO = new UserTO();
        // plain
        AttrTO firstname = attrTO("firstname", "defaultFirstname");
        AttrTO surname = attrTO("surname", "surnameValue");
        AttrTO additionalCtype = attrTO("additional#ctype", "ctypeValue");
        AttrTO notAllowed = attrTO("not_allowed", "notAllowedValue");
        userTO.getPlainAttrs().addAll(Arrays.asList(firstname, surname, notAllowed, additionalCtype));

        Map<String, CustomAttributesInfo> customForm = new ObjectMapper().readValue(new ClassPathResource(
                "customForm.json").getFile(), new TypeReference<HashMap<String, CustomAttributesInfo>>() {
        });

        // not allowed because of presence of notAllowed attribute
        assertFalse(UserRequestValidator.compliant(userTO, customForm, true));

        // remove notAllowed attribute and make it compliant
        userTO.getPlainAttrs().remove(notAllowed);
        assertTrue(UserRequestValidator.compliant(userTO, customForm, true));

        // firstname must have only one defaultValue
        userTO.getPlainAttr("firstname").get().getValues().add("notAllowedFirstnameValue");
        assertFalse(UserRequestValidator.compliant(userTO, customForm, true));
        assertTrue(UserRequestValidator.compliant(userTO, customForm, false));
        // clean
        userTO.getPlainAttr("firstname").get().getValues().remove("notAllowedFirstnameValue");

        // derived must not be present
        AttrTO derivedNotAllowed = attrTO("derivedNotAllowed");
        userTO.getDerAttrs().add(derivedNotAllowed);
        assertFalse(UserRequestValidator.compliant(userTO, customForm, true));
        // clean 
        userTO.getDerAttrs().clear();

        // virtual
        AttrTO virtualdata = attrTO("virtualdata", "defaultVirtualData");
        userTO.getVirAttrs().add(virtualdata);
        assertTrue(UserRequestValidator.compliant(userTO, customForm, true));

        // with empty form is compliant by definition
        assertTrue(UserRequestValidator.compliant(userTO, new HashMap<>(), true));
    }

}
