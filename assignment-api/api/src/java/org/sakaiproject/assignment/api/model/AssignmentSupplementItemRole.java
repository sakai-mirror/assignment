package org.sakaiproject.assignment.api.model;

/**
 * the role that can view the AssignmentSupplementItem object
 * @author zqian
 *
 */
public class AssignmentSupplementItemRole {
	private Long id;
	private String roleId;
	private AssignmentSupplementItem assignmentSupplementItem;
	
	public Long getId() {
		return this.id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getRoleId() {
		return this.roleId;
	}
	public void setRoleId(String roleId) {
		this.roleId = roleId;
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
