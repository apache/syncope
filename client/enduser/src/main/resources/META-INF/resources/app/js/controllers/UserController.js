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
  'UserSelfService', 'SchemaService', 'RealmService', 'ResourceService', 'SecurityQuestionService', 'GroupService',
  'AnyService', 'UserUtil', 'GenericUtil', "ValidationExecutor",
  function ($scope, $rootScope, $location, $compile, AuthService, UserSelfService, SchemaService, RealmService,
          ResourceService, SecurityQuestionService, GroupService, AnyService, UserUtil, GenericUtil, ValidationExecutor) {

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
        resources: [],
        groups: [],
        auxClasses: [],
        anyUserType: [],
        errorMessage: '',
        attributeTable: {},
        virtualAttributeTable: {},
        selectedResources: [],
        selectedGroups: [],
        selectedAuxClasses: []
      };

      var initUserSchemas = function (anyTypeClass) {
        // initialization is done here synchronously to have all schema fields populated correctly
        SchemaService.getUserSchemas(anyTypeClass).then(function (schemas) {
          //initializing user schemas values
          initSchemaValues(schemas);
        }, function (response) {
          var errorMessage;
          // parse error response 
          if (response !== undefined) {
            errorMessage = response.split("ErrorMessage{{")[1];
            errorMessage = errorMessage.split("}}")[0];
          }
          console.error("Error retrieving user schemas: ", errorMessage);
        });
      };

      var initSchemaValues = function (schemas) {
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
          } else if (schemas.plainSchemas[i].multivalue) {
            // initialize multivalue schema and support table: update mode, all provided values
            $scope.dynamicForm.attributeTable[schemas.plainSchemas[i].key] = {
              fields: [schemas.plainSchemas[i].key + "_" + 0]
            };
            // add other values
            for (var j = 1; j < $scope.user.plainAttrs[plainSchemaKey].values.length; j++) {
              $scope.dynamicForm.attributeTable[schemas.plainSchemas[i].key].fields.push(schemas.plainSchemas[i].key + "_" + j);
            }
          }
        }

        // initialize derived attributes
        for (var i = 0; i < schemas.derSchemas.length; i++) {

          var derSchemaKey = schemas.derSchemas[i].key;

          if (!$scope.user.derAttrs[derSchemaKey]) {

            $scope.user.derAttrs[derSchemaKey] = {
              schema: derSchemaKey,
              values: [],
              readonly: true
            };

          }
        }

        // initialize virtual attributes
        for (var i = 0; i < schemas.virSchemas.length; i++) {

          var virSchemaKey = schemas.virSchemas[i].key;

          if (!$scope.user.virAttrs[virSchemaKey]) {

            $scope.user.virAttrs[virSchemaKey] = {
              schema: virSchemaKey,
              values: [],
              readonly: schemas.virSchemas[i].readonly
            };
            // initialize multivalue schema and support table: create mode, only first value
            $scope.dynamicForm.virtualAttributeTable[schemas.virSchemas[i].key] = {
              fields: [schemas.virSchemas[i].key + "_" + 0]
            };
          } else {
            // initialize multivalue schema and support table: update mode, all provided values
            $scope.dynamicForm.virtualAttributeTable[schemas.virSchemas[i].key] = {
              fields: [schemas.virSchemas[i].key + "_" + 0]
            };
            // add other values
            for (var j = 1; j < $scope.user.virAttrs[virSchemaKey].values.length; j++) {
              $scope.dynamicForm.virtualAttributeTable[schemas.virSchemas[i].key].fields.push(schemas.virSchemas[i].key + "_" + j);
            }
          }
        }
        //appending new schemas
        $scope.dynamicForm.plainSchemas = $scope.dynamicForm.plainSchemas.concat(schemas.plainSchemas);
        $scope.dynamicForm.derSchemas = $scope.dynamicForm.derSchemas.concat(schemas.derSchemas);
        $scope.dynamicForm.virSchemas = $scope.dynamicForm.virSchemas.concat(schemas.virSchemas);
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
          console.error("Error retrieving avaliable security questions: ", errorMessage);
        });
      };

      var initRealms = function () {
        $scope.availableRealms = RealmService.getAvailableRealmsStub();
      };

      var initUserRealm = function () {
        $scope.user.realm = RealmService.getUserRealm();
      };

      var initResources = function () {
        ResourceService.getResources().then(function (response) {
          for (var i in response) {
            $scope.dynamicForm.resources.push(response[i].key);
          }
        });
      };

      var initGroups = function () {
        var realm = $scope.user.realm || "/";
        GroupService.getGroups(realm).then(function (response) {
          $scope.dynamicForm.groups = new Array();
          for (var i in response) {
            $scope.dynamicForm.groups.push({"rightKey": response[i].key, "groupName": response[i].name});
          }
        }, function (e) {
          $scope.showError("An error occur during retrieving groups " + e, $scope.notification)
        });
      };

      var initAuxClasses = function () {

        //fetching default user classes, that should remain in any case
        AnyService.getAnyType("USER").then(function (response) {
          $scope.dynamicForm.anyUserType = response.classes;
          AnyService.getAuxClasses().then(function (response) {
            for (var i = 0; i < response.length; i++) {
              //we should only add schemas that aren't in the anyUserType
              if ($scope.dynamicForm.anyUserType.indexOf(response[i].key) == -1) {
                $scope.dynamicForm.auxClasses.push(response[i].key);
              }
            }
          }, function (e) {
            $scope.showError("An error occur during retrieving auxiliary classes " + e, $scope.notification)
          });
        }, function (e) {
          $scope.showError("An error occur during retrieving auxiliary classes " + e, $scope.notification)
        });
      };

      var initProperties = function () {
        initRealms();
        //retrieve security available questions
        initSecurityQuestions();
        //initialize available groups
        initGroups();
        //initialize available auxiliary classes
        initAuxClasses();
        // initialize user attributes starting from any object schemas
        initUserSchemas();
        // initialize available resources
        initResources();
      };

      var readUser = function () {
        UserSelfService.read().then(function (response) {
          $scope.user = UserUtil.getUnwrappedUser(response);
          $scope.user.password = undefined;
          $scope.initialSecurityQuestion = $scope.user.securityQuestion;
          // initialize already assigned resources
          $scope.dynamicForm.selectedResources = $scope.user.resources;
          // initialize already assigned groups -- keeping the same structure of groups       
          for (var index in $scope.user.memberships) {
            $scope.dynamicForm.selectedGroups.push(
                    {
                      "rightKey": $scope.user.memberships[index]["rightKey"].toString(),
                      "groupName": $scope.user.memberships[index]["groupName"]
                    });
          }
          //initialize already assigned auxiliary classes
          $scope.dynamicForm.selectedAuxClasses = $scope.user.auxClasses;
          //we need to initialize axiliar attribute schemas
          for (var index in $scope.user.auxClasses) {
            $scope.$emit("auxClassAdded", $scope.user.auxClasses[index]);
          }
          if ($scope.user.mustChangePassword) {
            $location.path('/mustchangepassword')
          } else {
            initProperties();
          }
        }, function (e) {
          console.error("Error during user read ", e);
        });
      };

      var removeUserSchemas = function (anyTypeClass) {

        //removing plain schemas
        for (var i = 0; i < $scope.dynamicForm.plainSchemas.length; i++) {
          if ($scope.dynamicForm.plainSchemas[i].anyTypeClass == anyTypeClass) {
            //cleaning both form and user model
            delete $scope.user.plainAttrs[$scope.dynamicForm.plainSchemas[i].key];
            $scope.dynamicForm.plainSchemas.splice(i, 1);
            i--;
          }
        }
        //removing derived schemas
        for (var i = 0; i < $scope.dynamicForm.derSchemas.length; i++) {
          if ($scope.dynamicForm.derSchemas[i].anyTypeClass == anyTypeClass) {
            //cleaning both form and user model
            delete $scope.user.derAttrs[$scope.dynamicForm.derSchemas[i].key];
            $scope.dynamicForm.derSchemas.splice(i, 1);
            i--;
          }
        }
        //removing virtual schemas
        for (var i = 0; i < $scope.dynamicForm.virSchemas.length; i++) {
          if ($scope.dynamicForm.virSchemas[i].anyTypeClass == anyTypeClass) {
            //cleaning both form and user model
            delete $scope.user.virAttrs[$scope.dynamicForm.virSchemas[i].key];
            $scope.dynamicForm.virSchemas.splice(i, 1);
            i--;
          }
        }
      };

      //Event management
      $scope.$on('auxClassAdded', function (event, auxClass) {
        if (auxClass)
          initUserSchemas(auxClass);
      });

      $scope.$on('auxClassRemoved', function (event, auxClass) {
        if (auxClass)
          removeUserSchemas(auxClass);
      });

      if ($scope.createMode) {

        $scope.user = {
          username: '',
          password: '',
          realm: '',
          securityQuestion: undefined,
          securityAnswer: '',
          plainAttrs: {},
          derAttrs: {},
          virAttrs: {},
          resources: [],
          auxClasses: []
        };
        // retrieve user realm or all available realms
        initUserRealm();
        // initialize auxiliary schemas in case of pre-existing classes
        for (var index in $scope.dynamicForm.selectedAuxClasses) {
          initUserSchemas($scope.dynamicForm.selectedAuxClasses[index]);
        }
        initProperties();
      } else {
        // read user from syncope core
        readUser();
      }

    };

    $scope.saveUser = function (user) {
      console.debug("Save user: ", user);

      var wrappedUser = UserUtil.getWrappedUser(user);

      if ($scope.createMode) {

        UserSelfService.create(wrappedUser, $scope.captchaInput.value).then(function (response) {
          console.info("Created user: ", response);
          $scope.showSuccess("User " + $scope.user.username + " successfully created", $scope.notification);
          $location.path('/self');
        }, function (response) {
          console.error("Error during user creation: ", response);
          var errorMessage;
          // parse error response 
          if (response !== undefined) {
            errorMessage = response.split("ErrorMessage{{")[1];
            errorMessage = errorMessage.split("}}")[0];
          }
          $scope.showError("Error: " + (errorMessage || response), $scope.notification);
        });

      } else {

        UserSelfService.update(wrappedUser, $scope.captchaInput.value).then(function (response) {
          console.debug("Updated user: ", response);
          AuthService.logout().then(function (response) {
            console.info("LOGOUT SUCCESS: ", response);
            $location.path('/self');
            $scope.showSuccess("User " + $scope.user.username + " successfully updated", $scope.notification);
          }, function () {
            console.error("LOGOUT FAILED");
          });
        }, function (response) {
          console.info("Error during user update: ", response);
          var errorMessage;
          // parse error response 
          if (response !== undefined) {
            errorMessage = response.split("ErrorMessage{{")[1];
            errorMessage = errorMessage.split("}}")[0];
          }
          $scope.showError("Error: " + (errorMessage || response), $scope.notification);
        });
      }
    };
    $scope.retrieveSecurityQuestion = function (user) {
      if ($rootScope.pwdResetRequiringSecurityQuestions) {
        if (user && user.username && user.username.length) {
          return SecurityQuestionService.
                  getSecurityQuestionByUser(user.username).then(function (data) {
            $scope.userSecurityQuestion = data.content;
          }, function (response) {
            var errorMessage;
            // parse error response 
            if (response !== undefined) {
              errorMessage = response.split("ErrorMessage{{")[1];
              errorMessage = errorMessage.split("}}")[0];
              $scope.userSecurityQuestion = "";
            }
            $scope.showError("Error retrieving user security question: " + errorMessage, $scope.notification);
          });
        } else {
          $scope.userSecurityQuestion = "";
        }
      }
    };
    $scope.resetPassword = function (user) {
      if (user && user.username) {
        $scope.retrieveSecurityQuestion(user);
        UserSelfService.passwordReset(user, $scope.captchaInput.value).then(function (data) {
          $scope.showSuccess(data, $scope.notification);
          $location.path('/self');
        }, function (response) {
          var errorMessage;
          // parse error response 
          if (response !== undefined) {
            errorMessage = response.split("ErrorMessage{{")[1];
            errorMessage = errorMessage.split("}}")[0];
            $scope.showError("An error occured during password reset: " + errorMessage, $scope.notification);
            //we need to refresh captcha after a valid request
            $scope.$broadcast("refreshCaptcha");
          }
        });
      } else {
        $scope.showError("You should use a valid and non-empty username", $scope.notification);
      }
    };

    $scope.confirmPasswordReset = function (user, event) {

      //getting the enclosing form in order to access to its controller                
      var currentForm = GenericUtil.getEnclosingFormController(event.target, $scope);
      if (currentForm != null) {
        //check if password and confirmPassword are equals using angular built-in validation
        if (ValidationExecutor.validate(currentForm, $scope)) {
          var token = $location.search().token;
          if (user && user.password && token) {
            UserSelfService.confirmPasswordReset({"newPassword": user.password, "token": token}).then(function (data) {
              $scope.showSuccess(data, $scope.notification);
              $location.path('/self');
            }, function (response) {
              var errorMessage;
              // parse error response 
              if (response !== undefined) {
                errorMessage = response.split("ErrorMessage{{")[1];
                errorMessage = errorMessage.split("}}")[0];
                $scope.showError("An error occured during password reset: " + errorMessage, $scope.notification);
              }
            });
          } else {
            $scope.showError("You should use a valid and non-empty password", $scope.notification);
          }
        }
      }
    };

    $scope.changePassword = function (user, event) {

      //getting the enclosing form in order to access to its controller                
      var currentForm = GenericUtil.getEnclosingFormController(event.target, $scope);
      if (currentForm != null) {
        //check if password and confirmPassword are equals using angular built-in validation
        if (ValidationExecutor.validate(currentForm, $scope)) {
          if (user && user.password) {
            UserSelfService.changePassword({"newPassword": user.password}).then(function (data) {
              $scope.logout(data);
            }, function (response) {
              var errorMessage;
              // parse error response 
              if (response !== undefined) {
                errorMessage = response.split("ErrorMessage{{")[1];
                errorMessage = errorMessage.split("}}")[0];
                $scope.showError("An error occured during password change: " + errorMessage, $scope.notification);
              }
            });
          } else {
            $scope.showError("You should use a valid and non-empty password", $scope.notification);
          }
        }
      }
    };

    $scope.logout = function (message) {
      AuthService.logout().then(function (response) {
        console.info("Logout successfully");
        $location.path('/self');
        if (message) {
          $scope.showSuccess(message, $scope.notification);
        }
      }, function (response) {
        console.error("Logout failed: ", response);
      });
    };
  }]);
