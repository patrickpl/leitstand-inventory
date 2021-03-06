/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.rs;

import static io.leitstand.inventory.service.ElementId.randomElementId;
import static io.leitstand.inventory.service.ElementName.elementName;
import static io.leitstand.inventory.service.ModuleData.newModuleData;
import static io.leitstand.inventory.service.ModuleName.moduleName;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.leitstand.commons.messages.Messages;
import io.leitstand.inventory.service.ElementId;
import io.leitstand.inventory.service.ElementModuleService;
import io.leitstand.inventory.service.ElementName;
import io.leitstand.inventory.service.ModuleData;
import io.leitstand.inventory.service.ModuleName;

@RunWith(MockitoJUnitRunner.class)
public class ElementModuleResourceTest {
	
	private static final ElementId ELEMENT_ID = randomElementId();
	private static final ElementName ELEMENT_NAME = elementName("element");
	private static final ModuleName MODULE_NAME = moduleName("module");
	private static final ModuleData MODULE_DATA = newModuleData()
												  .withModuleName(MODULE_NAME)
												  .build();

	@Mock
	private ElementModuleService service;
	
	@Mock
	private Messages messages;
	
	@InjectMocks
	private ElementModulesResource resource = new ElementModulesResource();
	
	@Test
	public void send_created_response_when_new_module_was_added_for_element_identified_by_id() {
		when(service.storeElementModule(ELEMENT_ID, MODULE_NAME, MODULE_DATA)).thenReturn(true);
		
		Response response = resource.storeElementModule(ELEMENT_ID,
													 	MODULE_NAME, 
													 	MODULE_DATA);
		assertEquals(201,response.getStatus());
	}
	
	@Test
	public void send_created_response_when_new_module_was_added_for_element_identified_by_name() {
		when(service.storeElementModule(ELEMENT_NAME, MODULE_NAME, MODULE_DATA)).thenReturn(true);
		
		Response response = resource.storeElementModule(ELEMENT_NAME,
													 	MODULE_NAME, 
													 	MODULE_DATA);
		assertEquals(201,response.getStatus());
	}	
	
	@Test
	public void send_ok_response_when_new_module_was_added_for_element_identified_by_id() {
		when(service.storeElementModule(ELEMENT_ID, MODULE_NAME, MODULE_DATA)).thenReturn(false);
		
		Response response = resource.storeElementModule(ELEMENT_ID,
													 	MODULE_NAME, 
													 	MODULE_DATA);
		assertEquals(200,response.getStatus());
	}
	
	@Test
	public void send_ok_response_when_new_module_was_added_for_element_identified_by_name() {
		when(service.storeElementModule(ELEMENT_NAME, MODULE_NAME, MODULE_DATA)).thenReturn(false);
		
		Response response = resource.storeElementModule(ELEMENT_NAME,
													 	MODULE_NAME, 
													 	MODULE_DATA);
		assertEquals(200,response.getStatus());
	}	
}
