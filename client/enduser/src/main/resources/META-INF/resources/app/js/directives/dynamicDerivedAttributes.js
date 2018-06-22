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

angular.module('self')
        .directive('dynamicDerivedAttributes', function () {
          var getTemplateUrl = function () {
            return 'views/dynamicDerivedAttributes.html';
          };
          return {
            restrict: 'E',
            templateUrl: getTemplateUrl(),
            scope: {
              dynamicForm: "=form",
              user: "=",
              language: "="
            },
            controller: function ($scope) {

              $scope.getByGroup = function (group) {
                var currentDerivedSchemas = new Array();
                for (var i = 0; i < $scope.dynamicForm.derSchemas.length; i++) {
                  var attr = $scope.dynamicForm.derSchemas[i];
                  if (group === "own" && $scope.dynamicForm.derSchemas[i].key.indexOf('#') == -1) {
                    attr.simpleKey = $scope.dynamicForm.derSchemas[i].key;
                    currentDerivedSchemas.push(attr);
                  } else {
                    var prefix = $scope.dynamicForm.derSchemas[i].key.substr
                            (0, $scope.dynamicForm.derSchemas[i].key.indexOf('#'));
                    if (prefix === group)
                    {
                      var shaPosition = $scope.dynamicForm.derSchemas[i].key.indexOf("#") + 1;
                      attr.simpleKey = $scope.dynamicForm.derSchemas[i].key.substring(shaPosition);
                      currentDerivedSchemas.push(attr);
                    }
                  }
                }
                return currentDerivedSchemas;
              };

              $scope.addAttributeField = function (derSchemaKey) {
                console.debug("Add DERIVED value:", derSchemaKey);
                console.debug(" ", ($scope.dynamicForm.attributeTable[derSchemaKey].fields.length));
                $scope.dynamicForm.attributeTable[derSchemaKey].fields.push(derSchemaKey + "_" + ($scope.dynamicForm.attributeTable[derSchemaKey].fields.length));
              };

              $scope.removeAttributeField = function (derSchemaKey, index) {
                console.debug("Remove DERIVED value:", derSchemaKey);
                console.debug("attribute index:", index);
                $scope.dynamicForm.attributeTable[derSchemaKey].fields.splice(index, 1);
                // clean user model
                $scope.user.derAttrs[derSchemaKey].values.splice(index, 1);
              };
            }
          };
        });
