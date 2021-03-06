/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;

import static io.leitstand.inventory.service.MetricId.randomMetricId;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.AlertRule;
import io.leitstand.inventory.service.AlertRuleName;
import io.leitstand.inventory.service.MetricAlertRuleService;
import io.leitstand.inventory.service.MetricId;
import io.leitstand.inventory.service.MetricName;

@RunWith(MockitoJUnitRunner.class)
public class MetricAlertRuleResourceTest {
	
	private static final MetricId METRIC_ID = randomMetricId();
	private static final MetricName METRIC_NAME = MetricName.valueOf("metric");
	private static final AlertRuleName RULE_NAME = AlertRuleName.valueOf("rule");
	private static final AlertRule RULE = mock(AlertRule.class);
	
	@Mock
	private Messages messages;
	
	@Mock
	private MetricAlertRuleService service;
	
	@InjectMocks
	private MetricAlertRulesResource resource = new MetricAlertRulesResource();

	
	@Test
	public void send_created_response_when_adding_a_new_rule_for_metric_identified_by_id() {
		when(service.storeAlertRule(METRIC_ID, RULE)).thenReturn(true);
		
		Response response = resource.storeAlertRule(METRIC_ID, RULE);
		assertEquals(201,response.getStatus());
	}
	
	@Test
	public void send_created_response_when_adding_a_new_rule_for_metric_identified_by_name() {
		when(service.storeAlertRule(METRIC_NAME, RULE)).thenReturn(true);
		
		Response response = resource.storeAlertRule(METRIC_NAME, RULE);
		assertEquals(201,response.getStatus());
	}
	
	@Test
	public void send_ok_response_when_adding_a_new_rule_for_metric_identified_by_id() {
		Response response = resource.storeAlertRule(METRIC_ID, RULE);
		assertEquals(200,response.getStatus());
	}
	
	@Test
	public void send_ok_response_when_adding_a_new_rule_for_metric_identified_by_name() {
		Response response = resource.storeAlertRule(METRIC_NAME, RULE);
		assertEquals(200,response.getStatus());
	}
	
	@Test
	public void send_created_response_when_storing_a_new_rule_for_metric_identified_by_id() {
		when(service.storeAlertRule(METRIC_ID, RULE)).thenReturn(true);
		
		Response response = resource.storeAlertRule(METRIC_ID, RULE_NAME, RULE);
		assertEquals(201,response.getStatus());
	}
	
	@Test
	public void send_created_response_when_storing_a_new_rule_for_metric_identified_by_name() {
		when(service.storeAlertRule(METRIC_NAME, RULE)).thenReturn(true);
		
		Response response = resource.storeAlertRule(METRIC_NAME, RULE_NAME, RULE);
		assertEquals(201,response.getStatus());
	}
	
	@Test
	public void send_ok_response_when_storing_a_new_rule_for_metric_identified_by_id() {
		Response response = resource.storeAlertRule(METRIC_ID, RULE_NAME, RULE);
		assertEquals(200,response.getStatus());
	}
	
	@Test
	public void send_ok_response_when_storing_a_new_rule_for_metric_identified_by_name() {
		Response response = resource.storeAlertRule(METRIC_NAME, RULE_NAME, RULE);
		assertEquals(200,response.getStatus());
	}

}
