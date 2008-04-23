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

package org.sakaiproject.assignment.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.assignment.model.Assignment;
import org.sakaiproject.assignment.model.AssignmentSubmissionVersion;
import org.sakaiproject.time.api.Time;

/**
 * the assignment submission object
 * @author zqian
 *
 */
public class AssignmentSubmission {

	private Long id;
	private Assignment assignment;
	private String submitterId;
	private Date resubmitCloseTime;
	private Integer numSubmissionsAllowed;
	private List<AssignmentSubmissionVersion> submissionVersions;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Assignment getAssignment() {
		return assignment;
	}
	public void setAssignment(Assignment assignment) {
		this.assignment = assignment;
	}
	public String getSubmitterId() {
		return submitterId;
	}
	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}
	public Date getResubmitCloseTime() {
		return resubmitCloseTime;
	}
	public void setResubmitCloseTime(Date resubmitCloseTime) {
		this.resubmitCloseTime = resubmitCloseTime;
	}
	public Integer getNumSubmissionsAllowed() {
		return numSubmissionsAllowed;
	}
	public void setNumSubmissionsAllowed(Integer numSubmissionsAllowed) {
		this.numSubmissionsAllowed = numSubmissionsAllowed;
	}
	public List<AssignmentSubmissionVersion> getSubmissionVersions() {
		return submissionVersions;
	}
	public void setSubmissionVersions(
			List<AssignmentSubmissionVersion> submissionVersions) {
		this.submissionVersions = submissionVersions;
	}
	
}
