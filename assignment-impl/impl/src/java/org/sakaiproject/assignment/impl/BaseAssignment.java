package org.sakaiproject.assignment.impl;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentContent;
import org.sakaiproject.assignment.api.AssignmentConstants;
import org.sakaiproject.assignment.api.AssignmentEdit;
import org.sakaiproject.assignment.api.Assignment.AssignmentAccess;
import org.sakaiproject.entity.api.AttachmentContainer;
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.ResourcePropertiesEdit;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.cover.TimeService;
import org.sakaiproject.tool.api.SessionBindingEvent;
import org.sakaiproject.tool.api.SessionBindingListener;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.util.BaseResourcePropertiesEdit;
import org.sakaiproject.util.DefaultEntityHandler;
import org.sakaiproject.util.EntityCollections;
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

/**********************************************************************************************************************************************************************************************************************************************************
 * Assignment Implementation
 *********************************************************************************************************************************************************************************************************************************************************/

public class BaseAssignment implements Assignment, AttachmentContainer
{	

	/** Our logger. */
	private static Log M_log = LogFactory.getLog(BaseAssignment.class);
	
	/** the resource bundle */
	private static ResourceLoader rb = new ResourceLoader("assignment");
	
	DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, rb.getLocale());
	
	protected ResourcePropertiesEdit m_properties;

	private BaseAssignmentService assignmentService = null;
	
	protected String m_id;

	protected String m_title;

	protected String m_context;
	
	protected boolean m_isSiteRange;

	protected String m_section;

	protected Date m_openDate;

	protected Date m_dueDate;

	protected Date m_closeDate;

	protected Date m_dropDeadDate;

	protected List m_authors;

	protected boolean m_draft;
	
	protected int m_position_order;

	/** The Collection of groups (authorization group id strings). */
	protected Collection m_groups = new Vector();

	/** The assignment access. */
	protected AssignmentAccess m_access = AssignmentAccess.SITE;
	
	/**** attributes from AssignmentContent *****/

	protected List m_attachments;

	protected String m_instructions;

	protected int m_honorPledge;

	protected int m_typeOfSubmission;

	protected int m_typeOfGrade;

	protected int m_maxGradePoint;

	protected boolean m_groupProject;

	protected boolean m_individuallyGraded;

	protected boolean m_releaseGrades;
	
	protected boolean m_allowAttachments;
	
	protected boolean m_allowReviewService;
	
	protected boolean m_allowStudentViewReport;

	protected Date m_dateCreated;

	protected Date m_dateLastModified;
	
	/******************************************************************************************************************************************************************************************************************************************************
	 * AttachmentContainer Implementation
	 *****************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Add an attachment.
	 * 
	 * @param ref -
	 *        The attachment Reference.
	 */
	public void addAttachment(Reference ref)
	{
		if (ref != null) m_attachments.add(ref);
	}

	/**
	 * Remove an attachment.
	 * 
	 * @param ref -
	 *        The attachment Reference to remove (the one removed will equal this, they need not be ==).
	 */
	public void removeAttachment(Reference ref)
	{
		if (ref != null) m_attachments.remove(ref);
	}

	/**
	 * Replace the attachment set.
	 * 
	 * @param attachments -
	 *        A ReferenceVector that will become the new set of attachments.
	 */
	public void replaceAttachments(List attachments)
	{
		m_attachments = attachments;
	}

	/**
	 * Clear all attachments.
	 */
	public void clearAttachments()
	{
		m_attachments.clear();
	}
	
	/********************* methods *******************/

	/**
	 * constructor
	 */
	public BaseAssignment(BaseAssignmentService assignmentService)
	{
		this.assignmentService = assignmentService;
		m_properties = new BaseResourcePropertiesEdit();
	}// constructor
	
	/**
	 * Copy constructor
	 */
	public BaseAssignment(BaseAssignmentService assignmentService, Assignment assignment)
	{
		this.assignmentService = assignmentService;
		setAll(assignment);
	}// copy constructor

	public BaseAssignment(String id, String context, boolean isSiteRange, Date openDate, Date dueDate, Date closeDate, Date createdOn, 
						String createdBy, Date modifiedOn, String modifiedBy, int gradeType,
						int submissionType, int maxPoint, boolean draft, boolean deleted, String instruction, boolean announceOpenDate, 
						boolean scheduleDueDate, int emailNotificationOption, int resubmissionMaxNumber, Date resubmissionCloseDate, 
						String associatedGradebookEntry, boolean honorPledge)
	{
		// setup for properties
		m_properties = new BaseResourcePropertiesEdit();
		// setup for properties, but mark them lazy since we have not yet
		// established them from data
		((BaseResourcePropertiesEdit) m_properties).setLazy(true);

		m_id = id;
		m_context = context;
		m_isSiteRange = isSiteRange;
		m_openDate = openDate;
		m_dueDate = dueDate;
		m_closeDate = closeDate;
		m_dateCreated = createdOn;
		
		
		
	}
	
	/**
	 * Constructor used in addAssignment
	 */
	public BaseAssignment(BaseAssignmentService assignmentService, String id, String context)
	{
		this.assignmentService = assignmentService;
		
		m_properties = new BaseResourcePropertiesEdit();
		AssignmentUtil.addLiveProperties(m_properties);
		m_id = id;
		m_title = "";
		m_context = context;
		m_section = "";
		m_authors = new Vector();
		m_draft = true;
		m_groups = new Vector();
		m_position_order = 0;
		
		/*** from AssignmentContent ***/
		m_attachments = assignmentService.entityManager().newReferenceList();
		m_instructions = "";
		m_honorPledge = AssignmentConstants.HONOR_PLEDGE_NOT_SET;
		m_typeOfSubmission = AssignmentConstants.ASSIGNMENT_SUBMISSION_TYPE_NOT_SET;
		m_typeOfGrade = AssignmentConstants.GRADE_TYPE_NOT_SET;
		m_maxGradePoint = 0;
		m_dateCreated = new Date();
		m_dateLastModified = new Date();
	}
	
	/**
	 * Reads the Assignment's attribute values from xml.
	 * 
	 * @param s -
	 *        Data structure holding the xml info.
	 */
	public BaseAssignment(BaseAssignmentService assignmentService, Element el)
	{
		this.assignmentService = assignmentService;
		elementConstructor(el);
	}

	/**
	 * Reads the Assignment's attribute values from xml.
	 * 
	 * @param s -
	 *        Data structure holding the xml info.
	 */
	public BaseAssignment(Element el)
	{
		M_log.warn(this + " BASE ASSIGNMENT : ENTERING STORAGE CONSTRUCTOR");
		
		elementConstructor(el);

	}// storage constructor

	private void elementConstructor(Element el) {
		m_properties = new BaseResourcePropertiesEdit();

		int numAttributes = 0;
		String intString = null;
		String attributeString = null;
		String tempString = null;

		m_id = el.getAttribute("id");
		
			M_log.warn(this + " BASE ASSIGNMENT : STORAGE CONSTRUCTOR : ASSIGNMENT ID : " + m_id);
		m_title = el.getAttribute("title");
		m_section = el.getAttribute("section");
		m_draft = AssignmentUtil.getBool(el.getAttribute("draft"));
		
			M_log.warn(this + " BASE ASSIGNMENT : STORAGE CONSTRUCTOR : READ THROUGH REG ATTS");

		m_openDate = AssignmentUtil.getDateObject(el.getAttribute("opendate"));
		m_dueDate = AssignmentUtil.getDateObject(el.getAttribute("duedate"));
		m_dropDeadDate = AssignmentUtil.getDateObject(el.getAttribute("dropdeaddate"));
		m_closeDate = AssignmentUtil.getDateObject(el.getAttribute("closedate"));
		m_context = el.getAttribute("context");
		m_position_order = 0; // prevents null pointer if there is no position_order defined as well as helps with the sorting
		try
		{
			m_position_order = new Long(el.getAttribute("position_order")).intValue();
		}
		catch (Exception e)
		{
			M_log.warn(this + ": BaseAssignment(Element) " + e.getMessage());
		}

		// READ THE AUTHORS
		m_authors = new Vector();
		intString = el.getAttribute("numberofauthors");
		
			M_log.warn(this + " BASE ASSIGNMENT : STORAGE CONSTRUCTOR : number of authors : " + intString);
		try
		{
			numAttributes = Integer.parseInt(intString);

			for (int x = 0; x < numAttributes; x++)
			{
				
					M_log.warn(this + " BASE ASSIGNMENT : STORAGE CONSTRUCTOR : reading author # " + x);
				attributeString = "author" + x;
				tempString = el.getAttribute(attributeString);

				if (tempString != null)
				{
					
						M_log.warn(this + " BASE ASSIGNMENT : STORAGE CONSTRUCTOR : adding author # " + x
								+ " id :  " + tempString);
					m_authors.add(tempString);
				}
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " BASE ASSIGNMENT : STORAGE CONSTRUCTOR : Exception reading authors : " + e);
		}

		// READ THE PROPERTIES AND INSTRUCTIONS
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

			// look for an group
			else if (element.getTagName().equals("group"))
			{
				m_groups.add(element.getAttribute("authzGroup"));
			}
			// from old AssignmentContent
			else if (element.getTagName().equals("instructions-html") || element.getTagName().equals("instructions-formatted")
					|| element.getTagName().equals("instructions"))
			{
				if ((element.getChildNodes() != null) && (element.getChildNodes().item(0) != null))
				{
					m_instructions = element.getChildNodes().item(0).getNodeValue();
					if (element.getTagName().equals("instructions"))
						m_instructions = FormattedText.convertPlaintextToFormattedText(m_instructions);
					if (element.getTagName().equals("instructions-formatted"))
						m_instructions = FormattedText.convertOldFormattedText(m_instructions);
					
						M_log.warn(this + " BaseAssignment(Element): instructions : " + m_instructions);
				}
				if (m_instructions == null)
				{
					m_instructions = "";
				}
			}
		}

		// extract access
		AssignmentAccess access = AssignmentAccess.fromString(el.getAttribute("access"));
		if (access != null)
		{
			m_access = access;
		}

		M_log.warn(this + " BASE ASSIGNMENT : LEAVING STORAGE CONSTRUCTOR");
		
		/**** from AssignmentContent ****/
		Reference tempReference = null;
		M_log.warn(this + " BaseAssignment : Entering read");
		m_groupProject = AssignmentUtil.getBool(el.getAttribute("groupproject"));
		m_individuallyGraded = AssignmentUtil.getBool(el.getAttribute("indivgraded"));
		m_releaseGrades = AssignmentUtil.getBool(el.getAttribute("releasegrades"));
		m_allowAttachments = AssignmentUtil.getBool(el.getAttribute("allowattach"));
		m_allowReviewService = AssignmentUtil.getBool(el.getAttribute("allowreview"));
		m_allowStudentViewReport = AssignmentUtil.getBool(el.getAttribute("allowstudentview"));
		
		m_dateCreated = AssignmentUtil.getDateObject(el.getAttribute("datecreated"));
		m_dateLastModified = AssignmentUtil.getDateObject(el.getAttribute("lastmod"));

		m_instructions = FormattedText.decodeFormattedTextAttribute(el, "instructions");

		try
		{
			m_honorPledge = Integer.parseInt(el.getAttribute("honorpledge"));
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignment Exception parsing honor pledge int from xml file string : " + e);
		}

		try
		{
			m_typeOfSubmission = Integer.parseInt(el.getAttribute("submissiontype"));
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignment Exception parsing submission type int from xml file string : " + e);
		}

		try
		{
			m_typeOfGrade = Integer.parseInt(el.getAttribute("typeofgrade"));
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignment Exception parsing grade type int from xml file string : " + e);
		}

		try
		{
			// %%%zqian
			// read the scaled max grade point first; if there is none, get the old max grade value and multiple by 10
			String maxGradePoint = StringUtil.trimToNull(el.getAttribute("scaled_maxgradepoint"));
			if (maxGradePoint == null)
			{
				maxGradePoint = StringUtil.trimToNull(el.getAttribute("maxgradepoint"));
				if (maxGradePoint != null)
				{
					maxGradePoint = maxGradePoint + "0";
				}
			}
			m_maxGradePoint = Integer.parseInt(maxGradePoint);
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignment Exception parsing maxgradepoint int from xml file string : " + e);
		}

		// READ THE AUTHORS
		m_authors = new Vector();
		intString = el.getAttribute("numberofauthors");
		try
		{
			numAttributes = Integer.parseInt(intString);

			for (int x = 0; x < numAttributes; x++)
			{
				attributeString = "author" + x;
				tempString = el.getAttribute(attributeString);
				if (tempString != null) m_authors.add(tempString);
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignment: Exception reading authors : " + e);
		}

		// READ THE ATTACHMENTS
		m_attachments = assignmentService.entityManager().newReferenceList();
		M_log.warn(this + " BaseAssignment: Reading attachments : ");
		intString = el.getAttribute("numberofattachments");
		M_log.warn(this + " BaseAssignment: num attachments : " + intString);
		try
		{
			numAttributes = Integer.parseInt(intString);

			for (int x = 0; x < numAttributes; x++)
			{
				attributeString = "attachment" + x;
				tempString = el.getAttribute(attributeString);
				if (tempString != null)
				{
					tempReference = assignmentService.entityManager().newReference(tempString);
					m_attachments.add(tempReference);
					M_log.warn(this + " BaseAssignment: " + attributeString + " : " + tempString);
				}
			}
		}
		catch (Exception e)
		{
			M_log.warn(this + " BaseAssignment: Exception reading attachments : " + e);
		}
	}

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
					if ("assignment".equals(qName) && entity == null)
					{
						m_id = attributes.getValue("id");
						m_properties = new BaseResourcePropertiesEdit();
						
						int numAttributes = 0;
						String intString = null;
						String attributeString = null;
						String tempString = null;

						m_title = attributes.getValue("title");
						m_section = attributes.getValue("section");
						m_draft = AssignmentUtil.getBool(attributes.getValue("draft"));
						
							M_log.warn(this + " getContentHandler: READ THROUGH REG ATTS");

						m_openDate = AssignmentUtil.getDateObject(attributes.getValue("opendate"));
						m_dueDate = AssignmentUtil.getDateObject(attributes.getValue("duedate"));
						m_dropDeadDate = AssignmentUtil.getDateObject(attributes.getValue("dropdeaddate"));
						m_closeDate = AssignmentUtil.getDateObject(attributes.getValue("closedate"));
						m_context = attributes.getValue("context");
						m_position_order = 0; // prevents null pointer if there is no position_order defined as well as helps with the sorting
						try
						{
							m_position_order = new Long(attributes.getValue("position_order")).intValue();
						}
						catch (Exception e)
						{
							M_log.warn(this + ":getContentHandler:DefaultEntityHandler Long data parse problem " + attributes.getValue("position_order") + e.getMessage());
						}

						// READ THE AUTHORS
						m_authors = new Vector();
						intString = attributes.getValue("numberofauthors");
						try
						{
							numAttributes = Integer.parseInt(intString);

							for (int x = 0; x < numAttributes; x++)
							{
								attributeString = "author" + x;
								tempString = attributes.getValue(attributeString);

								if (tempString != null)
								{
									m_authors.add(tempString);
								}
							}
						}
						catch (Exception e)
						{
							M_log.warn(this + " BASE ASSIGNMENT getContentHandler startElement : Exception reading authors : " + e.toString());
						}

						// extract access
						AssignmentAccess access = AssignmentAccess.fromString(attributes.getValue("access"));
						if (access != null)
						{
							m_access = access;
						}
						
						entity = thisEntity;
					}
					else if (AssignmentConstants.GROUP_LIST.equals(qName))
					{
						String groupRef = attributes.getValue(AssignmentConstants.GROUP_NAME);
						if (groupRef != null)
						{
							m_groups.add(groupRef);
						}
					}
					else
					{
						M_log.warn(this + " BaseAssignment getContentHandler Unexpected Element " + qName);
					}

				}
			}
		};
	}
	
	/**
	 * Takes the Assignment's attribute values and puts them into the xml document.
	 * 
	 * @param s -
	 *        Data structure holding the object to be stored.
	 * @param doc -
	 *        The xml document.
	 */
	public Element toXml(Document doc, Stack stack)
	{
		M_log.warn(this + " BASE ASSIGNMENT : ENTERING TOXML");

		Element assignment = doc.createElement("assignment");

		if (stack.isEmpty())
		{
			doc.appendChild(assignment);
		}
		else
		{
			((Element) stack.peek()).appendChild(assignment);
		}
		stack.push(assignment);

		// SET ASSIGNMENT ATTRIBUTES
		String numItemsString = null;
		String attributeString = null;
		String itemString = null;
		assignment.setAttribute("id", m_id);
		assignment.setAttribute("title", m_title);
		assignment.setAttribute("section", m_section);
		assignment.setAttribute("context", m_context);
		assignment.setAttribute("draft", AssignmentUtil.getBoolString(m_draft));
		assignment.setAttribute("opendate", m_openDate.toString());
		assignment.setAttribute("duedate", m_dueDate.toString());
		assignment.setAttribute("dropdeaddate", m_dropDeadDate.toString());
		assignment.setAttribute("closedate", m_closeDate.toString());
		assignment.setAttribute("position_order", new Long(m_position_order).toString().trim());
		
		assignment.setAttribute("groupproject", AssignmentUtil.getBoolString(m_groupProject));
		assignment.setAttribute("indivgraded", AssignmentUtil.getBoolString(m_individuallyGraded));
		assignment.setAttribute("releasegrades", AssignmentUtil.getBoolString(m_releaseGrades));
		assignment.setAttribute("allowattach", AssignmentUtil.getBoolString(m_allowAttachments));
	
		assignment.setAttribute("allowreview", AssignmentUtil.getBoolString(m_allowReviewService));
		assignment.setAttribute("allowstudentview", AssignmentUtil.getBoolString(m_allowStudentViewReport));
		
		assignment.setAttribute("honorpledge", String.valueOf(m_honorPledge));
		assignment.setAttribute("submissiontype", String.valueOf(m_typeOfSubmission));
		assignment.setAttribute("typeofgrade", String.valueOf(m_typeOfGrade));
		assignment.setAttribute("scaled_maxgradepoint", String.valueOf(m_maxGradePoint));
		assignment.setAttribute("datecreated", m_dateCreated.toString());
		assignment.setAttribute("lastmod", m_dateLastModified.toString());

		M_log.warn(this + " BASE CONTENT : TOXML : SAVED REGULAR PROPERTIES");

		// SAVE THE AUTHORS
		numItemsString = "" + m_authors.size();
		assignment.setAttribute("numberofauthors", numItemsString);
		for (int x = 0; x < m_authors.size(); x++)
		{
			attributeString = "author" + x;
			itemString = (String) m_authors.get(x);
			if (itemString != null) assignment.setAttribute(attributeString, itemString);
		}

		M_log.warn(this + " BASE CONTENT : TOXML : SAVED AUTHORS");

		// SAVE THE ATTACHMENTS
		Reference tempReference = null;
		numItemsString = "" + m_attachments.size();
		assignment.setAttribute("numberofattachments", numItemsString);
		for (int x = 0; x < m_attachments.size(); x++)
		{
			attributeString = "attachment" + x;
			tempReference = (Reference) m_attachments.get(x);
			itemString = tempReference.getReference();
			if (itemString != null) assignment.setAttribute(attributeString, itemString);
		}

		// SAVE THE INSTRUCTIONS
		FormattedText.encodeFormattedTextAttribute(assignment, "instructions", m_instructions);

		// SAVE THE AUTHORS
		numItemsString = "" + m_authors.size();
		
		M_log.warn(this + " BASE ASSIGNMENT : TOXML : saving " + numItemsString + " authors");
		assignment.setAttribute("numberofauthors", numItemsString);
		for (int x = 0; x < m_authors.size(); x++)
		{
			attributeString = "author" + x;
			itemString = (String) m_authors.get(x);
			if (itemString != null)
			{
				assignment.setAttribute(attributeString, itemString);
				
					M_log.warn(this + " BASE ASSIGNMENT : TOXML : saving author : " + itemString);
			}
		}

		// add groups
		if ((m_groups != null) && (m_groups.size() > 0))
		{
			for (Iterator i = m_groups.iterator(); i.hasNext();)
			{
				String group = (String) i.next();
				Element sect = doc.createElement("group");
				assignment.appendChild(sect);
				sect.setAttribute("authzGroup", group);
			}
		}

		// add access
		assignment.setAttribute("access", m_access.toString());

		// SAVE THE PROPERTIES
		m_properties.toXml(doc, stack);
		M_log.warn(this + " BASE ASSIGNMENT : TOXML : SAVED PROPERTIES");
		stack.pop();

		M_log.warn("ASSIGNMENT : BASE ASSIGNMENT : LEAVING TOXML");

		return assignment;

	}// toXml

	protected void setAll(Assignment assignment)
	{
		if (assignment != null)
		{
			m_id = assignment.getId();
			m_authors = assignment.getAuthors();
			m_title = assignment.getTitle();
			m_context = assignment.getContext();
			m_section = assignment.getSection();
			m_openDate = assignment.getOpenDate();
			m_dueDate = assignment.getDueDate();
			m_closeDate = assignment.getCloseDate();
			m_dropDeadDate = assignment.getDropDeadDate();
			m_draft = assignment.getDraft();
			m_position_order = 0;
			try
			{
				m_position_order = assignment.getPosition_order();
			}
			catch (Exception e)
			{
				M_log.warn(this + ": setAll(Assignment) get position order " + e.getMessage());
			}
			m_groups = assignment.getGroups();
			m_access = assignment.getAccess();
			
			m_attachments = assignment.getAttachments();
			m_instructions = assignment.getInstructions();
			m_honorPledge = assignment.getHonorPledge();
			m_typeOfSubmission = assignment.getTypeOfSubmission();
			m_typeOfGrade = assignment.getTypeOfGrade();
			m_maxGradePoint = assignment.getMaxGradePoint();
			m_groupProject = assignment.getGroupProject();
			m_individuallyGraded = assignment.individuallyGraded();
			m_releaseGrades = assignment.releaseGrades();
			m_allowAttachments = assignment.getAllowAttachments();
			//Uct
			m_allowReviewService = assignment.getAllowReviewService();
			m_allowStudentViewReport = assignment.getAllowStudentViewReport();
			
			m_dateCreated = assignment.getDateCreated();
			m_dateLastModified = assignment.getDateLastModified();
			m_properties = new BaseResourcePropertiesEdit();
			m_properties.addAll(assignment.getProperties());
		}
	}

	public String getId()
	{
		return m_id;
	}

	/**
	 * Access the URL which can be used to access the resource.
	 * 
	 * @return The URL which can be used to access the resource.
	 */
	public String getUrl()
	{
		return assignmentService.getAccessPoint(false) + Entity.SEPARATOR + "a" + Entity.SEPARATOR + m_context + Entity.SEPARATOR + m_id;

	} // getUrl

	/**
	 * Access the internal reference which can be used to access the resource from within the system.
	 * 
	 * @return The the internal reference which can be used to access the resource from within the system.
	 */
	public String getReference()
	{
		return assignmentService.assignmentReference(m_context, m_id);

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
	 * Access the resource's properties.
	 * 
	 * @return The resource's properties.
	 */
	public ResourceProperties getProperties()
	{
		return m_properties;
	}

	/**
	 * Access the list of authors.
	 * 
	 * @return FlexStringArray of user ids.
	 */
	public List getAuthors()
	{
		return m_authors;
	}

	/**
	 * Add an author to the author list.
	 * 
	 * @param author -
	 *        The User to add to the author list.
	 */
	public void addAuthor(User author)
	{
		if (author != null) m_authors.add(author.getId());
	}

	/**
	 * Remove an author from the author list.
	 * 
	 * @param author -
	 *        the User to remove from the author list.
	 */
	public void removeAuthor(User author)
	{
		if (author != null) m_authors.remove(author.getId());
	}

	/**
	 * Access the creator of this object.
	 * 
	 * @return String The creator's user id.
	 */
	public String getCreator()
	{
		return m_properties.getProperty(ResourceProperties.PROP_CREATOR);
	}

	/**
	 * Access the person of last modificaiton
	 * 
	 * @return the User's Id
	 */
	public String getAuthorLastModified()
	{
		return m_properties.getProperty(ResourceProperties.PROP_MODIFIED_BY);
	}

	/**
	 * Access the title.
	 * 
	 * @return The Assignment's title.
	 */
	public String getTitle()
	{
		return m_title;
	}

	/**
	 * @inheritDoc
	 */
	public String getStatus()
	{
		Date currentDate = new Date();
		
		if (this.getDraft())
			return rb.getString("gen.dra1");
		else if (this.getOpenDate().after(currentDate))
			return rb.getString("gen.notope");
		else if (this.getDueDate().after(currentDate))
			return rb.getString("gen.open");
		else if ((this.getCloseDate() != null) && (this.getCloseDate().before(currentDate)))
			return rb.getString("gen.closed");
		else
			return rb.getString("gen.due1");
	}

	/**
	 * Access the date that this object was created.
	 * 
	 * @return The Date object representing the time of creation.
	 */
	public Date getDateCreated()
	{
		return m_dateCreated;
	}

	/**
	 * Access the date of last modification.
	 * 
	 * @return The Date of last modification.
	 */
	public Date getDateLastModified()
	{
		return m_dateLastModified;
	}

	/**
	 * @deprecated
	 * Access the AssignmentContent of this Assignment.
	 * 
	 * @return The Assignment's AssignmentContent.
	 */
	public AssignmentContent getContent()
	{
		return null;
	}

	/**
	 * @deprecated
	 * Access the reference of the AssignmentContent of this Assignment.
	 * 
	 * @return The Assignment's reference.
	 */
	public String getContentReference()
	{
		return null;
	}

	/**
	 * Access the id of the Assignment's group.
	 * 
	 * @return The id of the group for which this Assignment is designed.
	 */
	public String getContext()
	{
		return m_context;
	}

	/**
	 * Access the section info
	 * 
	 * @return The section String
	 */
	public String getSection()
	{
		return m_section;
	}

	/**
	 * Access the first time at which the assignment can be viewed; may be null.
	 * 
	 * @return The Date at which the assignment is due, or null if unspecified.
	 */
	public Date getOpenDate()
	{
		return m_openDate;
	}

  /**
	* @inheritDoc
	*/
	public String getOpenDateString()
	{
		if ( m_openDate == null )
			return "";
		else
			return df.format(m_openDate);
	}

	/**
	 * Access the date at which the assignment is due; may be null.
	 * 
	 * @return The Date at which the Assignment is due, or null if unspecified.
	 */
	public Date getDueDate()
	{
		return m_dueDate;
	}

  /**
	* @inheritDoc
	*/
	public String getDueDateString()
	{
		if ( m_dueDate == null )
			return "";
		else
			return df.format(m_dueDate);
	}

	/**
	 * Access the drop dead time after which responses to this assignment are considered late; may be null.
	 * 
	 * @return The Date object representing the drop dead time, or null if unspecified.
	 */
	public Date getDropDeadDate()
	{
		return m_dropDeadDate;
	}

  /**
	* @inheritDoc
	*/
	public String getDropDeadDateString()
	{
		if ( m_dropDeadDate == null )
			return "";
		else
			return df.format(m_dropDeadDate);
	}

	/**
	 * Access the close date after which this assignment can no longer be viewed, and after which submissions will not be accepted. May be null.
	 * 
	 * @return The Date after which the Assignment is closed, or null if unspecified.
	 */
	public Date getCloseDate()
	{
		if (m_closeDate == null)
		{
			m_closeDate = m_dueDate;
		}
		return m_closeDate;
	}

  /**
	* @inheritDoc
	*/
	public String getCloseDateString()
	{
		if ( m_closeDate == null )
			return "";
		else
			return df.format(m_closeDate);
	}

	/**
	 * Get whether this is a draft or final copy.
	 * 
	 * @return True if this is a draft, false if it is a final copy.
	 */
	public boolean getDraft()
	{
		return m_draft;
	}
	
	/**
	 * Access the position order.
	 * 
	 * @return The Assignment's positionorder.
	 */
	public int getPosition_order()
	{
		return m_position_order;
	}

	/**
	 * @inheritDoc
	 */
	public Collection getGroups()
	{
		return new Vector(m_groups);
	}

	/**
	 * @inheritDoc
	 */
	public AssignmentAccess getAccess()
	{
		return m_access;
	}
	
	/**
	 * Access the attachments.
	 * 
	 * @return The set of attachments (a ReferenceVector containing Reference objects) (may be empty).
	 */
	public List getAttachments()
	{
		return m_attachments;
	}

	/**
	 * Are these objects equal? If they are both Assignment objects, and they have matching id's, they are.
	 * 
	 * @return true if they are equal, false if not.
	 */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof Assignment)) return false;
		return ((Assignment) obj).getId().equals(getId());

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
		if (!(obj instanceof Assignment)) throw new ClassCastException();

		// if the object are the same, say so
		if (obj == this) return 0;

		// start the compare by comparing their sort names
		int compare = getTitle().compareTo(((Assignment) obj).getTitle());

		// if these are the same
		if (compare == 0)
		{
			// sort based on (unique) id
			compare = getId().compareTo(((Assignment) obj).getId());
		}

		return compare;

	} // compareTo
	
	/**
	 * Access the instructions.
	 * 
	 * @return The Assignment Content's instructions.
	 */
	public String getInstructions()
	{
		return m_instructions;
	}

	/**
	 * Get the type of valid submission.
	 * 
	 * @return int - Type of Submission.
	 */
	public int getTypeOfSubmission()
	{
		return m_typeOfSubmission;
	}

	/**
	 * Access a string describing the type of grade.
	 * 
	 * @param gradeType -
	 *        The integer representing the type of grade.
	 * @return Description of the type of grade.
	 */
	public String getTypeOfGradeString(int type)
	{
		String retVal = null;

		switch (type)
		{
			case 1:
				retVal = rb.getString("ungra");
				break;

			case 2:
				retVal = rb.getString("letter");
				break;

			case 3:
				retVal = rb.getString("points");
				break;

			case 4:
				retVal = rb.getString("pass");
				break;

			case 5:
				retVal = rb.getString("check");
				break;

			default:
				retVal = "Unknown Grade Type";
				break;
		}

		return retVal;
	}

	/**
	 * Get the grade type.
	 * 
	 * @return gradeType - The type of grade.
	 */
	public int getTypeOfGrade()
	{
		return m_typeOfGrade;
	}

	/**
	 * Get the maximum grade for grade type = SCORE_GRADE_TYPE(3)
	 * 
	 * @return The maximum grade score.
	 */
	public int getMaxGradePoint()
	{
		return m_maxGradePoint;
	}

	/**
	 * Get the maximum grade for grade type = SCORE_GRADE_TYPE(3) Formated to show one decimal place
	 * 
	 * @return The maximum grade score.
	 */
	public String getMaxGradePointDisplay()
	{
		// formated to show one decimal place, for example, 1000 to 100.0
		String one_decimal_maxGradePoint = m_maxGradePoint / 10 + "." + (m_maxGradePoint % 10);
		return one_decimal_maxGradePoint;
	}

	/**
	 * Get whether this project can be a group project.
	 * 
	 * @return True if this can be a group project, false otherwise.
	 */
	public boolean getGroupProject()
	{
		return m_groupProject;
	}

	/**
	 * Get whether group projects should be individually graded.
	 * 
	 * @return individGraded - true if projects are individually graded, false if grades are given to the group.
	 */
	public boolean individuallyGraded()
	{
		return m_individuallyGraded;
	}

	/**
	 * Gets whether grades can be released once submissions are graded.
	 * 
	 * @return true if grades can be released once submission are graded, false if they must be released manually.
	 */
	public boolean releaseGrades()
	{
		return m_releaseGrades;
	}

	/**
	 * Get the Honor Pledge type; values are NONE and ENGINEERING_HONOR_PLEDGE.
	 * 
	 * @return the Honor Pledge value.
	 */
	public int getHonorPledge()
	{
		return m_honorPledge;
	}

	/**
	 * Does this Assignment allow attachments?
	 * 
	 * @return true if the Assignment allows attachments, false otherwise?
	 */
	public boolean getAllowAttachments()
	{
		return m_allowAttachments;
	}
	
	/**
	 * Does this Assignment allow review service?
	 * 
	 * @return true if the Assignment allows review service, false otherwise?
	 */
	public boolean getAllowReviewService()
	{
		return m_allowReviewService;
	}
	
	public boolean getAllowStudentViewReport() {
		return m_allowStudentViewReport;
	}
	
	/**
	 * Set the title.
	 * 
	 * @param title -
	 *        The Assignment's title.
	 */
	public void setTitle(String title)
	{
		m_title = title;
	}

	/**
	 * Set the instructions.
	 * 
	 * @param instructions -
	 *        The Assignment's instructions.
	 */
	public void setInstructions(String instructions)
	{
		m_instructions = instructions;
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
	 * Set the type of valid submission.
	 * 
	 * @param int -
	 *        Type of Submission.
	 */
	public void setTypeOfSubmission(int type)
	{
		m_typeOfSubmission = type;
	}

	/**
	 * Set the grade type.
	 * 
	 * @param gradeType -
	 *        The type of grade.
	 */
	public void setTypeOfGrade(int gradeType)
	{
		m_typeOfGrade = gradeType;
	}

	/**
	 * Set the maximum grade for grade type = SCORE_GRADE_TYPE(3)
	 * 
	 * @param maxPoints -
	 *        The maximum grade score.
	 */
	public void setMaxGradePoint(int maxPoints)
	{
		m_maxGradePoint = maxPoints;
	}

	/**
	 * Set whether this project can be a group project.
	 * 
	 * @param groupProject -
	 *        True if this can be a group project, false otherwise.
	 */
	public void setGroupProject(boolean groupProject)
	{
		m_groupProject = groupProject;
	}

	/**
	 * Set whether group projects should be individually graded.
	 * 
	 * @param individGraded -
	 *        true if projects are individually graded, false if grades are given to the group.
	 */
	public void setIndividuallyGraded(boolean individGraded)
	{
		m_individuallyGraded = individGraded;
	}

	/**
	 * Sets whether grades can be released once submissions are graded.
	 * 
	 * @param release -
	 *        true if grades can be released once submission are graded, false if they must be released manually.
	 */
	public void setReleaseGrades(boolean release)
	{
		m_releaseGrades = release;
	}

	/**
	 * Set the Honor Pledge type; values are NONE and ENGINEERING_HONOR_PLEDGE.
	 * 
	 * @param pledgeType -
	 *        the Honor Pledge value.
	 */
	public void setHonorPledge(int pledgeType)
	{
		m_honorPledge = pledgeType;
	}

	
	/**
	 * Does this Assignment allow using the review service?
	 * 
	 * @param allow -
	 *        true if the Assignment allows review service, false otherwise?
	 */
	public void setAllowReviewService(boolean allow)
	{
		m_allowReviewService = allow;
	}
	
	/**
	 * Does this Assignment allow students to view the report?
	 * 
	 * @param allow -
	 *        true if the Assignment allows students to view the report, false otherwise?
	 */
	public void setAllowStudentViewReport(boolean allow) {
		m_allowStudentViewReport = allow;
	}
	
	/**
	 * Does this Assignment allow attachments?
	 * 
	 * @param allow -
	 *        true if the Assignment allows attachments, false otherwise?
	 */
	public void setAllowAttachments(boolean allow)
	{
		m_allowAttachments = allow;
	}
	
	/**
	 * Set the time last modified.
	 * 
	 * @param lastmod -
	 *        The Date at which the Content was last modified.
	 */
	public void setDateLastModified(Date lastmod)
	{
		if (lastmod != null) m_dateLastModified = lastmod;
	}

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


	/** The event code for this edit. */
	protected String m_event = null;

	/** Active flag. */
	protected boolean m_active = false;

	/**
	 * @deprecated
	 * Set the reference of the AssignmentContent of this Assignment.
	 * 
	 * @param String -
	 *        the reference of the AssignmentContent.
	 */
	public void setContentReference(String contentReference)
	{
		
	}

	/**
	 * @deprecated
	 * Set the AssignmentContent of this Assignment.
	 * 
	 * @param content -
	 *        the Assignment's AssignmentContent.
	 */
	public void setContent(AssignmentContent content)
	{
		
	}

	/**
	 * Set the section info
	 * 
	 * @param sectionId -
	 *        The section id
	 */
	public void setSection(String sectionId)
	{
		m_section = sectionId;
	}

	/**
	 * Set the first time at which the assignment can be viewed; may be null.
	 * 
	 * @param openDate -
	 *        The Date at which the Assignment opens.
	 */
	public void setOpenDate(Date openDate)
	{
		m_openDate = openDate;
	}

	/**
	 * Set the time at which the assignment is due; may be null.
	 * 
	 * @param dueDate -
	 *        The Date at which the Assignment is due.
	 */
	public void setDueDate(Date dueDate)
	{
		m_dueDate = dueDate;
	}

	/**
	 * Set the drop dead time after which responses to this assignment are considered late; may be null.
	 * 
	 * @param dropdeadtime -
	 *        The Date object representing the drop dead time.
	 */
	public void setDropDeadDate(Date dropdeadDate)
	{
		m_dropDeadDate = dropdeadDate;
	}

	/**
	 * Set the time after which this assignment can no longer be viewed, and after which submissions will not be accepted. May be null.
	 * 
	 * @param closeDate -
	 *        The Date after which the Assignment is closed, or null if unspecified.
	 */
	public void setCloseDate(Date closeDate)
	{
		m_closeDate = closeDate;
	}

	/**
	 * Set whether this is a draft or final copy.
	 * 
	 * @param draft -
	 *        true if this is a draft, false if it is a final copy.
	 */
	public void setDraft(boolean draft)
	{
		m_draft = draft;
	}
	
	/**
	 * Set the position order field for the an assignment.
	 * 
	 * @param position_order - 
	 *        The position order.
	 */
	public void setPosition_order(int position_order)
	{
		m_position_order = position_order;
	}

	/**
	 * Take all values from this object.
	 * 
	 * @param user
	 *        The user object to take values from.
	 */
	protected void set(Assignment assignment)
	{
		setAll(assignment);

	} // set
	
	/**
	 * Enable editing.
	 */
	protected void activate()
	{
		m_active = true;

	} // activate

	/**
	 * Check to see if the edit is still active, or has already been closed.
	 * 
	 * @return true if the edit is active, false if it's been closed.
	 */
	public boolean isActiveEdit()
	{
		return m_active;

	} // isActiveEdit

	/**
	 * Close the edit object - it cannot be used after this.
	 */
	protected void closeEdit()
	{
		m_active = false;

	} // closeEdit

	/******************************************************************************************************************************************************************************************************************************************************
	 * Group awareness implementation
	 *****************************************************************************************************************************************************************************************************************************************************/
	/**
	 * @inheritDoc
	 */
	public void setAccess(AssignmentAccess access)
	{
		m_access = access;
	}

	/**
	 * @inheritDoc
	 */
	public void setGroupAccess(Collection groups) throws PermissionException
	{	
		// convenience (and what else are we going to do?)
		if ((groups == null) || (groups.size() == 0))
		{
			clearGroupAccess();
			return;
		}
		
		// is there any change?  If we are already grouped, and the group list is the same, ignore the call
		if ((m_access == AssignmentAccess.GROUPED) && (EntityCollections.isEqualEntityRefsToEntities(m_groups, groups))) return;
		
		// there should not be a case where there's no context
		if (m_context == null)
		{
			M_log.warn(this + " setGroupAccess() called with null context: " + getReference());
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), "access:site", getReference());
		}

		// isolate any groups that would be removed or added
		Collection addedGroups = new Vector();
		Collection removedGroups = new Vector();
		EntityCollections.computeAddedRemovedEntityRefsFromNewEntitiesOldRefs(addedGroups, removedGroups, groups, m_groups);

		// verify that the user has permission to remove
		if (removedGroups.size() > 0)
		{
			// the Group objects the user has remove permission
			Collection allowedGroups = assignmentService.getGroupsAllowRemoveAssignment(m_context);

			for (Iterator i = removedGroups.iterator(); i.hasNext();)
			{
				String ref = (String) i.next();

				// is ref a group the user can remove from?
				if (!EntityCollections.entityCollectionContainsRefString(allowedGroups, ref))
				{
					throw new PermissionException(SessionManager.getCurrentSessionUserId(), "access:group:remove", ref);
				}
			}
		}
		
		// verify that the user has permission to add in those contexts
		if (addedGroups.size() > 0)
		{
			// the Group objects the user has add permission
			Collection allowedGroups = assignmentService.getGroupsAllowAddAssignment(m_context);

			for (Iterator i = addedGroups.iterator(); i.hasNext();)
			{
				String ref = (String) i.next();

				// is ref a group the user can remove from?
				if (!EntityCollections.entityCollectionContainsRefString(allowedGroups, ref))
				{
					throw new PermissionException(SessionManager.getCurrentSessionUserId(), "access:group:add", ref);
				}
			}
		}
		
		// we are clear to perform this
		m_access = AssignmentAccess.GROUPED;
		EntityCollections.setEntityRefsFromEntities(m_groups, groups);
	}

	/**
	 * @inheritDoc
	 */
	public void clearGroupAccess() throws PermissionException
	{			
		// is there any change?  If we are already site, ignore the call
		if (m_access == AssignmentAccess.SITE)
		{
			m_groups.clear();
			return;
		}

		if (m_context == null)
		{
			// there should not be a case where there's no context
			M_log.warn(this + " clearGroupAccess() called with null context. " + getReference());
			throw new PermissionException(SessionManager.getCurrentSessionUserId(), "access:site", getReference());
		}
		else
		{
			// verify that the user has permission to add in the site context
			if (!assignmentService.allowAddSiteAssignment(m_context))
			{
				throw new PermissionException(SessionManager.getCurrentSessionUserId(), "access:site", getReference());				
			}
		}

		// we are clear to perform this
		m_access = AssignmentAccess.SITE;
		m_groups.clear();
		
	}
	
} // BaseAssignment
