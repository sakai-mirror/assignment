package org.sakaiproject.assignment.api;

/**
 * A class to hold attachment associated with an assignment
 * @author zqian
 *
 */
public interface AssignmentAttachment {
	
	/**
	 * get assignment id
	 * @return
	 */
	public String getAssignmentId();

	/**
	 * set assignment id
	 * @param assignmentId
	 */
	public void setAssignmentId(String assignmentId);

	/**
	 * get attachment id
	 * @return
	 */
	public String getAttachmentId();

	/**
	 * set attachment id
	 * @param attachmentId
	 */
	public void setAttachmentId(String attachmentId);

}