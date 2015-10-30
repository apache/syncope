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

angular.module("login").controller("LoginController", ['$scope', '$rootScope', '$http', '$location', '$cookies',
  'AuthService', 'growl', function ($scope, $rootScope, $http, $location, $cookies, AuthService, growl) {

    $scope.credentials = {
      username: '',
      password: '',
      errorMessage: ''
    };

    $scope.login = function (credentials) {

      console.log("CREDENTIALS FROM PAGE: ", credentials);
      console.log("AUTHSERVICE: ", AuthService);

      AuthService.login($scope.credentials).then(function (user) {
        console.log("LOGIN SUCCESS FOR: ", user);
        console.log("DOPO AVER SETTATO CURRENT USER: ", $rootScope.currentUser);
        console.log("COOKIE CURRENT USER: ", $cookies.get('currentUser'));
        // reset error message
        $scope.credentials.errorMessage = '';
        // got to update page
        $location.path("/self/update");
      }, function (response) {
        console.log("LOGIN FAILED: ", response);
        $scope.credentials.errorMessage = "Login failed: " + response;
        growl.error($scope.credentials.errorMessage, {referenceId: 1});
      });
    };

    $scope.logout = function () {

      console.log("PERFORMING LOGOUT");

      AuthService.logout().then(function (response) {
        console.log("LOGOUT SUCCESS: ", response);
      }, function () {
        console.log("LOGOUT FAILED");
      });
    };

    $scope.isLogged = function () {
      return angular.isDefined($rootScope.currentUser) && $rootScope.currentUser;
    };

    $scope.selfCreate = function () {
      $location.path("/self/create");
    };

    $scope.passwordReset = function () {
      // TODO
      console.log("NOT YET IMPLEMENTED")
    };

    $scope.errorAPI = function () {
      $http.get("/syncope-enduser/api/error").success(function (data) {
        console.log("errorAPI response: ", data);
      });
    };

    $scope.sampleAPI = function () {
      $http.get("/syncope-enduser/api/user-self").success(function (data) {
        console.log("sampleAPI response: ", data);
      });
    };

    $scope.schemaAPI = function () {
      $http.get("/syncope-enduser/api/schema").success(function (data) {
        console.log("schemaAPI response: ", data);
      });
    };

  }]);
