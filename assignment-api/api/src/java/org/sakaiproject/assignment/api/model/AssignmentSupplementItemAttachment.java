package org.sakaiproject.assignment.api.model;

import org.sakaiproject.assignment.api.model.AssignmentSupplementItem;

/**
 * the attachment for the AssigmentSupplementItem object
 * @author zqian
 *
 */
public class AssignmentSupplementItemAttachment {
	private Long id;
	private String attachmentId;
	private AssignmentSupplementItem assignmentSupplementItem;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getAttachmentId() {
		return attachmentId;
	}
	public void setAttachmentId(String attachmentId) {
		this.attachmentId = attachmentId;
	}
	public AssignmentSupplementItem getAssignmentSupplementItem()
	{
		return this.assignmentSupplementItem;
	}
	public void setAssignmentSupplementItem(AssignmentSupplementItem assignmentSupplementItem)
	{
		this.assignmentSupplementItem = assignmentSupplementItem;
	}
	

}
