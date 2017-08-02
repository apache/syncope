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
describe('syncope enduser user edit', function () {
  it('should edit user', function () {
    console.log("");
    console.log("user edit");
    abstract.goHome();

    //login
    element(by.model('credentials.username')).sendKeys('bellini');
    element(by.model('credentials.password')).sendKeys('password');
    element.all(by.options('language.name for language in languages.availableLanguages track by language.id')).
            then(function (language) {
              expect(language.length).toBe(3);
            });
    element.all(by.options('language.name for language in languages.availableLanguages track by language.id')).
            get(1).click();
    element(by.id('login-btn')).click();

    //credential
    abstract.waitSpinner();
    browser.wait(element(by.model('user.username')).isPresent());
    element(by.model('user.username')).clear();
    element(by.model('user.username')).sendKeys('bellini');
    element(by.model('user.password')).clear();
    element(by.model('user.password')).sendKeys('Password123');
    element(by.model('confirmPassword.value')).sendKeys('Password123');
    var secQuestion = element(by.model('user.securityQuestion'));
    var selectedSecQuestion = secQuestion.all(by.options
            ('securityQuestion.key as securityQuestion.content for securityQuestion in availableSecurityQuestions'))
            .last();
    selectedSecQuestion.click();
    element(by.model('user.securityAnswer')).sendKeys('Agata Ferlito');
    abstract.doNext();

    //groups
    browser.wait(element(by.model('user.realm')).isPresent());
    element(by.model('user.realm')).click();
    element.all(by.repeater('realm in availableRealms')).get(0).click();
    var group = element(by.model('dynamicForm.selectedGroups'));
    var selectedGroup = group.element(by.css('.ui-select-search'));
    group.click();
    //adds group root
    selectedGroup.sendKeys('root');
    element.all(by.css('.ui-select-choices-row-inner span')).first().click();
    abstract.waitSpinner();
    abstract.doNext();

    //plainSchemas
    element.all(by.repeater('groupSchema in dynamicForm.groupSchemas')).then(function (groupSchema) {
      expect(groupSchema.length).toBe(1);
    });
    element.all(by.css('[name="fullname"]')).first().clear();
    element.all(by.css('[name="fullname"]')).first().sendKeys('Vincenzo Bellini');
    element.all(by.css('[name="userId"]')).first().clear();
    element.all(by.css('[name="userId"]')).first().sendKeys('bellini@apache.org');
    element.all(by.css('[name="firstname"]')).first().clear();
    element.all(by.css('[name="firstname"]')).first().sendKeys('Vincenzo');
    element.all(by.css('[name="ctype"]')).first().clear();
    element.all(by.css('[name="ctype"]')).first().sendKeys('bellinictype');
    abstract.doNext();
    //derSchemas
    abstract.doNext();
    //virSchemas
    abstract.doNext();
    //Resources
    abstract.doNext();
    //Captcha
    abstract.waitSpinner();
    element.all(by.id('save')).last().click();
    abstract.waitSpinner();
  });
});

