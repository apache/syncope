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

var abstract = require('./abstract.js');

describe('syncope enduser user password reset ', function () {
  it('should reset password for user donizetti', function () {
    console.log("");
    console.log("user password reset");
    abstract.goHome();

    abstract.waitSpinner();
    element(by.id('passwordreset')).click();
    abstract.waitSpinner();

    var user = element(by.model('user.username'));
    user.click();
    user.sendKeys('donizetti');
    user.click();
    element(by.model('user.username')).sendKeys(protractor.Key.TAB);

    var secQuest = element(by.model('userSecurityQuestion'));
    expect(secQuest.isEnabled()).toBe(false);
    expect(element(by.model('userSecurityQuestion')).getAttribute('value')).toEqual('What\'s your mother\'s maiden name?');
    abstract.waitSpinner();

    var secAns = element(by.model('user.securityAnswer'));
    var EC = protractor.ExpectedConditions;
    browser.wait(EC.presenceOf(secAns), 5000).then(function () {
      secAns.click();
      secAns.sendKeys('Domenica Oliva Nava');
    }, function (error) {
      expect(true).toBe(false);
    });

    element.all(by.id('resetpassword')).last().click();
  });
});