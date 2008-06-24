package org.sakaiproject.assignment.impl;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.assignment.api.AssignmentSubmissionVersion;
import org.sakaiproject.assignment.api.FeedbackAttachment;
import org.sakaiproject.assignment.api.SubmissionAttachment;

public class BaseAssignmentSubmissionVersion implements AssignmentSubmissionVersion {

	/** 
	 * The id of submission object
	 */
	private String submissionId;
	
	/**
	 * The version id 
	 */
	private String versionId;
	
	private Date createdDate;
	
	private Date modifiedDate;
	
	private Date submittedDate;
	
	private Date releasedDate;
	
	private String createdBy;
	
	private String modifiedBy;
	
	private String submittedText;
	
	private String feedbackText;
	
	private String feedbackComment;
	
	private boolean draft;
	
	private String grade;
	
	private String reviewReportUrl;
	
	private String reviewReportScore;
	
	private String reviewStatus;
	
	private String reviewIconUrl;
	
	private Set<FeedbackAttachment> feedbackAttachment;
	
	private Set<SubmissionAttachment> submissionAttachment;
	
	public BaseAssignmentSubmissionVersion() {
		super();
	}

	public BaseAssignmentSubmissionVersion(String submissionId, String versionId,
			Date createdDate, Date modifiedDate, Date submittedDate,
			Date releasedDate, String createdBy, String modifiedBy,
			String submittedText, String feedbackText, String feedbackComment,
			boolean draft, String grade, String reviewReportUrl, String reviewReportScore,
			String reviewStatus, String reviewIconUrl,
			Set<FeedbackAttachment> feedbackAttachment,
			Set<SubmissionAttachment> submissionAttachment) {
		super();
		this.submissionId = submissionId;
		this.versionId = versionId;
		this.createdDate = createdDate;
		this.modifiedDate = modifiedDate;
		this.submittedDate = submittedDate;
		this.releasedDate = releasedDate;
		this.createdBy = createdBy;
		this.modifiedBy = modifiedBy;
		this.submittedText = submittedText;
		this.feedbackText = feedbackText;
		this.feedbackComment = feedbackComment;
		this.draft = draft;
		this.grade = grade;
		this.reviewReportUrl = reviewReportUrl;
		this.reviewReportScore = reviewReportScore;
		this.reviewStatus = reviewStatus;
		this.reviewIconUrl = reviewIconUrl;
		this.feedbackAttachment = feedbackAttachment;
		this.submissionAttachment = submissionAttachment;
	}

	public String getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(String submissionId) {
		this.submissionId = submissionId;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
	}

	public Date getSubmittedDate() {
		return submittedDate;
	}

	public void setSubmittedDate(Date submittedDate) {
		this.submittedDate = submittedDate;
	}

	public Date getReleasedDate() {
		return releasedDate;
	}

	public void setReleasedDate(Date releasedDate) {
		this.releasedDate = releasedDate;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public String getSubmittedText() {
		return submittedText;
	}

	public void setSubmittedText(String submittedText) {
		this.submittedText = submittedText;
	}

	public String getFeedbackText() {
		return feedbackText;
	}

	public void setFeedbackText(String feedbackText) {
		this.feedbackText = feedbackText;
	}

	public String getFeedbackComment() {
		return feedbackComment;
	}

	public void setFeedbackComment(String feedbackComment) {
		this.feedbackComment = feedbackComment;
	}

	public boolean isDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	
	public String getGrade() {
		return grade;
	}

	public void setGrade(String grade) {
		this.grade = grade;
	}

	public String getReviewReportUrl() {
		return reviewReportUrl;
	}

	public void setReviewReportUrl(String reviewReportUrl) {
		this.reviewReportUrl = reviewReportUrl;
	}

	public String getReviewReportScore() {
		return reviewReportScore;
	}

	public void setReviewReportScore(String reviewReportScore) {
		this.reviewReportScore = reviewReportScore;
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

	public Set<FeedbackAttachment> getFeedbackAttachment() {
		return feedbackAttachment;
	}

	public void setFeedbackAttachment(Set<FeedbackAttachment> feedbackAttachment) {
		this.feedbackAttachment = feedbackAttachment;
	}

	public Set<SubmissionAttachment> getSubmissionAttachment() {
		return submissionAttachment;
	}

	public void setSubmissionAttachment(
			Set<SubmissionAttachment> submissionAttachment) {
		this.submissionAttachment = submissionAttachment;
	}
	
	public void postAttachment(List attachments)
	{
		//TODO
	}
}
