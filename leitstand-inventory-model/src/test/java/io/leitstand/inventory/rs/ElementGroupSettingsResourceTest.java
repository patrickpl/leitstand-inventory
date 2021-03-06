/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;

import static io.leitstand.commons.rs.ReasonCode.VAL0003E_IMMUTABLE_ATTRIBUTE;
import static io.leitstand.inventory.service.ElementGroupId.randomGroupId;
import static io.leitstand.testing.ut.LeitstandCoreMatchers.reason;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.ElementGroupId;
import io.leitstand.inventory.service.ElementGroupSettings;
import io.leitstand.inventory.service.ElementGroupSettingsService;

@RunWith(MockitoJUnitRunner.class)
public class ElementGroupSettingsResourceTest {
	
	private static final ElementGroupId GROUP_ID = randomGroupId();
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	@Mock
	private Messages messages;
	
	@Mock
	private ElementGroupSettingsService service;
	
	@InjectMocks
	private ElementGroupSettingsResource resource = new ElementGroupSettingsResource();
	
	private ElementGroupSettings group;
	
	@Before
	public void initTestEnvironment() {
		group =  mock(ElementGroupSettings.class);
	}
	
	@Test
	public void throws_UnprocessingEntityException_when_attempting_to_change_group_id() {
		exception.expect(UnprocessableEntityException.class);
		exception.expect(reason(VAL0003E_IMMUTABLE_ATTRIBUTE));
		
		resource.storeElementGroup(randomGroupId(), group);
	}
	
	@Test
	public void send_created_response_when_creating_a_new_group() {
		when(group.getGroupId()).thenReturn(GROUP_ID);
		when(service.storeElementGroupSettings(group)).thenReturn(true);
		
		assertEquals(201,resource.storeElementGroup(GROUP_ID,group).getStatus());
	}
	
	@Test
	public void send_success_response_when_update_an_existing_group() {
		when(group.getGroupId()).thenReturn(GROUP_ID);
		when(service.storeElementGroupSettings(group)).thenReturn(false);
		
		assertEquals(200,resource.storeElementGroup(GROUP_ID,group).getStatus());
	}
	
}
