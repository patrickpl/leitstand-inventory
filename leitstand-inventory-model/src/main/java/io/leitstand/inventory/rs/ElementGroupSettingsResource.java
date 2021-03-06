/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;

import static io.leitstand.commons.UniqueKeyConstraintViolationException.key;
import static io.leitstand.commons.model.ObjectUtil.isDifferent;
import static io.leitstand.commons.model.Patterns.UUID_PATTERN;
import static io.leitstand.commons.rs.ReasonCode.VAL0003E_IMMUTABLE_ATTRIBUTE;
import static io.leitstand.commons.rs.Responses.created;
import static io.leitstand.commons.rs.Responses.success;
import static io.leitstand.inventory.service.ReasonCode.IVT0103E_GROUP_NAME_ALREADY_IN_USE;
import static io.leitstand.security.auth.Role.OPERATOR;
import static io.leitstand.security.auth.Role.SYSTEM;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.noContent;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.PersistenceException;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.leitstand.commons.EntityNotFoundException;
import io.leitstand.commons.UniqueKeyConstraintViolationException;
import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.ElementGroupId;
import io.leitstand.inventory.service.ElementGroupName;
import io.leitstand.inventory.service.ElementGroupSettings;
import io.leitstand.inventory.service.ElementGroupSettingsService;
import io.leitstand.inventory.service.ElementGroupType;

@RequestScoped
@Path("/{group_type}s")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ElementGroupSettingsResource {

	@Inject
	private ElementGroupSettingsService service;

	@Inject
	private Messages messages;

	@POST
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response storeElementGroup(ElementGroupSettings settings) {
		service.storeElementGroupSettings(settings);
		return created("/%s/%/settings",
					   settings.getGroupType(),
					   settings.getGroupId());
	}

	@PUT
	@Path("/{group_id:" + UUID_PATTERN + "}/settings")
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response storeElementGroup(@Valid @PathParam("group_id") ElementGroupId groupId, 
							 		  @Valid ElementGroupSettings settings) {
		if (isDifferent(groupId, settings.getGroupId())) {
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, 
												   "group_id", 
												   groupId, 
												   settings.getGroupId());
		}
		try { 
			if (service.storeElementGroupSettings(settings)) {
				return created(messages,
							   "/%s/%s/settings",
							   settings.getGroupType(),
							   settings.getGroupId());	
			}
			return success(messages);
		} catch (PersistenceException e) {
			throw resolveRootCause(e, settings.getGroupType(), settings.getGroupName());
		}
	}

	@GET
	@Path("/{group_id:" + UUID_PATTERN + "}/settings")
	public ElementGroupSettings getGroupSettings(@Valid @PathParam("group_id") ElementGroupId id) {
		return service.getGroupSettings(id);
	}

	@GET
	@Path("/{group_name}/settings")
	public ElementGroupSettings getGroupSettings(@Valid @PathParam("group_type") ElementGroupType groupType,
												 @Valid @PathParam("group_name") ElementGroupName groupName) {
		return service.getGroupSettings(groupType,
										groupName);
	}

	@DELETE
	@Path("/{id:" + UUID_PATTERN + "}")
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response removeElementConfig(@Valid @PathParam("id") ElementGroupId id) {
		service.remove(id);
		return noContent().build();
	}
	
	@DELETE
	@Path("/{group_name}")
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response removeElementConfig(@Valid @PathParam("group_type") ElementGroupType groupType,
										@Valid @PathParam("group_name") ElementGroupName groupName) {
		service.remove(groupType,
					   groupName);
		return success(messages);
	}
	
	private PersistenceException resolveRootCause(PersistenceException p,
										          ElementGroupType groupType, 
										          ElementGroupName groupName) {
		try { 
			// A unique key constraint violation occurred if an element with the given name exists.
			service.getGroupSettings(groupType,
									 groupName);
			throw new UniqueKeyConstraintViolationException(IVT0103E_GROUP_NAME_ALREADY_IN_USE,
															key("group_name", groupName) );
		} catch(EntityNotFoundException e) {
			// Element does not exist. Continue with original exception
			throw p;
		}
	}

}
