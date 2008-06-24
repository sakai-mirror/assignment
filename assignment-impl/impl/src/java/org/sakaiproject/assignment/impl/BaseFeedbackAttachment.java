package org.sakaiproject.assignment.impl;

import org.sakaiproject.assignment.api.FeedbackAttachment;

public class BaseFeedbackAttachment implements FeedbackAttachment {
	/**
	 * The submission id
	 */
	private String submissionId;
	
	/**
	 * The submission version id
	 */
	private String versionId;
	
	/**
	 * The attachment id
	 */
	private String attachmentId;

	/**
	 * Constructor
	 */
	public BaseFeedbackAttachment() {
		super();
	}

	/**
	 * Constructor
	 * @param submissionId
	 * @param versionId
	 * @param attachmentId
	 */
	public BaseFeedbackAttachment(String submissionId, String versionId,
			String attachmentId) {
		super();
		this.submissionId = submissionId;
		this.versionId = versionId;
		this.attachmentId = attachmentId;
	}

	/**
	 * Get the submission id
	 * @return
	 */
	public String getSubmissionId() {
		return submissionId;
	}

	/**
	 * Set the submission id
	 * @param submissionId
	 */
	public void setSubmissionId(String submissionId) {
		this.submissionId = submissionId;
	}

	/**
	 * Get the submission version id
	 * @return
	 */
	public String getVersionId() {
		return versionId;
	}

	/**
	 * Set the submission version id
	 * @param versionId
	 */
	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	/**
	 * Get the attachment id
	 * @return
	 */
	public String getAttachmentId() {
		return attachmentId;
	}

	/**
	 * Set the attachment id
	 * @param attachmentId
	 */
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}
}
