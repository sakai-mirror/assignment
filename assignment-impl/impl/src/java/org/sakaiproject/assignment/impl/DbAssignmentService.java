/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.assignment.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentContent;
import org.sakaiproject.assignment.api.AssignmentContentEdit;
import org.sakaiproject.assignment.api.AssignmentEdit;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.api.AssignmentSubmissionEdit;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.util.BaseDbSingleStorage;
import org.sakaiproject.util.Xml;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * <p>
 * DbAssignmentService is the database-storing service class for Assignments.
 * </p>
 */
public class DbAssignmentService extends BaseAssignmentService
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(DbAssignmentService.class);

	/** The name of the db table holding assignment objects. */
	protected String m_assignmentsTableName = "ASSIGNMENT_ASSIGNMENT";

	/** The name of the db table holding assignment content objects. */
	protected String m_contentsTableName = "ASSIGNMENT_CONTENT";

	/** The name of the db table holding assignment submission objects. */
	protected String m_submissionsTableName = "ASSIGNMENT_SUBMISSION";

	/** If true, we do our locks in the remote database, otherwise we do them here. */
	protected boolean m_locksInDb = true;

	/** Extra fields to store in the db with the XML. */
	protected static final String[] FIELDS = { "CONTEXT" };

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Constructors, Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Dependency: SqlService */
	protected SqlService m_sqlService = null;

	/**
	 * Dependency: SqlService.
	 * 
	 * @param service
	 *        The SqlService.
	 */
	public void setSqlService(SqlService service)
	{
		m_sqlService = service;
	}

	/**
	 * Configuration: set the table name for assignments.
	 * 
	 * @param path
	 *        The table name for assignments.
	 */
	public void setAssignmentTableName(String name)
	{
		m_assignmentsTableName = name;
	}

	/**
	 * Configuration: set the table name for contents.
	 * 
	 * @param path
	 *        The table name for contents.
	 */
	public void setContentTableName(String name)
	{
		m_contentsTableName = name;
	}

	/**
	 * Configuration: set the table name for submissions.
	 * 
	 * @param path
	 *        The table name for submissions.
	 */
	public void setSubmissionTableName(String name)
	{
		m_submissionsTableName = name;
	}

	/**
	 * Configuration: set the locks-in-db
	 * 
	 * @param value
	 *        The locks-in-db value.
	 */
	public void setLocksInDb(String value)
	{
		m_locksInDb = new Boolean(value).booleanValue();
	}

	/** Set if we are to run the to-context conversion. */
	protected boolean m_convertToContext = false;

	/**
	 * Configuration: run the to-context conversion
	 * 
	 * @param value
	 *        The locks-in-db value.
	 */
	public void setConvertToContext(String value)
	{
		m_convertToContext = new Boolean(value).booleanValue();
	}

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		m_autoDdl = new Boolean(value).booleanValue();
	}

	/** Set if we are to run the from-old-schema conversion. */
	protected boolean m_convertOldSchema = false;

	/**
	 * Configuration: run the from-old-schema conversion.
	 * @param value The conversion desired value.
	 */
	public void setConvertOldSchema(String value)
	{
		m_convertOldSchema = new Boolean(value).booleanValue();
	}
	
	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// if we are auto-creating our schema, check and create
			if (m_autoDdl)
			{
				m_sqlService.ddl(this.getClass().getClassLoader(), "sakai_assignment");
			}

			super.init();

			M_log.info("init: assignments table: " + m_assignmentsTableName + " contents table: " + m_contentsTableName
					+ " submissions table: " + m_submissionsTableName + " locks-in-db" + m_locksInDb);

			// convert?
			if (m_convertToContext)
			{
				m_convertToContext = false;
				convertToContext();
			}
			
			// convert to new db tables
			if (m_convertOldSchema)
			{
				m_convertOldSchema = false;
				convertOldSchema();
			}
		}
		catch (Throwable t)
		{
			M_log.warn(this + ".init(): ", t);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * BaseAssignmentService extensions
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Construct a Storage object for Assignments.
	 * 
	 * @return The new storage object for Assignments.
	 */
	protected AssignmentStorage newAssignmentStorage()
	{
		return new DbCachedAssignmentStorage(new AssignmentStorageUser());

	} // newAssignmentStorage

	/**
	 * Construct a Storage object for AssignmentsContents.
	 * 
	 * @return The new storage object for AssignmentContents.
	 */
	protected AssignmentContentStorage newContentStorage()
	{
		return new DbCachedAssignmentContentStorage(new AssignmentContentStorageUser());

	} // newContentStorage

	/**
	 * Construct a Storage object for AssignmentSubmissions.
	 * 
	 * @return The new storage object for AssignmentSubmissions.
	 */
	protected AssignmentSubmissionStorage newSubmissionStorage()
	{
		return new DbCachedAssignmentSubmissionStorage(new AssignmentSubmissionStorageUser());

	} // newSubmissionStorage

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Storage implementations
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentStorage implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Covers for the BaseDbSingleStorage, providing Assignment and AssignmentEdit parameters
	 */
	protected class DbCachedAssignmentStorage extends BaseDbSingleStorage implements AssignmentStorage
	{
		/**
		 * Construct.
		 * 
		 * @param assignment
		 *        The StorageUser class to call back for creation of Resource and Edit objects.
		 */
		public DbCachedAssignmentStorage(AssignmentStorageUser assignment)
		{
			super(m_assignmentsTableName, "ASSIGNMENT_ID", FIELDS, m_locksInDb, "assignment", assignment, m_sqlService);

		} // DbCachedAssignmentStorage

		public boolean check(String id)
		{
			return super.checkResource(id);
		}

		public Assignment get(String id)
		{
			return (Assignment) super.getResource(id);
		}

		public List getAll(String context)
		{
			return super.getAllResourcesWhere(FIELDS[0], context);
		}

		public AssignmentEdit put(String id, String context)
		{
			// pack the context in an array
			Object[] others = new Object[1];
			others[0] = context;
			return (AssignmentEdit) super.putResource(id, others);
		}

		public AssignmentEdit edit(String id)
		{
			return (AssignmentEdit) super.editResource(id);
		}

		public void commit(AssignmentEdit edit)
		{
			super.commitResource(edit);
		}

		public void cancel(AssignmentEdit edit)
		{
			super.cancelResource(edit);
		}

		public void remove(AssignmentEdit edit)
		{
			super.removeResource(edit);
		}

	} // DbCachedAssignmentStorage

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentContentStorage implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Covers for the BaseDbSingleStorage, providing AssignmentContent and AssignmentContentEdit parameters
	 */
	protected class DbCachedAssignmentContentStorage extends BaseDbSingleStorage implements AssignmentContentStorage
	{
		/**
		 * Construct.
		 * 
		 * @param content
		 *        The StorageUser class to call back for creation of Resource and Edit objects.
		 */
		public DbCachedAssignmentContentStorage(AssignmentContentStorageUser content)
		{
			super(m_contentsTableName, "CONTENT_ID", FIELDS, m_locksInDb, "content", content, m_sqlService);

		} // DbCachedAssignmentContentStorage

		public boolean check(String id)
		{
			return super.checkResource(id);
		}

		public AssignmentContent get(String id)
		{
			return (AssignmentContent) super.getResource(id);
		}

		public List getAll(String context)
		{
			return super.getAllResourcesWhere(FIELDS[0], context);
		}

		public AssignmentContentEdit put(String id, String context)
		{
			// pack the context in an array
			Object[] others = new Object[1];
			others[0] = context;
			return (AssignmentContentEdit) super.putResource(id, others);
		}

		public AssignmentContentEdit edit(String id)
		{
			return (AssignmentContentEdit) super.editResource(id);
		}

		public void commit(AssignmentContentEdit edit)
		{
			super.commitResource(edit);
		}

		public void cancel(AssignmentContentEdit edit)
		{
			super.cancelResource(edit);
		}

		public void remove(AssignmentContentEdit edit)
		{
			super.removeResource(edit);
		}

	} // DbCachedAssignmentContentStorage

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentSubmissionStorage implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Covers for the BaseDbSingleStorage, providing AssignmentSubmission and AssignmentSubmissionEdit parameters
	 */
	protected class DbCachedAssignmentSubmissionStorage extends BaseDbSingleStorage implements AssignmentSubmissionStorage
	{
		/**
		 * Construct.
		 * 
		 * @param submission
		 *        The StorageUser class to call back for creation of Resource and Edit objects.
		 */
		public DbCachedAssignmentSubmissionStorage(AssignmentSubmissionStorageUser submission)
		{
			super(m_submissionsTableName, "SUBMISSION_ID", FIELDS, m_locksInDb, "submission", submission, m_sqlService);

		} // DbCachedAssignmentSubmissionStorage

		public boolean check(String id)
		{
			return super.checkResource(id);
		}

		public AssignmentSubmission get(String id)
		{
			return (AssignmentSubmission) super.getResource(id);
		}

		public List getAll(String context)
		{
			return super.getAllResourcesWhere(FIELDS[0], context);
		}

		public AssignmentSubmissionEdit put(String id, String context, String assignmentId)
		{
			// pack the context in an array
			Object[] others = new Object[2];
			others[0] = context;
			others[1] = assignmentId;
			return (AssignmentSubmissionEdit) super.putResource(id, others);
		}

		public AssignmentSubmissionEdit edit(String id)
		{
			return (AssignmentSubmissionEdit) super.editResource(id);
		}

		public void commit(AssignmentSubmissionEdit edit)
		{
			super.commitResource(edit);
		}

		public void cancel(AssignmentSubmissionEdit edit)
		{
			super.cancelResource(edit);
		}

		public void remove(AssignmentSubmissionEdit edit)
		{
			super.removeResource(edit);
		}

	} // DbCachedAssignmentSubmissionStorage

	/**
	 * fill in the context field for any record missing it
	 */
	protected void convertToContext()
	{
		M_log.info("convertToContext");

		try
		{
			// get a connection
			final Connection connection = m_sqlService.borrowConnection();
			boolean wasCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// read all assignment records
			String sql = "select XML from ASSIGNMENT_ASSIGNMENT where CONTEXT is null";
			m_sqlService.dbRead(connection, sql, null, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// create the Resource from the db xml
						String xml = result.getString(1);

						// read the xml
						Document doc = Xml.readDocumentFromString(xml);

						// verify the root element
						Element root = doc.getDocumentElement();
						if (!root.getTagName().equals("assignment"))
						{
							M_log.warn("convertToContext(): XML root element not assignment: " + root.getTagName());
							return null;
						}
						Assignment a = new BaseAssignment(root);
						// context is context
						String context = a.getContext();
						String id = a.getId();

						// update
						String update = "update ASSIGNMENT_ASSIGNMENT set CONTEXT = ? where ASSIGNMENT_ID = ?";
						Object fields[] = new Object[2];
						fields[0] = context;
						fields[1] = id;
						boolean ok = m_sqlService.dbWrite(connection, update, fields);

						M_log.info("convertToContext: assignment id: " + id + " context: " + context + " ok: " + ok);

						return null;
					}
					catch (SQLException ignore)
					{
						return null;
					}
				}
			});

			// read all content records
			sql = "select XML from ASSIGNMENT_CONTENT where CONTEXT is null";
			m_sqlService.dbRead(connection, sql, null, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// create the Resource from the db xml
						String xml = result.getString(1);

						// read the xml
						Document doc = Xml.readDocumentFromString(xml);

						// verify the root element
						Element root = doc.getDocumentElement();
						if (!root.getTagName().equals("content"))
						{
							M_log.warn("convertToContext(): XML root element not content: " + root.getTagName());
							return null;
						}
						AssignmentContent c = new BaseAssignmentContent(root);
						// context is creator
						String context = c.getCreator();
						String id = c.getId();

						// update
						String update = "update ASSIGNMENT_CONTENT set CONTEXT = ? where CONTENT_ID = ?";
						Object fields[] = new Object[2];
						fields[0] = context;
						fields[1] = id;
						boolean ok = m_sqlService.dbWrite(connection, update, fields);

						M_log.info("convertToContext: content id: " + id + " context: " + context + " ok: " + ok);

						return null;
					}
					catch (SQLException ignore)
					{
						return null;
					}
				}
			});

			// read all submission records
			sql = "select XML from ASSIGNMENT_SUBMISSION where CONTEXT is null";
			m_sqlService.dbRead(connection, sql, null, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// create the Resource from the db xml
						String xml = result.getString(1);

						// read the xml
						Document doc = Xml.readDocumentFromString(xml);

						// verify the root element
						Element root = doc.getDocumentElement();
						if (!root.getTagName().equals("submission"))
						{
							M_log.warn("convertToContext(): XML root element not submission: " + root.getTagName());
							return null;
						}
						AssignmentSubmission s = new BaseAssignmentSubmission(root);
						// context is assignment id
						String context = s.getAssignmentId();
						String id = s.getId();

						// update
						String update = "update ASSIGNMENT_SUBMISSION set CONTEXT = ? where SUBMISSION_ID = ?";
						Object fields[] = new Object[2];
						fields[0] = context;
						fields[1] = id;
						boolean ok = m_sqlService.dbWrite(connection, update, fields);

						M_log.info("convertToContext: submission id: " + id + " context: " + context + " ok: " + ok);

						return null;
					}
					catch (SQLException ignore)
					{
						return null;
					}
				}
			});

			connection.commit();
			connection.setAutoCommit(wasCommit);
			m_sqlService.returnConnection(connection);
		}
		catch (Throwable t)
		{
			M_log.warn("convertToContext: failed: " + t);
		}

		// TODO:
		M_log.info("convertToContext: done");
	}
	
	/**
	 * Create a new table record for all old table records found, and delete the old.
	 */
	protected void convertOldSchema()
	{
		M_log.info(this + ".convertOld");

		try
		{
			// get a connection
			final Connection connection = m_sqlService.borrowConnection();
			boolean wasCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			// read all submission we don't already have converted
			String sql = "select SUBMISSION_ID, CONTEXT, XML from ASSIGNMENT_SUBMISSION where SUBMISSION_ID NOT IN (select SUBMISSION_ID from ASSIGNMENT_SUBMISSION_DETAIL)";
			List submissions = m_sqlService.dbRead(sql, null,
				new SqlReader()
				{
					public Object readSqlResultRecord(ResultSet result)
					{
						String submission_id = null;
						String assignment_id = null;
						try
						{
							// create the Resource from the db xml
							submission_id = result.getString(1);
							assignment_id = result.getString(2);
							String xml = result.getString(3);
							
							// read the xml
							Document doc =  Xml.readDocumentFromString(xml);

							// verify the root element
							Element root = doc.getDocumentElement();
							if (!root.getTagName().equals("submission"))
							{
								M_log.warn(this + ".convertOldSchema: XML root element is not submission: " + root.getTagName());
								return null;
							}
							BaseAssignmentSubmissionEdit edit = new BaseAssignmentSubmissionEdit(root);
							return edit;
						}
						catch (Throwable e)
						{
							M_log.info(" ** exception converting : " + submission_id + " : ", e);
							return null;
						}
					}
				} );

			M_log.info(this + ".convertOldSchema: read submission: " + submissions.size());

			// write the submissions
			Object[] fields22 = new Object[22];
			Object[] fields2_1 = new Object[2];
			Object[] fields2_2 = new Object[2];
			String statement1 = "insert into ASSIGNMENT_SUBMISSION_DETAIL (SUBMISSION_ID, ASSIGNMENT_ID, SITE_ID, DATE_RETURNED, DATE_SUBMITTED, FEEDBACK_COMMENT, FEEDBACK_TEXT, " +
					"SUBMITTED, GRADED, RETURNED, GRADE_RELEASED, HONOR_PLEDGE_FLAG, SCALED_GRADE, SUBMITTED_TEXT, SUBMITTER, CREATED_BY, MODIFIED_BY, DATE_CREATED, DATE_MODIFIED," +
					"REVIEW_REPORT, REVIEW_SCORE, REVIEW_STATUS) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			String statement2 = "insert into ASSIGNMENT_SUBMITTED_ATTACHMENT (SUBMISSION_ID, ATTACHMENT_ID) values (?,?)";
			String statement3 = "insert into ASSIGNMENT_FEEDBACK_ATTACHMENT (SUBMISSION_ID, ATTACHMENT_ID) values (?,?)";

			int count = 0;
			for (Iterator iSubmissions = submissions.iterator(); iSubmissions.hasNext();)
			{
				BaseAssignmentSubmissionEdit submission = (BaseAssignmentSubmissionEdit) iSubmissions.next();
				ResourceProperties properties = submission.getProperties();
				
				// 1. write the main site record
				fields22[0] = StringUtil.trimToZero(submission.m_id);
				fields22[1] = StringUtil.trimToZero(submission.m_assignment);
				fields22[2] = StringUtil.trimToZero(submission.m_context);
				fields22[3] = submission.getTimeReturned();
				fields22[4] = submission.getTimeSubmitted();
				fields22[5] = StringUtil.trimToZero(submission.m_feedbackComment);
				fields22[6] = StringUtil.trimToZero(submission.m_feedbackText);
				fields22[7] = submission.getSubmitted() ? "1" : "0";
				fields22[8] = submission.getGraded() ? "1" : "0";
				fields22[9] = submission.getReturned() ? "1" : "0";
				fields22[10] = submission.getGradeReleased() ? "1" : "0";
				fields22[11] = submission.getHonorPledgeFlag() ? "1" : "0";
				fields22[12] = submission.m_grade;
				fields22[13] = StringUtil.trimToZero(submission.getSubmittedText());
				List submitters = submission.m_submitters;
				if ( submitters != null && submitters.size() > 0)
				{
					fields22[14] = StringUtil.trimToZero((String) submitters.get(0));
				}
				else
				{
					fields22[14]="";
				}
				fields22[15] = StringUtil.trimToZero(properties.getProperty(ResourceProperties.PROP_CREATOR));
				fields22[16] = StringUtil.trimToZero(properties.getProperty(ResourceProperties.PROP_MODIFIED_BY));
				fields22[17] = properties.getTimeProperty(ResourceProperties.PROP_CREATION_DATE);
				fields22[18] = properties.getTimeProperty(ResourceProperties.PROP_MODIFIED_DATE);
				fields22[19] = StringUtil.trimToZero(submission.getReviewReport());
				fields22[20] = new Integer(submission.getReviewScore());
				fields22[21] = StringUtil.trimToZero(submission.getReviewStatus());
				m_sqlService.dbWrite(connection, statement1, fields22);

				// 2.write the submitted attachments
				List submittedAttachments = submission.getSubmittedAttachments();
				if (submittedAttachments != null && submittedAttachments.size() > 0)
				{
					Iterator iSAttachments = submittedAttachments.iterator();
					while (iSAttachments.hasNext())
					{
						fields2_1[0] = submission.getId();
						fields2_1[1] = ((Reference) iSAttachments.next()).getId();
						m_sqlService.dbWrite(connection, statement2, fields2_1);
					}
				}
				
				// 3.write the feedback attachments
				List feedbackAttachments = submission.getFeedbackAttachments();
				if (feedbackAttachments != null && feedbackAttachments.size() > 0)
				{
					Iterator iFAttachments = feedbackAttachments.iterator();
					while (iFAttachments.hasNext())
					{
						fields2_2[0] = submission.getId();
						fields2_2[1] = ((Reference) iFAttachments.next()).getId();
						m_sqlService.dbWrite(connection, statement3, fields2_2);
					}
				}

				count++;
				if ((count % 1000) == 0) M_log.info(this + "convertOld: converted: " + count);
			}

			connection.commit();
			
			M_log.info(this + ".convertOldSchema: done submissions: " + count);

			connection.setAutoCommit(wasCommit);
			m_sqlService.returnConnection(connection);
		}
		catch (Throwable t)
		{
			M_log.warn(this + ".convertOldSchema: failed: " + t);		
		}

		M_log.info(this + ".convertOldSchema: done");
	}
}
