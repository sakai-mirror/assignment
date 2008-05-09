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

/**
 * The base class for attachments associated with AssignmentSubmission
 * @author zqian
 *
 */
public class SubmissionAttachmentBase extends AttachmentBase{
	
	public SubmissionAttachmentBase ()
	{
		
	}
	
	public SubmissionAttachmentBase (AssignmentSubmissionVersion submissionVersion, String attachmentReference)
	{
		this.submissionVersion = submissionVersion;
		this.attachmentReference = attachmentReference;
	}
	
	/** the associated AssignmentSubmissionVersion object */
	protected AssignmentSubmissionVersion submissionVersion;

	public AssignmentSubmissionVersion getSubmissionVersion() {
		return submissionVersion;
	}

	public void setSubmissionVersion(AssignmentSubmissionVersion submissionVersion) {
		this.submissionVersion = submissionVersion;
	}

}
