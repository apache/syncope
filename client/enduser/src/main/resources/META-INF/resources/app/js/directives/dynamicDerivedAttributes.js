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
          return {
            restrict: 'E',
            templateUrl: 'views/dynamicDerivedAttributes.html',
            scope: {
              dynamicForm: "=form",
              user: "="
            },
            controller: function ($scope) {
              $scope.byGroup = {};

              $scope.splitByGroup = function (schemas) {
                for (var i = 0; i < schemas.length; i++) {
                  var group;
                  var simpleKey;
                  if (schemas[i].key.indexOf('#') === -1) {
                    group = "own";
                    simpleKey = schemas[i].key;
                  } else {
                    group = schemas[i].key.substr(0, schemas[i].key.indexOf('#'));
                    simpleKey = schemas[i].key.substr(schemas[i].key.indexOf('#') + 1);
                  }
                  if (!$scope.byGroup[group]) {
                    $scope.byGroup[group] = new Array();
                  }
                  $scope.byGroup[group].push(schemas[i]);
                  schemas[i].simpleKey = simpleKey;
                }
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
