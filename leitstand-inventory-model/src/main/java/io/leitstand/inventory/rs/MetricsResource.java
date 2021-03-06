/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;


import static io.leitstand.commons.model.ObjectUtil.isDifferent;
import static io.leitstand.commons.model.Patterns.UUID_PATTERN;
import static io.leitstand.commons.rs.ReasonCode.VAL0003E_IMMUTABLE_ATTRIBUTE;
import static io.leitstand.commons.rs.Responses.created;
import static io.leitstand.commons.rs.Responses.success;
import static io.leitstand.security.auth.Role.OPERATOR;
import static io.leitstand.security.auth.Role.SYSTEM;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.MetricId;
import io.leitstand.inventory.service.MetricName;
import io.leitstand.inventory.service.MetricScope;
import io.leitstand.inventory.service.MetricSettings;
import io.leitstand.inventory.service.MetricSettingsService;

@RequestScoped
@Path("/metrics")
@Produces(APPLICATION_JSON)
public class MetricsResource {

	@Inject
	private MetricSettingsService service;
	
	@Inject
	private Messages messages;
	
	@GET
	public List<MetricSettings> findMetrics(@QueryParam("filter") String filter,
											@QueryParam("metric_scope") MetricScope scope){
		return service.findMetrics(filter,scope);
	}
	
	@GET
	@Path("/{metric:"+UUID_PATTERN+"}")
	public MetricSettings getMetricSettings(@PathParam("metric") @Valid MetricId metricId) {
		return service.getMetricSettings(metricId);
	}
	
	@GET
	@Path("/{metric_name}")
	public MetricSettings getMetricSettings(@Valid @PathParam("metric_name") MetricName metricName) {
		return service.getMetricSettings(metricName);
	}
		
	@PUT
	@Path("/{metric:"+UUID_PATTERN+"}")
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response storeMetricSettings(@Valid @PathParam("metric") MetricId metricId, 
										@Valid MetricSettings settings) {
		if(isDifferent(metricId, settings.getMetricId())) {
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, 
												   "metric_id",
												   metricId, 
												   settings.getMetricId());
		}
		
		boolean created = service.storeMetricSettings(settings);
		if(created) {
			return created(messages,settings.getMetricId());
		}
		return success(messages);
	}
	
	@POST
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response storeMetricSettings(@Valid MetricSettings settings) {
		return storeMetricSettings(settings.getMetricId(),settings);
	}
	

	@DELETE
	@Path("/{metric}")
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response removeMetric(@PathParam("metric") @Valid MetricName metricName,
								 @QueryParam("force") boolean force) {
		if(force) {
			service.forceRemoveMetric(metricName);
		} else {
			service.removeMetric(metricName);
		}
		return success(messages);
	}
	
	@DELETE
	@Path("/{metric:"+UUID_PATTERN+"}")
	@RolesAllowed({OPERATOR,SYSTEM})
	public Response removeMetric(@PathParam("metric") @Valid MetricId metricId,
								 @QueryParam("force") boolean force) {
		if(force) {
			service.forceRemoveMetric(metricId);
		} else {
			service.removeMetric(metricId);
		}
		return success(messages);
	}
	
}
