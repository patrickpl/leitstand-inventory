/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.inventory.event.ElementAddedEvent.newElementAddedEvent;
import static io.leitstand.inventory.event.ElementSettingsUpdatedEvent.newElementSettingsUpdatedEvent;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.leitstand.commons.EntityNotFoundException;
import io.leitstand.commons.model.Service;
import io.leitstand.inventory.event.ElementEvent;
import io.leitstand.inventory.event.ElementEvent.ElementEventBuilder;
import io.leitstand.inventory.service.ElementId;
import io.leitstand.inventory.service.ElementName;
import io.leitstand.inventory.service.ElementSettings;
import io.leitstand.inventory.service.ElementSettingsService;

@Service
public class DefaultElementSettingsService implements ElementSettingsService {
	//TODO Add messages and logging
	@Inject
	private ElementProvider elements;
	
	@Inject
	private ElementSettingsManager inventory;

	@Inject
	private Event<ElementEvent> sink;
	
	public DefaultElementSettingsService() {
		// EJB constructor
	}
	
	DefaultElementSettingsService(ElementSettingsManager inventory, 
								  ElementProvider elements,
								  Event<ElementEvent> sink) {
		this.inventory = inventory;
		this.elements = elements;
		this.sink = sink;
	}

	@Override
	public ElementSettings getElementSettings(ElementId id) {
		Element element = elements.fetchElement(id);
		return inventory.getElementSettings(element);
	}

	@Override
	public ElementSettings getElementSettings(ElementName name) {
		Element element = elements.fetchElement(name);
		return inventory.getElementSettings(element);
	}

	@Override
	public boolean storeElementSettings(ElementSettings settings) {
		try{
			Element element = elements.fetchElement(settings.getElementId());
			inventory.storeElementSettings(element, 
										   settings);
			fire(newElementSettingsUpdatedEvent(),
				 settings);
			return false;
		} catch(EntityNotFoundException e){
			inventory.createElement(settings);
			fire(newElementAddedEvent(),
				 settings);
			return true;
		}
	}
	
	
	private <E extends ElementEvent,B extends ElementEventBuilder<E,B>> void fire(B event, ElementSettings settings) {
		sink.fire(event.withGroupId(settings.getGroupId())
					   .withGroupName(settings.getGroupName())
					   .withGroupType(settings.getGroupType())
					   .withElementId(settings.getElementId())
					   .withElementName(settings.getElementName())
					   .withElementAlias(settings.getElementAlias())
					   .withElementRole(settings.getElementRole())
					   .build());
	}
	
}
