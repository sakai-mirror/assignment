package org.sakaiproject.assignment.impl;

import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.content.api.ContentResource;
import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.exception.TypeException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.util.DefaultEntityHandler;
import org.sakaiproject.util.FormattedText;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.sakaiproject.contentreview.exception.QueueException;
import org.sakaiproject.contentreview.service.ContentReviewService;

/**********************************************************************************************************************************************************************************************************************************************************
 * AssignmentSubmission implementation
 *********************************************************************************************************************************************************************************************************************************************************/

public class BaseAssignmentSubmission implements AssignmentSubmission
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(BaseAssignmentSubmission.class);
	
	/** the resource bundle */
	private static ResourceLoader rb = new ResourceLoader("assignment");
	
	ContentReviewService contentReviewService = (ContentReviewService) ComponentManager.get(ContentReviewService.class.getName());
	
	private BaseAssignmentService assignmentService = null;
	
	/** The event code */
	protected String m_event = null;
	
	protected final String STATUS_DRAFT = "Drafted";

	protected final String STATUS_SUBMITTED = "Submitted";

	protected final String STATUS_RETURNED = "Returned";

	protected final String STATUS_GRADED = "Graded";

	protected ResourcePropertiesEdit m_properties;

	protected String m_id;

	protected String m_assignment;

	protected String m_context;

	protected List m_submitters;

	protected Time m_timeSubmitted;

	protected Time m_timeReturned;

	protected Time m_timeLastModified;

	protected List m_submittedAttachments;

	protected List m_feedbackAttachments;

	protected String m_submittedText;

	protected String m_feedbackComment;

	protected String m_feedbackText;

	protected String m_grade;

	protected boolean m_submitted;

	protected boolean m_returned;

	protected boolean m_graded;

	protected boolean m_gradeReleased;

	protected boolean m_honorPledgeFlag;

	
	//The score given by the review service
	protected int m_reviewScore;
	// The report given by the content review service
	protected String m_reviewReport;
	// The status of the review service
	protected String m_reviewStatus;
	
	protected String m_reviewIconUrl;
	
	// return the variables
	// Get new values from review service if defaults
	public int getReviewScore() {
		// Code to get updated score if default
		M_log.warn(this + " getReviewScore for submission " + this.getId() + " and review service is: " + (this.getAssignment().getAllowReviewService()));
		if (!this.getAssignment().getAllowReviewService()) {
			M_log.warn(this + " getReviewScore Content review is not enabled for this assignment");
			return -2;
		}
		
		if (m_submittedAttachments.isEmpty()) M_log.warn(this + " getReviewScore No attachments submitted.");
		else
		{
			try {
				//we need to find the first attachment the CR will accept
				
				ContentResource cr = getFirstAcceptableAttachement();
				if (cr == null )
				{
					M_log.warn(this + " getReviewScore No suitable attachments found in list");
					return -2;
				}
				String contentId = cr.getId();
				M_log.warn(this + " getReviewScore checking for socre for content: " + contentId);
				int score =contentReviewService.getReviewScore(contentId);
				M_log.warn(this + " getReviewScore CR returned a score of: " + score);
				return contentReviewService.getReviewScore(contentId);
					
			} 
			catch (QueueException cie) {
				//should we add the item
				try {
					
						M_log.warn(this + " getReviewScore Item is not in queue we will try add it");
						ContentResource cr = getFirstAcceptableAttachement();
						if (cr == null )
						{
							M_log.warn(this + " getReviewScore No suitable attachments found in list");
							return -2;
						}
						String contentId = cr.getId();
						String userId = (String)this.getSubmitterIds().get(0);
						try {
							contentReviewService.queueContent(userId, null, getAssignment().getReference(), contentId);
						}
						catch (QueueException qe) {
							M_log.warn(this + " getReviewScore Unable to queue content with content review Service: " + qe.getMessage());
						}
							
						
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				return -1;
				
			}
			catch (Exception e) {
				
					M_log.warn(this + " getReviewScore " + e.getMessage());
				return -1;
			}
				
		}
		//No assignment available
		return -2;
		
	}
	
	public String getReviewReport() {
//		 Code to get updated report if default
		if (m_submittedAttachments.isEmpty()) M_log.warn(this + " getReviewReport No attachments submitted.");
		else
		{
			try {
				ContentResource cr = getFirstAcceptableAttachement();
				if (cr == null )
				{
					M_log.warn(this + " getReviewReport No suitable attachments found in list");
					return "error";
				}
				
				String contentId = cr.getId();
				
				if (SecurityService.unlock(UserDirectoryService.getCurrentUser(), "asn.grade", "/site/" + this.m_context))
					return contentReviewService.getReviewReportInstructor(contentId);
				else
					return contentReviewService.getReviewReportStudent(contentId);
				
			} catch (Exception e) {
				//e.printStackTrace();
				M_log.warn(this + ":getReviewReport() " + e.getMessage());
				return "Error";
			}
				
		}
		return "Error";
	}
	
	private ContentResource getFirstAcceptableAttachement() {
		String contentId = null;
		try {
		for( int i =0; i < m_submittedAttachments.size();i++ ) {
			Reference ref = (Reference)m_submittedAttachments.get(i);
			ContentResource contentResource = (ContentResource)ref.getEntity();
			if (contentReviewService.isAcceptableContent(contentResource)) {
				return (ContentResource)contentResource;
			}
		}
		}
		catch (Exception e) {
			M_log.warn(this + ":getFirstAcceptableAttachment() " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	public String getReviewStatus() {
		return m_reviewStatus;
	}
	
	public String getReviewIconUrl() {
		if (m_reviewIconUrl == null )
			m_reviewIconUrl = contentReviewService.getIconUrlforScore(new Long(this.getReviewScore()));
			
		return m_reviewIconUrl;
	}
	
	/**
	 * constructor
	 */
	public BaseAssignmentSubmission()
	{
		m_properties = new BaseResourcePropertiesEdit();
	}// constructor
	
	/**
	 * Copy constructor.
	 */
	public BaseAssignmentSubmission(AssignmentSubmission submission)
	{
		setAll(submission);
	}

	/**
	 * Constructor used by addSubmission.
	 */
	public BaseAssignmentSubmission(String id, String assignId, String submitterId, String submitTime, String submitted, String graded)
	{
		
		// must set initial review status
		m_reviewStatus = "";
		m_reviewScore = -1;
		m_reviewReport = "Not available yet";
		
		m_id = id;
		m_assignment = assignId;
		m_properties = new BaseResourcePropertiesEdit();
		AssignmentUtil.addLiveProperties(m_properties);
		m_submitters = new Vector();
		m_feedbackAttachments = assignmentService.entityManager().newReferenceList();
		m_submittedAttachments = assignmentService.entityManager().newReferenceList();
		m_submitted = false;
		m_returned = false;
		m_graded = false;
		m_gradeReleased = false;
		m_submittedText = "";
		m_feedbackComment = "";
		m_feedbackText = "";
		m_grade = "";
		m_timeLastModified = TimeService.newTime();

		if (submitterId == null)
		{
			String currentUser = SessionManager.getCurrentSessionUserId();
			if (currentUser == null) currentUser = "";
			m_submitters.add(currentUser);
		}
		else
		{
			m_submitters.add(submitterId);
		}
		
		if (submitted != null)
		{
			m_submitted = Boolean.valueOf(submitted).booleanValue();
		}
		
		if (graded != null)
		{
			m_graded = Boolean.valueOf(graded).booleanValue();
		}
	}

	
	// todo work out what this does
	/**
	 * Reads the AssignmentSubmission's attribute values from xml.
	 * 
	 * @param s -
	 *        Data structure holding the xml info.
	 */
	public BaseAssignmentSubmission(Element el)
	{
		int numAttributes = 0;
		String intString = null;
		String attributeString = null;
		String tempString = null;
		Reference tempReference = null;

		M_log.warn(this + " BaseAssigmentSubmission : ENTERING STORAGE CONSTRUCTOR");

		m_id = el.getAttribute("id");
		m_context = el.getAttribute("context");

		// %%%zqian
		// read the scaled grade point first; if there is none, get the old grade value
		String grade = StringUtil.trimToNull(el.getAttribute("scaled_grade"));
		if (grade == null)
		{
			grade = StringUtil.trimToNull(el.getAttribute("grade"));
			if (grade != null)
			{
				try
				{
					Integer.parseInt(grade);
					// for the grades in points, multiple those by 10
					grade = grade + "0";
				}
				catch (Exception e)
				{
					M_log.warn(this + ":BaseAssignmentSubmission(Element el) " + e.getMessage());
				}
			}
		}
		m_grade = grade;

		m_assignment = el.getAttribute("assignment");

		m_timeSubmitted = AssignmentUtil.getTimeObject(el.getAttribute("datesubmitted"));
		m_timeReturned = AssignmentUtil.getTimeObject(el.getAttribute("datereturned"));
		m_assignment = el.getAttribute("assignment");
		m_timeLastModified = AssignmentUtil.getTimeObject(el.getAttribute("lastmod"));

		m_submitted = AssignmentUtil.getBool(el.getAttribute("submitted"));
		m_returned = AssignmentUtil.getBool(el.getAttribute("returned"));
		m_graded = AssignmentUtil.getBool(el.getAttribute("graded"));
		m_gradeReleased = AssignmentUtil.getBool(el.getAttribute("gradereleased"));
		m_honorPledgeFlag = AssignmentUtil.getBool(el.getAttribute("pledgeflag"));

		m_submittedText = FormattedText.decodeFormattedTextAttribute(el, "submittedtext");
		m_feedbackComment = FormattedText.decodeFormattedTextAttribute(el, "feedbackcomment");
		m_feedbackText = FormattedText.decodeFormattedTextAttribute(el, "feedbacktext");

		// READ THE SUBMITTERS
		m_submitters = new Vector();
		M_log.warn(this + " BaseAssignmentSubmission : CONSTRUCTOR : Reading submitters : ");
		intString = el.getAttribute("numberofsubmitters");
		try
		{
			numAttributes = Integer.parseInt(intString);

			for (int x = 0; x < numAttributes; x++)
			{
				attributeString = "submitter" + x;
				tempString = el.getAttribute(attributeString);
				if (tempString != null) m_submitters.add(tempString);
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : Exception reading submitters : " + e);
		}

		// READ THE FEEDBACK ATTACHMENTS
		m_feedbackAttachments = assignmentService.entityManager().newReferenceList();
		intString = el.getAttribute("numberoffeedbackattachments");
		
			M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : num feedback attachments : " + intString);
		try
		{
			numAttributes = Integer.parseInt(intString);

			for (int x = 0; x < numAttributes; x++)
			{
				attributeString = "feedbackattachment" + x;
				tempString = el.getAttribute(attributeString);
				if (tempString != null)
				{
					tempReference = assignmentService.entityManager().newReference(tempString);
					m_feedbackAttachments.add(tempReference);
					
						M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : " + attributeString + " : "
								+ tempString);
				}
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : Exception reading feedback attachments : " + e);
		}

		// READ THE SUBMITTED ATTACHMENTS
		m_submittedAttachments = assignmentService.entityManager().newReferenceList();
		intString = el.getAttribute("numberofsubmittedattachments");
		
			M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : num submitted attachments : " + intString);
		try
		{
			numAttributes = Integer.parseInt(intString);

			for (int x = 0; x < numAttributes; x++)
			{
				attributeString = "submittedattachment" + x;
				tempString = el.getAttribute(attributeString);
				if (tempString != null)
				{
					tempReference = assignmentService.entityManager().newReference(tempString);
					m_submittedAttachments.add(tempReference);
					
						M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : " + attributeString + " : "
								+ tempString);
				}
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : Exception reading submitted attachments : " + e);
		}

		// READ THE PROPERTIES, SUBMITTED TEXT, FEEDBACK COMMENT, FEEDBACK TEXT
		NodeList children = el.getChildNodes();
		final int length = children.getLength();
		for (int i = 0; i < length; i++)
		{
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			Element element = (Element) child;

			// look for properties
			if (element.getTagName().equals("properties"))
			{
				// re-create properties
				m_properties = new BaseResourcePropertiesEdit(element);
			}
			// old style encoding
			else if (element.getTagName().equals("submittedtext"))
			{
				if ((element.getChildNodes() != null) && (element.getChildNodes().item(0) != null))
				{
					m_submittedText = element.getChildNodes().item(0).getNodeValue();
					
						M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : submittedtext : " + m_submittedText);
				}
				if (m_submittedText == null)
				{
					m_submittedText = "";
				}
			}
			// old style encoding
			else if (element.getTagName().equals("feedbackcomment"))
			{
				if ((element.getChildNodes() != null) && (element.getChildNodes().item(0) != null))
				{
					m_feedbackComment = element.getChildNodes().item(0).getNodeValue();
					
						M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : feedbackcomment : "
								+ m_feedbackComment);
				}
				if (m_feedbackComment == null)
				{
					m_feedbackComment = "";
				}
			}
			// old style encoding
			else if (element.getTagName().equals("feedbacktext"))
			{
				if ((element.getChildNodes() != null) && (element.getChildNodes().item(0) != null))
				{
					m_feedbackText = element.getChildNodes().item(0).getNodeValue();
					
						M_log.warn(this + " BaseAssignmentSubmission: CONSTRUCTOR : FEEDBACK TEXT : " + m_feedbackText);
				}
				if (m_feedbackText == null)
				{
					m_feedbackText = "";
				}
			}
		}

	
		
		try {
			if (el.getAttribute("reviewScore")!=null)
				m_reviewScore = Integer.parseInt(el.getAttribute("reviewScore"));
			else
				m_reviewScore = -1;
		}
		catch (NumberFormatException nfe) {
			m_reviewScore = -1;
			M_log.warn(this + ":BaseAssignmentSubmission(Element) " + nfe.getMessage());
		}
		try {
		// The report given by the content review service
			if (el.getAttribute("reviewReport")!=null)
				m_reviewReport = el.getAttribute("reviewReport");
			else 
				m_reviewReport = "no report available";
			
		// The status of the review service
			if (el.getAttribute("reviewStatus")!=null)
				m_reviewStatus = el.getAttribute("reviewStatus");
			else 
				m_reviewStatus = "";
		}
		catch (Exception e) {
			M_log.error("error constructing Submission: " + e);
		}
		
		//get the review Status from ContentReview rather than using old ones
		m_reviewStatus = this.getReviewStatus();
		m_reviewScore  = this.getReviewScore();
		
		
		
		M_log.warn(this + " BaseAssignmentSubmission: LEAVING STORAGE CONSTRUCTOR");

	}// storage constructor
	
	/**
	 * @param services
	 * @return
	 */
	public ContentHandler getContentHandler(Map<String, Object> services)
	{
		final Entity thisEntity = this;
		return new DefaultEntityHandler()
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.sakaiproject.util.DefaultEntityHandler#startElement(java.lang.String,
			 *      java.lang.String, java.lang.String,
			 *      org.xml.sax.Attributes)
			 */
			@Override
			public void startElement(String uri, String localName, String qName,
					Attributes attributes) throws SAXException
			{
				if (doStartElement(uri, localName, qName, attributes))
				{
					if ("submission".equals(qName) && entity == null)
					{
						try {
							if (attributes.getValue("reviewScore")!=null)
								m_reviewScore = Integer.parseInt(attributes.getValue("reviewScore"));
							else
								m_reviewScore = -1;
						}
						catch (NumberFormatException nfe) {
							m_reviewScore = -1;
							M_log.warn(this + ":AssignmentSubmission:getContentHandler:DefaultEntityHandler " + nfe.getMessage());
						}
						try {
						// The report given by the content review service
							if (attributes.getValue("reviewReport")!=null)
								m_reviewReport = attributes.getValue("reviewReport");
							else 
								m_reviewReport = "no report available";
							
						// The status of the review service
							if (attributes.getValue("reviewStatus")!=null)
								m_reviewStatus = attributes.getValue("reviewStatus");
							else 
								m_reviewStatus = "";
						}
						catch (Exception e) {
							M_log.error("error constructing Submission: " + e);
						}
						
						
						int numAttributes = 0;
						String intString = null;
						String attributeString = null;
						String tempString = null;
						Reference tempReference = null;

						m_id = attributes.getValue("id");
						// M_log.info(this + " BASE SUBMISSION : CONSTRUCTOR : m_id : " + m_id);
						m_context = attributes.getValue("context");
						// M_log.info(this + " BASE SUBMISSION : CONSTRUCTOR : m_context : " + m_context);

						// %%%zqian
						// read the scaled grade point first; if there is none, get the old grade value
						String grade = StringUtil.trimToNull(attributes.getValue("scaled_grade"));
						if (grade == null)
						{
							grade = StringUtil.trimToNull(attributes.getValue("grade"));
							if (grade != null)
							{
								try
								{
									Integer.parseInt(grade);
									// for the grades in points, multiple those by 10
									grade = grade + "0";
								}
								catch (Exception e)
								{
									M_log.warn(this + ":BaseAssignmentSubmission:getContentHanler:DefaultEnityHandler " + e.getMessage());
								}
							}
						}
						m_grade = grade;

						m_assignment = attributes.getValue("assignment");

						m_timeSubmitted = AssignmentUtil.getTimeObject(attributes.getValue("datesubmitted"));
						m_timeReturned = AssignmentUtil.getTimeObject(attributes.getValue("datereturned"));
						m_assignment = attributes.getValue("assignment");
						m_timeLastModified = AssignmentUtil.getTimeObject(attributes.getValue("lastmod"));

						m_submitted = AssignmentUtil.getBool(attributes.getValue("submitted"));
						m_returned = AssignmentUtil.getBool(attributes.getValue("returned"));
						m_graded = AssignmentUtil.getBool(attributes.getValue("graded"));
						m_gradeReleased = AssignmentUtil.getBool(attributes.getValue("gradereleased"));
						m_honorPledgeFlag = AssignmentUtil.getBool(attributes.getValue("pledgeflag"));

						m_submittedText = AssignmentUtil.FormattedTextDecodeFormattedTextAttribute(attributes, "submittedtext");
						m_feedbackComment = AssignmentUtil.FormattedTextDecodeFormattedTextAttribute(attributes, "feedbackcomment");
						m_feedbackText = AssignmentUtil.FormattedTextDecodeFormattedTextAttribute(attributes, "feedbacktext");
						 

						// READ THE SUBMITTERS
						m_submitters = new Vector();
						intString = attributes.getValue("numberofsubmitters");
						try
						{
							numAttributes = Integer.parseInt(intString);

							for (int x = 0; x < numAttributes; x++)
							{
								attributeString = "submitter" + x;
								tempString = attributes.getValue(attributeString);
								if (tempString != null) m_submitters.add(tempString);
							}
						}
						catch (Exception e)
						{
							M_log.warn(this + " BaseAssignmentSubmission getContentHandler : Exception reading submitters : " + e);
						}

						// READ THE FEEDBACK ATTACHMENTS
						m_feedbackAttachments = assignmentService.entityManager().newReferenceList();
						intString = attributes.getValue("numberoffeedbackattachments");
						try
						{
							numAttributes = Integer.parseInt(intString);

							for (int x = 0; x < numAttributes; x++)
							{
								attributeString = "feedbackattachment" + x;
								tempString = attributes.getValue(attributeString);
								if (tempString != null)
								{
									tempReference = assignmentService.entityManager().newReference(tempString);
									m_feedbackAttachments.add(tempReference);
								}
							}
						}
						catch (Exception e)
						{
							M_log.warn(this + " BaseAssignmentSubmission getContentHandler : Exception reading feedback attachments : " + e);
						}

						// READ THE SUBMITTED ATTACHMENTS
						m_submittedAttachments = assignmentService.entityManager().newReferenceList();
						intString = attributes.getValue("numberofsubmittedattachments");
						try
						{
							numAttributes = Integer.parseInt(intString);

							for (int x = 0; x < numAttributes; x++)
							{
								attributeString = "submittedattachment" + x;
								tempString = attributes.getValue(attributeString);
								if (tempString != null)
								{
									tempReference = assignmentService.entityManager().newReference(tempString);
									m_submittedAttachments.add(tempReference);
								}
							}
						}
						catch (Exception e)
						{
							M_log.warn(this + " BaseAssignmentSubmission getContentHandler: Exception reading submitted attachments : " + e);
						}
						
						entity = thisEntity;
					}
				}
			}
		};
	}

	
	/**
	 * Takes the AssignmentContent's attribute values and puts them into the xml document.
	 * 
	 * @param s -
	 *        Data structure holding the object to be stored.
	 * @param doc -
	 *        The xml document.
	 */
	public Element toXml(Document doc, Stack stack)
	{
		M_log.warn(this + " BaseAssignmentSubmission : ENTERING TOXML");

		Element submission = doc.createElement("submission");
		if (stack.isEmpty())
		{
			doc.appendChild(submission);
		}
		else
		{
			((Element) stack.peek()).appendChild(submission);
		}

		stack.push(submission);

		String numItemsString = null;
		String attributeString = null;
		String itemString = null;
		Reference tempReference = null;

		
		submission.setAttribute("reviewScore",Integer.toString(m_reviewScore));
		submission.setAttribute("reviewReport",m_reviewReport);
		submission.setAttribute("reviewStatus",m_reviewStatus);
		
		
		submission.setAttribute("id", m_id);
		submission.setAttribute("context", m_context);
		submission.setAttribute("scaled_grade", m_grade);
		submission.setAttribute("assignment", m_assignment);
		submission.setAttribute("datesubmitted", AssignmentUtil.getTimeString(m_timeSubmitted));
		submission.setAttribute("datereturned", AssignmentUtil.getTimeString(m_timeReturned));
		submission.setAttribute("lastmod", AssignmentUtil.getTimeString(m_timeLastModified));
		submission.setAttribute("submitted", AssignmentUtil.getBoolString(m_submitted));
		submission.setAttribute("returned", AssignmentUtil.getBoolString(m_returned));
		submission.setAttribute("graded", AssignmentUtil.getBoolString(m_graded));
		submission.setAttribute("gradereleased", AssignmentUtil.getBoolString(m_gradeReleased));
		submission.setAttribute("pledgeflag", AssignmentUtil.getBoolString(m_honorPledgeFlag));

		M_log.warn(this + " BaseAssignmentSubmission: SAVED REGULAR PROPERTIES");

		// SAVE THE SUBMITTERS
		numItemsString = "" + m_submitters.size();
		submission.setAttribute("numberofsubmitters", numItemsString);
		for (int x = 0; x < m_submitters.size(); x++)
		{
			attributeString = "submitter" + x;
			itemString = (String) m_submitters.get(x);
			if (itemString != null) submission.setAttribute(attributeString, itemString);
		}

		M_log.warn(this + " BaseAssignmentSubmission: SAVED SUBMITTERS");

		// SAVE THE FEEDBACK ATTACHMENTS
		numItemsString = "" + m_feedbackAttachments.size();
		submission.setAttribute("numberoffeedbackattachments", numItemsString);
		
		M_log.warn("DB : DbCachedStorage : DbCachedAssignmentSubmission : entering fb attach loop : size : "
					+ numItemsString);
		for (int x = 0; x < m_feedbackAttachments.size(); x++)
		{
			attributeString = "feedbackattachment" + x;
			tempReference = (Reference) m_feedbackAttachments.get(x);
			itemString = tempReference.getReference();
			if (itemString != null) submission.setAttribute(attributeString, itemString);
		}

		M_log.warn(this + " BaseAssignmentSubmission: SAVED FEEDBACK ATTACHMENTS");

		// SAVE THE SUBMITTED ATTACHMENTS
		numItemsString = "" + m_submittedAttachments.size();
		submission.setAttribute("numberofsubmittedattachments", numItemsString);
		for (int x = 0; x < m_submittedAttachments.size(); x++)
		{
			attributeString = "submittedattachment" + x;
			tempReference = (Reference) m_submittedAttachments.get(x);
			itemString = tempReference.getReference();
			if (itemString != null) submission.setAttribute(attributeString, itemString);
		}

		M_log.warn(this + " BaseAssignmentSubmission: SAVED SUBMITTED ATTACHMENTS");

		// SAVE THE PROPERTIES
		m_properties.toXml(doc, stack);
		stack.pop();

		FormattedText.encodeFormattedTextAttribute(submission, "submittedtext", m_submittedText);
		FormattedText.encodeFormattedTextAttribute(submission, "feedbackcomment", m_feedbackComment);
		FormattedText.encodeFormattedTextAttribute(submission, "feedbacktext", m_feedbackText);

		M_log.warn(this + " BaseAssignmentSubmission: LEAVING TOXML");

		return submission;

	}// toXml

	
	protected void setAll(AssignmentSubmission submission)
	{
		
		
		m_reviewScore = submission.getReviewScore();
		// The report given by the content review service
		m_reviewReport = submission.getReviewReport();
		// The status of the review service
		m_reviewStatus = submission.getReviewStatus();
		
		
		m_id = submission.getId();
		m_context = submission.getContext();
		m_assignment = submission.getAssignmentId();
		m_grade = submission.getGrade();
		m_submitters = submission.getSubmitterIds();
		m_submitted = submission.getSubmitted();
		m_timeSubmitted = submission.getTimeSubmitted();
		m_timeReturned = submission.getTimeReturned();
		m_timeLastModified = submission.getTimeLastModified();
		m_submittedAttachments = submission.getSubmittedAttachments();
		m_feedbackAttachments = submission.getFeedbackAttachments();
		m_submittedText = submission.getSubmittedText();
		m_feedbackComment = submission.getFeedbackComment();
		m_feedbackText = submission.getFeedbackText();
		m_returned = submission.getReturned();
		m_graded = submission.getGraded();
		m_gradeReleased = submission.getGradeReleased();
		m_honorPledgeFlag = submission.getHonorPledgeFlag();
		m_properties = new BaseResourcePropertiesEdit();
		m_properties.addAll(submission.getProperties());
	}

	/**
	 * Access the URL which can be used to access the resource.
	 * 
	 * @return The URL which can be used to access the resource.
	 */
	public String getUrl()
	{
		return assignmentService.getAccessPoint(false) + Entity.SEPARATOR + "s" + Entity.SEPARATOR + m_context + Entity.SEPARATOR + m_id;

	} // getUrl

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String getReference()
	{
		return assignmentService.submissionReference(m_context, m_id, m_assignment);

	} // getReference

	/**
	 * @inheritDoc
	 */
	public String getReference(String rootProperty)
	{
		return getReference();
	}

	/**
	 * @inheritDoc
	 */
	public String getUrl(String rootProperty)
	{
		return getUrl();
	}

	/**
	 * Access the id of the resource.
	 * 
	 * @return The id.
	 */
	public String getId()
	{
		return m_id;
	}

	/**
	 * Access the resource's properties.
	 * 
	 * @return The resource's properties.
	 */
	public ResourceProperties getProperties()
	{
		return m_properties;
	}

	/******************************************************************************************************************************************************************************************************************************************************
	 * AssignmentSubmission implementation
	 *****************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Access the AssignmentSubmission's context at the time of creation.
	 * 
	 * @return String - the context string.
	 */
	public String getContext()
	{
		return m_context;
	}

	/**
	 * Access the Assignment for this Submission
	 * 
	 * @return the Assignment
	 */
	public Assignment getAssignment()
	{
		Assignment retVal = null;
		if (m_assignment != null)
		{
			try
			{
				retVal = assignmentService.getAssignment(m_assignment);
			}
			catch (Exception e)
			{
				M_log.info(this + ":getAssignment assignment=" + m_assignment + e.getMessage());
			}
		}
		
		// track event
		//EventTrackingService.post(EventTrackingService.newEvent(EVENT_ACCESS_ASSIGNMENT, retVal.getReference(), false));

		return retVal;
	}

	/**
	 * Access the Id for the Assignment for this Submission
	 * 
	 * @return String - the Assignment Id
	 */
	public String getAssignmentId()
	{
		return m_assignment;
	}

	/**
	 * Get whether this is a final submission.
	 * 
	 * @return True if a final submission, false if still a draft.
	 */
	public boolean getSubmitted()
	{
		return m_submitted;
	}

	/**
	 * Access the list of Users who submitted this response to the Assignment.
	 * 
	 * @return Array of User objects.
	 */
	public User[] getSubmitters()
	{
		List retVal = new Vector();
		for (int x = 0; x < m_submitters.size(); x++)
		{
			String userId = (String) m_submitters.get(x);
			try
			{
				retVal.add(UserDirectoryService.getUser(userId));
			}
			catch (Exception e)
			{
				M_log.warn(this + " BaseAssignmentSubmission getSubmitters" + e.getMessage() + userId);
			}
		}
		
		// get the User[] array
		int size = retVal.size();
		User[] rv = new User[size];
		for(int k = 0; k<size; k++)
		{
			rv[k] = (User) retVal.get(k);
		}
		
		return rv;
	}

	/**
	 * Access the list of Users who submitted this response to the Assignment.
	 * 
	 * @return FlexStringArray of user ids.
	 */
	public List getSubmitterIds()
	{
		return m_submitters;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getSubmitterIdString ()
	{
		String rv = "";
		if (m_submitters != null)
		{
			for (int j = 0; j < m_submitters.size(); j++)
			{
				rv = rv.concat((String) m_submitters.get(j));
			}
		}
		return rv;
	}
	
	/**
	 * Set the time at which this response was submitted; null signifies the response is unsubmitted.
	 * 
	 * @return Time of submission.
	 */
	public Time getTimeSubmitted()
	{
		return m_timeSubmitted;
	}

	/**
	 * @inheritDoc
	 */
	public String getTimeSubmittedString()
	{
		if ( m_timeSubmitted == null )
			return "";
		else
			return m_timeSubmitted.toStringLocalFull();
	}

	/**
	 * Get whether the grade has been released.
	 * 
	 * @return True if the Submissions's grade has been released, false otherwise.
	 */
	public boolean getGradeReleased()
	{
		return m_gradeReleased;
	}

	/**
	 * Access the grade recieved.
	 * 
	 * @return The Submission's grade..
	 */
	public String getGrade()
	{
		return m_grade;
	}

	/**
	 * Access the grade recieved.
	 * 
	 * @return The Submission's grade..
	 */
	public String getGradeDisplay()
	{
		Assignment m = getAssignment();
		if (m.getContent().getTypeOfGrade() == Assignment.SCORE_GRADE_TYPE)
		{
			if (m_grade != null && m_grade.length() > 0 && !m_grade.equals("0"))
			{
				try
				{
					Integer.parseInt(m_grade);
					// if point grade, display the grade with one decimal place
					return m_grade.substring(0, m_grade.length() - 1) + "." + m_grade.substring(m_grade.length() - 1);
				}
				catch (Exception e)
				{
					return m_grade;
				}
			}
			else
			{
				return StringUtil.trimToZero(m_grade);
			}
		}
		else
		{
			return StringUtil.trimToZero(m_grade);
		}
	}

	/**
	 * Get the time of last modification;
	 * 
	 * @return The time of last modification.
	 */
	public Time getTimeLastModified()
	{
		return m_timeLastModified;
	}

	/**
	 * Text submitted in response to the Assignment.
	 * 
	 * @return The text of the submission.
	 */
	public String getSubmittedText()
	{
		return m_submittedText;
	}

	/**
	 * Access the list of attachments to this response to the Assignment.
	 * 
	 * @return ReferenceVector of the list of attachments as Reference objects;
	 */
	public List getSubmittedAttachments()
	{
		return m_submittedAttachments;
	}

	/**
	 * Get the general comments by the grader
	 * 
	 * @return The text of the grader's comments; may be null.
	 */
	public String getFeedbackComment()
	{	
		return m_feedbackComment;
	}

	/**
	 * Access the text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @return The text of the grader's feedback.
	 */
	public String getFeedbackText()
	{
		return m_feedbackText;
	}

	/**
	 * Access the formatted text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @return The formatted text of the grader's feedback.
	 */
	public String getFeedbackFormattedText()
	{
		if (m_feedbackText == null || m_feedbackText.length() == 0) 
			return m_feedbackText;

		String value = fixAssignmentFeedback(m_feedbackText);

		StringBuffer buf = new StringBuffer(value);
		int pos = -1;

		while ((pos = buf.indexOf("{{")) != -1)
		{
			buf.replace(pos, pos + "{{".length(), "<span class='highlight'>");
		}

		while ((pos = buf.indexOf("}}")) != -1)
		{
			buf.replace(pos, pos + "}}".length(), "</span>");
		}

		return FormattedText.escapeHtmlFormattedText(buf.toString());
	}

	/**
	 * Apply the fix to pre 1.1.05 assignments submissions feedback.
	 */
	private String fixAssignmentFeedback(String value)
	{
		if (value == null || value.length() == 0) return value;
		
		StringBuffer buf = new StringBuffer(value);
		int pos = -1;
		
		// <br/> -> \n
		while ((pos = buf.indexOf("<br/>")) != -1)
		{
			buf.replace(pos, pos + "<br/>".length(), "\n");
		}
		
		// <span class='chefAlert'>( -> {{
		while ((pos = buf.indexOf("<span class='chefAlert'>(")) != -1)
		{
			buf.replace(pos, pos + "<span class='chefAlert'>(".length(), "{{");
		}
		
		// )</span> -> }}
		while ((pos = buf.indexOf(")</span>")) != -1)
		{
			buf.replace(pos, pos + ")</span>".length(), "}}");
		}
		
		while ((pos = buf.indexOf("<ins>")) != -1)
		{
			buf.replace(pos, pos + "<ins>".length(), "{{");
		}
		
		while ((pos = buf.indexOf("</ins>")) != -1)
		{
			buf.replace(pos, pos + "</ins>".length(), "}}");
		}
		
		return buf.toString();
		
	} // fixAssignmentFeedback

	/**
	 * Access the list of attachments returned to the students in the process of grading this assignment; usually a modified or annotated version of the attachment submitted.
	 * 
	 * @return ReferenceVector of the Resource objects pointing to the attachments.
	 */
	public List getFeedbackAttachments()
	{
		return m_feedbackAttachments;
	}

	/**
	 * Get whether this Submission was rejected by the grader.
	 * 
	 * @return True if this response was rejected by the grader, false otherwise.
	 */
	public boolean getReturned()
	{
		return m_returned;
	}

	/**
	 * Get whether this Submission has been graded.
	 * 
	 * @return True if the submission has been graded, false otherwise.
	 */
	public boolean getGraded()
	{
		return m_graded;
	}

	/**
	 * Get the time on which the graded submission was returned; null means the response is not yet graded.
	 * 
	 * @return the time (may be null)
	 */
	public Time getTimeReturned()
	{
		return m_timeReturned;
	}

	/**
	 * Access the checked status of the honor pledge flag.
	 * 
	 * @return True if the honor pledge is checked, false otherwise.
	 */
	public boolean getHonorPledgeFlag()
	{
		return m_honorPledgeFlag;
	}

	/**
	 * Returns the status of the submission : Not Started, submitted, returned or graded.
	 * 
	 * @return The Submission's status.
	 */
	public String getStatus()
	{
		boolean allowGrade = assignmentService.allowGradeSubmission(getReference());
		String retVal = "";
		
		Time submitTime = getTimeSubmitted();
		Time returnTime = getTimeReturned();
		Time lastModTime = getTimeLastModified();
	
		if (getSubmitted() || (!getSubmitted() && allowGrade))
		{
			if (submitTime != null)
			{
				if (getReturned())
				{
					if (returnTime != null && returnTime.before(submitTime))
					{
						if (!getGraded())
						{
							retVal = rb.getString("listsub.resubmi") + " " + submitTime.toStringLocalFull();
							if (submitTime.after(getAssignment().getDueTime()))
								retVal = retVal + rb.getString("gen.late2");
						}
								
						else
							retVal = rb.getString("gen.returned");
					}
					else
						retVal = rb.getString("gen.returned");
				}
				else if (getGraded() && allowGrade)
				{
						retVal = getGradeOrComment();
				}
				else 
				{
					if (allowGrade)
					{
						// ungraded submission
						retVal = rb.getString("gen.ung1");
					}
					else
					{
						// submitted
						retVal = rb.getString("gen.subm4");
						
						if(submitTime != null)
						{
							retVal = rb.getString("gen.subm4") + " " + submitTime.toStringLocalFull();
						}
					}
				}
			}
			else
			{
				if (getReturned())
				{
					// instructor can return grading to non-submitted user
					retVal = rb.getString("gen.returned");
				}
				else if (getGraded() && allowGrade)
				{
					// instructor can grade non-submitted ones
					retVal = getGradeOrComment();
				}
				else
				{
					if (allowGrade)
					{
						// show "no submission" to graders
						retVal = rb.getString("listsub.nosub");
					}
					else
					{
						// show "not started" to students
						retVal = rb.getString("gen.notsta");
					}
				}
			}
		}
		else
		{
			if (getGraded())
			{
				if (getReturned())
				{
					if (lastModTime != null && returnTime != null && lastModTime.after(TimeService.newTime(returnTime.getTime() + 1000 * 10)) && !allowGrade)
					{
						// working on a returned submission now
						retVal = rb.getString("gen.dra2") + " " + rb.getString("gen.inpro");
					}
					else
					{
						// not submitted submmission has been graded and returned
						retVal = rb.getString("gen.returned");
					}
				}
				else if (allowGrade)
					// grade saved but not release yet, show this to graders
					retVal = getGradeOrComment();
			}
			else	
			{
				if (allowGrade)
					retVal = rb.getString("gen.ung1");
				else
					// submission saved, not submitted.
					retVal = rb.getString("gen.dra2") + " " + rb.getString("gen.inpro");
			}
		}

		return retVal;
	}

	private String getGradeOrComment() {
		String retVal;
		if (getGrade() != null && getGrade().length() > 0)
			retVal = rb.getString("grad3");
		else
			retVal = rb.getString("gen.commented");
		return retVal;
	}

	/**
	 * Are these objects equal? If they are both AssignmentSubmission objects, and they have matching id's, they are.
	 * 
	 * @return true if they are equal, false if not.
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof AssignmentSubmission)) return false;
		return ((AssignmentSubmission) obj).getId().equals(getId());

	} // equals

	/**
	 * Make a hash code that reflects the equals() logic as well. We want two objects, even if different instances, if they have the same id to hash the same.
	 */
	public int hashCode()
	{
		return getId().hashCode();

	} // hashCode

	/**
	 * Compare this object with the specified object for order.
	 * 
	 * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
	 */
	public int compareTo(Object obj)
	{
		if (!(obj instanceof AssignmentSubmission)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// start the compare by comparing their sort names
		int compare = getTimeSubmitted().toString().compareTo(((AssignmentSubmission) obj).getTimeSubmitted().toString());

		// if these are the same
		if (compare == 0)
		{
			// sort based on (unique) id
			compare = getId().compareTo(((AssignmentSubmission) obj).getId());
		}

		return compare;

	} // compareTo
	
	/**
	 * {@inheritDoc}
	 */
	public int getResubmissionNum()
	{
		String numString = StringUtil.trimToNull(m_properties.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_NUMBER));
		return numString != null?Integer.valueOf(numString).intValue():0;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Time getCloseTime()
	{
		String closeTimeString = StringUtil.trimToNull(m_properties.getProperty(AssignmentSubmission.ALLOW_RESUBMIT_CLOSETIME));
		if (closeTimeString != null && getResubmissionNum() != 0)
		{
			// return the close time if it is set
			return TimeService.newTime(Long.parseLong(closeTimeString));
		}
		else
		{
			// else use the assignment close time setting
			Assignment a = getAssignment();
			return a!=null?a.getCloseTime():null;	
		}
	}
	
	/**
	 * Set the context at the time of creation.
	 * 
	 * @param context -
	 *        the context string.
	 */
	public void setContext(String context)
	{
		m_context = context;
	}

	/**
	 * Set the Assignment for this Submission
	 * 
	 * @param assignment -
	 *        the Assignment
	 */
	public void setAssignment(Assignment assignment)
	{
		if (assignment != null)
		{
			m_assignment = assignment.getId();
		}
		else
			m_assignment = "";
	}

	/**
	 * Set whether this is a final submission.
	 * 
	 * @param submitted -
	 *        True if a final submission, false if still a draft.
	 */
	public void setSubmitted(boolean submitted)
	{
		m_submitted = submitted;
	}

	/**
	 * Add a User to the submitters list.
	 * 
	 * @param submitter -
	 *        the User to add.
	 */
	public void addSubmitter(User submitter)
	{
		if (submitter != null) m_submitters.add(submitter.getId());
	}

	/**
	 * Remove an User from the submitter list
	 * 
	 * @param submitter -
	 *        the User to remove.
	 */
	public void removeSubmitter(User submitter)
	{
		if (submitter != null) m_submitters.remove(submitter.getId());
	}

	/**
	 * Remove all user from the submitter list
	 */
	public void clearSubmitters()
	{
		m_submitters.clear();
	}

	/**
	 * Set the time at which this response was submitted; setting it to null signifies the response is unsubmitted.
	 * 
	 * @param timeSubmitted -
	 *        Time of submission.
	 */
	public void setTimeSubmitted(Time value)
	{
		m_timeSubmitted = value;
	}

	/**
	 * Set whether the grade has been released.
	 * 
	 * @param released -
	 *        True if the Submissions's grade has been released, false otherwise.
	 */
	public void setGradeReleased(boolean released)
	{
		m_gradeReleased = released;
	}

	/**
	 * Sets the grade for the Submisssion.
	 * 
	 * @param grade -
	 *        The Submission's grade.
	 */
	public void setGrade(String grade)
	{
		m_grade = grade;
	}

	/**
	 * Text submitted in response to the Assignment.
	 * 
	 * @param submissionText -
	 *        The text of the submission.
	 */
	public void setSubmittedText(String value)
	{
		m_submittedText = value;
	}

	/**
	 * Add an attachment to the list of submitted attachments.
	 * 
	 * @param attachment -
	 *        The Reference object pointing to the attachment.
	 */
	public void addSubmittedAttachment(Reference attachment)
	{
		if (attachment != null) m_submittedAttachments.add(attachment);
	}

	/**
	 * Remove an attachment from the list of submitted attachments
	 * 
	 * @param attachment -
	 *        The Reference object pointing to the attachment.
	 */
	public void removeSubmittedAttachment(Reference attachment)
	{
		if (attachment != null) m_submittedAttachments.remove(attachment);
	}

	/**
	 * Remove all submitted attachments.
	 */
	public void clearSubmittedAttachments()
	{
		m_submittedAttachments.clear();
	}

	/**
	 * Set the general comments by the grader.
	 * 
	 * @param comment -
	 *        the text of the grader's comments; may be null.
	 */
	public void setFeedbackComment(String value)
	{
		m_feedbackComment = value;
	}

	/**
	 * Set the text part of the instructors feedback; usually an annotated copy of the submittedText
	 * 
	 * @param feedback -
	 *        The text of the grader's feedback.
	 */
	public void setFeedbackText(String value)
	{
		m_feedbackText = value;
	}

	/**
	 * Add an attachment to the list of feedback attachments.
	 * 
	 * @param attachment -
	 *        The Resource object pointing to the attachment.
	 */
	public void addFeedbackAttachment(Reference attachment)
	{
		if (attachment != null) m_feedbackAttachments.add(attachment);
	}

	/**
	 * Remove an attachment from the list of feedback attachments.
	 * 
	 * @param attachment -
	 *        The Resource pointing to the attachment to remove.
	 */
	public void removeFeedbackAttachment(Reference attachment)
	{
		if (attachment != null) m_feedbackAttachments.remove(attachment);
	}

	/**
	 * Remove all feedback attachments.
	 */
	public void clearFeedbackAttachments()
	{
		m_feedbackAttachments.clear();
	}

	/**
	 * Set whether this Submission was rejected by the grader.
	 * 
	 * @param returned -
	 *        true if this response was rejected by the grader, false otherwise.
	 */
	public void setReturned(boolean value)
	{
		m_returned = value;
	}

	/**
	 * Set whether this Submission has been graded.
	 * 
	 * @param graded -
	 *        true if the submission has been graded, false otherwise.
	 */
	public void setGraded(boolean value)
	{
		m_graded = value;
	}

	/**
	 * Set the time at which the graded Submission was returned; setting it to null means it is not yet graded.
	 * 
	 * @param timeReturned -
	 *        The time at which the graded Submission was returned.
	 */
	public void setTimeReturned(Time timeReturned)
	{
		m_timeReturned = timeReturned;
	}

	/**
	 * Set the checked status of the honor pledge flag.
	 * 
	 * @param honorPledgeFlag -
	 *        True if the honor pledge is checked, false otherwise.
	 */
	public void setHonorPledgeFlag(boolean honorPledgeFlag)
	{
		m_honorPledgeFlag = honorPledgeFlag;
	}

	/**
	 * Set the time last modified.
	 * 
	 * @param lastmod -
	 *        The Time at which the Assignment was last modified.
	 */
	public void setTimeLastModified(Time lastmod)
	{
		if (lastmod != null) m_timeLastModified = lastmod;
	}
	
	
	
	public void postAttachment(List attachments){
		//Send the attachment to the review service

		try {
			ContentResource cr = getFirstAcceptableAttachement(attachments);
			Assignment ass = this.getAssignment();
			contentReviewService.queueContent(null, null, ass.getReference(), cr.getId());
		} catch (QueueException qe) {
			M_log.warn(this + " BaseAssignmentSubmissionEdit postAttachment: Unable to add content to Content Review queue: " + qe.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private ContentResource getFirstAcceptableAttachement(List attachments) {
		
		for( int i =0; i < attachments.size();i++ ) { 
			Reference attachment = (Reference)attachments.get(i);
			try {
				ContentResource res = assignmentService.contentHostingService().getResource(attachment.getId());
				if (contentReviewService.isAcceptableContent(res)) {
					return res;
				}
			} catch (PermissionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				M_log.warn(this + ":geFirstAcceptableAttachment " + e.getMessage());
			} catch (IdUnusedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				M_log.warn(this + ":geFirstAcceptableAttachment " + e.getMessage());
			} catch (TypeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				M_log.warn(this + ":geFirstAcceptableAttachment " + e.getMessage());
			}

			
		}
		return null;
	}
	
	/**
	 * Take all values from this object.
	 * 
	 * @param AssignmentSubmission
	 *        The AssignmentSubmission object to take values from.
	 */
	protected void set(AssignmentSubmission assignmentSubmission)
	{
		setAll(assignmentSubmission);

	} // set

	/**
	 * Access the event code for this edit.
	 * 
	 * @return The event code for this edit.
	 */
	protected String getEvent()
	{
		return m_event;
	}

	/**
	 * Set the event code for this edit.
	 * 
	 * @param event
	 *        The event code for this edit.
	 */
	protected void setEvent(String event)
	{
		m_event = event;
	}


	/**
	 * Access the resource's properties for modification
	 * 
	 * @return The resource's properties.
	 */
	public ResourcePropertiesEdit getPropertiesEdit()
	{
		return m_properties;

	} // getPropertiesEdit
	
	boolean m_active = false;
	
	/**
	 * Enable editing.
	 */
	protected void activate()
	{
		m_active = true;
	}

	/**
	 * @inheritDoc
	 */
	public boolean isActiveEdit()
	{
		return m_active;
	}

	/**
	 * Close the edit object - it cannot be used after this.
	 */
	protected void closeEdit()
	{
		m_active = false;
	}
	
} // AssignmentSubmission

