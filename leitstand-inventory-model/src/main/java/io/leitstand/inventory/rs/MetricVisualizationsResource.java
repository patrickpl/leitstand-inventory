/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;


import static io.leitstand.commons.model.ObjectUtil.isDifferent;
import static io.leitstand.commons.model.Patterns.UUID_PATTERN;
import static io.leitstand.commons.rs.ReasonCode.VAL0003E_IMMUTABLE_ATTRIBUTE;
import static io.leitstand.commons.rs.Responses.created;
import static io.leitstand.commons.rs.Responses.success;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.MetricId;
import io.leitstand.inventory.service.MetricName;
import io.leitstand.inventory.service.MetricVisualization;
import io.leitstand.inventory.service.MetricVisualizationService;
import io.leitstand.inventory.service.MetricVisualizations;
import io.leitstand.inventory.service.VisualizationConfig;
import io.leitstand.inventory.service.VisualizationConfigId;
import io.leitstand.inventory.service.VisualizationConfigName;

@RequestScoped
@Path("/metrics")
@Produces(APPLICATION_JSON)
public class MetricVisualizationsResource {
	
	private static final Logger LOG = Logger.getLogger(MetricVisualizationsResource.class.getName());

	@Inject
	private MetricVisualizationService service;
	
	@Inject
	private Messages messages;
	
	
	@GET
	@Path("/{metric:"+UUID_PATTERN+"}/visualizations")
	public MetricVisualizations getMetricVisualizations(@PathParam("metric") @Valid MetricId metricId) {
		return service.getMetricVisualizations(metricId);
	}
	
	@GET
	@Path("/{metric_name}/visualizations")
	public MetricVisualizations getMetricVisualizations(@Valid @PathParam("metric_name") MetricName metricName) {
		return service.getMetricVisualizations(metricName);
	}
	
	
	@GET
	@Path("/{metric:"+UUID_PATTERN+"}/visualizations/{visualization}")
	public MetricVisualization getMetricVisualizations(@PathParam("metric") @Valid MetricId metricId,
													   @PathParam("rule") @Valid VisualizationConfigName visualizationName) {
		return service.getMetricVisualization(metricId, 
											  visualizationName);
	}
	
	@GET
	@Path("/{metric_name}/visualizations/{visualization}")
	public MetricVisualization getMetricVisualizations(@PathParam("metric_name") @Valid MetricName metricName,
													   @PathParam("rule") @Valid VisualizationConfigName visualizationName) {
		return service.getMetricVisualization(metricName,
											  visualizationName);
	}
	
	@PUT
	@Path("/{metric:"+UUID_PATTERN+"}/visualizations/{visualization:"+UUID_PATTERN+"}")
	public Response storeMetricVisualization(@PathParam("metric") @Valid MetricId metricId, 
											 @PathParam("visualization") @Valid VisualizationConfigId configId,
											 VisualizationConfig visualization) {
		
		if(isDifferent(configId, visualization.getVisualizationId())) {
			LOG.fine(() -> format("%s: Visualization ID is immutable and must not be changed from %s to %s", 
								  VAL0003E_IMMUTABLE_ATTRIBUTE.getReasonCode(), 
								  configId,visualization.getVisualizationId()));
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, "visualization_id", configId,visualization.getVisualizationId());
		}
		
		boolean created = service.storeMetricVisualization(metricId, 
														   visualization);
		if(created) {
			return created(messages,configId);
		}
		return success(messages);
	}

	
	@PUT
	@Path("/{metric}/visualizations/{visualization:"+UUID_PATTERN+"}")
	public Response storeMetricVisualization(@PathParam("metric") @Valid MetricName metricName, 
											 @PathParam("visualization") @Valid VisualizationConfigId configId,
											 VisualizationConfig visualization) {
		
		if(isDifferent(configId, visualization.getVisualizationId())) {
			LOG.fine(() -> format("%s: Visualization ID is immutable and must not be changed from %s to %s", 
								  VAL0003E_IMMUTABLE_ATTRIBUTE.getReasonCode(), 
								  configId,visualization.getVisualizationId()));
			throw new UnprocessableEntityException(VAL0003E_IMMUTABLE_ATTRIBUTE, "visualization_id", configId,visualization.getVisualizationId());
		}
		
		boolean created = service.storeMetricVisualization(metricName, 
														   visualization);
		if(created) {
			return created(messages,configId);
		}
		return success(messages);
	}
	
	@DELETE
	@Path("/{metric:"+UUID_PATTERN+"}/visualizations/{visualization:"+UUID_PATTERN+"}")
	public Response removeMetricVisualization(@PathParam("metric") @Valid MetricId metricId, 
											  @PathParam("name") @Valid VisualizationConfigName visualizationName) {
		service.removeMetricVisualization(metricId, 
										  visualizationName);
		return success(messages);
	}
	
	@DELETE
	@Path("/{metric}/visualizations/{name}")
	public Response removeMetricVisualization(@PathParam("metric") @Valid MetricName metricName, 
											  @PathParam("name") @Valid VisualizationConfigName visualizationName) {
		service.removeMetricVisualization(metricName, 
										  visualizationName);
		return success(messages);
	}
	
}
