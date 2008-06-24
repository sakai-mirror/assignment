package org.sakaiproject.assignment.impl;

import org.sakaiproject.assignment.api.SubmissionAttachment;

public class BaseSubmissionAttachment implements SubmissionAttachment{
	
	/** 
	 * The submission id
	 */
	private String submissionId;
	
	/**
	 * The version id
	 */
	private String versionId;
	
	/**
	 * The attachment id
	 */
	private String attachmentId;
	
	/**
	 * Constructor
	 */
	public BaseSubmissionAttachment() {
		super();
	}

	/**
	 * Constructor
	 * @param submissionId
	 * @param versionId
	 * @param attachmentId
	 */
	public BaseSubmissionAttachment(String submissionId, String versionId,
			String attachmentId) {
		super();
		this.submissionId = submissionId;
		this.versionId = versionId;
		this.attachmentId = attachmentId;
	}

	/**
	 * get the submission id
	 * @return
	 */
	public String getSubmissionId() {
		return submissionId;
	}

	/**
	 * set the submission id
	 * @param submissionId
	 */
	public void setSubmissionId(String submissionId) {
		this.submissionId = submissionId;
	}

	/**
	 * get the submission version id
	 * @return
	 */
	public String getVersionId() {
		return versionId;
	}

	/**
	 * set the submission version id
	 * @param versionId
	 */
	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	/**
	 * get the attachment id
	 * @return
	 */
	public String getAttachmentId() {
		return attachmentId;
	}

	/**
	 * set the attachment id
	 * @param attachmentId
	 */
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}

}
