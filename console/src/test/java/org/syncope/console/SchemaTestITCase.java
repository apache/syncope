/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.console;

import org.junit.Test;

public class SchemaTestITCase extends AbstractTest {

    @Test
    public void create() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Schema\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[3]/div/div/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//*[@id=\"_wicket_window_0\"]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent("//*[@name=\"name:textField\"]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.select("name=type:dropDownChoiceField", "value=0");
        selenium.type("name=name:textField", "newschema");
        selenium.click("name=apply");
        assertTrue(selenium.isTextPresent("newschema"));
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Schema\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='membership']/ul/li[3]/a/span");
        selenium.click("//div[3]/div[3]/div/span/table/tbody/tr/td[3]/span/a");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }
}
