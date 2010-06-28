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
package org.syncope.identityconnectors.bundles.staticwebservice.wstarget.test;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.junit.Before;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSAttributeValue;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSChange;
import org.syncope.identityconnectors.bundles.commons.staticwebservice.to.WSUser;
import org.syncope.identityconnectors.bundles.staticwebservice.provisioning.interfaces.Provisioning;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operand;
import org.syncope.identityconnectors.bundles.staticwebservice.utilities.Operator;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:beans.xml"})
public class ProvisioningTestITCase {

    private static final Logger log =
            LoggerFactory.getLogger(ProvisioningTestITCase.class);

    final private String ENDPOINT_PREFIX =
            "http://localhost:8888/wstarget/services";

    final private String SERVICE =
            "/provisioning";

    @Autowired
    JaxWsProxyFactoryBean proxyFactory;

    Provisioning provisioning;

    @Before
    public void init() {
        assertNotNull(proxyFactory);

        proxyFactory.setAddress(ENDPOINT_PREFIX + SERVICE);

        proxyFactory.setServiceClass(Provisioning.class);

        provisioning = (Provisioning) proxyFactory.create();
    }

    @Test
    public void authenticate() {
        Throwable t = null;

        try {

            String uid = provisioning.authenticate(
                    "TESTUSER",
                    "password");

            assertEquals("TESTUSER", uid);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void checkAlive() {

        Throwable t = null;

        try {

            provisioning.checkAlive();

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);

    }

    @Test
    public void schema() {

        Throwable t = null;

        try {

            provisioning.schema();

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void create() {

        Throwable t = null;

        try {
            WSAttributeValue uid = new WSAttributeValue();
            uid.setName("username");
            uid.setValue("test1");
            uid.setKey(true);

            WSAttributeValue password = new WSAttributeValue();
            password.setName("password");
            password.setValue("password");
            password.setPassword(true);

            WSAttributeValue name = new WSAttributeValue();
            name.setName("nome");
            name.setValue("test1");

            WSAttributeValue surname = new WSAttributeValue();
            surname.setName("cognome");
            surname.setValue("test1");

            WSAttributeValue privacy = new WSAttributeValue();
            privacy.setName("privacy");
            privacy.setValue(Boolean.TRUE);

            WSAttributeValue birthday = new WSAttributeValue();
            birthday.setName("data di nascita");
            birthday.setValue("12/09/1990");

            Set<WSAttributeValue> attrs = new HashSet<WSAttributeValue>();
            attrs.add(uid);
            attrs.add(password);
            attrs.add(name);
            attrs.add(surname);
            attrs.add(privacy);
            attrs.add(birthday);

            provisioning.create(attrs);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void update() {

        Throwable t = null;

        try {

            WSAttributeValue surname = new WSAttributeValue();
            surname.setName("surname");
            surname.setValue("test1");
            surname.setKey(true);

            WSAttributeValue name = new WSAttributeValue();
            name.setName("nome");
            name.setValue("test1");


            Set<WSAttributeValue> attrs = new HashSet<WSAttributeValue>();
            attrs.add(surname);
            attrs.add(name);

            String uid = provisioning.update("test1", attrs);

            assertNotNull(uid);
            assertEquals("test1", uid);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void delete() {

        Throwable t = null;


        try {

            provisioning.delete("TESTUSER");

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void query() {

        Throwable t = null;


        try {

            Operand op1 = new Operand(Operator.EQ, "nome", "john");
            Operand op2 = new Operand(Operator.EQ, "cognome", "doe");
            Operand op3 = new Operand(Operator.EQ, "cognome", "black");

            Set<Operand> sop1 = new HashSet<Operand>();
            sop1.add(op1);
            sop1.add(op2);

            Set<Operand> sop2 = new HashSet<Operand>();
            sop2.add(op1);
            sop2.add(op3);

            Operand op4 = new Operand(Operator.AND, sop1);
            Operand op5 = new Operand(Operator.AND, sop2);

            Set<Operand> sop = new HashSet<Operand>();
            sop.add(op4);
            sop.add(op5);

            Operand query = new Operand(Operator.OR, sop, true);

            Set<WSUser> results = provisioning.query(query);

            for (WSUser user : results) {
                log.debug("Name: " + user.getAccountid());
            }


        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void resolve() {

        Throwable t = null;

        try {

            String uid = provisioning.resolve("fmartelli");

            assertEquals("TESTUSER", uid);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void getLatestChangeNumber() {

        Throwable t = null;

        try {

            int token = provisioning.getLatestChangeNumber();

            assertEquals(1, token);

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }

    @Test
    public void sync() {

        Throwable t = null;

        try {

            Set<WSChange> results = provisioning.sync();

            for (WSChange change : results) {
                log.debug("Delta: " + change.getId());
            }

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unknown exception!", e);
            }

            t = e;
        }

        assertNull(t);
    }
}
