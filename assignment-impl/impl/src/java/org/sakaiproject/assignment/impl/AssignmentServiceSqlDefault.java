/**********************************************************************************
 * $URL: https://source.sakaiproject.org/contrib/rsmart/dbrefactor/assignment/assignment-impl/impl/src/java/org/sakaiproject/assignment/impl/AssignmentServiceSqlDefault.java $
 * $Id: AssignmentServiceSqlDefault.java 3560 2007-02-19 22:08:01Z jbush@rsmart.com $
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
package org.sakaiproject.assignment.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.assignment.api.AssignmentServiceSql;



/**
 * methods for accessing assignment data in a database.
 */
public class AssignmentServiceSqlDefault implements AssignmentServiceSql {

   // logger
   protected final transient Log logger = LogFactory.getLog(getClass());

   /**
    * returns the sql statement which retrieves a list of all assignments whose context is null.
    */
   public String getListAssignmentsSql() {
      return "select XML from ASSIGNMENT_ASSIGNMENT where CONTEXT is null";
   }

   /**
    * returns the sql statement which retrieves a list of all assignment contents whose context is null.
    */
   public String getListAssignmentContentsSql() {
      return "select XML from ASSIGNMENT_CONTENT where CONTEXT is null";
   }

   /**
    * returns the sql statement which retrieves a list of all assignment submissions whose context is null.
    */
   public String getListAssignmentSubmissionsSql() {
      return "select XML from ASSIGNMENT_SUBMISSION where CONTEXT is null";
   }

   /**
    * returns the sql statement which updates an assignment.
    */
   public String getUpdateAssignmentSql() {
      return "update ASSIGNMENT_ASSIGNMENT set CONTEXT = ? where ASSIGNMENT_ID = ?";
   }

   /**
    * returns the sql statement which updates an assignment's content.
    */
   public String getUpdateAssignmentContentSql() {
      return "update ASSIGNMENT_CONTENT set CONTEXT = ? where CONTENT_ID = ?";
   }

   /**
    * returns the sql statement which updates an assignment's submission.
    */
   public String getUpdateAssignmentSubmissionSql() {
      return "update ASSIGNMENT_SUBMISSION set CONTEXT = ? where SUBMISSION_ID = ?";
   }
}
