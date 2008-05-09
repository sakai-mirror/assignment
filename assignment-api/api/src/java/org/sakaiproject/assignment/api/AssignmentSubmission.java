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

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentSubmissionVersion;
import org.sakaiproject.assignment.api.constants.AssignmentConstants;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.time.api.Time;

/**
 * the assignment submission object
 * @author zqian
 *
 */
public class AssignmentSubmission {

	private Long id;
	/** the Hibernate version number */
    private int hibernateVersion;
	private Assignment assignment;
	private String submitterId;
	private Date resubmitCloseTime;
	private Integer numSubmissionsAllowed;
	private Set<AssignmentSubmissionVersion> submissionHistorySet;
	// the current submission version must be populated manually b/c we want
	// to retrieve the version rec with the highest id
	private AssignmentSubmissionVersion currentSubmissionVersion;
	
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
	public int getHibernateVersion() {
		return hibernateVersion;
	}
	public void setHibernateVersion(int hibernateVersion) {
		this.hibernateVersion = hibernateVersion;
	}
	public AssignmentSubmissionVersion getCurrentSubmissionVersion() {
		return currentSubmissionVersion;
	}
	public void setCurrentSubmissionVersion(
			AssignmentSubmissionVersion currentSubmissionVersion) {
		this.currentSubmissionVersion = currentSubmissionVersion;
	}
	

	public String getReference()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(AssignmentConstants.REFERENCE_ROOT);
		sb.append(Entity.SEPARATOR);
		sb.append(AssignmentConstants.SUBMISSION_TYPE);
		sb.append(Entity.SEPARATOR);
		sb.append(getAssignment().getContext());
		sb.append(Entity.SEPARATOR);
		sb.append(Long.toString(id));
		return sb.toString();
	}
	public Set<AssignmentSubmissionVersion> getSubmissionHistorySet() {
		return submissionHistorySet;
	}
	public void setSubmissionHistorySet(
			Set<AssignmentSubmissionVersion> submissionHistorySet) {
		this.submissionHistorySet = submissionHistorySet;
	}
}
