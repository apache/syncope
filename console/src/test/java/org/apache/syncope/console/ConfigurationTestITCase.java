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

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class ConfigurationTestITCase extends AbstractTest {

    @Test
    public void editParameters() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@title='Parameters']")));

        seleniumDriver.findElement(By.xpath("//img[@title='Parameters']/ancestor::a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        
        seleniumDriver.findElement(
                By.xpath("//span[contains(text(), 'log.lastlogindate')]/../../div[2]/span/input")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/input[@type='submit']")).click();
        
        seleniumDriver.switchTo().defaultContent();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(
                seleniumDriver.findElement(By.tagName("body")).getText().contains("Operation executed successfully"));
    }

    @Test
    public void browsePasswordPolicy() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='policies']/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='password']/span/div/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='id:textField']")));

        WebElement element = seleniumDriver.findElement(By.name("description:textField"));
        element.sendKeys("new description");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/input[@type='submit']")).click();
        seleniumDriver.switchTo().defaultContent();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(
                seleniumDriver.findElement(By.tagName("body")).getText().contains("Operation executed successfully"));
    }

    @Test
    public void browseWorkflowDef() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[5]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='workflow']/div/span/img")));
    }

    @Test
    public void setLogLevel() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[6]/a")).click();

        final Select select = new Select(
                seleniumDriver.findElement(By.xpath("//div[@id='core']/div/span/table/tbody/tr/td[2]/select")));
        select.selectByVisibleText("ERROR");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='workflow']/div/span/img")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("feedback")));
        assertTrue(
                seleniumDriver.findElement(By.tagName("body")).getText().contains("Operation executed successfully"));
    }

    @Test
    public void createNotification() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a")).click();

        seleniumDriver.findElement(By.xpath("//div[@id='notifications']/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[3]/div/div/div/div/label")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='sender:textField']")));

        seleniumDriver.findElement(By.name("sender:textField")).sendKeys("test@syncope.it");

        seleniumDriver.findElement(By.name("subject:textField")).sendKeys("test@syncope.it");

        Select select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[3]/div[2]/span/select")));
        select.selectByVisibleText("UserSchema");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[3]/div/div/div[4]/div[2]/span/select/option[2]")));

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[4]/div[2]/span/select")));
        select.selectByVisibleText("fullname");

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[5]/div[2]/span/select")));
        select.selectByVisibleText("optin");

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[6]/div[2]/span/select")));
        select.selectByVisibleText("ALL");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[3]/a/span")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[2]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='eventSelection:categoryContainer:type:dropDownChoiceField']"
                        + "/option[text()='PROPAGATION']")));

        select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='eventSelection:categoryContainer:type:dropDownChoiceField']")));
        select.selectByVisibleText("PROPAGATION");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='eventSelection:categoryContainer:category:dropDownChoiceField']"
                        + "/option[text()='role']")));

        select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='eventSelection:categoryContainer:category:dropDownChoiceField']")));
        select.selectByVisibleText("role");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='eventSelection:categoryContainer:subcategory:dropDownChoiceField']"
                        + "/option[text()='resource-db-sync']")));

        select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='eventSelection:categoryContainer:subcategory:dropDownChoiceField']")));
        select.selectByVisibleText("resource-db-sync");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@name='eventSelection:eventsContainer:eventsPanel:failureGroup']")));

        seleniumDriver.findElement(By.xpath("//div[@class='eventSelectionWidzard']/div[2]/div[3]/span/div/input")).
                click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[4]/a")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div[4]/div/div/span/input")).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.name(
                "staticRecipients:multiValueContainer:view:0:panel:textField")));

        seleniumDriver.findElement(By.name(
                "staticRecipients:multiValueContainer:view:0:panel:textField")).
                sendKeys("syncope445@syncope.apache.org");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[3]/div[4]/div/div[2]/label")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[4]/input")).click();

        seleniumDriver.switchTo().defaultContent();
    }

    @Test
    public void createDisabledNotification() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a")).click();

        seleniumDriver.findElement(By.xpath("//div[@id='notifications']/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[3]/div/div/div/div/label")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='sender:textField']")));

        seleniumDriver.findElement(By.name("sender:textField")).sendKeys("test@syncope.it");

        Select select = new Select(seleniumDriver.findElement(
                By.xpath("//div[2]/form/div[3]/div/div[1]/div[3]/div[2]/span/select")));
        select.selectByVisibleText("UserSchema");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[2]/form/div[3]/div/div/div[4]/div[2]/span/select/option[2]")));

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[4]/div[2]/span/select")));
        select.selectByVisibleText("fullname");

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[5]/div[2]/span/select")));
        select.selectByVisibleText("optin");

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[6]/div[2]/span/select")));
        select.selectByVisibleText("ALL");

        // disable notification
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[7]/div[2]/span/input")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[3]/a/span")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[2]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//select[@name='eventSelection:categoryContainer:type:dropDownChoiceField']"
                + "/option[text()='PROPAGATION']")));

        select = new Select(
                seleniumDriver.findElement(By.xpath(
                                "//select[@name='eventSelection:categoryContainer:type:dropDownChoiceField']")));
        select.selectByVisibleText("PROPAGATION");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//select[@name='eventSelection:categoryContainer:category:dropDownChoiceField']"
                + "/option[text()='role']")));

        select = new Select(
                seleniumDriver.findElement(By.xpath(
                                "//select[@name='eventSelection:categoryContainer:category:dropDownChoiceField']")));
        select.selectByVisibleText("role");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//select[@name='eventSelection:categoryContainer:subcategory:dropDownChoiceField']"
                + "/option[text()='resource-db-sync']")));

        select = new Select(
                seleniumDriver.findElement(By.xpath(
                                "//select[@name='eventSelection:categoryContainer:subcategory:dropDownChoiceField']")));
        select.selectByVisibleText("resource-db-sync");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//input[@name='eventSelection:eventsContainer:eventsPanel:failureGroup']")));

        seleniumDriver.findElement(By.xpath("//div[@class='eventSelectionWidzard']/div[2]/div[3]/span/div/input")).
                click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[4]/a/span")).click();

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div[4]/div/div/span/input")).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.name(
                "staticRecipients:multiValueContainer:view:0:panel:textField")));

        seleniumDriver.findElement(
                By.name("staticRecipients:multiValueContainer:view:0:panel:textField"))
                .sendKeys("syncope492@syncope.apache.org");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//div[2]/form/div[3]/div[4]/div/div[2]/label")));

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[4]/input")).click();

        seleniumDriver.switchTo().defaultContent();
    }

    @Test
    public void issueSYNCOPE446() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a")).click();

        seleniumDriver.findElement(By.xpath("//div[@id='notifications']/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//iframe")));
        seleniumDriver.switchTo().frame(0);

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[2]/form/div[3]/div/div/div/div/label")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//input[@name='sender:textField']")));

        seleniumDriver.findElement(By.name("sender:textField")).sendKeys("syncope446@syncope.it");
        seleniumDriver.findElement(By.name("subject:textField")).sendKeys("Test issue Syncope 446");

        Select select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[3]/div[2]/span/select")));
        select.selectByVisibleText("UserSchema");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[2]/form/div[3]/div/div/div[4]/div[2]/span/select/option[2]")));

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[4]/div[2]/span/select")));
        select.selectByVisibleText("email");

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[5]/div[2]/span/select")));
        select.selectByVisibleText("optin");

        select = new Select(
                seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div/div/div[6]/div[2]/span/select")));
        select.selectByVisibleText("ALL");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[2]/a/span")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='eventSelection:categoryContainer:type:dropDownChoiceField']"
                        + "/option[text()='REST']")));

        select = new Select(
                seleniumDriver.findElement(By.xpath(
                                "//select[@name='eventSelection:categoryContainer:type:dropDownChoiceField']")));
        select.selectByVisibleText("REST");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='eventSelection:categoryContainer:category:dropDownChoiceField']"
                        + "/option[text()='RoleController']")));

        select = new Select(
                seleniumDriver.findElement(By.xpath(
                                "//select[@name='eventSelection:categoryContainer:category:dropDownChoiceField']")));
        select.selectByVisibleText("RoleController");

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@name='eventSelection:eventsContainer:eventsPanel:successGroup']")));

        seleniumDriver.findElement(
                By.xpath("//div[@class='eventSelectionWidzard']/div[2]/div[3]/span/div/input")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/div[3]/span/div[4]/div/span/input")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//select[@name='aboutContainer:roleAbout:searchFormContainer:searchView:0:type']"
                        + "/option[text()='ENTITLEMENT']")));

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//select[@name='aboutContainer:roleAbout:searchFormContainer:searchView:0:type']")));

        select = new Select(
                seleniumDriver.findElement(By.xpath(
                                "//select[@name='aboutContainer:roleAbout:searchFormContainer:searchView:0:type']")));
        select.selectByVisibleText("ENTITLEMENT");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(
                "//select[@name='aboutContainer:roleAbout:searchFormContainer:searchView:0:property']"
                + "/option[text()='ROLE_CREATE']")));

        select = new Select(seleniumDriver.findElement(By.xpath(
                "//select[@name='aboutContainer:roleAbout:searchFormContainer:searchView:0:property']")));
        select.selectByVisibleText("ROLE_CREATE");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[3]/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//input[@name='recipientsContainer:checkRecipients:checkboxField']")).
                click();

        wait.until(ExpectedConditions.elementToBeClickable(By.name(
                "staticRecipients:multiValueContainer:view:0:panel:textField")));

        seleniumDriver.findElement(By.name("staticRecipients:multiValueContainer:view:0:panel:textField")).
                sendKeys("syncope446@syncope.apache.org");

        seleniumDriver.findElement(By.xpath("//div[2]/form/div[4]/input")).click();

        seleniumDriver.switchTo().defaultContent();
    }
}
