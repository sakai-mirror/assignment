/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Sakai Foundation.
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

package org.sakaiproject.assignment.taggable.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.assignment.taggable.api.AssignmentActivityProducer;
import org.sakaiproject.taggable.activity.api.TaggableActivity;
import org.sakaiproject.taggable.activity.api.TaggableItem;
import org.sakaiproject.taggable.api.Taggable;
import org.sakaiproject.taggable.api.TaggingManager;
import org.sakaiproject.taggable.api.TaggingProvider;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.util.ResourceLoader;

public class AssignmentActivityProducerImpl implements
		AssignmentActivityProducer {

	private static final Log logger = LogFactory
			.getLog(AssignmentActivityProducerImpl.class);

	private static ResourceLoader rb = new ResourceLoader("assignment");

	protected AssignmentService assignmentService;

	protected EntityManager entityManager;

	protected TaggingManager taggingManager;

	protected SiteService siteService;

	protected SecurityService securityService;

	protected UserDirectoryService userDirectoryService;

	public boolean allowRemoveTags(Taggable taggable) {
		boolean allowed = false;
		if (taggable instanceof TaggableActivity) {
			allowed = securityService.unlock(
					AssignmentService.SECURE_REMOVE_ASSIGNMENT, taggable
							.getReference());
		} else if (taggable instanceof TaggableItem) {
			allowed = securityService.unlock(
					AssignmentService.SECURE_REMOVE_ASSIGNMENT_SUBMISSION,
					parseSubmissionRef(taggable.getReference()));
		}
		return allowed;
	}

	public boolean allowTransferCopyTags(Taggable taggable) {
		return securityService.unlock(SiteService.SECURE_UPDATE_SITE,
				siteService.siteReference(taggable.getContext()));
	}

	public boolean checkReference(String ref) {
		return ref.startsWith(AssignmentService.REFERENCE_ROOT);
	}

	public Taggable get(String ref, TaggingProvider provider) {
		/*
		 * We aren't picky about the provider, so ignore that argument.
		 */
		Taggable taggable = null;
		if (checkReference(ref)) {
			try {
				/*
				 * Pass the reference through parseSubmissionRef(). This should
				 * pull off the appended userId if it exists.
				 */
				String pureRef = parseSubmissionRef(ref);
				String subType = entityManager.newReference(pureRef)
						.getSubType();
				/*
				 * If the reference is for an assignment
				 */
				if (AssignmentService.REF_TYPE_ASSIGNMENT.equals(subType)) {
					taggable = new AssignmentActivityImpl(assignmentService
							.getAssignment(pureRef), this, assignmentService,
							userDirectoryService);
				}
				/*
				 * If the reference is for a submission
				 */
				else if (AssignmentService.REF_TYPE_SUBMISSION.equals(subType)) {
					AssignmentSubmission submission = assignmentService
							.getSubmission(pureRef);
					taggable = new AssignmentItemImpl(submission,
							parseAuthor(ref), new AssignmentActivityImpl(
									submission.getAssignment(), this,
									assignmentService, userDirectoryService),
							userDirectoryService);
				}
			} catch (IdUnusedException iue) {
				logger.error(iue.getMessage(), iue);
			} catch (PermissionException pe) {
				logger.error(pe.getMessage(), pe);
			}
		}
		return taggable;
	}

	public TaggableActivity getActivity(Assignment assignment) {
		return new AssignmentActivityImpl(assignment, this, assignmentService,
				userDirectoryService);
	}

	public List<Taggable> getAll(String context, TaggingProvider provider) {
		/*
		 * We aren't picky about the provider, so ignore that argument.
		 */
		List<Taggable> taggables = new ArrayList<Taggable>();
		List<Assignment> assignments = assignmentService
				.getListAssignmentsForContext(context);
		for (Assignment assignment : assignments) {
			taggables.add(new AssignmentActivityImpl(assignment, this,
					assignmentService, userDirectoryService));
		}
		return taggables;
	}

	public String getContext(String ref) {
		return entityManager.newReference(ref).getContext();
	}

	public String getId() {
		return PRODUCER_ID;
	}

	public TaggableItem getItem(AssignmentSubmission assignmentSubmission,
			String userId) {
		return new AssignmentItemImpl(assignmentSubmission, userId,
				new AssignmentActivityImpl(
						assignmentSubmission.getAssignment(), this,
						assignmentService, userDirectoryService),
				userDirectoryService);
	}

	public String getName() {
		return rb.getString("service_name");
	}

	public void init() {
		logger.info("init()");

		taggingManager.registerProducer(this);
	}

	protected String parseAuthor(String itemRef) {
		return itemRef.split(AssignmentItemImpl.ITEM_REF_SEPARATOR)[1];
	}

	protected String parseSubmissionRef(String itemRef) {
		return itemRef.split(AssignmentItemImpl.ITEM_REF_SEPARATOR)[0];
	}

	public void setAssignmentService(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void setSecurityService(SecurityService securityService) {
		this.securityService = securityService;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setTaggingManager(TaggingManager taggingManager) {
		this.taggingManager = taggingManager;
	}

	public void setUserDirectoryService(
			UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}
}
