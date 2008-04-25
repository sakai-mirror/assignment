/**********************************************************************************
 * $URL:  $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

public interface AssignmentEvent {
	
	/**********************************************************************************************************************************************************************************************************************************************************
	 * EVENT STRINGS
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Event for adding an assignment. */
	public static final String EVENT_ADD_ASSIGNMENT = "asn.new.assignment";

	/** Event for adding an assignment submission. */
	public static final String EVENT_ADD_SUBMISSION = "asn.new.submission";

	/** Event for removing an assignment. */
	public static final String EVENT_REMOVE_ASSIGNMENT = "asn.delete.assignment";

	/** Event for removing an assignment submission. */
	public static final String EVENT_REMOVE_SUBMISSION = "asn.delete.submission";

	/** Event for accessing an assignment. */
	public static final String EVENT_ACCESS_ASSIGNMENT = "asn.read.assignment";

	/** Event for accessing an assignment submission. */
	public static final String EVENT_ACCESS_SUBMISSION = "asn.read.submission";

	/** Event for updating an assignment. */
	public static final String EVENT_UPDATE_ASSIGNMENT = "asn.revise.assignment";

	/** Event for updating an assignment submission. */
	public static final String EVENT_UPDATE_SUBMISSION = "asn.revise.submission";

	/** Event for submitting an assignment submission. */
	public static final String EVENT_SUBMIT_SUBMISSION = "asn.submit.submission";
	
	/** Event for grading an assignment submission. */
	public static final String EVENT_GRADE_SUBMISSION = "asn.grade.submission";
	
	/** Event for returning an assignment submission. */
	public static final String EVENT_RETURN_SUBMISSION = "asn.return.submission";
	
	
    /**
    * Post a sakai event
    * @param message
    * @param referenceObject
    */
	public void postEvent(String message, String referenceObject); 

}
