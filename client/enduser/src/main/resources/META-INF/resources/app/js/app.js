/**
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at
 
 http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 **/

'use strict';

angular.module('home', []);
angular.module('login', []);
angular.module('language', []);
angular.module('self', []);
angular.module('info', []);

// Declare app level module which depends on views, and components
var app = angular.module('SyncopeEnduserApp', [
  'ui.router',
  'ui.bootstrap',
  'ui.select',
  'ngSanitize',
  'ngAnimate',
  'ngResource',
  'ngCookies',
  'angular-growl',
  'home',
  'login',
  'language',
  'self',
  'info'
]);

app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', 'growlProvider',
  function ($stateProvider, $urlRouterProvider, $httpProvider, growlProvider) {
    // route configuration
    $stateProvider
            .state('home', {
              url: '/',
              templateUrl: 'views/self.html'
            })
            .state('self', {
              url: '/self',
              templateUrl: 'views/self.html'
            })
            .state('user-self-update', {
              url: '/user-self-update',
              templateUrl: 'views/home.html',
              controller: 'HomeController',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('create', {
              url: '/self/create',
              templateUrl: 'views/editUser.html'
            })
            // nested states 
            // each of these sections will have their own view
            // url will be nested (/self/create)
            .state('create.credentials', {
              url: '/credentials',
              templateUrl: 'views/user-credentials.html'
            })
            .state('create.groups', {
              url: '/groups',
              templateUrl: 'views/user-groups.html'
            })
            .state('create.plainSchemas', {
              url: '/plainSchemas',
              templateUrl: 'views/user-plain-schemas.html'
            })
            .state('create.derivedSchemas', {
             url: '/derivedSchemas',
             templateUrl: 'views/user-derived-schemas.html'
             })
             .state('create.virtualSchemas', {
             url: '/virtualSchemas',
             templateUrl: 'views/user-virtual-schemas.html'
             })
            .state('create.resources', {
              url: '/resources',
              templateUrl: 'views/user-resources.html'
            })
            .state('create.finish', {
              url: '/finish',
              templateUrl: 'views/user-form-finish.html'
            })
            .state('update', {
              url: '/self/update',
              templateUrl: 'views/editUser.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            // nested states 
            // each of these sections will have their own view
            // url will be nested (/self/update)
            .state('update.credentials', {
              url: '/credentials',
              templateUrl: 'views/user-credentials.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('update.plainSchemas', {
              url: '/plainSchemas',
              templateUrl: 'views/user-plain-schemas.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('update.derivedSchemas', {
              url: '/derivedSchemas',
              templateUrl: 'views/user-derived-schemas.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('update.virtualSchemas', {
              url: '/virtualSchemas',
              templateUrl: 'views/user-virtual-schemas.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('update.groups', {
              url: '/groups',
              templateUrl: 'views/user-groups.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('update.resources', {
              url: '/resources',
              templateUrl: 'views/user-resources.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            })
            .state('update.finish', {
              url: '/finish',
              templateUrl: 'views/user-form-finish.html',
              resolve: {
                'authenticated': function (AuthenticationHelper) {
                  return AuthenticationHelper.authenticated();
                }
              }
            });

    // catch all other routes
    // send users to the home page 
    $urlRouterProvider.otherwise('/');

    // HTTP service configuration
    $httpProvider.defaults.withCredentials = true;

    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
      var numLoadings = 0;
      return {
//        'request': function (config) {
//          numLoadings++;
//          // Show loader
//          if (config.url.indexOf("skipLoader=true") == -1) {
//            $rootScope.$broadcast("loader_show");
//          }
//          return config || $q.when(config);
//        },
//        'response': function (response) {
//          if ((--numLoadings) === 0) {
//            // Hide loader
//            $rootScope.$broadcast("loader_hide");
//          }
//          return response || $q.when(response);
//        },
        'responseError': function (response) {
          if (response.config.url.indexOf("acceptError=true") == -1) {
            var status = response.status;
            if (status == 401) {
              console.log("ERROR " + status);
//              $location.path("/self");
            }
            if (status == 403) {
              console.log("UNAUTHORIZED " + status);
//              $location.path("/self");
            }
            if (status == 400 || status == 404 || status == 412 || status == 500) {
//              if (response.data.validationErrors != undefined) {
//                for (var i in response.data.validationErrors) {
//                  $rootScope.$broadcast('growlMessage', {text: response.data.validationErrors[i] || '', severity: 'error'});
//                }
//              } else if (response.data.message != undefined) {
//                $rootScope.$broadcast('growlMessage', {text: response.data.message || '', severity: 'error'})
//              }
              console.log("GENERIC ERROR " + status);
            }
          }
          return $q.reject(response);
        }
      };
    });

    growlProvider.globalTimeToLive(10000);
    growlProvider.globalPosition('bottom-left');
    growlProvider.globalInlineMessages(true);
    growlProvider.globalDisableIcons(true);
    //to enable html in growl
//    growlProvider.globalEnableHtml(true);
  }]);

app.run(['$rootScope', '$location', '$cookies', '$state',
  function ($rootScope, $location, $cookies, $state) {
    // main program
    // keep user logged in after page refresh
    // check if user is logged or not
    $rootScope.currentUser = $cookies.get('currentUser') || null;
//If the route change failed due to authentication error, redirect them out
    $rootScope.$on('$routeChangeError', function (event, current, previous, rejection) {
      if (rejection === 'Not Authenticated') {
        $location.path('/self');
      }
    });

//    $rootScope.$on('success', function (event, args) {
//      console.log("IN CONFIG EVENTO: ", event);
//      $rootScope.$broadcast("error", "success");
//    });

    $rootScope.$on('$stateChangeSuccess', function (event, toState) {
      if (toState.name === 'create') {
        $state.go('create.credentials');
      } else if (toState.name === 'update') {
        $state.go('update.credentials');
      }
    });
//        $rootScope.$on('$locationChangeStart', function (event, next, current) {
//            // redirect to login page if not logged in
//            if ($location.path() !== '/self' && !$rootScope.globals.currentUser) {
//                $location.path('/self');
//            }
//        });
  }]);

app.controller('ApplicationController', function ($scope) {
// DO NOTHING
//  $scope.$on('success', function (event, args) {
//    console.log("IN CONFIG EVENTO: ", event)
//    $scope.$broadcast("error", "success");
//  });
});

app.factory('AuthenticationHelper', ['$q', '$rootScope',
  function ($q, $rootScope) {
    return {
      authenticated: function () {

        var currentUser = $rootScope.currentUser;

        console.log("AuthenticationHelper, currentUser: ", currentUser);

        if (angular.isDefined(currentUser) && currentUser) {
          return true;
        } else {
          console.log("NOT AUTHENTICATED, REDIRECT TO LOGIN PAGE");
          return $q.reject('Not Authenticated');
        }
      }
    };
  }]);
