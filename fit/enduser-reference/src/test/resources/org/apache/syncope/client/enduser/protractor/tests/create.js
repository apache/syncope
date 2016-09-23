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

describe('syncope enduser user create', function () {

  it('should create user donizetti', function () {
    console.log("create user test");
    abstract.goHome();

    element(by.id('register')).click();
    element(by.model('user.username')).sendKeys('donizetti');
    element(by.model('user.password')).sendKeys('password123');
    element(by.model('confirmPassword.value')).sendKeys('password123');

    element(by.cssContainingText('option', 'What\'s your mother\'s maiden name?')).click();

    browser.wait(element(by.id('user.securityAnswer')).isPresent());
    element(by.model('user.securityAnswer')).sendKeys('Agata Ferlito');

    element.all(by.id('next')).first().click();
    element.all(by.id('next')).first().click();

    element.all(by.name('fullname')).first().sendKeys('Gaetano Donizetti');
    element.all(by.name('userId')).first().sendKeys('donizetti@apache.org');
    element.all(by.name('firstname')).first().sendKeys('Gaetano');
    element.all(by.name('surname')).first().sendKeys('Donizetti');

    element.all(by.id('next')).first().click();
    element.all(by.id('next')).first().click();
    element.all(by.id('next')).first().click();
    element.all(by.id('next')).first().click();
    element.all(by.id('save')).last().click();
  });
});