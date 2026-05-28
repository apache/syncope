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

abstract class RuntimeExecOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript RuntimeExecOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def runtime = java.lang.Runtime.getRuntime()
  def append = { Process process ->
    process.waitFor()
    output.append(process.inputStream.text)
  }

  append(runtime.exec('/bin/printf runtime-string|'))
  append(runtime.exec('/bin/printf runtime-string-env|', null as String[]))
  append(runtime.exec('/bin/printf runtime-string-env-dir|', null as String[], null as java.io.File))
  append(runtime.exec(['/bin/sh', '-c', "printf 'runtime-array|'"] as String[]))
  append(runtime.exec(['/bin/sh', '-c', "printf 'runtime-array-env|'"] as String[], null as String[]))
  append(runtime.exec(['/bin/sh', '-c', "printf 'runtime-array-env-dir|'"] as String[], null as String[], null as java.io.File))

  return output
}
