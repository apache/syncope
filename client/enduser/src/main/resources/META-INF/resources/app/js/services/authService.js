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
        .factory('AuthService', ['$rootScope', '$resource', '$q', '$http',
          function ($rootScope, $resource, $q, $http) {

            var authService = {};
            authService.login = function (credentials) {
              return $http
                      .post('../api/login', credentials)
                      .then(function (response) {
                        return response.data;
                      }, function (response) {
                        console.error("Something went wrong during login, exit with status: ", response.statusText);
                        return $q.reject(response.data || response.statusText);
                      });
            };

            authService.islogged = function () {
              return $http
                      .get('../api/self/islogged')
                      .then(function (response) {
                        return response.data;
                      }, function (response) {
                        console.error("error retrieving user login status");
                      });
            };

            return authService;
          }]);
