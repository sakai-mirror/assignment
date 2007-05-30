/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/rsmart/dbrefactor/assignment/assignment-api/api/src/java/org/sakaiproject/assignment/api/AssignmentServiceSql.java $
 * $Id: AssignmentServiceSql.java 3560 2007-02-19 22:08:01Z jbush@rsmart.com $
 ***********************************************************************************
 *
 * Copyright (c) 2004, 2005, 2006 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/
package org.sakaiproject.assignment.api;




/**
 * database methods.
 */
public interface AssignmentServiceSql {

   /**
    * returns the sql statement which retrieves a list of all assignments whose context is null.
    */
   public String getListAssignmentsSql();

   /**
    * returns the sql statement which retrieves a list of all assignment contents whose context is null.
    */
   public String getListAssignmentContentsSql();

   /**
    * returns the sql statement which retrieves a list of all assignment submissions whose context is null.
    */
   public String getListAssignmentSubmissionsSql();

   /**
    * returns the sql statement which updates an assignment.
    */
   public String getUpdateAssignmentSql();

   /**
    * returns the sql statement which updates an assignment's content.
    */
   public String getUpdateAssignmentContentSql();

   /**
    * returns the sql statement which updates an assignment's submission.
    */
   public String getUpdateAssignmentSubmissionSql();
}
