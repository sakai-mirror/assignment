package org.sakaiproject.assignment.api;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.assignment.api.FeedbackAttachment;
import org.sakaiproject.assignment.api.SubmissionAttachment;

/**
 * A version of submission
 * @author zqian
 *
 */
public interface AssignmentSubmissionVersion {

	public String getSubmissionId();

	public void setSubmissionId(String submissionId);

	public String getVersionId();

	public void setVersionId(String versionId);

	public Date getCreatedDate();

	public void setCreatedDate(Date createdDate);

	public Date getModifiedDate();

	public void setModifiedDate(Date modifiedDate);

	public Date getSubmittedDate();

	public void setSubmittedDate(Date submittedDate);

	public Date getReleasedDate();

	public void setReleasedDate(Date releasedDate);

	public String getCreatedBy();

	public void setCreatedBy(String createdBy);

	public String getModifiedBy();

	public void setModifiedBy(String modifiedBy);

	public String getSubmittedText();

	public void setSubmittedText(String submittedText);

	public String getFeedbackText();

	public void setFeedbackText(String feedbackText);

	public String getFeedbackComment();

	public void setFeedbackComment(String feedbackComment);

	public boolean isDraft();

	public void setDraft(boolean draft);
	
	public String getGrade();

	public void setGrade(String grade);

	public String getReviewReportUrl();

	public void setReviewReportUrl(String reviewReportUrl);

	public String getReviewReportScore();

	public void setReviewReportScore(String reviewReportScore);

	public String getReviewStatus();

	public void setReviewStatus(String reviewStatus);

	public String getReviewIconUrl();

	public void setReviewIconUrl(String reviewIconUrl);

	public Set<FeedbackAttachment> getFeedbackAttachment();

	public void setFeedbackAttachment(Set<FeedbackAttachment> feedbackAttachment);

	public Set<SubmissionAttachment> getSubmissionAttachment();

	public void setSubmissionAttachment(
			Set<SubmissionAttachment> submissionAttachment);
	
	/**
	 * Post attachments to the content review service
	 * @param attachments
	 */
	public void postAttachment(List attachments);
	
}
