/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;

import static io.leitstand.commons.UniqueKeyConstraintViolationException.key;
import static io.leitstand.commons.model.ObjectUtil.isDifferent;
import static io.leitstand.commons.model.Patterns.UUID_PATTERN;
import static io.leitstand.commons.rs.ReasonCode.VAL0003E_IMMUTABLE_ATTRIBUTE;
import static io.leitstand.inventory.service.ReasonCode.IVT0404E_ELEMENT_ROLE_NAME_ALREADY_IN_USE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;

import java.net.URI;
import java.util.List;

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

import io.leitstand.commons.UniqueKeyConstraintViolationException;
import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.ElementRoleId;
import io.leitstand.inventory.service.ElementRoleName;
import io.leitstand.inventory.service.ElementRoleService;
import io.leitstand.inventory.service.ElementRoleSettings;

@RequestScoped
@Path("/roles")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
public class ElementRoleResource {
	@Inject
	private ElementRoleService service;
	
	@Inject
	private Messages messages;
	
	@GET
	public List<ElementRoleSettings> getElementRoles(){
		return service.getElementRoles();
	}
	
	@GET
	@Path("/{role_name}")
	public ElementRoleSettings getElementRole(@Valid @PathParam("role_name") ElementRoleName roleName) {
		return service.getElementRole(roleName);
	}
	
	@GET
	@Path("/{role_id:"+UUID_PATTERN+"}")
	public ElementRoleSettings getElementRole(@Valid @PathParam("role_id") ElementRoleId roleId) {
		return service.getElementRole(roleId);
	}

	@POST
	@Path("")
	public Response storeRole(@Valid ElementRoleSettings role) {
		return storeRole(role.getRoleId(),role);
	}
	
	@PUT
	@Path("/{role_id:"+UUID_PATTERN+"}")
	public Response storeRole(@Valid @PathParam("role_id") ElementRoleId roleId, 
							  @Valid ElementRoleSettings role) {
		if(isDifferent(roleId, role.getRoleId())) {
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, 
												   "role_id", 
												   roleId, 
												   role.getRoleId());
		}
		try {
			if(service.storeElementRole(role)) {
				if(messages.isEmpty()) {
					return created(URI.create(role.getRoleId().toString())).build();
				}
				return created(URI.create(role.getRoleId().toString())).entity(messages).build();
			};
			if(messages.isEmpty()) {
				return noContent().build();
			}
			return ok(messages).build();
		} catch (PersistenceException e) {
			// Check whether role exist
			service.getElementRole(role.getRoleName());
			throw new UniqueKeyConstraintViolationException(IVT0404E_ELEMENT_ROLE_NAME_ALREADY_IN_USE, 
															key("role_name", role.getRoleName()));
		}
	}
	
	@DELETE
	@Path("/{role_id:"+UUID_PATTERN+"}")
	public Response removeElementRole(@Valid @PathParam("role_id") ElementRoleId roleId) {
		service.removeElementRole(roleId);
		if(messages.isEmpty()) {
			return noContent().build();
		}
		return ok(messages).build();
	}
	
	@DELETE
	@Path("/{role_name}")
	public Response removeElementRole(@Valid @PathParam("role_name") ElementRoleName roleName) {
		service.removeElementRole(roleName);
		if(messages.isEmpty()) {
			return noContent().build();
		}
		return ok(messages).build();
	}

	
}
