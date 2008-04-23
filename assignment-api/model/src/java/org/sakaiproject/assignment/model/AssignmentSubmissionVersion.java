package org.sakaiproject.assignment.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.assignment.model.AssignmentSubmission;
import org.sakaiproject.assignment.model.FeedbackAttachment;
import org.sakaiproject.assignment.model.SubmissionAttachment;
import org.sakaiproject.time.api.Time;

public class AssignmentSubmissionVersion {	
	
	private Long id;
	private int revisionNum;
	private AssignmentSubmission assignmentSubmission;
	private String submitterId;
	private Date timeSubmitted;
	private Date timeReleased;
	
	private List<SubmissionAttachment> submittedAttachments;
	private List<FeedbackAttachment> feedbackAttachments;
	private String submittedText;
	private String feedbackComment;
	private String feedbackText;
	private String grade;
	private boolean draft;
	
	/*private boolean returned;
	private boolean graded;
	private boolean gradeReleased;*/
	
	//The score given by the review service
	private int reviewScore;
	// The report given by the content review service
	private String reviewReport;
	// The status of the review service
	private String reviewStatus;
	private String reviewIconUrl;
	
	/** create and last modified */
	private String createdBy;
	private Date createdTime;
	private String lastModifiedBy;
	private Date lastModifiedTime;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public int getRevisionNum() {
		return revisionNum;
	}
	public void setRevisionNum(int revisionNum) {
		this.revisionNum = revisionNum;
	}
	public AssignmentSubmission getAssignmentSubmission() {
		return assignmentSubmission;
	}
	public void setAssignmentSubmission(AssignmentSubmission assignmentSubmission) {
		this.assignmentSubmission = assignmentSubmission;
	}
	public String getSubmitterId() {
		return submitterId;
	}
	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}
	public Date getTimeSubmitted() {
		return timeSubmitted;
	}
	public void setTimeSubmitted(Date timeSubmitted) {
		this.timeSubmitted = timeSubmitted;
	}
	public Date getTimeReleased() {
		return timeReleased;
	}
	public void setTimeReleased(Date timeReleased) {
		this.timeReleased = timeReleased;
	}
	public List<SubmissionAttachment> getSubmittedAttachments() {
		return submittedAttachments;
	}
	public void setSubmittedAttachments(
			List<SubmissionAttachment> submittedAttachments) {
		this.submittedAttachments = submittedAttachments;
	}
	public List<FeedbackAttachment> getFeedbackAttachments() {
		return feedbackAttachments;
	}
	public void setFeedbackAttachments(List<FeedbackAttachment> feedbackAttachments) {
		this.feedbackAttachments = feedbackAttachments;
	}
	public String getSubmittedText() {
		return submittedText;
	}
	public void setSubmittedText(String submittedText) {
		this.submittedText = submittedText;
	}
	public String getFeedbackComment() {
		return feedbackComment;
	}
	public void setFeedbackComment(String feedbackComment) {
		this.feedbackComment = feedbackComment;
	}
	public String getFeedbackText() {
		return feedbackText;
	}
	public void setFeedbackText(String feedbackText) {
		this.feedbackText = feedbackText;
	}
	public String getGrade() {
		return grade;
	}
	public void setGrade(String grade) {
		this.grade = grade;
	}
	public boolean isDraft() {
		return draft;
	}
	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	public int getReviewScore() {
		return reviewScore;
	}
	public void setReviewScore(int reviewScore) {
		this.reviewScore = reviewScore;
	}
	public String getReviewReport() {
		return reviewReport;
	}
	public void setReviewReport(String reviewReport) {
		this.reviewReport = reviewReport;
	}
	public String getReviewStatus() {
		return reviewStatus;
	}
	public void setReviewStatus(String reviewStatus) {
		this.reviewStatus = reviewStatus;
	}
	public String getReviewIconUrl() {
		return reviewIconUrl;
	}
	public void setReviewIconUrl(String reviewIconUrl) {
		this.reviewIconUrl = reviewIconUrl;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Date getCreatedTime() {
		return createdTime;
	}
	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}
	public String getLastModifiedBy() {
		return lastModifiedBy;
	}
	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	public Date getLastModifiedTime() {
		return lastModifiedTime;
	}
	public void setLastModifiedTime(Date lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}
}
