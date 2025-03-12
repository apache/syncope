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
package org.apache.syncope.core.provisioning.api.jexl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;

public class MailTemplateTest extends AbstractTest {

    private static final String CONFIRM_PASSWORD_RESET_TEMPLATE =
            "<html><body>"
            + "<p>Hi,<br/> we are happy to inform you that the password request was successfully executed for "
            + "your account.</p>  <p>Best regards.</p> </body> </html>";

    private static final String REQUEST_PASSWORD_RESET_TEMPLATE = "Hi, a password reset was requested for "
            + "${user.getUsername()}.  In order to complete this request, you need to visit this link: "
            + "http://localhost:9080/syncope-enduser/app/#!/confirmpasswordreset?token="
            + "${input.get(0).replaceAll(' ', '%20')}"
            + "If you did not request this reset, just ignore the present e-mail.  Best regards.";

    private static final String OPTIN_TEMPLATE =
            "<html> <body> <h3>Hi ${user.getPlainAttr(\"firstname\").get().values[0]} "
            + "${user.getPlainAttr(\"surname\").get().values[0]}, welcome to Syncope!</h3>"
            + "<p> Your username is ${user.username}.<br/>"
            + "Your email address is ${user.getPlainAttr(\"email\").get().values[0]}."
            + "Your email address inside a <a href=\"http://localhost/?email="
            + "${user.getPlainAttr(\"email\").get().values[0].replace('@', '%40')}\">link</a>.</p>"
            + "<p>This message was sent to the following recipients: <ul>\n $$ for (recipient: recipients) {\n"
            + "   <li>${recipient.getPlainAttr(\"email\").get().values[0]}</li>\n $$ }\n </ul>\n"
            + "  because one of the following events occurred: <ul>\n $$ for (event: events) {\n"
            + "   <li>${event}</li>\n $$ }\n </ul>\n </p> \n $$ if (!empty(user.memberships)) {\n"
            + " You have been provided with the following groups:\n <ul>\n"
            + " $$ for(membership : user.memberships) {\n   <li>${membership.groupName}</li>\n $$ }\n"
            + " </ul>\n $$ }\n </body> </html>";

    @Test
    public void confirmPasswordReset() throws IOException {
        String htmlBody = JexlUtils.evaluateTemplate(CONFIRM_PASSWORD_RESET_TEMPLATE, new MapContext());
        assertNotNull(htmlBody);
    }

    @Test
    public void requestPasswordReset() throws IOException {
        Map<String, Object> ctx = new HashMap<>();

        String username = "test" + UUID.randomUUID();
        UserTO user = new UserTO();
        user.setUsername(username);
        ctx.put("user", user);

        String token = "token " + UUID.randomUUID();
        List<String> input = new ArrayList<>();
        input.add(token);
        ctx.put("input", input);

        String textBody = JexlUtils.evaluateTemplate(REQUEST_PASSWORD_RESET_TEMPLATE, new MapContext(ctx));

        assertNotNull(textBody);
        assertTrue(textBody.contains("a password reset was requested for " + username + "."));
        assertFalse(textBody.contains(
                "http://localhost:9080/syncope-enduser/app/#!/confirmpasswordreset?token="
                + token));
        assertTrue(textBody.contains(
                "http://localhost:9080/syncope-enduser/app/#!/confirmpasswordreset?token="
                + token.replaceAll(" ", "%20")));
    }

    @Test
    public void optin() throws IOException {
        Map<String, Object> ctx = new HashMap<>();

        String username = "test" + UUID.randomUUID();
        UserTO user = new UserTO();
        user.setUsername(username);
        user.getPlainAttrs().add(new Attr.Builder("firstname").value("John").build());
        user.getPlainAttrs().add(new Attr.Builder("surname").value("Doe").build());
        user.getPlainAttrs().add(new Attr.Builder("email").value("john.doe@syncope.apache.org").build());
        user.getMemberships().add(new MembershipTO.Builder(UUID.randomUUID().toString()).groupName("a group").build());
        ctx.put("user", user);

        String token = "token " + UUID.randomUUID();
        List<String> input = new ArrayList<>();
        input.add(token);
        ctx.put("input", input);

        UserTO recipient = SerializationUtils.clone(user);
        recipient.getPlainAttr("email").get().getValues().set(0, "another@syncope.apache.org");
        ctx.put("recipients", List.of(recipient));

        ctx.put("events", List.of("event1"));

        String htmlBody = JexlUtils.evaluateTemplate(OPTIN_TEMPLATE, new MapContext(ctx));

        assertNotNull(htmlBody);

        assertTrue(htmlBody.contains("Hi John Doe,"));
        assertTrue(htmlBody.contains("Your email address is john.doe@syncope.apache.org."));
        assertTrue(htmlBody.contains("<li>another@syncope.apache.org</li>"));
        assertTrue(htmlBody.contains("<li>a group</li>"));
        assertTrue(htmlBody.contains("<li>event1</li>"));
    }
}
