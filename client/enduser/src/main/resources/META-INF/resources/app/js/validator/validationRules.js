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

angular.module('SyncopeEnduserApp').
        directive('checktype', function () {
          return {
            require: 'ngModel',
            link: function (scope, elem, attr, ngModel) {
              var expectedType = attr.checktype;

              //For DOM -> model validation
              ngModel.$parsers.unshift(function (value) {
                var valid = (typeof value) === expectedType;
                ngModel.$setValidity('checktype', valid);                
                return valid ? value : undefined;
              });

              //For model -> DOM validation
              ngModel.$formatters.unshift(function (value) {
                ngModel.$setValidity('checktype', typeof value === expectedType);
                return value;
              });
            }
          };
        });

angular.module('SyncopeEnduserApp')
        .directive('equals', function () {
          return {
            restrict: 'A',
            require: '?ngModel',
            scope: {
              equals: "="
            },
            link: function (scope, elem, attrs, ngModel) {

              ngModel.$parsers.unshift(function (value) {
                var match = (value === scope.equals);
                ngModel.$setValidity('equals', match);
                return value;
              });

              scope.$watch("equals", function (value) {
                var confirmPassword = value || "";
                ngModel.$setValidity('equals', confirmPassword === ngModel.$viewValue);
              });
            }
          };
        });
