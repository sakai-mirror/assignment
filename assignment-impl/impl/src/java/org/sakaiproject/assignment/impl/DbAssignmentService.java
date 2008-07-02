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
import java.util.Date;
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
import org.sakaiproject.entity.api.Entity;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.util.BaseDbFlatStorage;
import org.sakaiproject.util.StringUtil;
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
	protected String m_assignmentTableName = "ASSIGNMENT_ASSIGNMENT_T";

	/** The name of the db table holding assignment submission objects. */
	protected String m_submissionsTableName = "ASSIGNMENT_SUBMISSION_T";

	/** If true, we do our locks in the remote database, otherwise we do them here. */
	protected boolean m_locksInDb = true;

	/** assignment table fields. */
	protected static final String[] m_assignmentFields = { "CONTEXT", "IS_SITE_RANGE", "OPEN_DATE", "DUE_DATE", "CLOSE_DATE", "CREATED_ON", "CREATED_BY", "MODIFIED_ON", "MODIFIED_BY", "GRADE_TYPE", "SUBMISSION_TYPE", "MAX_POINT", "DRAFT", "DELETED", "INSTRUCTION", "ANNOUNCE_OPEN_DATE", "SCHEDULE_DUE_DATE", "EMAIL_NOTIFICATION_OPTION", "RESUBMISSION_MAX_NUMBER", "RESUBMISSION_CLOSE_DATE", "ASSOCIATED_GRADEBOOK_ENTRY", "HONOR_PLEDGE"};
	
	/** assignment property table*/
	protected static final String m_assignmentPropTableName = "ASSIGNMENT_PROPERTY";
	
	/** assignment table id*/
	protected static final String m_assignmentTableId = "ASSIGNMENT_ID";
	
	/** submission table fields*/
	protected static final String[] m_submissionFields = { "CONTEXT", "SUBMITTER_ID", "SUBMIT_TIME", "SUBMITTED", "GRADED"};
	
	/** submission property table*/
	protected static final String m_submissionPropTableName = "SUBMISSION_PROPERTY";
	
	/** submission table id*/
	protected static final String m_submissionTableId = "SUBMISSION_ID";

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
	
	/** Dependency: session Service */
	protected SessionManager m_sessionManager = null;

	/**
	 * Dependency: SessionManager.
	 * 
	 * @param sessionManager
	 *        The SessionManager.
	 */
	public void setSessionManager(SessionManager sessionManager)
	{
		m_sessionManager = sessionManager;
	}
	
	/** Dependency: time Service */
	protected TimeService m_timeService = null;

	/**
	 * Dependency: TimeService.
	 * 
	 * @param timeService
	 *        The timeService.
	 */
	public void settimeService(TimeService timeService)
	{
		m_timeService = timeService;
	}

	/**
	 * Configuration: set the table name for assignments.
	 * 
	 * @param path
	 *        The table name for assignments.
	 */
	public void setAssignmentTableName(String name)
	{
		m_assignmentTableName = name;
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

			M_log.info("init: assignments table: " + m_assignmentTableName + " submissions table: " + m_submissionsTableName + " locks-in-db" + m_locksInDb);

			// convert?
			if (m_convertToContext)
			{
				m_convertToContext = false;
				//TO-DO: convertToContext();
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
	protected class DbCachedAssignmentStorage extends BaseDbFlatStorage implements AssignmentStorage, SqlReader
	{
		/**
		 * Construct.
		 * 
		 * @param assignment
		 *        The StorageUser class to call back for creation of Resource and Edit objects.
		 */
		public DbCachedAssignmentStorage(AssignmentStorageUser assignment)
		{
			super(m_assignmentTableName, m_assignmentTableId, m_assignmentFields, m_assignmentPropTableName, m_locksInDb, null, m_sqlService);
			m_reader = this;

			// no locking
			setLocking(false);

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
			String where = m_assignmentFields[0] + "='" + context + "'";
			String order = "ASSIGNMENT_ASSIGNMENT_T.DUE_DATE";
			int first = 1;
			int last = 20;
			return super.getSelectedResources(where, order, null, first, last, null);
		}

		public Assignment put(String id, String context)
		{
			// pack the context in an array
			Object[] others = new Object[1];
			others[0] = context;

			BaseAssignment rv = (BaseAssignment) super.putResource(id, fields(id, context, null, false));
			if (rv != null) rv.activate();
			return rv;
		}
		
		/**
		 * Get the fields for the database from the edit for this id, and the id again at the end if needed
		 * 
		 * @param id
		 *        The resource id
		 * @param context
		 * 	      The context id
		 * @param edit
		 *        The edit (may be null in a new)
		 * @param idAgain
		 *        If true, include the id field again at the end, else don't.
		 * @return The fields for the database.
		 */
		protected Object[] fields(String id, String context, BaseAssignment edit, boolean idAgain)
		{
			Object[] rv = new Object[idAgain ? 24 : 23];
			rv[0] = caseId(id);
			if (idAgain)
			{
				rv[23] = rv[0];
			}


			String current = m_sessionManager.getCurrentSessionUserId();
			Time now = m_timeService.newTime();
			
			if (edit == null)
			{
				rv[1] = context;
				rv[2] = "1";
				rv[3] = now;
				rv[4] = now;
				rv[5] = now;
				rv[6] = now;
				rv[7] = current;
				rv[8] = now;
				rv[9] = current;
				rv[10] = new Integer("1"); // not graded type
				rv[11] = new Integer("3");	// text and attachment
				rv[12] = "";
				rv[13] = new Integer("1");
				rv[14] = new Integer("0");
				rv[15] = "";
				rv[16] = new Integer("0");	// not announced
				rv[17] = new Integer("0");	// not scheduled
				rv[18] = new Integer("0");	// no notification
				rv[19] = "";
				rv[20] = "";
				rv[21] = "";
				rv[22] = new Integer("0");	// honor pledge to be off
			}
			else
			{
				// TO-DO
			}

			return rv;
		}

		public Assignment edit(String id)
		{
			return (Assignment) super.editResource(id);
		}

		public void commit(final Assignment edit)
		{
			// run our save code in a transaction that will restart on deadlock
			// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
			m_sqlService.transact(new Runnable()
			{
				public void run()
				{
					saveAssignmentTx(edit);
				}
			}, "Assignment:" + edit.getId());
		}
		
		/**
		 * The transaction code to save an assignment.
		 * 
		 * @param edit
		 *        The assignment to save.
		 */
		protected void saveAssignmentTx(Assignment edit)
		{
			// TO-DO
		}

		public void cancel(Assignment edit)
		{
			super.cancelResource(edit);
		}

		public void remove(Assignment edit)
		{
			super.removeResource(edit);
		}
		
		public Object readSqlResultRecord(ResultSet result)
		{
			try
			{
				String id = result.getString(1);
				String context = result.getString(2);
				boolean isSiteRange = "1".equals(result.getString(3))?true:false;
				java.sql.Date openDate = result.getDate(4, m_sqlService.getCal());
				java.sql.Date dueDate = result.getDate(5, m_sqlService.getCal());
				java.sql.Date closeDate = result.getDate(6, m_sqlService.getCal());
				java.sql.Date createdOn = result.getDate(7, m_sqlService.getCal());
				String createdBy = result.getString(8);
				java.sql.Date modifiedOn = result.getDate(9, m_sqlService.getCal());
				String modifiedBy = result.getString(10);
				int gradeType = result.getInt(11);
				int submissionType = result.getInt(12);
				int maxPoint = result.getInt(13);
				boolean draft = "1".equals(result.getString(14))?true:false;
				boolean deleted = "1".equals(result.getString(15))?true:false;
				String instruction = result.getString(16);
				boolean announceOpenDate = "1".equals(result.getString(17))?true:false;
				boolean scheduleDueDate = "1".equals(result.getString(18))?true:false;
				int emailNotificationOption = result.getInt(19);
				int resubmissionMaxNumber = result.getInt(20);
				java.sql.Date resubmissionCloseDate = result.getDate(21, m_sqlService.getCal());
				String associatedGradebookEntry = result.getString(22);
				boolean honorPledge = "1".equals(result.getString(23))?true:false;

				// create the Resource from these fields
				return new BaseAssignment(id, context, isSiteRange, openDate, dueDate, closeDate, createdOn, createdBy, modifiedOn, modifiedBy, gradeType,
						submissionType, maxPoint, draft, deleted, instruction, announceOpenDate, scheduleDueDate, emailNotificationOption, 
						resubmissionMaxNumber, resubmissionCloseDate, associatedGradebookEntry, honorPledge);
			}
			catch (SQLException e)
			{
				M_log.warn("readSqlResultRecord: " + e);
				return null;
			}
		}

	} // DbCachedAssignmentStorage

	/**********************************************************************************************************************************************************************************************************************************************************
	 * AssignmentSubmissionStorage implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Covers for the BaseDbSingleStorage, providing AssignmentSubmission and AssignmentSubmissionEdit parameters
	 */
	protected class DbCachedAssignmentSubmissionStorage extends BaseDbFlatStorage implements AssignmentSubmissionStorage, SqlReader
	{
		/*m_assignmentFields: "CONTEXT", "SUBMITTER_ID", "SUBMIT_TIME", "SUBMITTED", "GRADED"*/
		String m_submissionIdFieldName = "SUBMISSION_ID";
		
		
		/**
		 * Construct.
		 * 
		 * @param submission
		 *        The StorageUser class to call back for creation of Resource and Edit objects.
		 */
		public DbCachedAssignmentSubmissionStorage(AssignmentSubmissionStorageUser submission)
		{
			super(m_submissionsTableName, m_submissionTableId, m_submissionFields, m_submissionPropTableName, m_locksInDb, null, m_sqlService);
			m_reader = this;

			// no locking
			setLocking(false);

		} // DbCachedAssignmentSubmissionStorage

		public boolean check(String id)
		{
			return super.checkResource(id);
		}

		public AssignmentSubmission get(String id)
		{
			return (AssignmentSubmission) super.getResource(id);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public AssignmentSubmission get (String assignmentId, String userId)
		{
			Entity entry = null;

			// get the user from the db 
			// need to construct the query here instead of relying on the SingleStorage client
			String sql = "select * from " + m_submissionsTableName + " where (" + m_submissionFields[0] + " = ? AND "+  m_submissionFields[1] + " = ?)";

			Object fields[] = new Object[2];
			fields[0] = caseId(assignmentId);
			fields[1] = caseId(userId);
			/*List xml = m_sql.dbRead(sql, fields, null);
			if (!xml.isEmpty())
			{
				// create the Resource from the db xml
				entry = readResource((String) xml.get(0));
				return (AssignmentSubmission) entry;
			}
			else
			{
				return null;
			}*/
			// TO-DO 
			return null;
		}
		
		/**
		 * {@inheritDoc}
		 */
		public int getSubmittedSubmissionsCount(String assignmentId)
		{
			Object[] values = new Object[1];
			values[0] = assignmentId;
			String where = "where context=? AND " + m_submissionFields[2] + " IS NOT NULL AND " + m_submissionFields[3] + "='" + Boolean.TRUE.toString() + "'" ;
			return super.countSelectedResources(where, values);
		}
		
		/**
		 * {@inheritDoc}
		 */
		public int getUngradedSubmissionsCount(String assignmentId)
		{
			Object[] values = new Object[1];
			values[0] = assignmentId;
			String where = "where context=? AND " + m_submissionFields[2] + " IS NOT NULL AND " + m_submissionFields[3] + "='" + Boolean.TRUE.toString() + "' AND " + m_submissionFields[4] + "='" + Boolean.FALSE.toString() + "'"  ;
			return super.countSelectedResources(where, values);
		}
		

		public List getAll(String context)
		{
			String where = m_submissionFields[0] + "=" + context;
			String order = "";
			int first = 0;
			int last = 20;
			String join = "";
			return super.getSelectedResources(where, order, null, first, last, join);
		}

		public AssignmentSubmission put(String id, String assignmentId, String submitterId, String submitTime, String submitted, String graded)
		{
			// pack the context in an array
			Object[] others = new Object[5];
			others[0] = assignmentId;
			others[1] = submitterId;
			others[2] = submitTime;
			others[3] = submitted;
			others[4] = graded;
			
			return (AssignmentSubmission) super.putResource(id, others);
		}

		public AssignmentSubmission edit(String id)
		{
			return (AssignmentSubmission) super.editResource(id);
		}

		public void commit(final AssignmentSubmission edit)
		{
			// run our save code in a transaction that will restart on deadlock
			// if deadlock retry fails, or any other error occurs, a runtime error will be thrown
			m_sqlService.transact(new Runnable()
			{
				public void run()
				{
					saveSubmissionTx(edit);
				}
			}, "Assignment:" + edit.getId());
		}
		
		/**
		 * The transaction code to save an assignment submission.
		 * 
		 * @param edit
		 *        The assignment to save.
		 */
		protected void saveSubmissionTx(AssignmentSubmission edit)
		{
			// TO-DO
		}

		public void cancel(AssignmentSubmission edit)
		{
			super.cancelResource(edit);
		}

		public void remove(AssignmentSubmission edit)
		{
			super.removeResource(edit);
		}
		
		public Object readSqlResultRecord(ResultSet result)
		{
			return null;
		}

	} // DbCachedAssignmentSubmissionStorage

	/**
	 * fill in the context field for any record missing it
	 */
	protected void convertToContext()
	{
		M_log.info(this + " convertToContext");

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
							M_log.warn(this + " convertToContext(): XML root element not assignment: " + root.getTagName());
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

						M_log.info(this + " convertToContext: assignment id: " + id + " context: " + context + " ok: " + ok);

						return null;
					}
					catch (SQLException ignore)
					{
						M_log.warn(this + ":convertToContext " + ignore.getMessage());
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
							M_log.warn(this + " convertToContext(): XML root element not submission: " + root.getTagName());
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

						M_log.info(this + " convertToContext: submission id: " + id + " context: " + context + " ok: " + ok);

						return null;
					}
					catch (SQLException ignore)
					{
						M_log.warn(this + ":convertToContext:SqlReader " + ignore.getMessage());
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
			M_log.warn(this + " convertToContext: failed: " + t);
		}

		// TODO:
		M_log.info(this + " convertToContext: done");
	}
}
