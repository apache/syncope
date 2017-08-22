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
package org.apache.syncope.core.provisioning.java;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.java.jexl.JexlUtils;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional("Master")
public class MailTemplateTest extends AbstractTest {

    @Autowired
    private MailTemplateDAO mailTemplateDAO;

    private String evaluate(final String template, final Map<String, Object> jexlVars) {
        StringWriter writer = new StringWriter();
        JexlUtils.newJxltEngine().
                createTemplate(template).
                evaluate(new MapContext(jexlVars), writer);
        return writer.toString();
    }

    @Test
    public void confirmPasswordReset() throws IOException {
        String htmlBody = evaluate(
                mailTemplateDAO.find("confirmPasswordReset").getHTMLTemplate(),
                new HashMap<>());

        assertNotNull(htmlBody);
    }

    @Test
    public void requestPasswordReset() throws IOException {
        Map<String, Object> ctx = new HashMap<>();

        String username = "test" + UUID.randomUUID().toString();
        UserTO user = new UserTO();
        user.setUsername(username);
        ctx.put("user", user);

        String token = "token " + UUID.randomUUID().toString();
        List<String> input = new ArrayList<>();
        input.add(token);
        ctx.put("input", input);

        String htmlBody = evaluate(
                mailTemplateDAO.find("requestPasswordReset").getHTMLTemplate(),
                ctx);

        assertNotNull(htmlBody);
        assertTrue(htmlBody.contains("a password reset was request for " + username + "."));
        assertFalse(htmlBody.contains(
                "http://localhost:9080/syncope-enduser/app/#!/confirmpasswordreset?token="
                + token));
        assertTrue(htmlBody.contains(
                "http://localhost:9080/syncope-enduser/app/#!/confirmpasswordreset?token="
                + token.replaceAll(" ", "%20")));
    }

    @Test
    public void optin() throws IOException {
        Map<String, Object> ctx = new HashMap<>();

        String username = "test" + UUID.randomUUID().toString();
        UserTO user = new UserTO();
        user.setUsername(username);
        user.getPlainAttrs().add(new AttrTO.Builder().schema("firstname").value("John").build());
        user.getPlainAttrs().add(new AttrTO.Builder().schema("surname").value("Doe").build());
        user.getPlainAttrs().add(new AttrTO.Builder().schema("email").value("john.doe@syncope.apache.org").build());
        user.getMemberships().add(new MembershipTO.Builder().group(UUID.randomUUID().toString(), "a group").build());
        ctx.put("user", user);

        String token = "token " + UUID.randomUUID().toString();
        List<String> input = new ArrayList<>();
        input.add(token);
        ctx.put("input", input);

        UserTO recipient = SerializationUtils.clone(user);
        recipient.getPlainAttr("email").get().getValues().set(0, "another@syncope.apache.org");
        ctx.put("recipients", Collections.singletonList(recipient));

        String htmlBody = evaluate(
                mailTemplateDAO.find("optin").getHTMLTemplate(),
                ctx);

        assertNotNull(htmlBody);

        assertTrue(htmlBody.contains("Hi John Doe,"));
        assertTrue(htmlBody.contains("Your email address is john.doe@syncope.apache.org."));
        assertTrue(htmlBody.contains("<li>another@syncope.apache.org</li>"));
        assertTrue(htmlBody.contains("<li>a group</li>"));
    }
}
