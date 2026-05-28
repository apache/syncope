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

abstract class FileSystemProviderOverloadsMacroActions extends Script implements MacroActions {}
@BaseScript FileSystemProviderOverloadsMacroActions _

@Override
StringBuilder afterAll(Map<String, Serializable> ctx, StringBuilder output) {
  def provider = java.nio.file.spi.FileSystemProvider.installedProviders().find { it.scheme == 'file' }
  def path = ctx.path as java.nio.file.Path

  provider.getFileSystem(java.net.URI.create('file:///'))
  output.append('fsp-get-filesystem|')

  try {
    provider.newFileSystem(java.net.URI.create('file:///'), [:])
  } catch (java.lang.IllegalArgumentException | java.nio.file.FileSystemAlreadyExistsException e) {
    output.append('fsp-new-uri-map|')
  }

  try {
    provider.newFileSystem(path, [:])
  } catch (java.io.IOException | java.lang.UnsupportedOperationException e) {
    output.append('fsp-new-path-map|')
  }

  return output
}
