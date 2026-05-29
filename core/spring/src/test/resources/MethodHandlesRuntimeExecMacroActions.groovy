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

abstract class MethodHandlesRuntimeExecMacroActions extends Script implements MacroActions {}
@BaseScript MethodHandlesRuntimeExecMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def lookup = java.lang.invoke.MethodHandles.publicLookup()
  def getRuntime = lookup.findStatic(
      java.lang.Runtime,
      'getRuntime',
      java.lang.invoke.MethodType.methodType(java.lang.Runtime))
  def runtime = getRuntime.invokeWithArguments([] as Object[])
  def exec = lookup.findVirtual(
      java.lang.Runtime,
      'exec',
      java.lang.invoke.MethodType.methodType(java.lang.Process, String[].class))
  def process = exec.invokeWithArguments(
      runtime,
      ['/bin/sh', '-c', 'printf sandbox-method-handles-ok'] as String[])
  output.append(process.inputStream.text)
  return output
}
