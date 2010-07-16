<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
under the License.
-->
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <style type="text/css">
            body {
                font: 12.0px Verdana;
                color: black; 
                margin-top: 0px;
            }
        </style>
    </head>
    <body>
        <div id="confirmation">
            Please, <a href="${param.confirmationLink}">confirm</a> your request.
        </div>

        <div id="greetings">
            Best regards.
        </div>
    </body>
</html>
