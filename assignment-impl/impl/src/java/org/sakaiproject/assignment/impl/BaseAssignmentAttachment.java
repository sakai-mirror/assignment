package org.sakaiproject.assignment.impl;

import org.sakaiproject.assignment.api.AssignmentAttachment;

public class BaseAssignmentAttachment implements AssignmentAttachment {
	
	/**
	 * the associated assignment id
	 */
	private String assignmentId;
	
	/**
	 * the attachment id
	 */
	private String attachmentId;

	/**
	 * get assignment id
	 * @return
	 */
	public String getAssignmentId() {
		return assignmentId;
	}

	/**
	 * set assignment id
	 * @param assignmentId
	 */
	public void setAssignmentId(String assignmentId) {
		this.assignmentId = assignmentId;
	}

	/**
	 * get attachment id
	 * @return
	 */
	public String getAttachmentId() {
		return attachmentId;
	}

	/**
	 * set attachment id
	 * @param attachmentId
	 */
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}

}
