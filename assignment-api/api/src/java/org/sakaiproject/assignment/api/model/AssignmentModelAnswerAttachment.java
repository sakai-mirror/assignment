package org.sakaiproject.assignment.api.model;

/**
 * the attachment for the AssigmentSupplementItem object
 * @author zqian
 *
 */
public class AssignmentModelAnswerAttachment {
	private Long id;
	private String attachmentId;
	private AssignmentModelAnswerItem assignmentModelAnswerItem;
	
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
	public AssignmentModelAnswerItem getAssignmentModelAnswerItem()
	{
		return this.assignmentModelAnswerItem;
	}
	public void setAssignmentModelAnswerItem(AssignmentModelAnswerItem assignmentModelAnswerItem)
	{
		this.assignmentModelAnswerItem = assignmentModelAnswerItem;
	}
	

}
