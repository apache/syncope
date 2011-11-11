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
    public void createRootNodeModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");

        selenium.click("//div[3]/span/div/div/table"
                + "/tbody/tr/td[2]/table/tbody/tr/td/img");
        
        selenium.click("//div[3]/span[2]/span/div/p/span/span/a");
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

        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void browseCreateModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td/a[1]");
        
        selenium.click("//div[3]/span[2]/span/span/div/p/span/span/a[1]/");
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

        selenium.click("//div[2]/form/div[2]/ul/li[1]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[5]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[6]/a/span");
        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void browseEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td/a[1]");
        
        selenium.click("//div[3]/span[2]/span/span/div/p/span[2]/span/a[2]");
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

        selenium.click("//div[2]/form/div[2]/ul/li[1]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[5]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[6]/a/span");
        selenium.selectFrame("relative=up");
        selenium.click("css=a.w_close");
    }

    @Test
    public void checkSecurityTab() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/"
                + "td[4]/table/tbody/tr/td[2]/a");
        
        selenium.click("//div[3]/span[2]/span/span/div/p/span[2]/span/a[2]");

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

        selenium.click("//div[2]/form/div[2]/ul/li[6]/a/span");

        assertTrue(selenium.isElementPresent("//div[2]/form/div[2]/div/div[6]/"
                + "span/div/div[2]/div/label"));

        selenium.click("//div[2]/form/div[2]/ul/li[1]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[2]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[3]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[4]/a/span");
        selenium.click("//div[2]/form/div[2]/ul/li[5]/a/span");

        selenium.click("css=a.w_close");
    }

    @Test
    public void displayRoleAttributes() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td/a[1]");
        
        selenium.click("//div[3]/span[2]/span/span/div/p/span/span/a[1]/");
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

            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[1]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[2]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[3]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[4]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[5]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[6]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[7]/span");
            selenium.selectFrame("relative=up");
            selenium.click("css=a.w_close");
        }
    }

    @Test
    public void browseUserTable() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/span/div/div/table[3]/tbody/tr/td[4]/"
                + "table/tbody/tr/td/a[1]");

        selenium.click("//div[3]/span[2]/span/span/"
                + "div/form/div[2]/ul/li[7]/a/span");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent("//div[3]/span[2]/span/"
                        + "span/div/form/div[2]/div[2]/div/div/span/"
                        + "div/table/thead/tr/th/a/"
                        + "span[contains(text(),'Id')]")) {
                    break;
                }
            } catch (Exception e) {
            }

            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[1]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[2]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[3]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[4]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[5]/span");
            selenium.click("//div[3]/span[2]/"
                    + "span/span/div/form/div[2]/ul/li/a[6]/span");
        }
    }

    @Test
    public void browseUserEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        //Prende Root
        selenium.click("//div[3]/span/div/div/table[2]/tbody/"
                + "tr/td[3]/table/tbody/tr/td[2]/a/span");
        
        selenium.click("//div[3]/span[2]/span/"
                + "span/div/form/div[2]/ul/li[7]/a/span");

        selenium.click("//input[@name=\"userListContainer:search\"]");

        selenium.click("//div[3]/span[2]/span/span/div/form/div"
                + "[2]/div[2]/div/div/span/div/table/tbody/tr/td[4]/span/a");
        for (int second = 0;; second++) {
            if (second >= 60) {
                fail("timeout");
            }
            try {
                if (selenium.isElementPresent(
                        "//div[2]/form/div[2]/div/"
                        + "span/div/div/div[contains(text(),'Username')]")) {
                    break;
                }
            } catch (Exception e) {
            }
            
            selenium.selectFrame("relative=up");
            selenium.click("css=a.w_close");
        }
    }

    @Test
    public void deleteUser() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        //Remove user4
        selenium.click("//div[3]/span/div/div/table[10]/tbody/"
                + "tr/td[6]/table/tbody/tr/td[2]/a/span");
        
        selenium.click("//div[3]/span[2]/span/"
                + "span/div/form/div[2]/ul/li[7]/a/span");

        selenium.click("//input[@name=\"userListContainer:search\"]");

        selenium.click("//div[3]/span[2]/span/span/div/form/div[2]/"
                + "div[2]/div/div/span/div/table/tbody/tr/td[5]/span/a");

        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }

    @Test
    public void deleteRole() {
        selenium.setSpeed("1000");
        selenium.click("css=img[alt=\"Roles\"]");
        selenium.waitForPageToLoad("30000");
        
        //Remove managing director
        selenium.click("//div[3]/span/div/div/table[10]/tbody"
                + "/tr/td[6]/table/tbody/tr/td[2]/a");
        
        selenium.click("//div[3]/span[2]/span/span/div/p/span[2]/span/a[3]");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
    }
}