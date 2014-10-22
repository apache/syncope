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
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AccessTestITCase extends AbstractTest {

    @Test
    public void clickAround() {
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Schema\"]")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']/ul/li[2]/a")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[@id='uschema']/div/div/span/ul/li[2]/a")));

        seleniumDriver.findElement(By.xpath("//div[@id='uschema']/div/div/span/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='uschema']/div/div/span/ul/li[3]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='mschema']/div/div/span/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='mschema']/div/div/span/ul/li[3]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[4]/a")).click();

        seleniumDriver.findElement(By.xpath("//div[@id='rschema']/div/div/span/ul/li[2]/a")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='rschema']/div/div/span/ul/li[3]/a")).click();

        seleniumDriver.findElement(By.xpath("//img[@alt=\"Users\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[3]/ul/li[2]/a/span")));
        seleniumDriver.findElement(By.xpath("//div[3]/ul/li[2]/a/span")).click();

        seleniumDriver.findElement(By.xpath("//img[@alt=\"Roles\"]")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt=\"Resources\"]")));

        seleniumDriver.findElement(By.xpath("//img[@alt=\"Resources\"]")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt=\"TODO\"]")));

        seleniumDriver.findElement(By.xpath("//img[@alt=\"TODO\"]")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt=\"Reports\"]")));

        seleniumDriver.findElement(By.xpath("//img[@alt=\"Reports\"]")).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//img[@alt=\"Configuration\"]")));

        seleniumDriver.findElement(By.xpath("//img[@alt=\"Configuration\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a/span")).click();

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[3]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Tasks\"]")).click();

        wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")));

        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[2]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//img[@alt=\"Tasks\"]")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[4]/a/span")).click();
        seleniumDriver.findElement(By.xpath("//div[@id='tabs']/ul/li[5]/a/span")).click();
    }
}
