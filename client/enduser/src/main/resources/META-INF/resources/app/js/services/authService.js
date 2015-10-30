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
        .factory('AuthService', ['$rootScope', '$resource', '$q', '$http', '$cookies',
          function ($rootScope, $resource, $q, $http, $cookies) {

            var authService = {};

            var clearUserCookie = function () {
              $rootScope.currentUser = null;
              $cookies.remove('currentUser');
            };

            authService.login = function (credentials) {
              return $http
                      .post('/syncope-enduser/api/login', credentials)
                      .then(function (response) {
                        var username = response.data;
                        $cookies.put('currentUser', username);
                        $rootScope.currentUser = username;
                        return username;
                      }, function (response) {
                        clearUserCookie();
                        console.log("Something went wrong during login, exit with status: ", response);
                        return $q.reject(response.data || response.statusText);
                      });
            };

            authService.logout = function () {
              return $http
                      .get('/syncope-enduser/api/logout')
                      .then(function (response) {
                        clearUserCookie();
                        return response;
                      }, function (response) {
                        clearUserCookie();
                        console.log("Something went wrong during logout, exit with status: ", response);
                      });
            };

            return authService;
//            return {
//              login: $resource('/syncope-enduser/api/login', {}, {
//                do: {method: 'POST', params: {}, isArray: false}
//              })
//            };
//            return {
//              logout: $resource('/cradleDashboard/api/logout', {}, {
//                query: {method: 'GET', params: {}, isArray: false}
//              })
//            };

          }]);


