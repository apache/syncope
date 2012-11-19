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
package org.apache.syncope.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

public class ConnInstanceTestITCase extends AbstractTest {

    private static String connidSoapVersion;

    @BeforeClass
    public static void init() {
        Properties props = new Properties();
        InputStream propStream = null;
        try {
            propStream = ConnInstanceTestITCase.class.getResourceAsStream("/bundles.properties");
            props.load(propStream);
            connidSoapVersion = props.getProperty("connid.soap.version");
        } catch (Exception e) {
            LOG.error("Could not load bundles.properties", e);
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException e) {
                    LOG.error("While reading bundles.properties", e);
                }
            }
        }
        assertNotNull(connidSoapVersion);
    }

    @Test
    public void browseCreateModal() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/div[2]/a");

        selenium.waitForCondition("selenium.isElementPresent(" + "\"//input[@name='version:textField']\");", "30000");

        selenium.select("//select[@name='bundle:dropDownChoiceField']",
                "label=org.connid.bundles.soap " + connidSoapVersion);

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//form/div[@id='tabs']/ul/li[1]/a/span");

        assertTrue(selenium.isElementPresent("//form/div[2]/div/div/div[4]/div[2]"));

        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//tr[4]/td[6]/div/span[7]/a");

        selenium.waitForCondition("selenium.isElementPresent(" + "\"//input[@name='version:textField']\");", "30000");

        assertEquals("ConnInstance103", selenium.getAttribute("//input[@name='displayName:textField']/@value"));

        assertEquals("org.connid.bundles.soap#" + connidSoapVersion, selenium
                .getSelectedValue("//select[@name='bundle:dropDownChoiceField']"));

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("css=a.w_close");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//tr[4]/td[6]/div/span[9]/a");

        assertTrue(selenium.getConfirmation().matches("^Do you really want to delete the selected item[\\s\\S]$"));

        selenium.waitForCondition("selenium.isTextPresent(" + "\"Error occurred during the requested operation\");",
                "10000");
    }

    @Test
    public void checkConnection() {
        selenium.click("css=img[alt=\"Resources\"]");

        selenium.waitForCondition("selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//tr[2]/td[6]/div/span[7]/a");

        selenium.waitForCondition("selenium.isElementPresent(" + "\"//input[@name='version:textField']\");", "30000");

        selenium.click("//div[2]/form/div[2]/ul/li[2]/a");

        selenium.waitForCondition("selenium.isElementPresent(\"//input[@name='version:textField']\");", "30000");

        selenium.click("//div[2]/form/div[2]/div[2]/div/span/div[2]/div[29]/a/img");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div/ul/li/span[contains(text(),'Successful connection')]\");", "30000");
    }
}
