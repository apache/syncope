#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

echo "Creating PostgreSQL role for syncope. If already exists, the error message should be ignored"
su - postgres -c "psql -c \"create role $SYNCOPE_USER with login password '$SYNCOPE_PASS'\""
echo "Creating PostgreSQL database for syncope. If already exists, the error message should be ignored"
su - postgres -c "psql -c \"create database syncope owner $SYNCOPE_USER\""
