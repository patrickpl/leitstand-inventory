/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.commons.messages.MessageFactory.createMessage;
import static io.leitstand.inventory.model.DefaultPackageService.packageVersionInfo;
import static io.leitstand.inventory.model.Element_Image.findInstalledImage;
import static io.leitstand.inventory.model.Element_Image.findInstalledImages;
import static io.leitstand.inventory.model.Image.findByElementAndImageTypeAndVersion;
import static io.leitstand.inventory.model.Image.findUpdates;
import static io.leitstand.inventory.service.ElementAvailableUpdate.newElementAvailableUpdate;
import static io.leitstand.inventory.service.ElementAvailableUpdate.UpdateType.MAJOR;
import static io.leitstand.inventory.service.ElementAvailableUpdate.UpdateType.MINOR;
import static io.leitstand.inventory.service.ElementAvailableUpdate.UpdateType.PATCH;
import static io.leitstand.inventory.service.ElementAvailableUpdate.UpdateType.PRERELEASE;
import static io.leitstand.inventory.service.ElementImageState.ACTIVE;
import static io.leitstand.inventory.service.ElementImageState.CACHED;
import static io.leitstand.inventory.service.ElementInstalledImage.newElementInstalledImage;
import static io.leitstand.inventory.service.ElementInstalledImageData.newElementInstalledImageData;
import static io.leitstand.inventory.service.ElementInstalledImageReference.newElementInstalledImageReference;
import static io.leitstand.inventory.service.ElementInstalledImages.newElementInstalleImages;
import static io.leitstand.inventory.service.ReasonCode.IVT0340W_ELEMENT_IMAGE_NOT_FOUND;
import static io.leitstand.inventory.service.ReasonCode.IVT0341E_ELEMENT_IMAGE_ACTIVE;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import io.leitstand.commons.ConflictException;
import io.leitstand.commons.UnprocessableEntityException;
import io.leitstand.commons.messages.Messages;
import io.leitstand.commons.model.Repository;
import io.leitstand.commons.tx.SubtransactionService;
import io.leitstand.inventory.service.ElementAvailableUpdate;
import io.leitstand.inventory.service.ElementAvailableUpdate.UpdateType;
import io.leitstand.inventory.service.ElementImageState;
import io.leitstand.inventory.service.ElementInstalledImage;
import io.leitstand.inventory.service.ElementInstalledImageData;
import io.leitstand.inventory.service.ElementInstalledImageReference;
import io.leitstand.inventory.service.ElementInstalledImages;
import io.leitstand.inventory.service.ImageId;
import io.leitstand.inventory.service.ImageName;
import io.leitstand.inventory.service.ImageType;
import io.leitstand.inventory.service.PackageVersionInfo;
import io.leitstand.inventory.service.Version;

@Dependent
public class ElementImageManager {

	private static final Logger LOG = Logger.getLogger(ElementImageManager.class.getName());
	
	private static Comparator<? super ElementInstalledImageReference> INSTALLED_ELEMENT_COMPARATOR = (a,b)->{
			int key = a.getImageType().compareTo(b.getImageType());
			if(key != 0) {
				return key;
			}
			key = a.getImageName().compareTo(b.getImageName());
			if(key != 0) {
				return key;
			}
			return a.getImageVersion().compareTo(b.getImageVersion());
		};
	
	private Repository repository;
	private Messages messages;
	private SubtransactionService inventory;
	
	@Inject
	protected ElementImageManager(@Inventory Repository repository, 
							      @Inventory SubtransactionService inventory,
							      Messages messages){
		this.repository = repository;
		this.inventory  = inventory;
		this.messages 	= messages;
	}
	
	
	protected ElementImageManager() {
		// CDI
	}


	public ElementInstalledImages getElementInstalledImages(Element element) {
		ElementGroup group = element.getGroup();
		List<ElementInstalledImageData> installed = new LinkedList<>();
		
		for(Element_Image elementImage : repository.execute(findInstalledImages(element))){
			Image image = elementImage.getImage();
			
			List<PackageVersionInfo> packages = new LinkedList<>();
			for(PackageVersion revision : image.getPackages()){
				packages.add(packageVersionInfo(revision));
			}
			
			List<ElementAvailableUpdate> updates = new LinkedList<>();
			for(Image update : repository.execute(findUpdates(image.getPlatform(),
															  image.getImageType(), 
															  image.getImageName(),
															  image.getElementRole(), 
															  image.getImageVersion(), 
															  element))){
				UpdateType type = updateType(image, update);
				
				updates.add(newElementAvailableUpdate()
						    .withImageId(update.getImageId())
							.withImageVersion(update.getImageVersion())
							.withBuildDate(update.getBuildDate())
							.withUpdateType(type)
							.build());
			}
			installed.add(newElementInstalledImageData()
						  .withOrganization(image.getOrganization())
						  .withImageId(image.getImageId())
						  .withImageType(image.getImageType())
						  .withImageState(image.getImageState())
						  .withImageName(image.getImageName())
						  .withElementRole(image.getElementRoleName())
						  .withElementImageState(elementImage.getInstallationState())
						  .withImageExtension(image.getImageExtension())
						  .withImageVersion(image.getImageVersion())
						  .withChecksums(image.getChecksums().stream().collect(Collectors.toMap(c -> c.getAlgorithm().name(), Checksum::getValue)))
						  .withInstallationDate(elementImage.getDeployDate())
						  .withBuildDate(image.getBuildDate())
						  .withPackages(packages)
						  .withAvailableUpdates(updates)
						  .build());
		}	
		
		 return newElementInstalleImages()
				.withGroupId(group.getGroupId())
				.withGroupName(group.getGroupName())
				.withGroupType(group.getGroupType())
				.withElementId(element.getElementId())
				.withElementName(element.getElementName())
				.withElementAlias(element.getElementAlias())
				.withInstalledImages(installed)
				.build(); 
		
	}

	private UpdateType updateType(Image image, Image update) {
		UpdateType type = PRERELEASE;
		if(update.getImageVersion().getMajorLevel() > image.getImageVersion().getMajorLevel()){
			type = MAJOR;
		}
		if(update.getImageVersion().getMinorLevel() > image.getImageVersion().getMinorLevel()){
			type = MINOR;
		}
		if(update.getImageVersion().getPatchLevel() > image.getImageVersion().getPatchLevel()){
			type = PATCH;
		}
		return type;
	}

	public ElementInstalledImage getElementInstalledImage(Element element, ImageId imageId) {
		ElementGroup group = element.getGroup();
		
		Element_Image elementImage = repository.execute(findInstalledImage(element,imageId));
		Image image = elementImage.getImage();
			
		List<PackageVersionInfo> packages = new LinkedList<>();
		for(PackageVersion revision : image.getPackages()){
			packages.add(packageVersionInfo(revision));
		}
		
		List<ElementAvailableUpdate> updates = new LinkedList<>();
		for(Image update : repository.execute(findUpdates(image.getPlatform(),
														  image.getImageType(), 
														  image.getImageName(),
														  image.getElementRole(),
														  image.getImageVersion(),
														  element))){
			UpdateType type = updateType(image, update);
			
			updates.add(newElementAvailableUpdate()
					 	.withImageId(update.getImageId())
					 	.withImageVersion(update.getImageVersion())
					 	.withBuildDate(update.getBuildDate())
						.withUpdateType(type)
						.build());
		}
		return newElementInstalledImage()
			   .withGroupId(group.getGroupId())
			   .withGroupName(group.getGroupName())
			   .withGroupType(group.getGroupType())
			   .withElementId(element.getElementId())
			   .withElementName(element.getElementName())
			   .withElementAlias(element.getElementAlias())
			   .withElementRole(element.getElementRoleName())
			   .withImage(newElementInstalledImageData()
					   	  .withImageId(image.getImageId())
					   	  .withOrganization(image.getOrganization())
				   		  .withImageType(image.getImageType())
				   		  .withImageState(image.getImageState())
				   		  .withImageName(image.getImageName())
				   		  .withElementRole(image.getElementRoleName())
				   		  .withImageExtension(image.getImageExtension())
						  .withElementImageState(elementImage.getInstallationState())
				   		  .withImageVersion(image.getImageVersion())
				   		  .withChecksums(image.getChecksums()
				   				  			  .stream()
				   				  			  .collect(toMap(c -> c.getAlgorithm().name(), 
				   				  					  		 Checksum::getValue)))
				   		  .withInstallationDate(elementImage.getDeployDate())
				   		  .withBuildDate(image.getBuildDate())
				   		  .withPackages(packages)
				   		  .withAvailableUpdates(updates))
			   .build();
			
	}
	
	public void storeInstalledImages(Element element, List<ElementInstalledImageReference> refs) {
		Map<ElementInstalledImageReference,Element_Image> images = new TreeMap<>(INSTALLED_ELEMENT_COMPARATOR);
		for(Element_Image image : repository.execute(findInstalledImages(element))){
			ElementInstalledImageReference installed = newElementInstalledImageReference()
													   .withImageType(image.getImageType())
													   .withImageName(image.getImageName())
													   .withImageVersion(image.getImageVersion())
													   .build();
			images.put(installed,image);
		}
		
		for(ElementInstalledImageReference installed : refs){
			Element_Image image = images.remove(installed);
			if(image != null) {
				image.setImageInstallationState(imageInstallationState(installed));
				continue;
			}
			Image artefact = repository.execute(findByElementAndImageTypeAndVersion(element, 
																					 installed.getImageType(), 
																					 installed.getImageName(),
																					 installed.getImageVersion()));
			if(artefact == null){
				LOG.warning(() -> format("%s: %s %s in group %s attempted to register image %s-%s-%s which is unknown to the inventory!",
										 IVT0340W_ELEMENT_IMAGE_NOT_FOUND.getReasonCode(),
										 element.getElementRoleName(),
										 element.getElementName(),
										 element.getGroup().getGroupName(),
										 installed.getImageType(),
										 installed.getImageName(),
										 installed.getImageVersion()));
				messages.add(createMessage(IVT0340W_ELEMENT_IMAGE_NOT_FOUND,
						  				   element.getElementName(),
						  				   format("%s-%s-%s",
						  						   installed.getImageType(), 
						  						   installed.getImageName(), 
						  						   installed.getImageVersion())));
				
				CreateImageStubRecordFlow flow = new CreateImageStubRecordFlow(element, installed);
				artefact = inventory.run(flow);
				if(artefact == null) {
					LOG.fine(() -> format("Attemt to create image stub record failed. Proceed ignoring the image registration attempt of element %s!",
										  element.getElementName()));
					continue; // With next entry.
				}
			}
			image = new Element_Image(element,artefact);
			image.setImageInstallationState(imageInstallationState(installed));
			repository.add(image);
		}
		
		for(Element_Image image : images.values()){
			repository.remove(image);
		}
		
	}

	private ElementImageState imageInstallationState(ElementInstalledImageReference installed) {
		return installed.isActive() ? ACTIVE : CACHED;
	}

	
	public void removeInstalledImage(Element element, 
									 ImageType type, 
									 ImageName name,
									 Version version) {
		Element_Image installed = repository.execute(findInstalledImage(element, 
																		type,
																		name,
																		version));
		if(installed == null) {
			return;
		}
		
		if(installed.isActive()) {
			throw new ConflictException(IVT0341E_ELEMENT_IMAGE_ACTIVE, 
										installed.getImageType(),
										installed.getImageVersion());
		}
		
		repository.remove(installed);
		
	}

	public void storeCachedImages(Element element, List<ElementInstalledImageReference> refs) {
		Map<ElementInstalledImageReference,Element_Image> images = new TreeMap<>(INSTALLED_ELEMENT_COMPARATOR);
		for(Element_Image image : repository.execute(findInstalledImages(element))){
			ElementInstalledImageReference installed = newElementInstalledImageReference()
					   								   .withImageType(image.getImageType())
					   								   .withImageName(image.getImageName())
					   								   .withImageVersion(image.getImageVersion())
					   								   .build();
			images.put(installed, image);
		}
		
		for(ElementInstalledImageReference installed : refs){
			if(installed.isActive()) {
				throw new UnprocessableEntityException(IVT0341E_ELEMENT_IMAGE_ACTIVE, 
													   installed.getImageType(),
													   installed.getImageName(),
													   installed.getImageVersion());
			}
		
			Image artefact = repository.execute(findByElementAndImageTypeAndVersion(element, 
																					 installed.getImageType(), 
																					 installed.getImageName(),
																					 installed.getImageVersion()));
			if(artefact == null){
				LOG.warning(() -> format("%s: %s %s in group %s attempted to register image %s-%s-%s which is unknown to the inventory!",
										 IVT0340W_ELEMENT_IMAGE_NOT_FOUND.getReasonCode(),
										 element.getElementRoleName(),
										 element.getElementName(),
										 element.getGroup().getGroupName(),
										 installed.getImageType(),
										 installed.getImageName(),
										 installed.getImageVersion()));
				messages.add(createMessage(IVT0340W_ELEMENT_IMAGE_NOT_FOUND,
		  				   				  element.getElementName(),
		  				   				  format("%s-%s-%s",
		  				   						  installed.getImageType(), 
		  				   						  installed.getImageName(), 
		  				   						  installed.getImageVersion())));
				continue; // With next entry.
			}
			
			Element_Image image = images.get(installed);
			if(image != null) {
				continue;
			}
					
			image = new Element_Image(element,artefact);
			image.setImageInstallationState(CACHED);
			repository.add(image);
		}
	}

	public void removeCachedImages(Element element, List<ElementInstalledImageReference> refs) {
		Map<ElementInstalledImageReference,Element_Image> images = new HashMap<>();
		for(Element_Image image : repository.execute(findInstalledImages(element))){
			ElementInstalledImageReference installed = newElementInstalledImageReference()
					   								   .withImageType(image.getImageType())
					   								   .withImageName(image.getImageName())
					   								   .withImageVersion(image.getImageVersion())
					   								   .withElementImageState(image.getInstallationState())
					   								   .build();
			images.put(installed,image);
		}
		
		for(ElementInstalledImageReference installed : refs){
			Element_Image image = images.remove(installed);
			if(image == null){
				continue; // Already removed
			}
			if(image.isActive()) {
				throw new ConflictException(IVT0341E_ELEMENT_IMAGE_ACTIVE, 
											installed.getImageType(),
											installed.getImageVersion());
			}
			repository.remove(image);
		}
	}

}
