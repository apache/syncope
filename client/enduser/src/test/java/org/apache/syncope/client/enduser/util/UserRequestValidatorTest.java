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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.syncope.client.enduser.model.CustomAttributesInfo;
import org.apache.syncope.client.enduser.model.CustomTemplateInfo;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;
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

        Map<String, CustomAttributesInfo> customFormAttributes = new ObjectMapper().readValue(new ClassPathResource(
                "customFormAttributes.json").getFile(), new TypeReference<HashMap<String, CustomAttributesInfo>>() {
        });

        CustomTemplateInfo customTemplate = new ObjectMapper().readValue(new ClassPathResource(
                "customTemplate.json").getFile(), CustomTemplateInfo.class);

        // not allowed because of presence of notAllowed attribute
        assertFalse(UserRequestValidator.compliant(userTO, customFormAttributes, true));

        // remove notAllowed attribute and make it compliant
        userTO.getPlainAttrs().remove(notAllowed);
        assertTrue(UserRequestValidator.compliant(userTO, customFormAttributes, true));

        // firstname must have only one defaultValue
        userTO.getPlainAttr("firstname").get().getValues().add("notAllowedFirstnameValue");
        assertFalse(UserRequestValidator.compliant(userTO, customFormAttributes, true));
        assertTrue(UserRequestValidator.compliant(userTO, customFormAttributes, false));

        // clean
        userTO.getPlainAttr("firstname").get().getValues().remove("notAllowedFirstnameValue");

        // virtual
        AttrTO virtualdata = attrTO("virtualdata", "defaultVirtualData");
        userTO.getVirAttrs().add(virtualdata);
        assertTrue(UserRequestValidator.compliant(userTO, customFormAttributes, true));

        // with empty form is compliant by definition
        assertTrue(UserRequestValidator.compliant(userTO, new HashMap<>(), true));

        // check wizard steps
        // only "credentials", "plainSchemas" and "finish" steps must be visible
        assertTrue(UserRequestValidator.validateSteps(customTemplate));

        assertTrue(UserRequestValidator.validateStep("credentials", customTemplate));
        assertTrue(UserRequestValidator.validateStep("plainSchemas", customTemplate));
        assertTrue(UserRequestValidator.validateStep("finish", customTemplate));

        assertFalse(UserRequestValidator.validateStep("test", customTemplate));
        assertFalse(UserRequestValidator.validateStep("resources", customTemplate));
        assertFalse(UserRequestValidator.validateStep("virtualSchemas", customTemplate));
        assertFalse(UserRequestValidator.validateStep("derivedSchemas", customTemplate));
        assertFalse(UserRequestValidator.validateStep("groups", customTemplate));
    }

}
