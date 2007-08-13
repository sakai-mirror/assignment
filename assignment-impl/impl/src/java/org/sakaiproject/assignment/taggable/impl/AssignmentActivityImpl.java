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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.Assignment;
import org.sakaiproject.assignment.api.AssignmentService;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.taggable.activity.api.TaggableActivity;
import org.sakaiproject.taggable.activity.api.TaggableItem;
import org.sakaiproject.taggable.api.TaggableProducer;
import org.sakaiproject.taggable.api.TaggingProvider;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

public class AssignmentActivityImpl implements TaggableActivity {

	protected Assignment assignment;

	protected TaggableProducer producer;

	protected AssignmentService assignmentService;

	protected UserDirectoryService userDirectoryService;

	protected String currentUserId;

	private static final Log logger = LogFactory
			.getLog(AssignmentActivityImpl.class);

	public AssignmentActivityImpl(Assignment assignment,
			TaggableProducer producer, AssignmentService assignmentService,
			UserDirectoryService userDirectoryService) {
		this.assignment = assignment;
		this.producer = producer;
		this.assignmentService = assignmentService;
		this.userDirectoryService = userDirectoryService;
		this.currentUserId = this.userDirectoryService.getCurrentUser().getId();
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof TaggableActivity) {
			TaggableActivity activity = (TaggableActivity) object;
			return activity.getReference().equals(this.getReference());
		}
		return false;
	}

	public String getContext() {
		return assignment.getContext();
	}

	public String getDescription() {
		return assignment.getContent().getInstructions();
	}

	public Object getObject() {
		return assignment;
	}

	public TaggableProducer getProducer() {
		return producer;
	}

	public String getReference() {
		return assignment.getReference();
	}

	public String getTitle() {
		return assignment.getTitle();
	}

	public boolean allowGetItems(TaggingProvider provider) {
		/*
		 * We aren't picky about the provider, so ignore that argument. Only
		 * allow this if the user can grade submissions.
		 */
		return assignmentService.allowGradeSubmission(getReference());
	}

	public boolean allowGetItems(String userId, TaggingProvider provider) {
		/*
		 * We aren't picky about the provider, so ignore that argument. Only
		 * allow this if the user can grade submissions, or if the identified
		 * user is the current one.
		 */
		return (currentUserId.equals(userId) || assignmentService
				.allowGradeSubmission(getReference()));
	}

	public List<TaggableItem> getItems(TaggingProvider provider)
			throws PermissionException {
		/*
		 * We aren't picky about the provider, so ignore that argument.
		 */
		if (allowGetItems(provider)) {
			List<TaggableItem> items = new ArrayList<TaggableItem>();
			for (Iterator<AssignmentSubmission> i = assignmentService
					.getSubmissions(assignment).iterator(); i.hasNext();) {
				AssignmentSubmission submission = i.next();
				for (Object submitterId : submission.getSubmitterIds()) {
					items.add(new AssignmentItemImpl(submission,
							(String) submitterId, this, userDirectoryService));
				}
			}
			return items;
		}
		throw new PermissionException(currentUserId, this
				+ ".allowGetItems(provider)", getReference());
	}

	public List<TaggableItem> getItems(String userId, TaggingProvider provider)
			throws PermissionException {
		/*
		 * We aren't picky about the provider, so ignore that argument.
		 */
		if (allowGetItems(userId, provider)) {
			List<TaggableItem> items = new ArrayList<TaggableItem>();
			try {
				AssignmentSubmission submission = assignmentService
						.getSubmission(assignment.getReference(),
								userDirectoryService.getUser(userId));
				if (submission != null) {
					TaggableItem item = new AssignmentItemImpl(submission,
							userId, this, userDirectoryService);
					items.add(item);
				}
			} catch (IdUnusedException iue) {
				logger.error(iue.getMessage(), iue);
			} catch (UserNotDefinedException unde) {
				logger.error(unde.getMessage(), unde);
			}
		}
		throw new PermissionException(currentUserId, this
				+ ".allowGetItems(userId, provider)", getReference());
	}
}
