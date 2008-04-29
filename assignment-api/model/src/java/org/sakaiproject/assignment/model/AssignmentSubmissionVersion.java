package org.sakaiproject.assignment.model;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.sakaiproject.assignment.model.AssignmentSubmission;
import org.sakaiproject.assignment.model.FeedbackAttachment;
import org.sakaiproject.assignment.model.SubmissionAttachment;
import org.sakaiproject.assignment.model.constants.AssignmentConstants;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.time.api.Time;

import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.content.api.ResourceType;
import org.sakaiproject.content.api.GroupAwareEntity.AccessMode;
import org.sakaiproject.content.api.ContentHostingService;

import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.entity.cover.EntityManager;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.contentreview.service.ContentReviewService;

import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.exception.IdUnusedException;

public class AssignmentSubmissionVersion {	
	/** Our logger. */
	private static Log log = LogFactory.getLog(AssignmentSubmissionVersion.class);
	
	private ContentReviewService contentReviewService;
	public String getReportURL(Long score) {
		getContentReviewService();
		return contentReviewService.getIconUrlforScore(score);
	}
	
	private void getContentReviewService() {
		if (contentReviewService == null)
		{
			contentReviewService = (ContentReviewService) ComponentManager.get(ContentReviewService.class.getName());
		}
	}
	
	public void setContentReviewService(ContentReviewService contentReviewService) {
		this.contentReviewService = contentReviewService;
	}
	
	private ContentHostingService contentHostingService = (ContentHostingService) ComponentManager.get(ContentHostingService.class.getName());  
	
	private Long id;
	/** the Hibernate version number */
    private int hibernateVersion;
	private AssignmentSubmission assignmentSubmission;
	private String submitterId;
	private Date submittedTime;
	private Date releasedTime;
	
	private List<SubmissionAttachment> submittedAttachments;
	private List<FeedbackAttachment> feedbackAttachments;
	private String submittedText;
	private String feedbackComment;
	private String feedbackText;
	private String grade;
	private boolean draft;
	private boolean returned;
	private boolean honorPledgeFlag;
	/*private boolean returned;
	private boolean graded;
	private boolean gradeReleased;*/
	
	//The score given by the review service
	private int reviewScore;
	private String reviewReportUrl;
	// The report given by the content review service
	private String reviewReport;
	// The status of the review service
	private String reviewStatus;
	private String reviewIconUrl;
	
	/** create and last modified */
	private String createdBy;
	private Date createdTime;
	private String lastModifiedBy;
	private Date lastModifiedTime;
	

	private Set<FeedbackAttachment> feedbackAttachSet;
	private Set<SubmissionAttachment> submissionAttachSet;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public AssignmentSubmission getAssignmentSubmission() {
		return assignmentSubmission;
	}
	public void setAssignmentSubmission(AssignmentSubmission assignmentSubmission) {
		this.assignmentSubmission = assignmentSubmission;
	}
	public String getSubmitterId() {
		return submitterId;
	}
	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}
	public List<SubmissionAttachment> getSubmittedAttachments() {
		return submittedAttachments;
	}
	public void setSubmittedAttachments(
			List<SubmissionAttachment> submittedAttachments) {
		this.submittedAttachments = submittedAttachments;
	}
	public List<FeedbackAttachment> getFeedbackAttachments() {
		return feedbackAttachments;
	}
	public void setFeedbackAttachments(List<FeedbackAttachment> feedbackAttachments) {
		this.feedbackAttachments = feedbackAttachments;
	}
	public String getSubmittedText() {
		return submittedText;
	}
	public void setSubmittedText(String submittedText) {
		this.submittedText = submittedText;
	}
	public String getFeedbackComment() {
		return feedbackComment;
	}
	public void setFeedbackComment(String feedbackComment) {
		this.feedbackComment = feedbackComment;
	}
	public String getFeedbackText() {
		return feedbackText;
	}
	public void setFeedbackText(String feedbackText) {
		this.feedbackText = feedbackText;
	}
	public String getGrade() {
		return grade;
	}
	public void setGrade(String grade) {
		this.grade = grade;
	}
	public boolean isDraft() {
		return draft;
	}
	public void setDraft(boolean draft) {
		this.draft = draft;
	}
	public int getReviewScore() {
		return reviewScore;
	}
	public void setReviewScore(int reviewScore) {
		this.reviewScore = reviewScore;
	}
	public String getReviewReport() {
		return reviewReport;
	}
	public void setReviewReport(String reviewReport) {
		this.reviewReport = reviewReport;
	}
	public String getReviewStatus() {
		return reviewStatus;
	}
	public void setReviewStatus(String reviewStatus) {
		this.reviewStatus = reviewStatus;
	}
	public String getReviewIconUrl() {
		return reviewIconUrl;
	}
	public void setReviewIconUrl(String reviewIconUrl) {
		this.reviewIconUrl = reviewIconUrl;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Date getCreatedTime() {
		return createdTime;
	}
	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}
	public String getLastModifiedBy() {
		return lastModifiedBy;
	}
	public void setLastModifiedBy(String lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	public Date getLastModifiedTime() {
		return lastModifiedTime;
	}
	public void setLastModifiedTime(Date lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}
	public int getHibernateVersion() {
		return hibernateVersion;
	}
	public void setHibernateVersion(int hibernateVersion) {
		this.hibernateVersion = hibernateVersion;
	}
	public boolean isReturned() {
		return returned;
	}
	public void setReturned(boolean returned) {
		this.returned = returned;
	}


	public AssignmentSubmissionVersion(
			ContentHostingService contentHostingService, Long id,
			int hibernateVersion, AssignmentSubmission assignmentSubmission,
			String submitterId, Date submittedTime, Date releasedTime,
			List<SubmissionAttachment> submittedAttachments,
			List<FeedbackAttachment> feedbackAttachments, String submittedText,
			String feedbackComment, String feedbackText, String grade,
			boolean draft, boolean returned, boolean honorPledgeFlag,
			int reviewScore, String reviewReport, String reviewStatus,
			String reviewIconUrl, String createdBy, Date createdTime,
			String lastModifiedBy, Date lastModifiedTime) {
		super();
		this.contentHostingService = contentHostingService;
		this.id = id;
		this.hibernateVersion = hibernateVersion;
		this.assignmentSubmission = assignmentSubmission;
		this.submitterId = submitterId;
		this.submittedTime = submittedTime;
		this.releasedTime = releasedTime;
		this.submittedAttachments = submittedAttachments;
		this.feedbackAttachments = feedbackAttachments;
		this.submittedText = submittedText;
		this.feedbackComment = feedbackComment;
		this.feedbackText = feedbackText;
		this.grade = grade;
		this.draft = draft;
		this.returned = returned;
		this.honorPledgeFlag = honorPledgeFlag;
		this.reviewScore = reviewScore;
		this.reviewReport = reviewReport;
		this.reviewStatus = reviewStatus;
		this.reviewIconUrl = reviewIconUrl;
		this.createdBy = createdBy;
		this.createdTime = createdTime;
		this.lastModifiedBy = lastModifiedBy;
		this.lastModifiedTime = lastModifiedTime;
	}

	/**
	 * default constructor
	 */
	public AssignmentSubmissionVersion()
	{
		
	}
	
	public String getReference()
	{
		AssignmentSubmission submission = getAssignmentSubmission();
		StringBuilder sb = new StringBuilder();
		sb.append(AssignmentConstants.REFERENCE_ROOT);
		sb.append(Entity.SEPARATOR);
		sb.append(AssignmentConstants.ASSIGNMENT_TYPE);
		sb.append(Entity.SEPARATOR);
		sb.append(submission.getAssignment().getContext());
		sb.append(Entity.SEPARATOR);
		sb.append(Long.toString(submission.getId()));		
		sb.append(Entity.SEPARATOR);
		sb.append(Long.toString(getId()));
		return sb.toString();
	}
	public boolean isHonorPledgeFlag() {
		return honorPledgeFlag;
	}
	public void setHonorPledgeFlag(boolean honorPledgeFlag) {
		this.honorPledgeFlag = honorPledgeFlag;
	}
	
	public void postAttachment(List attachments){
		//Send the attachment to the review service

		try {
			ContentResource cr = getFirstAcceptableAttachement(attachments);
			Assignment ass = this.getAssignmentSubmission().getAssignment();
			contentReviewService.queueContent(null, null, ass.getReference(), cr.getId());
		} catch (QueueException qe) {
			log.warn(this + " BaseAssignmentSubmissionEdit postAttachment: Unable to add content to Content Review queue: " + qe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private ContentResource getFirstAcceptableAttachement(List attachments) {
		
		for( int i =0; i < attachments.size();i++ ) { 
			Reference attachment = (Reference)attachments.get(i);
			try {
				ContentResource res = contentHostingService.getResource(attachment.getId());
				if (contentReviewService.isAcceptableContent(res)) {
					return res;
				}
			} catch (PermissionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IdUnusedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
		}
		return null;
	}

	public Date getSubmittedTime() {
		return submittedTime;
	}

	public void setSubmittedTime(Date submittedTime) {
		this.submittedTime = submittedTime;
	}

	public Date getReleasedTime() {
		return releasedTime;
	}

	public void setReleasedTime(Date releasedTime) {
		this.releasedTime = releasedTime;
	}

	public String getReviewReportUrl() {
		return reviewReportUrl;
	}

	public void setReviewReportUrl(String reviewReportUrl) {
		this.reviewReportUrl = reviewReportUrl;
	}

	public Set<FeedbackAttachment> getFeedbackAttachSet() {
		return feedbackAttachSet;
	}

	public void setFeedbackAttachSet(Set<FeedbackAttachment> feedbackAttachSet) {
		this.feedbackAttachSet = feedbackAttachSet;
	}

	public Set<SubmissionAttachment> getSubmissionAttachSet() {
		return submissionAttachSet;
	}

	public void setSubmissionAttachSet(Set<SubmissionAttachment> submissionAttachSet) {
		this.submissionAttachSet = submissionAttachSet;
	}
}
