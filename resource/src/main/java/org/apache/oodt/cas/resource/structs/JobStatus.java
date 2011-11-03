/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.oodt.cas.resource.structs;

/**
 * @author mattmann
 * @version $Revision$
 *
 * <p>Describe your class here</p>.
 */
public interface JobStatus {
  
  public static final String QUEUED = "__Queued__";
  
  public static final String EXECUTED = "__Executed__";
  
  public static final String SCHEDULED = "__Scheduled__";
  
  public static final String SUCCESS = "__Success__";
  
  public static final String FAILURE = "__Failure__";
  
  public static final String KILLED = "__Killed__";

}