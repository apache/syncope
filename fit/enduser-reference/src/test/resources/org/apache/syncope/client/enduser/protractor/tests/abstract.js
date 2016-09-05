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

var home = 'http://localhost:9080/syncope-enduser/app/';

exports.doCreate = function () {
  describe('doCreate', function () {

    it('create donizetti', function () {
      browser.get(home);

      browser.wait(element(by.id('register')).isPresent());
      element(by.id('register')).click();

      element(by.model('user.username')).sendKeys('donizetti');
      element(by.model('user.password')).sendKeys('Password123');
      element(by.model('confirmPassword.value')).sendKeys('Password123');

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

      element(by.id('save')).click();
     
    });
  });
}

exports.doLogin = function (username, password) {
  describe('doLogin', function () {

    it('login user ', function () {
      browser.get(home);

      element(by.model('credentials.username')).sendKeys(username);
      element(by.model('credentials.password')).sendKeys(password);
      element(by.id('login-btn')).click();

      expect(element(by.model('user.username')).getAttribute('value')).toEqual(username);

      element(by.id('logout')).click();
    });
  });
}


