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

public class RoleTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//a[2]/span");
        selenium.click("//span[2]/span/span/span/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");
        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
        selenium.waitForPageToLoad("30000");
    }

    @Test
    public void browseEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[4]/span/span/span/a[2]/span[2]");
        selenium.click("//span[2]/span/span/span/a[2]");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");
        selenium.click("css=a.w_close");
        selenium.waitForPageToLoad("30000");
    }

    @Test
    public void delete() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[7]/span/span/span/a[2]/span[2]");
        selenium.click("//a[3]");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }

    @Test
    public void checkSecurityTab() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[9]/span/span/span/a[2]/span[2]");
        selenium.click("//span[2]/span/span/span/a[2]");

        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//span[contains(text(),'Attributes')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        assertTrue(selenium.isElementPresent("//div[@id='formtable']"));

        selenium.click("//div[@id='tabs']/ul/li[1]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[2]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[3]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[4]/a/span");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        selenium.click("css=a.w_close");
        selenium.waitForPageToLoad("30000");
    }
}
