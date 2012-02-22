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
package org.syncope.console;

import com.thoughtworks.selenium.SeleneseTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
    "classpath:applicationContext.xml"
})
public abstract class AbstractTest extends SeleneseTestCase {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(
            AbstractTest.class);

    protected static final String ADMIN = "admin";

    protected static final String PASSWORD = "password";

    protected static final String BASE_URL =
            "http://localhost:9080/syncope-console/";

    @Override
    @Before
    public void setUp()
            throws Exception {

        super.setUp(BASE_URL, "*firefox");

        selenium.open("/syncope-console/");
        selenium.type("name=userId", ADMIN);
        selenium.type("name=password", PASSWORD);
        selenium.click("name=:submit");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//img[@alt='Logout']\");",
                "60000");
    }

    @Override
    @After
    public void tearDown()
            throws Exception {

        selenium.click("css=img[alt=\"Logout\"]");
        selenium.stop();
        super.tearDown();
    }
}
