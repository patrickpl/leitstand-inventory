/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.commons.db.DatabaseService.prepare;
import static io.leitstand.commons.messages.MessageFactory.createMessage;
import static io.leitstand.commons.model.ObjectUtil.optional;
import static io.leitstand.commons.model.StringUtil.isNonEmptyString;
import static io.leitstand.inventory.event.ImageAddedEvent.newImageAddedEvent;
import static io.leitstand.inventory.event.ImageRemovedEvent.newImageRemovedEvent;
import static io.leitstand.inventory.event.ImageStateChangedEvent.newImageStateChangedEvent;
import static io.leitstand.inventory.event.ImageStoredEvent.newImageStoredEvent;
import static io.leitstand.inventory.jpa.ImageStateConverter.toImageState;
import static io.leitstand.inventory.model.Application.findAll;
import static io.leitstand.inventory.model.Checksum.newChecksum;
import static io.leitstand.inventory.model.DefaultPackageService.packageVersionInfo;
import static io.leitstand.inventory.model.ElementRole.findRoleByName;
import static io.leitstand.inventory.model.Image.countImageReferences;
import static io.leitstand.inventory.model.Image.findByElementAndImageTypeAndVersion;
import static io.leitstand.inventory.model.Image.findByImageId;
import static io.leitstand.inventory.model.Image.markAllSuperseded;
import static io.leitstand.inventory.model.Image.prerelease;
import static io.leitstand.inventory.model.Image.restoreCandidates;
import static io.leitstand.inventory.model.Platform.findByVendor;
import static io.leitstand.inventory.service.ElementName.elementName;
import static io.leitstand.inventory.service.ElementPlatformInfo.newPlatformInfo;
import static io.leitstand.inventory.service.ElementRoleInfo.newElementRoleInfo;
import static io.leitstand.inventory.service.ImageInfo.newImageInfo;
import static io.leitstand.inventory.service.ImageMetaData.newImageMetaData;
import static io.leitstand.inventory.service.ImageName.imageName;
import static io.leitstand.inventory.service.ImageReference.newImageReference;
import static io.leitstand.inventory.service.ImageState.RELEASE;
import static io.leitstand.inventory.service.ImageState.SUPERSEDED;
import static io.leitstand.inventory.service.ImageStatistics.newImageStatistics;
import static io.leitstand.inventory.service.PlatformId.randomPlatformId;
import static io.leitstand.inventory.service.ReasonCode.IVT0200E_IMAGE_NOT_FOUND;
import static io.leitstand.inventory.service.ReasonCode.IVT0201I_IMAGE_STATE_UPDATED;
import static io.leitstand.inventory.service.ReasonCode.IVT0202I_IMAGE_STORED;
import static io.leitstand.inventory.service.ReasonCode.IVT0203I_IMAGE_REMOVED;
import static io.leitstand.inventory.service.ReasonCode.IVT0204E_IMAGE_NOT_REMOVABLE;
import static io.leitstand.inventory.service.ReasonCode.IVT0400E_ELEMENT_ROLE_NOT_FOUND;
import static io.leitstand.inventory.service.RoleImage.newRoleImage;
import static io.leitstand.inventory.service.RoleImages.newRoleImages;
import static java.lang.String.format;
import static java.util.EnumSet.allOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import io.leitstand.commons.ConflictException;
import io.leitstand.commons.EntityNotFoundException;
import io.leitstand.commons.db.DatabaseService;
import io.leitstand.commons.messages.Messages;
import io.leitstand.commons.model.Repository;
import io.leitstand.commons.model.Service;
import io.leitstand.commons.tx.SubtransactionService;
import io.leitstand.inventory.event.ImageEvent;
import io.leitstand.inventory.event.ImageEvent.ImageEventBuilder;
import io.leitstand.inventory.jpa.ImageStateConverter;
import io.leitstand.inventory.jpa.PlaneConverter;
import io.leitstand.inventory.service.ApplicationName;
import io.leitstand.inventory.service.ElementGroupName;
import io.leitstand.inventory.service.ElementId;
import io.leitstand.inventory.service.ElementName;
import io.leitstand.inventory.service.ElementPlatformInfo;
import io.leitstand.inventory.service.ElementRoleInfo;
import io.leitstand.inventory.service.ElementRoleName;
import io.leitstand.inventory.service.ImageId;
import io.leitstand.inventory.service.ImageInfo;
import io.leitstand.inventory.service.ImageMetaData;
import io.leitstand.inventory.service.ImageName;
import io.leitstand.inventory.service.ImageReference;
import io.leitstand.inventory.service.ImageService;
import io.leitstand.inventory.service.ImageState;
import io.leitstand.inventory.service.ImageStatistics;
import io.leitstand.inventory.service.ImageType;
import io.leitstand.inventory.service.PackageVersionInfo;
import io.leitstand.inventory.service.RoleImage;
import io.leitstand.inventory.service.RoleImages;
import io.leitstand.inventory.service.Version;

@Service
public class DefaultImageService implements ImageService {
	
	private static final Logger LOG = Logger.getLogger(DefaultImageService.class.getName());
	
	@Inject
	private Messages messages;

	@Inject
	@Inventory
	private Repository repository;
	
	@Inject
	@Inventory
	private DatabaseService db;
	
	@Inject
	private PackageVersionService packages;
	
	@Inject
	private ElementProvider elements;
	
	@Inject
	@Inventory
	private SubtransactionService transaction;
	
	@Inject
	private Event<ImageEvent> sink;
	
	public DefaultImageService(){
		// EJB
	}
	
	DefaultImageService(SubtransactionService transaction, 
						PackageVersionService packages, 
						Repository repository,
						DatabaseService db,
						Messages messages,
						Event<ImageEvent> sink){
		this.transaction = transaction;
		this.packages = packages;
		this.repository = repository;
		this.db = db;
		this.messages =messages;
		this.sink = sink;
	}
	
	@Override
	public List<ImageReference> findImages(String filter, 
										   ElementRoleName role, 
										   ImageType type, 
										   ImageState state, 
										   Version version, 
										   int limit) {
		List<Object> arguments = new LinkedList<>();
		
		String sql = "SELECT d.uuid, d.tsbuild, d.state, d.type, d.name, d.major, d.minor, d.patch, d.prerelease, e.name, r.name, p.vendor, p.model "+
		             "FROM inventory.image d "+
				     "JOIN inventory.platform p "+
		             "ON d.platform_id = p.id "+
				     "JOIN inventory.elementrole r "+
		             "ON d.elementrole_id = r.id "+
				     "LEFT OUTER JOIN inventory.element e "+
		             "ON d.element_id = e.id "+
				     "WHERE (p.vendor ~ ? OR p.model ~ ? OR r.name ~ ? OR e.name ~ ? OR d.name ~ ?) ";
		
		// Add the same filter expression four times, for vendor, model, role, element and image name
		arguments.add(filter);
		arguments.add(filter);
		arguments.add(filter);
		arguments.add(filter);
		arguments.add(filter);
		
		if(role != null) {
			sql += "AND r.name=? ";
			arguments.add(role.toString());
		}
		
		if(type != null) {
			sql += "AND d.type=? ";
			arguments.add(type.name());
		}
		
		if(state != null) {
			sql += "AND d.state=? ";
			arguments.add(ImageStateConverter.toDbValue(state));
		}

		if(version != null) {
			sql += "AND d.major=? AND d.minor=? AND d.patch=? ";
			arguments.add(Integer.valueOf(version.getMajorLevel()));
			arguments.add(Integer.valueOf(version.getMinorLevel()));
			arguments.add(Integer.valueOf(version.getPatchLevel()));
			if(isNonEmptyString(version.getPreRelease())) {
				sql += "AND d.prerelease=?";
				arguments.add(version.getPreRelease());
			}
		}
		
		
		sql += "ORDER BY r.name, e.name, p.vendor, d.name, p.model";
		
		return db.executeQuery(prepare(sql,arguments),
							   rs -> newImageReference()
									 .withImageId(new ImageId(rs.getString(1)))
									 .withBuildDate(rs.getTimestamp(2))
								     .withImageState(toImageState(rs.getString(3)))
									 .withImageType(ImageType.imageType(rs.getString(4)))
									 .withImageName(imageName(rs.getString(5)))
									 .withImageVersion(new Version(rs.getInt(6),
											 					   rs.getInt(7),
											 					   rs.getInt(8),
											 					   prerelease(rs.getString(9))))
									 .withElementName(elementName(rs.getString(10)))
									 .withElementRole(ElementRoleName.valueOf(rs.getString(11)))
									 .withPlatform(newPlatformInfo()
												   .withVendorName(rs.getString(12))
												   .withModelName(rs.getString(13))
												   .build())
									 .build());
		
	}

	@Override
	public boolean storeImage(ImageInfo submission) {
		ElementRole elementRole = repository.execute(findRoleByName(submission.getElementRole()));
		if(elementRole == null){
			throw new EntityNotFoundException(IVT0400E_ELEMENT_ROLE_NOT_FOUND, 
											  submission.getElementRole());
		}
		Element element = null;
		ElementName elementName = submission.getElementName();
		if(elementName != null){
			element = elements.fetchElement(elementName);
		}
		
		List<PackageVersion> versions = new LinkedList<>();
		for(PackageVersionInfo info : submission.getPackages()){
			PackageVersion version = packages.getPackageVersion(info.getOrganization(), 
																info.getPackageName(), 
																info.getPackageVersion());
			if(version == null){
				version = packages.storePackageVersion(info);
			} 
			version.setBuildDate(info.getBuildDate());
			List<Checksum> checksums = info.getChecksums()
										   .entrySet()
										   .stream()
										   .map(c -> newChecksum()
												   	 .withAlgorithm(Checksum.Algorithm.valueOf(c.getKey()))
												   	 .withValue(c.getValue())
												   	 .build())
										   .collect(toList());
			version.setChecksums(checksums);
			versions.add(version);
		}
		
		Map<ApplicationName,Application> applications = repository.execute(findAll());
		List<Application> imageApplications = new LinkedList<>();
		for(ApplicationName name: submission.getApplications()) {
			Application app = applications.get(name);
			if(app != null) {
				imageApplications.add(app);
				continue;
			}
			app = new Application(name);
			repository.add(app);
			imageApplications.add(app);
		}
		
		ElementPlatformInfo vendor = submission.getPlatform();
		Platform platform = repository.execute(findByVendor(vendor));
		if(platform == null) {
			platform = transaction.run(repo -> repo.add(new Platform(randomPlatformId(),
																	 vendor.getVendorName(), 
																	 vendor.getModelName())),
									   repo -> repo.execute(findByVendor(vendor)));
		}
		
		Image image = repository.execute(findByImageId(submission.getImageId()));
		boolean created = false;
		if(image == null){
			image = new Image(submission.getImageId());
			repository.add(image);
			created = true;
		} 
		image.setPlatform(platform);
		image.setCategory(submission.getCategory());
		image.setImageState(submission.getImageState());
		image.setImageType(submission.getImageType());
		image.setImageName(submission.getImageName());
		image.setElementRole(elementRole);
		image.setExtension(submission.getExtension());
		image.setImageVersion(submission.getImageVersion());
		image.setBuildDate(submission.getBuildDate());
		image.setBuildId(submission.getBuildId());
		image.setPackages(versions);
		image.setApplications(imageApplications);
		image.setChecksums(submission.getChecksums()
				   			   		 .entrySet()
				   			   		 .stream()
				   			   		 .map(c -> newChecksum()
				   			   				   .withAlgorithm(Checksum.Algorithm.valueOf(c.getKey()))
				   			   				   .withValue(c.getValue())
				   			   				   .build())
				   			   		 .collect(toList()));
		image.setOrganization(submission.getOrganization());			
		image.setElement(element);

		messages.add(createMessage(IVT0202I_IMAGE_STORED, 
				   				   image.getQualifiedName()));		
		if (created) {
			fire(newImageAddedEvent(),
				 submission);
			return true;
		}
		fire(newImageStoredEvent(),
			 submission);
		return false;
	}


	private <E extends ImageEvent, B extends ImageEventBuilder<E,B>> void fire(B event, ImageInfo image) {
		sink.fire(event
				  .withImageId(image.getImageId())
				  .withOrganization(image.getOrganization())
				  .withImageType(image.getImageType())
				  .withElementRole(image.getElementRole())
				  .withImageName(image.getImageName())
				  .withImageVersion(image.getImageVersion())
				  .withImageExtension(image.getExtension())
				  .withImageState(image.getImageState())
				  .withChecksums(image.getChecksums())
				  .build());		
	}

	@Override
	public ImageInfo getImage(ImageId id) {
		Image image = repository.execute(findByImageId(id));
		if(image == null){
			throw new EntityNotFoundException(IVT0200E_IMAGE_NOT_FOUND,id);
		}
		return imageInfo(image);
		
	}

	public static ImageInfo imageInfo(Image image) {
		List<PackageVersionInfo> pkgVersions = new LinkedList<>();
		for(PackageVersion p : image.getPackages()){
			pkgVersions.add(packageVersionInfo(p));
		}
		
		List<ApplicationName> applications = image.getApplications()
												  .stream()
												  .map(Application::getName)
												  .collect(toList());
		
		ElementPlatformInfo vendor = null;
		Platform platform = image.getPlatform();
		if(platform != null) {
			vendor = newPlatformInfo()
					 .withVendorName(platform.getVendor())
					 .withModelName(platform.getModel())
					 .build();
		}

		
		return newImageInfo()
			   .withImageId(image.getImageId())
		   	   .withImageType(image.getImageType())
		   	   .withImageName(image.getImageName())
		   	   .withImageState(image.getImageState())
		   	   .withElementRole(image.getElementRoleName())
		   	   .withPlatform(vendor)
		   	   .withElementName(optional(image.getElement(), Element::getElementName))
		   	   .withExtension(image.getImageExtension())
		   	   .withImageVersion(image.getImageVersion())
		   	   .withBuildDate(image.getBuildDate())
		   	   .withBuildId(image.getBuildId())
		   	   .withPackages(pkgVersions)
		   	   .withApplications(applications)
		   	   .withOrganization(image.getOrganization())
		   	   .withCategory(image.getCategory())
		   	   .withChecksums(image.getChecksums()
		   				 		   .stream()
		   				 		   .collect(toMap(c -> c.getAlgorithm().name(), 
		   				 				   		  Checksum::getValue)))
			   .build();
		
	}

	@Override
	public ImageInfo removeImage(ImageId id) {
		Image image = repository.execute(findByImageId(id));
		if(image == null){
			return null;
		}
		long count = repository.execute(countImageReferences(image));
		if(count > 0) {
			LOG.fine(()->format("%s: Cannot remove %s image %s %s %s %s (%s) because it is referenced by %d elements.",
								IVT0204E_IMAGE_NOT_REMOVABLE.getReasonCode(),
								image.getImageState(),
								image.getElementRoleName(),
								image.getImageName(),
								image.getImageType(),
								image.getImageVersion(),
								image.getImageId(),
								count));
			throw new ConflictException(IVT0204E_IMAGE_NOT_REMOVABLE, 
										image.getImageId(), 
										image.getElementRoleName(), 
										image.getImageName(), 
										image.getImageType(), 
										image.getImageVersion(), 
										image.getImageState());
		}
		ImageInfo info = imageInfo(image);
		repository.remove(image);
		messages.add(createMessage(IVT0203I_IMAGE_REMOVED,
								   image.getQualifiedName()));
		fire(newImageRemovedEvent(),
			 info);
		return info;
	}

	@Override
	public void updateImageState(ImageId id, ImageState state) {
		Image image = repository.execute(findByImageId(id));
		if(image.getImageState() == state) {
			return;
		}
		if(state == SUPERSEDED) {
			throw new IllegalArgumentException("Images must not be set to superseded manually.");
		}
		if(state == RELEASE) {
			repository.execute(markAllSuperseded(image));
			repository.execute(restoreCandidates(image));
		}
		ImageState prev = image.getImageState();
		image.setImageState(state);
		sink.fire(newImageStateChangedEvent()
				  .withImageId(image.getImageId())
				  .withOrganization(image.getOrganization())
				  .withImageType(image.getImageType())
				  .withElementRole(image.getElementRole().getRoleName())
				  .withImageName(image.getImageName())
				  .withImageVersion(image.getImageVersion())
				  .withImageExtension(image.getImageExtension())
				  .withImageState(image.getImageState())
				  .withPreviousState(prev)
				  .withChecksums(image.getChecksums()
						  			  .stream()
						  			  .collect(toMap(c -> c.getAlgorithm().name(), 
						  					  		 Checksum::getValue)))
				  .build());			
		messages.add(createMessage(IVT0201I_IMAGE_STATE_UPDATED, 
								   image.getQualifiedName(),
								   state));
		
	}

	@Override
	public ImageInfo getImage(ImageType imageType, 
							  ImageName imageName, 
							  Version version, 
							  ElementId elementId) {
		Element element = elements.fetchElement(elementId);
		Image image = repository.execute(findByElementAndImageTypeAndVersion(element, 
																			  imageType,
																			  imageName,
																			  version));
		return imageInfo(image);
	}

	@Override
	public ImageInfo getImage(ImageType imageType, 
							  ImageName imageName, 
							  Version version, 
							  ElementName name) {
		Element element = elements.fetchElement(name);
		Image image = repository.execute(findByElementAndImageTypeAndVersion(element, 
																			  imageType, 
																			  imageName,
																			  version));
		return imageInfo(image);
	}

	@Override
	public RoleImages findRoleImages(ElementRoleName role) {
		
		List<RoleImage> images = db.executeQuery(prepare("SELECT DISTINCT i.name, i.type "+
														 "FROM inventory.image i "+
														 "JOIN inventory.elementrole r "+
														 "ON i.elementrole_id = r.id "+
														 "WHERE r.name=? "+
														 "ORDER BY i.name, i.type", role.toString()), 
												 rs -> newRoleImage()
												 	   .withImageType(ImageType.valueOf(rs.getString(2)))
												 	   .withImageName(ImageName.valueOf(rs.getString(1)))
												 	   .build());
		
		return newRoleImages()
			   .withElementRole(role)
			   .withImages(images)
			   .build();
	}

	@Override
	public ImageStatistics getImageStatistics(ImageId imageId) {
		
		ImageInfo image = getImage(imageId);
		
		
		
		Map<ElementGroupName,Integer> activeCount = new HashMap<>();
		db.processQuery(prepare("SELECT eg.name, count(*) "+
							    "FROM inventory.image i "+
							    "JOIN inventory.element_image ei "+
							    "ON ei.image_id = i.id "+
							    "JOIN inventory.element e "+
							    "ON e.id = ei.element_id "+
							    "JOIN inventory.elementgroup eg "+
							    "ON eg.id = e.elementgroup_id "+
							    "WHERE i.uuid=? "+
							    "AND ei.state='ACTIVE' "+
							    "GROUP BY eg.name", 
							    image.getImageId()),
						rs -> activeCount.put(ElementGroupName.valueOf(rs.getString(1)),
									 	 	  rs.getInt(2)));
		
		Map<ElementGroupName,Integer> cachedCount = new HashMap<>();
		db.processQuery(prepare("SELECT eg.name, count(*) "+
							    "FROM inventory.image i "+
							    "JOIN inventory.element_image ei "+
							    "ON ei.image_id = i.id "+
							    "JOIN inventory.element e "+
							    "ON e.id = ei.element_id "+
							    "JOIN inventory.elementgroup eg "+
							    "ON eg.id = e.elementgroup_id "+
							    "WHERE i.uuid=? "+
							    "AND ei.state='CACHED' "+
							    "GROUP BY eg.name", 
							    image.getImageId()),
						rs -> cachedCount.put(ElementGroupName.valueOf(rs.getString(1)),
									 	 	  rs.getInt(2)));

		return newImageStatistics()
			   .withImage(image)
			   .withActiveCount(activeCount)
			   .withCachedCount(cachedCount)
			   .build();
	}
	
	@Override
	public ImageMetaData getImageMetaData() {
		List<ImageType> types = new ArrayList<>(allOf(ImageType.class));
		List<ElementRoleInfo> roles = db.executeQuery(prepare("SELECT name,plane FROM inventory.elementrole ORDER BY name,plane"), 
													  rs -> newElementRoleInfo()
															.withElementRole(ElementRoleName.valueOf(rs.getString(1)))
						  									.withPlane(PlaneConverter.parse(rs.getString(2)))
															.build());
		List<ElementPlatformInfo> platforms = db.executeQuery(prepare("SELECT vendor,model FROM inventory.platform ORDER BY vendor,model"), 
															  rs -> newPlatformInfo()
																    .withVendorName(rs.getString(1))
																    .withModelName(rs.getString(2))
																	.build());
		
		return newImageMetaData()
			   .withPlatforms(platforms)
			   .withElementRoles(roles)
			   .withImageTypes(types)
			   .build();
	}

}
