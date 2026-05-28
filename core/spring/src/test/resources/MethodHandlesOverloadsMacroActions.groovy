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
import groovy.transform.BaseScript
import org.apache.syncope.core.provisioning.api.macro.MacroActions
import java.io.Serializable

abstract class MethodHandlesOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript MethodHandlesOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def lookup = java.lang.invoke.MethodHandles.lookup()
  def publicLookup = java.lang.invoke.MethodHandles.publicLookup()

  def staticHandle = publicLookup.findStatic(
      java.lang.Integer,
      'toString',
      java.lang.invoke.MethodType.methodType(java.lang.String, int.class))
  output.append(staticHandle.invokeWithArguments([7] as java.util.List)).append('|')

  def virtualHandle = publicLookup.findVirtual(
      java.lang.String,
      'toUpperCase',
      java.lang.invoke.MethodType.methodType(java.lang.String))
  output.append(virtualHandle.invokeWithArguments(['mh-virtual'] as java.util.List)).append('|')

  def constructorHandle = publicLookup.findConstructor(
      java.lang.String,
      java.lang.invoke.MethodType.methodType(void.class, byte[].class))
  output.append(constructorHandle.invokeWithArguments(['mh-constructor'.bytes] as java.util.List)).append('|')

  def boundHandle = publicLookup.bind(
      'mh-bind',
      'toUpperCase',
      java.lang.invoke.MethodType.methodType(java.lang.String))
  output.append(boundHandle.invokeWithArguments([] as Object[])).append('|')

  output.append(java.lang.invoke.MethodHandles.reflectAs(
      java.lang.reflect.Method,
      staticHandle).name).append('|')

  return output
}
