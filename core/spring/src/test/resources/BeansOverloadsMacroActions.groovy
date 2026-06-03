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

abstract class BeansOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript BeansOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def expression = new java.beans.Expression('beans-', 'concat', ['expression|'] as Object[])
  output.append(expression.value)

  def preset = new java.beans.Expression(null, 'beans-', 'concat', ['preset|'] as Object[])
  preset.execute()
  output.append(preset.value)
  preset.setValue('beans-set-value|')
  output.append(preset.value)

  def buffer = new StringBuffer()
  def statement = new java.beans.Statement(buffer, 'append', ['beans-statement-execute|'] as Object[])
  statement.execute()
  output.append(buffer.toString())

  return output
}
