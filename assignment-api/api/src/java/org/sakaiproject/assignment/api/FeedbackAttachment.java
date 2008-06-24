package org.sakaiproject.assignment.api;

/**
 * A class for submission feedback attachment
 * @author zqian
 *
 */
public interface FeedbackAttachment {
	
	/**
	 * Get the submission id
	 * @return
	 */
	public String getSubmissionId();

	/**
	 * Set the submission id
	 * @param submissionId
	 */
	public void setSubmissionId(String submissionId);

	/**
	 * Get the submission version id
	 * @return
	 */
	public String getVersionId();

	/**
	 * Set the submission version id
	 * @param versionId
	 */
	public void setVersionId(String versionId);

	/**
	 * Get the attachment id
	 * @return
	 */
	public String getAttachmentId();

	/**
	 * Set the attachment id
	 * @param attachmentId
	 */
	public void setAttachmentId(String attachmentId);

}
