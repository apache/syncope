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
/*
 * AngularJS application modules from which depend views and components
 */
var app = angular.module('SyncopeEnduserApp', [
  'ui.router',
  'ui.bootstrap',
  'ui.select',
  'ngSanitize',
  'ngAnimate',
  'ngResource',
  'treasure-overlay-spinner',
  'ngPasswordStrength',
  'kendo.directives',
  'home',
  'login',
  'language',
  'self',
  'info',
  'ngCookies',
  'pascalprecht.translate'
]);

app.config(['$stateProvider', '$urlRouterProvider', '$httpProvider', '$translateProvider', '$translatePartialLoaderProvider',
  function ($stateProvider, $urlRouterProvider, $httpProvider, $translateProvider, $translatePartialLoaderProvider) {
    /*
     |--------------------------------------------------------------------------
     | Syncope Enduser AngularJS providers configuration
     |--------------------------------------------------------------------------
     */

    /*
     * i18n provider
     */
    $translatePartialLoaderProvider.addPart('static');
    $translatePartialLoaderProvider.addPart('dynamic');
    $translateProvider.useLoader('$translatePartialLoader', {
      urlTemplate: 'languages/{lang}/{part}.json'
    }).preferredLanguage('en');
    /*
     * State provider
     */
    $stateProvider
            .state('home', {
              url: '/',
              templateUrl: 'views/self.html'
            })
            .state('self', {
              url: '/self?errorMessage',
              templateUrl: 'views/self.html'
            })
            .state('user-self-update', {
              url: '/user-self-update',
              templateUrl: 'views/home.html',
              controller: 'HomeController',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
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
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            // nested states 
            // each of these sections will have their own view
            // url will be nested (/self/update)
            .state('update.credentials', {
              url: '/credentials',
              templateUrl: 'views/user-credentials.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('update.plainSchemas', {
              url: '/plainSchemas',
              templateUrl: 'views/user-plain-schemas.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('update.derivedSchemas', {
              url: '/derivedSchemas',
              templateUrl: 'views/user-derived-schemas.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('update.virtualSchemas', {
              url: '/virtualSchemas',
              templateUrl: 'views/user-virtual-schemas.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('update.groups', {
              url: '/groups',
              templateUrl: 'views/user-groups.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('update.resources', {
              url: '/resources',
              templateUrl: 'views/user-resources.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('update.finish', {
              url: '/finish',
              templateUrl: 'views/user-form-finish.html',
              resolve: {
                'authenticated': ['AuthService',
                  function (AuthService) {
                    return AuthService.islogged();
                  }]
              }
            })
            .state('passwordreset', {
              url: '/passwordreset',
              templateUrl: 'views/passwordreset.html'
            })
            .state('confirmpasswordreset', {
              url: '/confirmpasswordreset?token',
              templateUrl: 'views/confirmpasswordreset.html'
            })
            .state('mustchangepassword', {
              url: '/mustchangepassword',
              templateUrl: 'views/mustchangepassword.html'
            });
    /*
     * catch all other routes and send users to the home page 
     */
    $urlRouterProvider.otherwise('/');
    /*
     * HTTP provider
     */
    $httpProvider.defaults.withCredentials = true;
    $httpProvider.defaults.xsrfCookieName = 'XSRF-TOKEN';
    $httpProvider.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';
    /*
     * SYNCOPE-780
     */
    $httpProvider.defaults.headers.common["If-Modified-Since"] = "0";
    $httpProvider.interceptors.push(function ($q, $rootScope, $location) {
      return {
        'request': function (config, a, b) {
          //if the url is an html, we're changing page
          if (config.url.indexOf('.html', config.url.length - 5) === -1) {
            $rootScope.$broadcast("xhrStarted");
            var separator = config.url.indexOf('?') === -1 ? '?' : '&';
            config.url = config.url + separator + 'noCache=' + new Date().getTime();
          }
          $rootScope.spinner.on();
          return config || $q.when(config);
        },
        'response': function (response) {
          $rootScope.spinner.off();
          return response || $q.when(response);
        },
        'responseError': function (response) {
          $rootScope.spinner.off();
          if (response.config && response.config.url.indexOf("acceptError=true") === -1) {
            var status = response.status;
            if (status === 401) {
              console.error("ERROR ", status);
            }
            if (status === 403) {
              console.error("UNAUTHORIZED ", status);
            }
            if (status === 400 || status === 404 || status === 412 || status === 500) {
              console.error("GENERIC ERROR ", status);
            }
          }
          return $q.reject(response);
        }
      };
    });
  }]);
app.run(['$rootScope', '$location', '$state', 'AuthService',
  function ($rootScope, $location, $state, AuthService) {
    /*
     |--------------------------------------------------------------------------
     | Main of Syncope Enduser application
     |
     | keep user logged in after page refresh
     | If the route change failed due to authentication error, redirect them out
     |--------------------------------------------------------------------------
     */
    $rootScope.$on('$routeChangeError', function (event, current, previous, rejection) {
      if (rejection === 'Not Authenticated') {
        $location.path('/self');
      }
    });
    $rootScope.$on('$stateChangeSuccess', function (event, toState) {
      if (toState.name === 'create') {
        $state.go('create.credentials');
      } else if (toState.name === 'update') {
        $state.go('update.credentials');
      } else if (toState.name.indexOf("update") > -1) {
        AuthService.islogged().then(function (response) {
          if (response === "true") {
            $state.go(toState);
          } else {
            $state.go('self');
          }
        }, function (response) {
          console.error("not logged");
          $state.go('self');
        }
        );

      } else if (toState.name === 'home' || toState.name === 'self') {
        AuthService.islogged().then(function (response) {
          if (response === "true") {
            $state.go('update.credentials');
          } else {
            $state.go('self');
          }
        }, function (response) {
          console.error("not logged");
          $state.go('self');
        }
        );
        /*
         * enable "finish" button on every page in create mode
         */
      } else if (toState.name === 'create.finish') {
        $rootScope.endReached = true;
      } else {
        $state.go(toState);
      }
    });
    $rootScope.spinner = {
      active: false,
      on: function () {
        this.active = true;
      },
      off: function () {
        this.active = false;
      }
    };
  }]);
app.controller('ApplicationController', ['$scope', '$rootScope', '$location', 'InfoService', 'SAML2IdPService',
  function ($scope, $rootScope, $location, InfoService, SAML2IdPService) {
    $scope.initApplication = function () {
      /* 
       * disable by default wizard buttons in self-registration
       */
      $rootScope.endReached = false;
      /*
       |--------------------------------------------------------------------------
       | Syncope Enduser i18n initialization
       |--------------------------------------------------------------------------
       */
      $rootScope.languages = {
        availableLanguages: [
          {id: '1', name: 'Italiano', code: 'it', format: 'dd/MM/yyyy HH:mm'},
          {id: '2', name: 'English', code: 'en', format: 'MM/dd/yyyy HH:mm'},
          {id: '3', name: 'Deutsch', code: 'de', format: 'dd/MM/yyyy HH:mm'}
        ]
      };
      $rootScope.languages.selectedLanguage = $rootScope.languages.availableLanguages[1];
      /*
       |--------------------------------------------------------------------------
       | Syncope Enduser properties initialization
       | get info from InfoService API (info settings are initialized every time an user reloads the login page)
       |--------------------------------------------------------------------------
       */
      $rootScope.selfRegAllowed = false;
      $rootScope.pwdResetAllowed = false;
      $rootScope.version = "";
      $rootScope.pwdResetRequiringSecurityQuestions = false;
      $rootScope.captchaEnabled = false;
      $rootScope.validationEnabled = true;
      $rootScope.saml2idps = {
        available: [],
        selected: {}
      };

      InfoService.getInfo().then(
              function (response) {
                $rootScope.pwdResetAllowed = response.pwdResetAllowed;
                $rootScope.selfRegAllowed = response.selfRegAllowed;
                $rootScope.version = response.version;
                $rootScope.pwdResetRequiringSecurityQuestions = response.pwdResetRequiringSecurityQuestions;
                $rootScope.captchaEnabled = response.captchaEnabled;
                /* 
                 * USER form customization JSON
                 */
                $rootScope.customForm = response.customForm;
              },
              function (response) {
                console.error("Something went wrong while accessing info resource", response);
              });
      /* <Extensions> */
      SAML2IdPService.getAvailableSAML2IdPs().then(
              function (response) {
                $rootScope.saml2idps.available = response;
              },
              function (response) {
                console.debug("No SAML 2.0 SP extension available", response);
              });
      /* </Extensions> */
      /* 
       * configuration getters
       */
      $rootScope.isSelfRegAllowed = function () {
        return $rootScope.selfRegAllowed === true;
      };
      $rootScope.isPwdResetAllowed = function () {
        return $rootScope.pwdResetAllowed === true;
      };
      $rootScope.saml2spExtAvailable = function () {
        return $rootScope.saml2idps.available.length > 0;
      }
      $rootScope.saml2login = function () {
        window.location.href = '../saml2sp/login?idp=' + $rootScope.saml2idps.selected.entityID;
      }
      $rootScope.getVersion = function () {
        return $rootScope.version;
      };
      /* 
       * USER Attributes sorting strategies
       */
      $rootScope.attributesSorting = {
        ASC: function (a, b) {
          var schemaNameA = a.key;
          var schemaNameB = b.key;
          return schemaNameA < schemaNameB ? -1 : schemaNameA > schemaNameB ? 1 : 0;
        },
        DESC: function (a, b) {
          var schemaNameA = a.key;
          var schemaNameB = b.key;
          return schemaNameA < schemaNameB ? 1 : schemaNameA > schemaNameB ? -1 : 0;
        }
      };
      /*
       |--------------------------------------------------------------------------
       | Notification mgmt
       |--------------------------------------------------------------------------
       */
      $scope.notification = $('#notifications').kendoNotification().data("kendoNotification");
      $scope.notification.setOptions({stacking: "down"});
      $scope.notification.options.position["top"] = 20;
      $scope.showSuccess = function (message, component) {
        if (!$scope.notificationExists(message)) {
          //forcing scrollTo since kendo doesn't disable scrollTop if pinned is true
          window.scrollTo(0, 0);
          component.options.autoHideAfter = 3000;
          component.show(message, "success");
        }
      };
      $scope.showError = function (message, component) {
        if (!$scope.notificationExists(message)) {
          //forcing scrollTo since kendo doesn't disable scrollTop if pinned is true
          window.scrollTo(0, 0);
          component.options.autoHideAfter = 0;
          component.show(message, "error");
        }
      };
      $scope.hideError = function (message, component) {
        var popup;
        if (popup = $scope.notificationExists(message)) {
          popup.hide = true;
          popup.close();
        }
      };
      $scope.notificationExists = function (message) {
        var result = false;
        if ($scope.notification !== null) {
          var pendingNotifications = $scope.notification.getNotifications();
          pendingNotifications.each(function (idx, element) {
            var popup = $(element).data("kendoPopup");
            if (!popup.hide && popup.wrapper.html().indexOf(message) > -1) {
              result = popup;
              return false; //breaking the each and storing the real result
            }
          });
        }
        return result;
      };
      $scope.hideNotifications = function (timer) {
        if ($scope.notification !== null) {
          var pendingNotifications = $scope.notification.getNotifications();
          if (timer && timer > 0) {
            setTimeout(function () {
              pendingNotifications.each(function (idx, element) {
                var popup = $(element).data("kendoPopup");
                if (popup) {
                  popup.hide = true;
                  popup.close();
                }
              });
            }, timer);
          } else {
            pendingNotifications.each(function (idx, element) {
              var popup = $(element).data("kendoPopup");
              if (popup) {
                popup.hide = true;
                //we should destroy the message immediately
                popup.destroy();
              }
            });
          }
        }
      };
      /*
       * Intercepting location change event
       * When a location changes, old notifications should be removed
       */
      $rootScope.$on("$locationChangeStart", function (event, next, current) {
        $scope.hideNotifications(3000);
      });
      //Intercepting xhr start event
      $scope.$on('xhrStarted', function (event, next, current) {
        $scope.hideNotifications(0);
      });
      //Intercepting hide popup errors event
      $scope.$on('hideErrorMessage', function (event, popupMessage) {
        $scope.hideError(popupMessage, $scope.notification);
      });
      /*
       |--------------------------------------------------------------------------
       | Wizard configuration
       |--------------------------------------------------------------------------
       */
      $scope.wizard = {
        "credentials": {url: "/credentials"},
        "groups": {url: "/groups"},
        "plainSchemas": {url: "/plainSchemas"},
        "derivedSchemas": {url: "/derivedSchemas"},
        "virtualSchemas": {url: "/virtualSchemas"},
        "resources": {url: "/resources"},
        "finish": {url: "/finish"}
      };
      /*
       |--------------------------------------------------------------------------
       | Utilities
       |--------------------------------------------------------------------------
       */
      $scope.clearCache = function () {
        $templateCache.removeAll();
      };
    };
  }]);
