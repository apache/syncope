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

public class ConfigurationTestITCase extends AbstractTest {

    @Test
    public void browseCreateModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//a[contains(text(),'Create new configuration')]");
        assertTrue(selenium.isElementPresent("//input[@name='key:textField']"));
        selenium.type("name=key:textField", "test1");
        selenium.type("name=value:textField", "value1");
        selenium.click("name=apply");
        assertTrue(selenium.isTextPresent("Operation executed successfully"));
    }

    @Test
    public void browseEditModal() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//td[3]/span/a");
        assertTrue(selenium.isElementPresent("//input[@name='key:textField']"));
        assertEquals("connid.bundles.directory",
                selenium.getAttribute("//input[@name='key:textField']@value"));
        selenium.click("css=a.w_close");

    }

    @Test
    public void browsePasswordPolicy() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/ul/li[2]/a");
        selenium.click("//div[3]/div[2]/span/div/a");

        assertTrue(selenium.isElementPresent("//input[@name='id:textField']"));
        selenium.type("name=description:textField", "new description");
        selenium.click("//div[2]/form/div[3]/input");
        assertTrue(selenium.isTextPresent("new description"));
    }

    @Test
    public void browseAccountPolicy() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[3]/ul/li[3]/a");
        selenium.click("//div[3]/div[3]/span/div/a");

        assertTrue(selenium.isElementPresent("//input[@name='id:textField']"));
        selenium.type("name=description:textField", "new description");
        selenium.click("//div[2]/form/div[3]/input");
        assertTrue(selenium.isTextPresent("new description"));
    }

    @Test
    public void browseWorkflowDef() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[@id='tabs']/ul/li[5]/a/span");
        assertTrue(selenium.isElementPresent("//*[@id=\"workflowDefArea\"]"));
    }

    @Test
    public void delete() {
        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//tr[8]/td[4]/span/a");
        assertTrue(selenium.getConfirmation().matches(
                "^Do you really want to delete the selected item[\\s\\S]$"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        assertTrue(selenium.isTextPresent("Operation executed successfully"));
    }

    @Test
    public void setLogLevel() {
        selenium.setSpeed("1000");

        selenium.click("css=img[alt=\"Configuration\"]");
        selenium.waitForPageToLoad("30000");
        selenium.click("//div[@id='tabs']/ul/li[6]/a/span");
        selenium.select(
                "//div[6]/div/span/table/tbody/tr/td[2]/select", "label=ERROR");
        assertTrue(selenium.isTextPresent("Operation executed successfully"));
    }
}
