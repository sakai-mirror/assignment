package org.sakaiproject.assignment.api;

/**
 * The attachment(s) associated with the specified submission and its version
 * @author zqian
 *
 */
public interface SubmissionAttachment {

	/**
	 * get the submission id
	 * @return
	 */
	public String getSubmissionId();

	/**
	 * set the submission id
	 * @param submissionId
	 */
	public void setSubmissionId(String submissionId);

	/**
	 * get the submission version id
	 * @return
	 */
	public String getVersionId();

	/**
	 * set the submission version id
	 * @param versionId
	 */
	public void setVersionId(String versionId);

	/**
	 * get the attachment id
	 * @return
	 */
	public String getAttachmentId();

	/**
	 * set the attachment id
	 * @param attachmentId
	 */
	public void setAttachmentId(String attachmentId);
}
