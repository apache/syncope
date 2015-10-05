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

'use strict'

angular.module('self', [])
        .directive('checkStrength', function () {
          return {
            replace: false,
            restrict: 'EACM',
            link: function (scope, iElement, iAttrs) {

              var strength = {
                colors: ['#F00', '#F90', '#FF0', '#9F0', '#0F0'],
                mesureStrength: function (p) {

                  var _force = 0;
                  var _regex = /[$-/:-?{-~!"^_`\[\]]/g;

                  var _lowerLetters = /[a-z]+/.test(p);
                  var _upperLetters = /[A-Z]+/.test(p);
                  var _numbers = /[0-9]+/.test(p);
                  var _symbols = _regex.test(p);

                  var _flags = [_lowerLetters, _upperLetters, _numbers, _symbols];
                  var _passedMatches = $.grep(_flags, function (el) {
                    return el === true;
                  }).length;

                  _force += 2 * p.length + ((p.length >= 10) ? 1 : 0);
                  _force += _passedMatches * 10;

                  // penality (short password)
                  _force = (p.length <= 6) ? Math.min(_force, 10) : _force;

                  // penality (poor variety of characters)
                  _force = (_passedMatches == 1) ? Math.min(_force, 10) : _force;
                  _force = (_passedMatches == 2) ? Math.min(_force, 20) : _force;
                  _force = (_passedMatches == 3) ? Math.min(_force, 40) : _force;

                  return _force;

                },
                getColor: function (s) {

                  var idx = 0;
                  if (s <= 10) {
                    idx = 0;
                  }
                  else if (s <= 20) {
                    idx = 1;
                  }
                  else if (s <= 30) {
                    idx = 2;
                  }
                  else if (s <= 40) {
                    idx = 3;
                  }
                  else {
                    idx = 4;
                  }

                  return {idx: idx + 1, col: this.colors[idx]};

                }
              };

              scope.$watch(iAttrs.checkStrength, function () {
                if (scope.pw === '') {
                  iElement.css({"display": "none"});
                } else {
                  var strength = strength.mesureStrength(scope.pw);
                  var c = strength.getColor(strength);
                  iElement.css({"display": "inline"});
                  iElement.children('li')
                          .css({"background": "#DDD"})
                          .slice(0, c.idx)
                          .css({"background": c.col});
                }
              });

            },
            template: ''
          };
        });

