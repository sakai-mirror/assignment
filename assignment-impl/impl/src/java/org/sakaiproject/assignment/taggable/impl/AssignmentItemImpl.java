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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.assignment.api.AssignmentSubmission;
import org.sakaiproject.taggable.activity.api.TaggableActivity;
import org.sakaiproject.taggable.activity.api.TaggableItem;
import org.sakaiproject.taggable.api.TaggableProducer;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.util.ResourceLoader;

public class AssignmentItemImpl implements TaggableItem {

	private static final Log logger = LogFactory
			.getLog(AssignmentItemImpl.class);

	private static ResourceLoader rb = new ResourceLoader("assignment");

	protected static final String ITEM_REF_SEPARATOR = "@";

	protected AssignmentSubmission submission;

	protected String userId;

	protected TaggableActivity activity;

	protected UserDirectoryService userDirectoryService;

	public AssignmentItemImpl(AssignmentSubmission submission, String userId,
			TaggableActivity activity, UserDirectoryService userDirectoryService) {
		this.submission = submission;
		this.userId = userId;
		this.activity = activity;
		this.userDirectoryService = userDirectoryService;
	}

	public TaggableActivity getActivity() {
		return activity;
	}

	public String getContent() {
		return submission.getSubmittedText();
	}

	public String getContext() {
		return activity.getContext();
	}

	public String getDescription() {
		return activity.getDescription();
	}

	public Object getObject() {
		return submission;
	}

	public TaggableProducer getProducer() {
		return activity.getProducer();
	}

	public String getReference() {
		StringBuffer sb = new StringBuffer();
		sb.append(submission.getReference());
		sb.append(ITEM_REF_SEPARATOR);
		sb.append(userId);
		return sb.toString();
	}

	public String getTitle() {
		StringBuffer sb = new StringBuffer();
		try {
			User user = userDirectoryService.getUser(userId);
			sb.append(user.getFirstName());
			sb.append(' ');
			sb.append(user.getLastName());
			sb.append(' ');
			sb.append(rb.getString("submission"));
		} catch (UserNotDefinedException unde) {
			logger.error(unde.getMessage(), unde);
		}
		return sb.toString();
	}

	public String getUserId() {
		return userId;
	}
}
