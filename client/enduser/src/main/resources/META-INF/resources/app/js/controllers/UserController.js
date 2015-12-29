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

angular.module("self").controller("UserController", ['$scope', '$rootScope', '$location', '$compile', 'AuthService',
  'UserSelfService', 'SchemaService', 'RealmService', 'SecurityQuestionService', 'CaptchaService', 'growl', function ($scope,
          $rootScope, $location, $compile, AuthService, UserSelfService, SchemaService, RealmService, SecurityQuestionService,
          CaptchaService, growl) {

    $scope.user = {};
    $scope.confirmPassword = {
      value: ''
    };
    $scope.userFormValid = false;
    $scope.createMode = $location.path().indexOf("/self/create") > -1;

    $scope.availableRealms = [];
    $scope.availableSecurityQuestions = [];

    $scope.initialSecurityQuestion = undefined;
    $scope.captchaInput = {
      value: ""
    };

    $scope.initUser = function () {

      $scope.dynamicForm = {
        plainSchemas: [],
        derSchemas: [],
        virSchemas: [],
        selectedDerSchemas: [],
        selectedVirSchemas: [],
        errorMessage: '',
        attributeTable: {}
      };

      var initSchemas = function () {
        // initialization is done here synchronously to have all schema fields populated correctly
        SchemaService.getUserSchemas().then(function (schemas) {
          $scope.dynamicForm.plainSchemas = schemas.plainSchemas;
          $scope.dynamicForm.derSchemas = schemas.derSchemas;
          $scope.dynamicForm.virSchemas = schemas.virSchemas;

          // initialize plain attributes
          for (var i = 0; i < schemas.plainSchemas.length; i++) {

            var plainSchemaKey = schemas.plainSchemas[i].key;

            if (!$scope.user.plainAttrs[plainSchemaKey]) {

              $scope.user.plainAttrs[plainSchemaKey] = {
                schema: plainSchemaKey,
                values: [],
                readonly: schemas.plainSchemas[i].readonly
              };

              // initialize multivalue schema and support table: create mode, only first value
              if (schemas.plainSchemas[i].multivalue) {
                $scope.dynamicForm.attributeTable[schemas.plainSchemas[i].key] = {
                  fields: [schemas.plainSchemas[i].key + "_" + 0]
                };
              }
            } else {
              // initialize multivalue schema and support table: update mode, all provided values
              if (schemas.plainSchemas[i].multivalue) {
                $scope.dynamicForm.attributeTable[schemas.plainSchemas[i].key] = {
                  fields: [schemas.plainSchemas[i].key + "_" + 0]
                };
                // add other values
                for (var j = 1; j < $scope.user.plainAttrs[plainSchemaKey].values.length; j++) {
                  $scope.dynamicForm.attributeTable[schemas.plainSchemas[i].key].fields.push(schemas.plainSchemas[i].key + "_" + j);
                }
              }
            }
          }

          // initialize derived attributes
          for (var i = 0; i < schemas.derSchemas.length; i++) {

            var derSchemaKey = schemas.derSchemas[i].key;

            if ($scope.user.derAttrs[derSchemaKey]) {
              $scope.dynamicForm.selectedDerSchemas.push(schemas.derSchemas[i]);
            }
          }

          // initialize virtual attributes
          for (var i = 0; i < schemas.virSchemas.length; i++) {

            var virSchemaKey = schemas.virSchemas[i].key;

            if ($scope.user.virAttrs[virSchemaKey]) {
              $scope.dynamicForm.selectedVirSchemas.push(schemas.virSchemas[i]);
            }
          }

        }, function (response) {
          var errorMessage;
          // parse error response 
          if (response !== undefined) {
            errorMessage = response.split("ErrorMessage{{")[1];
            errorMessage = errorMessage.split("}}")[0];
          }
          console.log("Error retrieving user schemas: ", errorMessage);
        });
        console.log("USER WITH ATTRTO: ", $scope.user);

      };

      var initSecurityQuestions = function () {
        SecurityQuestionService.getAvailableSecurityQuestions().then(function (response) {
          $scope.availableSecurityQuestions = response;
        }, function (response) {
          var errorMessage;
          // parse error response 
          if (response !== undefined) {
            errorMessage = response.split("ErrorMessage{{")[1];
            errorMessage = errorMessage.split("}}")[0];
          }
          console.log("Error retrieving avaliable security questions: ", errorMessage);
        });
      };

      var initRealms = function () {
        $scope.availableRealms = RealmService.getAvailableRealmsStub();
      };

      var initUserRealm = function () {
        $scope.user.realm = RealmService.getUserRealm();
      };


      var readUser = function () {
        UserSelfService.read().then(function (response) {
          $scope.user = response;
          $scope.user.password = undefined;
          $scope.initialSecurityQuestion = $scope.user.securityQuestion;
        }, function () {
          console.log("Error");
        });
      };

      if ($scope.createMode) {

        $scope.user = {
          username: '',
          password: '',
          realm: '',
          securityQuestion: undefined,
          securityAnswer: '',
          plainAttrs: {},
          derAttrs: {},
          virAttrs: {}
        };

        // retrieve user realm or all available realms
        initUserRealm();

      } else {

        // read user from syncope core
        readUser();
        // read user security question

      }



      initRealms();
      //retrieve security available questions
      initSecurityQuestions();
      // initialize user attributes starting from any object schemas
      initSchemas();
    };

    $scope.saveUser = function (user) {
      console.log("Save user: ", user);
      // validate captcha and then save user
      CaptchaService.validate($scope.captchaInput).then(function (response) {
        if (!(response === 'true')) {
          growl.error("Captcha inserted is not valid, please digit the correct captcha", {referenceId: 2});
          return;
        }

        if ($scope.createMode) {

          UserSelfService.create(user).then(function (response) {
            console.log("Created user: ", response);
            growl.success("User " + $scope.user.username + " successfully created", {referenceId: 1});
            $location.path('/self');
          }, function (response) {
            console.log("Error during user creation: ", response);
            var errorMessage;
            // parse error response 
            if (response !== undefined) {
              errorMessage = response.split("ErrorMessage{{")[1];
              errorMessage = errorMessage.split("}}")[0];
            }
            growl.error("Error: " + (errorMessage || response), {referenceId: 2});
          });

        } else {

          UserSelfService.update(user).then(function (response) {
            console.log("Updated user: ", response);
            AuthService.logout().then(function (response) {
              console.log("LOGOUT SUCCESS: ", response);
              $location.path('/self');
              growl.success("User " + $scope.user.username + " successfully updated", {referenceId: 1});
            }, function () {
              console.log("LOGOUT FAILED");
            });
          }, function (response) {
            console.log("Error during user update: ", response);
            var errorMessage;
            // parse error response 
            if (response !== undefined) {
              errorMessage = response.split("ErrorMessage{{")[1];
              errorMessage = errorMessage.split("}}")[0];
            }
            growl.error("Error: " + (errorMessage || response), {referenceId: 2});
          });
        }
      }, function (response) {
        console.log("Error during validate captcha ", response);
        var errorMessage;
        // parse error response 
        if (response !== undefined) {
          errorMessage = response.split("ErrorMessage{{")[1];
          errorMessage = errorMessage.split("}}")[0];
        }
        growl.error("Error: " + (errorMessage || response), {referenceId: 2});
        return;
      });
    };
  }]);
