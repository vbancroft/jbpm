/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.jbpm.services.task.wih.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.PeopleAssignmentsImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.PeopleAssignments;
import org.kie.api.task.model.Task;
import org.kie.internal.task.api.model.InternalPeopleAssignments;
import org.kie.internal.task.api.model.InternalTask;
import org.kie.internal.task.api.model.InternalTaskData;


/**
 * A class responsible for assigning the various ownerships (actors, groups, business 
 * administrators, and task stakeholders) from a <code>WorkItem</code> to a <code>Task</code>. 
 * This class consolidates common code for reuse across multiple <code>WorkItemHandler</code>s.
 *
 */
public class PeopleAssignmentHelper {

	public static final String ACTOR_ID = "ActorId";
	public static final String GROUP_ID = "GroupId";
	public static final String BUSINESSADMINISTRATOR_ID = "BusinessAdministratorId";
	public static final String TASKSTAKEHOLDER_ID = "TaskStakeholderId";
    public static final String EXCLUDED_OWNER_ID = "ExcludedOwnerId";
    public static final String RECIPIENT_ID = "RecipientId";
    
    private String separator;
    
    public PeopleAssignmentHelper() {
        this.separator = System.getProperty("org.jbpm.ht.user.separator", ",");
    }
	
    public PeopleAssignmentHelper(String separator) {
        this.separator = separator;
    }
    
	public void handlePeopleAssignments(WorkItem workItem, InternalTask task, InternalTaskData taskData) {
		
		InternalPeopleAssignments peopleAssignments = getNullSafePeopleAssignments(task);
        
		assignActors(workItem, peopleAssignments, taskData);
		assignGroups(workItem, peopleAssignments);		
		assignBusinessAdministrators(workItem, peopleAssignments);
		assignTaskStakeholders(workItem, peopleAssignments);
        assignExcludedOwners(workItem, peopleAssignments);
        assignRecipients(workItem, peopleAssignments);
		
		task.setPeopleAssignments(peopleAssignments);
        
	}
	
	protected void assignActors(WorkItem workItem, PeopleAssignments peopleAssignments, InternalTaskData taskData) {
		
        String actorIds = (String) workItem.getParameter(ACTOR_ID);        
        List<OrganizationalEntity> potentialOwners = peopleAssignments.getPotentialOwners();
        
        processPeopleAssignments(actorIds, potentialOwners, true);

        // Set the first user as creator ID??? hmmm might be wrong
        if (potentialOwners.size() > 0 && taskData.getCreatedBy() == null) {
        	
        	OrganizationalEntity firstPotentialOwner = potentialOwners.get(0);
        	taskData.setCreatedBy((UserImpl) firstPotentialOwner);

        }
        
	}
	
	protected void assignGroups(WorkItem workItem, PeopleAssignments peopleAssignments) {
	
        String groupIds = (String) workItem.getParameter(GROUP_ID);
        List<OrganizationalEntity> potentialOwners = peopleAssignments.getPotentialOwners();
        
        processPeopleAssignments(groupIds, potentialOwners, false);
        
	}

	protected void assignBusinessAdministrators(WorkItem workItem, PeopleAssignments peopleAssignments) {
		
		String businessAdministratorIds = (String) workItem.getParameter(BUSINESSADMINISTRATOR_ID);
        List<OrganizationalEntity> businessAdministrators = peopleAssignments.getBusinessAdministrators();
        if (!hasAdminAssigned(businessAdministrators)) {
            UserImpl administrator = new UserImpl("Administrator");        
            businessAdministrators.add(administrator);
            GroupImpl adminGroup = new GroupImpl("Administrators");        
            businessAdministrators.add(adminGroup);
        }
        processPeopleAssignments(businessAdministratorIds, businessAdministrators, true);
        
	}
	
	protected void assignTaskStakeholders(WorkItem workItem, InternalPeopleAssignments peopleAssignments) {
		
		String taskStakehodlerIds = (String) workItem.getParameter(TASKSTAKEHOLDER_ID);
		List<OrganizationalEntity> taskStakeholders = peopleAssignments.getTaskStakeholders();

		processPeopleAssignments(taskStakehodlerIds, taskStakeholders, true);
		
	}

    protected void assignExcludedOwners(WorkItem workItem, InternalPeopleAssignments peopleAssignments) {

        String excludedOwnerIds = (String) workItem.getParameter(EXCLUDED_OWNER_ID);
        List<OrganizationalEntity> excludedOwners = peopleAssignments.getExcludedOwners();

        processPeopleAssignments(excludedOwnerIds, excludedOwners, true);

    }

    protected void assignRecipients(WorkItem workItem, InternalPeopleAssignments peopleAssignments) {

        String recipientIds = (String) workItem.getParameter(RECIPIENT_ID);
        List<OrganizationalEntity> recipients = peopleAssignments.getRecipients();

        processPeopleAssignments(recipientIds, recipients, true);

    }

	protected void processPeopleAssignments(String peopleAssignmentIds, List<OrganizationalEntity> organizationalEntities, boolean user) {

        if (peopleAssignmentIds != null && peopleAssignmentIds.trim().length() > 0) {

            String[] ids = peopleAssignmentIds.split(separator);
            for (String id : ids) {
                id = id.trim();
                boolean exists = false;
                for (OrganizationalEntity orgEntity : organizationalEntities) {
                    if (orgEntity.getId().equals(id)) {
                        exists = true;
                    }
                }

                if (!exists) {
                    OrganizationalEntity organizationalEntity = null;
                    if (user) {
                        organizationalEntity = new UserImpl(id);
                    } else {
                        organizationalEntity = new GroupImpl(id);
                    }
                    organizationalEntities.add(organizationalEntity);

                }
            }
        }
	}
	
	protected InternalPeopleAssignments getNullSafePeopleAssignments(Task task) {
		
		InternalPeopleAssignments peopleAssignments = (InternalPeopleAssignments) task.getPeopleAssignments();
        
        if (peopleAssignments == null) {
        	
        	peopleAssignments = new PeopleAssignmentsImpl();
        	peopleAssignments.setPotentialOwners(new ArrayList<OrganizationalEntity>());
        	peopleAssignments.setBusinessAdministrators(new ArrayList<OrganizationalEntity>());
        	peopleAssignments.setExcludedOwners(new ArrayList<OrganizationalEntity>());
        	peopleAssignments.setRecipients(new ArrayList<OrganizationalEntity>());
        	peopleAssignments.setTaskStakeholders(new ArrayList<OrganizationalEntity>());

        }
        
		return peopleAssignments;
		
	}
	
	protected boolean hasAdminAssigned(Collection<OrganizationalEntity> businessAdmins) {
	    for (OrganizationalEntity entity : businessAdmins) {
	        if ("Administrator".equals(entity.getId()) || "Administrators".equals(entity.getId())) {
	            return true;
	        }
	    }
	    return false;
	}
	
}
