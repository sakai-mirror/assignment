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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.time.api.Time;

/**
 * Assignment object
 * @author zqian
 *
 */
public class Assignment {

	/** the assignment id */
	private Long id;

	/** the assignment title */
	private String title;

	/** the context, usually the site id */
	private String context;

	/** the start date for submit to assignment*/
	private Date openTime;

	/** the due date of assignment */
	private Date dueTime;

	/** the close date that submission will no longer be accepted */
	private Date closeTime;

	private Date resubmission_closeTime;

	/** is assignment a draft*/
	private boolean draft;
	
	/** has the assignment been deleted */
	private boolean deleted;
	
	/** the ordering of assignment object*/
	private int position_order;

	/** The Collection of groups (authorization group id strings). */
	private List<AssignmentGroup> groups;
	
	/** The Collection of groups (authorization group id strings). */
	private List<AssignmentAttachment> attachments;

	/** whether the honor pledge is required*/
	private Boolean honorPledge;

	/** the type of submission */
	private Integer typeOfSubmission;

    private Integer numSubmissionsAllowed;

	private Integer typeOfGrade;

	private int maxGradePoint;
	
    private Long gradableObjectId;
	
	private boolean allowReviewService;
	
	private boolean allowStudentViewReport;

	private String creator;
	
	private Date timeCreated;

	private String lastModifiedBy;
	
	private Date timeLastModified;

    private int notificationType;
    
    private Boolean hasAnnouncement;
    
    private String announcementId;
    
    private Boolean addedToSchedule;
    
    private String scheduleEventId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public Date getOpenTime() {
		return openTime;
	}

	public void setOpenTime(Date openTime) {
		this.openTime = openTime;
	}

	public Date getDueTime() {
		return dueTime;
	}

	public void setDueTime(Date dueTime) {
		this.dueTime = dueTime;
	}

	public Date getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(Date closeTime) {
		this.closeTime = closeTime;
	}

	public Date getDropDeadTime() {
		return resubmission_closeTime;
	}

	public void setDropDeadTime(Date resubmission_closeTime) {
		this.resubmission_closeTime = resubmission_closeTime;
	}

	public boolean isDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public int getPosition_order() {
		return position_order;
	}

	public void setPosition_order(int position_order) {
		this.position_order = position_order;
	}

	public List<AssignmentGroup> getGroups() {
		return groups;
	}

	public void setGroups(List<AssignmentGroup> groups) {
		this.groups = groups;
	}

	public List<AssignmentAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<AssignmentAttachment> attachments) {
		this.attachments = attachments;
	}

	public Boolean getHonorPledge() {
		return honorPledge;
	}

	public void setHonorPledge(Boolean honorPledge) {
		this.honorPledge = honorPledge;
	}

	public Integer getTypeOfSubmission() {
		return typeOfSubmission;
	}

	public void setTypeOfSubmission(Integer typeOfSubmission) {
		this.typeOfSubmission = typeOfSubmission;
	}

	public Integer getNumSubmissionsAllowed() {
		return numSubmissionsAllowed;
	}

	public void setNumSubmissionsAllowed(Integer numSubmissionsAllowed) {
		this.numSubmissionsAllowed = numSubmissionsAllowed;
	}

	public Integer getTypeOfGrade() {
		return typeOfGrade;
	}

	public void setTypeOfGrade(Integer typeOfGrade) {
		this.typeOfGrade = typeOfGrade;
	}

	public int getMaxGradePoint() {
		return maxGradePoint;
	}

	public void setMaxGradePoint(int maxGradePoint) {
		this.maxGradePoint = maxGradePoint;
	}

	public Long getGradableObjectId() {
		return gradableObjectId;
	}

	public void setGradableObjectId(Long gradableObjectId) {
		this.gradableObjectId = gradableObjectId;
	}

	public boolean isAllowReviewService() {
		return allowReviewService;
	}

	public void setAllowReviewService(boolean allowReviewService) {
		this.allowReviewService = allowReviewService;
	}

	public boolean isAllowStudentViewReport() {
		return allowStudentViewReport;
	}

	public void setAllowStudentViewReport(boolean allowStudentViewReport) {
		this.allowStudentViewReport = allowStudentViewReport;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public Date getTimeCreated() {
		return timeCreated;
	}

	public void setTimeCreated(Date timeCreated) {
		this.timeCreated = timeCreated;
	}

	public String getLastModifiedBy() {
		return lastModifiedBy;
	}

	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	public Date getTimeLastModified() {
		return timeLastModified;
	}

	public void setTimeLastModified(Date timeLastModified) {
		this.timeLastModified = timeLastModified;
	}

	public int getNotificationType() {
		return notificationType;
	}

	public void setNotificationType(int notificationType) {
		this.notificationType = notificationType;
	}

	public Boolean getHasAnnouncement() {
		return hasAnnouncement;
	}

	public void setHasAnnouncement(Boolean hasAnnouncement) {
		this.hasAnnouncement = hasAnnouncement;
	}

	public String getAnnouncementId() {
		return announcementId;
	}

	public void setAnnouncementId(String announcementId) {
		this.announcementId = announcementId;
	}

	public Boolean getAddedToSchedule() {
		return addedToSchedule;
	}

	public void setAddedToSchedule(Boolean addedToSchedule) {
		this.addedToSchedule = addedToSchedule;
	}

	public String getScheduleEventId() {
		return scheduleEventId;
	}

	public void setScheduleEventId(String scheduleEventId) {
		this.scheduleEventId = scheduleEventId;
	}
}
