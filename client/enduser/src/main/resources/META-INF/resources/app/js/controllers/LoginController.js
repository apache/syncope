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
angular.module("login").controller("LoginController", ['$scope', '$rootScope', '$http', '$location', 'AuthService',
  function ($scope, $rootScope, $http, $location, AuthService) {

    $scope.credentials = {
      username: '',
      password: '',
      errorMessage: ''
    };

    $scope.login = function (credentials) {
      AuthService.login($scope.credentials).then(function (user) {
        console.info("Login success for: ", user);
        // reset error message
        $scope.credentials.errorMessage = '';
        // reset SAML 2.0 entityID
        $rootScope.saml2idps.selected.entityID = null;
        // got to update page
        $location.path("/self/update");
      }, function (response) {
        console.info("Login failed for: ", response);
        var errorMessage;
        // parse error response 
        if (response !== undefined) {
          errorMessage = response.split("ErrorMessage{{")[1];
          errorMessage = errorMessage.split("}}")[0];
        }
        $scope.credentials.errorMessage = "Login failed: " + errorMessage;
        $scope.showError($scope.credentials.errorMessage, $scope.notification);
      });
    };

    $scope.logout = function () {
      window.location.href = '../wicket/bookmarkable/org.apache.syncope.client.enduser.pages.Logout';
    };

    $scope.islogged = function () {
      AuthService.islogged().then(function (response) {
        console.debug("user login status detected", response);
        return response.data === true;
      }, function (response) {
        console.error("error retrieving user login status", response);
      });
    };
    $scope.selfCreate = function () {
      $location.path("/self/create");
    };
    $scope.passwordReset = function () {
      $location.path("/passwordreset");
    };
    $scope.errorAPI = function () {
      $http.get("/syncope-enduser/api/error").success(function (data) {
        console.debug("errorAPI response: ", data);
      });
    };
    $scope.sampleAPI = function () {
      $http.get("/syncope-enduser/api/user-self").success(function (data) {
        console.debug("sampleAPI response: ", data);
      });
    };
    $scope.schemaAPI = function () {
      $http.get("/syncope-enduser/api/schema").success(function (data) {
        console.debug("schemaAPI response: ", data);
      });
    };
    $scope.$watch(function () {
      return $location.search().errorMessage;
    }, function (errorMessage) {
      if (errorMessage) {
        var message = (' ' + errorMessage).slice(1);
        $scope.showError(message, $scope.notification);
        delete $location.$$search.errorMessage;
      }
    });
  }]);
