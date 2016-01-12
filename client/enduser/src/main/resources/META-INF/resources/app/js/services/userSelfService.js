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

'use strict';
angular.module('login')
        .factory('UserSelfService', ['$resource', '$q', '$http',
          function ($resource, $q, $http) {

            var userSelfService = {};
            userSelfService.read = function () {
              return $http
                      .get('/syncope-enduser/api/self/read')
                      .then(function (response) {
                        console.log("response read: ", response.data);
                        return response.data;
                      }, function (response) {
                        console.log("Something went wrong during user self read, exit with status: ", response);
                        return $q.reject(response.data || response.statusText);
                      });
            };
            userSelfService.create = function (user) {
              return $http
                      .post('/syncope-enduser/api/self/create', user)
                      .then(function (response) {
                        console.log("response save: ", response);
                        var username = response;
                      }, function (response) {
                        console.log("Something went wrong during user self creation, exit with status: ", response);
                        return $q.reject(response.data || response.statusText);
                      });
            };
            userSelfService.update = function (user) {
              return $http
                      .post('/syncope-enduser/api/self/update', user)
                      .then(function (response) {
                        var username = response;
                      }, function (response) {
                        console.log("Something went wrong during user self update, exit with status: ", response);
                        return $q.reject(response.data || response.statusText);
                      });
            };
            userSelfService.passwordReset = function (user) {
              return $http
                      .post('/syncope-enduser/api/self/requestPasswordReset', user,
                              {
                                headers: {'Content-Type': 'application/x-www-form-urlencoded;charset=utf-8'},
                                transformRequest: function (obj) {
                                  var str = [];
                                  for (var p in obj)
                                    str.push(encodeURIComponent(p) + "=" + encodeURIComponent(obj[p]));
                                  return str.join("&");
                                }
                              })
                      .then(function (response) {
                        return response.data || response.statusText;
                      }, function (response) {
                        return $q.reject(response.data || response.statusText);
                      });
            };
            return userSelfService;
          }]);


