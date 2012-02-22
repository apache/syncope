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

import org.junit.Test;

public class SchemaTestITCase extends AbstractTest {

    @Test
    public void create() {
        selenium.click("css=img[alt=\"Schema\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[3]/div/div/a");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//*[@id='_wicket_window_0']\");",
                "30000");

        selenium.waitForCondition("selenium.isElementPresent("
                + "\"//*[@name='name:textField']\");",
                "30000");

        selenium.select("name=type:dropDownChoiceField", "value=0");
        selenium.type("name=name:textField", "newschema");
        selenium.click("name=apply");

        selenium.waitForCondition(
                "selenium.isTextPresent(\"newschema\");", "30000");
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Schema\"]");

        selenium.waitForCondition(
                "selenium.isElementPresent(\"//div[@id='tabs']\");", "30000");

        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='membership']/ul/li[3]/a/span");

        selenium.click("//table/tbody/tr/td[3]/span/span[8]/a");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }
}
