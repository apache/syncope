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
grammar SCIMFilter;

options
{
  language = Java;
}

scimFilter
 : expression* EOF
 ;

expression
 : NOT WS+? expression                        # NOT_EXPR
 | expression WS+? AND WS+? expression        # EXPR_AND_EXPR
 | expression WS+? OR WS+ expression          # EXPR_OR_EXPR
 | expression WS+? operator WS+? expression   # EXPR_OPER_EXPR
 | ATTRNAME WS+? PR                           # ATTR_PR
 | ATTRNAME WS+? operator WS+? expression     # ATTR_OPER_EXPR
 | ATTRNAME WS+? operator WS+? criteria       # ATTR_OPER_CRITERIA
 | LPAREN WS*? expression WS*? RPAREN         # LPAREN_EXPR_RPAREN
 | ATTRNAME LBRAC WS*? expression WS*? RBRAC  # LBRAC_EXPR_RBRAC
 ;

criteria : '"' .+? '"';

operator
 : EQ | NE | CO | SW | EW | GT | LT | GE | LE
 ;

EQ : [eE][qQ];
NE : [nN][eE];
CO : [cC][oO];
SW : [sS][wW];
EW : [eE][wW];
GT : [gG][tT];
LT : [lL][tT];
GE : [gG][eE];
LE : [lL][eE];

NOT : [nN][oO][tT];

AND : [aA][nN][dD];
OR  : [oO][rR];

PR : [pP][rR];

LPAREN : '(';
RPAREN : ')';

LBRAC : '[';
RBRAC : ']';

WS : ' ';

ATTRNAME : [-_.:a-zA-Z0-9]+;

ANY : ~('"' | '(' | ')' | '[' | ']');

EOL : [\t\r\n\u000C]+ -> skip;
