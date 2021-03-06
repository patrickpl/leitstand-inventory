/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.inventory.model.Element.findElementByName;
import static io.leitstand.inventory.model.ElementGroup.findElementGroupByName;
import static io.leitstand.inventory.model.ElementRole.findRoleByName;
import static io.leitstand.inventory.service.ElementGroupId.randomGroupId;
import static io.leitstand.inventory.service.ElementGroupName.groupName;
import static io.leitstand.inventory.service.ElementGroupType.groupType;
import static io.leitstand.inventory.service.ElementId.randomElementId;
import static io.leitstand.inventory.service.ElementImageState.ACTIVE;
import static io.leitstand.inventory.service.ElementImageState.CACHED;
import static io.leitstand.inventory.service.ElementName.elementName;
import static io.leitstand.inventory.service.ElementPlatformInfo.newPlatformInfo;
import static io.leitstand.inventory.service.ElementRoleName.elementRoleName;
import static io.leitstand.inventory.service.ImageId.randomImageId;
import static io.leitstand.inventory.service.ImageInfo.newImageInfo;
import static io.leitstand.inventory.service.ImageState.CANDIDATE;
import static io.leitstand.inventory.service.ImageState.NEW;
import static io.leitstand.inventory.service.ImageType.LXC;
import static io.leitstand.inventory.service.PackageVersionInfo.newPackageVersionInfo;
import static io.leitstand.inventory.service.Plane.DATA;
import static io.leitstand.inventory.service.ReasonCode.IVT0200E_IMAGE_NOT_FOUND;
import static io.leitstand.inventory.service.ReasonCode.IVT0201I_IMAGE_STATE_UPDATED;
import static io.leitstand.inventory.service.ReasonCode.IVT0202I_IMAGE_STORED;
import static io.leitstand.inventory.service.ReasonCode.IVT0203I_IMAGE_REMOVED;
import static io.leitstand.inventory.service.ReasonCode.IVT0204E_IMAGE_NOT_REMOVABLE;
import static io.leitstand.inventory.service.ReasonCode.IVT0400E_ELEMENT_ROLE_NOT_FOUND;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.event.Event;
import javax.inject.Provider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.leitstand.commons.ConflictException;
import io.leitstand.commons.EntityNotFoundException;
import io.leitstand.commons.messages.Message;
import io.leitstand.commons.messages.Messages;
import io.leitstand.commons.model.Repository;
import io.leitstand.commons.tx.SubtransactionService;
import io.leitstand.inventory.event.ImageAddedEvent;
import io.leitstand.inventory.event.ImageEvent;
import io.leitstand.inventory.event.ImageRemovedEvent;
import io.leitstand.inventory.event.ImageStateChangedEvent;
import io.leitstand.inventory.event.ImageStoredEvent;
import io.leitstand.inventory.service.ApplicationName;
import io.leitstand.inventory.service.ElementGroupName;
import io.leitstand.inventory.service.ElementGroupType;
import io.leitstand.inventory.service.ElementName;
import io.leitstand.inventory.service.ElementRoleName;
import io.leitstand.inventory.service.ImageId;
import io.leitstand.inventory.service.ImageInfo;
import io.leitstand.inventory.service.ImageName;
import io.leitstand.inventory.service.ImageService;
import io.leitstand.inventory.service.ImageStatistics;
import io.leitstand.inventory.service.ImageType;
import io.leitstand.inventory.service.PlatformId;
import io.leitstand.inventory.service.Version;

public class ImageServiceIT extends InventoryIT{

	
	private static final ElementName ELEMENT_NAME = elementName("image-element");
	private static final ElementGroupName GROUP_NAME = groupName("image_test");
	private static final ElementGroupType GROUP_TYPE = groupType("unittest");
	private static final ElementRoleName ELEMENT_ROLE = elementRoleName("image-test");
	private ImageService service;
	private ArgumentCaptor<Message> messageCaptor;
	private ArgumentCaptor<ImageEvent> eventCaptor;
	private Repository repository;
	
	@Before
	public void initTestEnvironment() {
		repository = new Repository(getEntityManager());
		Provider<SubtransactionService> provider = mock(Provider.class);
		SubtransactionService transactions = new InventorySubtransactionService(repository, provider);
		when(provider.get()).thenReturn(transactions);
		messageCaptor = ArgumentCaptor.forClass(Message.class);
		Messages messages = mock(Messages.class);
		doNothing().when(messages).add(messageCaptor.capture());
		eventCaptor = ArgumentCaptor.forClass(ImageEvent.class);
		Event<ImageEvent> events = mock(Event.class);
		doNothing().when(events).fire(eventCaptor.capture());
		PackageVersionService pkgVersions = new PackageVersionService(repository);
		service = new DefaultImageService(transactions,
										  pkgVersions,
										  repository,
										  getDatabase(),
										  messages,
										  events);
		
		transaction(()->{
			repository.addIfAbsent(findRoleByName(ELEMENT_ROLE), 
								   () -> new ElementRole(ELEMENT_ROLE,DATA));
		});
	}
	
	@Test
	public void raise_entity_not_found_exception_when_image_does_not_exist() {
		transaction(()->{
			try {
				service.getImage(randomImageId());
				fail("EntityNotFoundException expected");
			} catch (EntityNotFoundException e) {
				assertEquals(IVT0200E_IMAGE_NOT_FOUND,e.getReason());
			}
		});
	}
	
	@Test
	public void raise_entity_not_found_exception_when_role_does_not_exist() {
		ImageInfo imageInfo = newImageInfo()
				  			  .withImageId(randomImageId())
				  			  .withImageType(LXC)
				  			  .withImageName(ImageName.valueOf("store_image_metadata"))
				  			  .withImageState(NEW)
				  			  .withImageVersion(new Version(1,0,0))
				  			  .withExtension("tar.gz")
				  			  .withElementRole(ElementRoleName.valueOf("non-existent"))
				  			  .withOrganization("io.leitstand")
				  			  .withCategory("unittest")
				  			  .withPlatform(newPlatformInfo()
				  					  		.withVendorName("unittest")
				  					  		.withModelName("on-demand"))
				  			  .build();
		
		
		transaction(()->{
			try {
				service.storeImage(imageInfo);
				fail("EntityNotFoundException expected");
			} catch (EntityNotFoundException e) {
				assertEquals(IVT0400E_ELEMENT_ROLE_NOT_FOUND,e.getReason());
			}
		});
	}
	
	@Test
	public void store_image_metadata() {
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("store_image_metadata"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
									  		.withVendorName("unittest")
									  		.withModelName("on-demand"))
							  .build();
		transaction(()->{
			boolean created = service.storeImage(imageInfo);
			assertTrue(created);
			assertThat(eventCaptor.getValue(),is(ImageAddedEvent.class));
			assertEquals(IVT0202I_IMAGE_STORED.getReasonCode(), 
						 messageCaptor.getValue().getReason());
		});
		
		transaction(()->{
			ImageInfo storedImage = service.getImage(imageInfo.getImageId());
			assertEquals(imageInfo.getImageId(),storedImage.getImageId());
			assertEquals(ImageName.valueOf("store_image_metadata"),storedImage.getImageName());
			assertEquals(NEW,storedImage.getImageState());
			assertEquals(LXC,storedImage.getImageType());
			assertEquals(new Version(1,0,0),storedImage.getImageVersion());
			assertEquals(ELEMENT_ROLE,storedImage.getElementRole());
			assertEquals("tar.gz",storedImage.getExtension());
			assertEquals("io.leitstand",storedImage.getOrganization());
			assertEquals("unittest",storedImage.getCategory());
			assertTrue(storedImage.getApplications().isEmpty());
			assertTrue(storedImage.getPackages().isEmpty());
			assertTrue(storedImage.getChecksums().isEmpty());
			assertEquals("unittest",storedImage.getPlatform().getVendorName());
			assertEquals("on-demand",storedImage.getPlatform().getModelName());
			assertNull(storedImage.getBuildId());
			assertNull(storedImage.getBuildDate());
			
		});
	}
	
	@Test
	public void store_image_metadata_with_build_info() {
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("store_image_metadata_with_build_info"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
								  			.withVendorName("unittest")
								  			.withModelName("on-demand"))
							  .withBuildId(UUID.randomUUID().toString())
							  .withBuildDate(new Date())

							  .build();
		transaction(()->{
			boolean created = service.storeImage(imageInfo);
			assertTrue(created);
			assertThat(eventCaptor.getValue(),is(ImageAddedEvent.class));
			assertEquals(IVT0202I_IMAGE_STORED.getReasonCode(), 
					 	 messageCaptor.getValue().getReason());
		});
		
		transaction(()->{
			ImageInfo storedImage = service.getImage(imageInfo.getImageId());
			assertEquals(imageInfo.getImageId(),storedImage.getImageId());
			assertEquals(ImageName.valueOf("store_image_metadata_with_build_info"),storedImage.getImageName());
			assertEquals(NEW,storedImage.getImageState());
			assertEquals(LXC,storedImage.getImageType());
			assertEquals(new Version(1,0,0),storedImage.getImageVersion());
			assertEquals(ELEMENT_ROLE,storedImage.getElementRole());
			assertEquals("tar.gz",storedImage.getExtension());
			assertEquals("io.leitstand",storedImage.getOrganization());
			assertEquals("unittest",storedImage.getCategory());
			assertTrue(storedImage.getApplications().isEmpty());
			assertTrue(storedImage.getPackages().isEmpty());
			assertTrue(storedImage.getChecksums().isEmpty());
			assertEquals(imageInfo.getBuildDate(),storedImage.getBuildDate());
			assertEquals(imageInfo.getBuildId(),storedImage.getBuildId());
			
		});
	}
	
	@Test
	public void store_image_metadata_with_checksums() {
		Map<String,String> checksums = new HashMap<>();
		checksums.put("MD5", "md5-checksum");
		checksums.put("SHA256", "sha256-checksum");
		
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("store_image_metadata_with_build_info"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
								  			.withVendorName("unittest")
								  			.withModelName("on-demand"))
							  .withBuildId(UUID.randomUUID().toString())
							  .withBuildDate(new Date())
							  .withChecksums(checksums)	
							  .build();
		transaction(()->{
			boolean created = service.storeImage(imageInfo);
			assertTrue(created);
			assertThat(eventCaptor.getValue(),is(ImageAddedEvent.class));
		});
		
		transaction(()->{
			ImageInfo storedImage = service.getImage(imageInfo.getImageId());
			assertEquals(imageInfo.getImageId(),storedImage.getImageId());
			assertEquals(ImageName.valueOf("store_image_metadata_with_build_info"),storedImage.getImageName());
			assertEquals(NEW,storedImage.getImageState());
			assertEquals(LXC,storedImage.getImageType());
			assertEquals(new Version(1,0,0),storedImage.getImageVersion());
			assertEquals(ELEMENT_ROLE,storedImage.getElementRole());
			assertEquals("tar.gz",storedImage.getExtension());
			assertEquals("io.leitstand",storedImage.getOrganization());
			assertEquals("unittest",storedImage.getCategory());
			assertTrue(storedImage.getApplications().isEmpty());
			assertTrue(storedImage.getPackages().isEmpty());
			assertEquals(imageInfo.getBuildDate(),storedImage.getBuildDate());
			assertEquals(imageInfo.getBuildId(),storedImage.getBuildId());
			assertEquals(checksums,imageInfo.getChecksums());
		});
	}
	
	@Test
	public void store_image_with_applications() {
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("store_image_metadata_with_build_info"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
								  			.withVendorName("unittest")
								  			.withModelName("on-demand"))
							  .withBuildId(UUID.randomUUID().toString())
							  .withBuildDate(new Date())
							  .withApplications(ApplicationName.valueOf("app1"),
									  			ApplicationName.valueOf("app2"))	
							  .build();
		transaction(()->{
			boolean created = service.storeImage(imageInfo);
			assertTrue(created);
			assertThat(eventCaptor.getValue(),is(ImageAddedEvent.class));
			assertEquals(IVT0202I_IMAGE_STORED.getReasonCode(), 
					 	 messageCaptor.getValue().getReason());
		});
		
		transaction(()->{
			ImageInfo storedImage = service.getImage(imageInfo.getImageId());
			assertEquals(imageInfo.getImageId(),storedImage.getImageId());
			assertEquals(ImageName.valueOf("store_image_metadata_with_build_info"),storedImage.getImageName());
			assertEquals(NEW,storedImage.getImageState());
			assertEquals(LXC,storedImage.getImageType());
			assertEquals(new Version(1,0,0),storedImage.getImageVersion());
			assertEquals(ELEMENT_ROLE,storedImage.getElementRole());
			assertEquals("tar.gz",storedImage.getExtension());
			assertEquals("io.leitstand",storedImage.getOrganization());
			assertEquals("unittest",storedImage.getCategory());
			assertTrue(storedImage.getPackages().isEmpty());
			assertTrue(storedImage.getChecksums().isEmpty());
			assertEquals(imageInfo.getBuildDate(),storedImage.getBuildDate());
			assertEquals(imageInfo.getBuildId(),storedImage.getBuildId());
			assertEquals(asList(ApplicationName.valueOf("app1"),
								ApplicationName.valueOf("app2")),
						 storedImage.getApplications());
		});
	}
	
	@Test
	public void remove_image_with_applications_and_package_revisions() {
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("remove_image_with_applications_and_package_revisions"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
									  		.withVendorName("unittest")
									  		.withModelName("on-demand"))
							  .withApplications(ApplicationName.valueOf("app1"))
							  .withPackages(newPackageVersionInfo()
									  		.withOrganization("io.leitstand")
									  		.withPackageName("sample")
									  		.withPackageExtension("so")
									  		.withPackageVersion(new Version(1,0,0)))
							  .build();
		transaction(()->{
			boolean created = service.storeImage(imageInfo);
			assertTrue(created);
			assertThat(eventCaptor.getValue(),is(ImageAddedEvent.class));
			assertEquals(IVT0202I_IMAGE_STORED.getReasonCode(), 
						 messageCaptor.getValue().getReason());
		});
		
		transaction(()->{
			service.removeImage(imageInfo.getImageId());
			assertThat(eventCaptor.getValue(),is(ImageRemovedEvent.class));
			assertEquals(IVT0203I_IMAGE_REMOVED.getReasonCode(), 
					 	 messageCaptor.getValue().getReason());

		});
		
		transaction(()->{
			try {
				service.getImage(imageInfo.getImageId());
				fail("EntityNotFoundException expected");
			} catch (EntityNotFoundException e) {
				assertEquals(IVT0200E_IMAGE_NOT_FOUND,e.getReason());
			}
		});
	}
	
	@Test
	public void update_image_state() {
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("update_image_state"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
									  		.withVendorName("unittest")
									  		.withModelName("on-demand"))
							  .withApplications(ApplicationName.valueOf("app1"))
							  .withPackages(newPackageVersionInfo()
									  		.withOrganization("io.leitstand")
									  		.withPackageName("sample")
									  		.withPackageExtension("so")
									  		.withPackageVersion(new Version(1,0,0)))
							  .build();
		transaction(()->{
			service.storeImage(imageInfo);
		});
		
		transaction(()->{
			service.updateImageState(imageInfo.getImageId(),CANDIDATE);
			assertThat(eventCaptor.getValue(),is(ImageStateChangedEvent.class));
			assertEquals(IVT0201I_IMAGE_STATE_UPDATED.getReasonCode(), 
					 	 messageCaptor.getValue().getReason());

		});
		
		transaction(()->{
			assertEquals(CANDIDATE,service.getImage(imageInfo.getImageId()).getImageState());

		});
		
	}
	
	
	@Test
	public void rename_image() {
		ImageInfo imageInfo = newImageInfo()
							  .withImageId(randomImageId())
							  .withImageType(LXC)
							  .withImageName(ImageName.valueOf("rename_image"))
							  .withImageState(NEW)
							  .withImageVersion(new Version(1,0,0))
							  .withExtension("tar.gz")
							  .withElementRole(ELEMENT_ROLE)
							  .withOrganization("io.leitstand")
							  .withCategory("unittest")
							  .withPlatform(newPlatformInfo()
									  		.withVendorName("unittest")
									  		.withModelName("on-demand"))
							  .withApplications(ApplicationName.valueOf("app1"))
							  .withPackages(newPackageVersionInfo()
									  		.withOrganization("io.leitstand")
									  		.withPackageName("sample")
									  		.withPackageExtension("so")
									  		.withPackageVersion(new Version(1,0,0)))
							  .build();
		transaction(()->{
			service.storeImage(imageInfo);
		});
		
		ImageInfo renamedImage = newImageInfo()
								 .withImageId(imageInfo.getImageId())
								 .withImageType(LXC)
								 .withImageName(ImageName.valueOf("renamed_image"))
								 .withImageState(NEW)
								 .withImageVersion(new Version(1,0,0))
								 .withExtension("tar.gz")
								 .withElementRole(ELEMENT_ROLE)
								 .withOrganization("io.leitstand")
								 .withCategory("unittest")
								 .withPlatform(newPlatformInfo()
										 .withVendorName("unittest")
										 .withModelName("on-demand"))
								 .withApplications(ApplicationName.valueOf("app1"))
								 .withPackages(newPackageVersionInfo()
										 	   .withOrganization("io.leitstand")
										 	   .withPackageName("sample")
										 	   .withPackageExtension("so")
										 	   .withPackageVersion(new Version(1,0,0)))
								 .build();
		
		transaction(()->{
			service.storeImage(renamedImage);
			assertThat(eventCaptor.getValue(),is(ImageStoredEvent.class));
			assertEquals(IVT0202I_IMAGE_STORED.getReasonCode(), 
					 	 messageCaptor.getValue().getReason());

		});
		
		transaction(()->{
			assertEquals(ImageName.valueOf("renamed_image"),service.getImage(imageInfo.getImageId()).getImageName());

		});
		
	}
	
	@Test
	public void cannot_remove_bound_image() {
		
		ImageId imageId = randomImageId();
		transaction(() -> {
			ElementRole  role  = repository.execute(findRoleByName(ELEMENT_ROLE));
			ElementGroup group = repository.addIfAbsent(findElementGroupByName(GROUP_TYPE,GROUP_NAME), 
																  () -> new ElementGroup(randomGroupId(), 
																		  				 GROUP_TYPE, 
																		  				 GROUP_NAME));
			
			Platform platform = repository.addIfAbsent(Platform.findByModel("image_it", "dummy"),
													   ()->new Platform(PlatformId.randomPlatformId(), "image_it", "dummy"));
			
			
			Element element = repository.addIfAbsent(findElementByName(ELEMENT_NAME), 
													 () -> new Element(group, 
															 		   role, 
															 		   randomElementId(), 
															 		   ELEMENT_NAME));

			Image image = new Image(imageId,
									"io.leitstand",
									ImageType.LXC,
									ImageName.valueOf("remove_bound_image"),
									role,
									platform,
									new Version(1,0,0));
			repository.add(image);
			repository.add(new Element_Image(element, image));
		});
		
		transaction(()->{
			try {
				service.removeImage(imageId);
				fail("ConflictException expected");
			} catch(ConflictException e) {
				assertEquals(IVT0204E_IMAGE_NOT_REMOVABLE,e.getReason());
			}
		});
		
	}
	
	@Test
	public void read_active_image_statistics() {
		
		ImageId imageId = randomImageId();
		transaction(() -> {
			ElementRole  role  = repository.execute(findRoleByName(ELEMENT_ROLE));
			ElementGroup group = repository.addIfAbsent(findElementGroupByName(GROUP_TYPE, 
																  						 GROUP_NAME), 
																  () -> new ElementGroup(randomGroupId(), 
																		  				 GROUP_TYPE, 
																		  				 GROUP_NAME));
			
			Platform platform = repository.addIfAbsent(Platform.findByModel("image_it", "dummy"),
													   ()->new Platform(PlatformId.randomPlatformId(), "image_it", "dummy"));
			
			
			Element element = repository.addIfAbsent(findElementByName(ELEMENT_NAME), 
													 () -> new Element(group, 
															 		   role, 
															 		   randomElementId(), 
															 		   ELEMENT_NAME));

			Image image = new Image(imageId,
									"io.leitstand",
									ImageType.LXC,
									ImageName.valueOf("stats_active_image"),
									role,
									platform,
									new Version(1,0,0));
			repository.add(image);
			Element_Image ei = new Element_Image(element, image);
			ei.setImageInstallationState(ACTIVE);
			repository.add(ei);
		});
		
		transaction(()->{
			ImageStatistics stats = service.getImageStatistics(imageId);
			assertEquals(stats.getImage(),service.getImage(imageId));
			assertEquals(Integer.valueOf(1),stats.getActiveCount().get(GROUP_NAME));
			assertNull(stats.getCachedCount().get(GROUP_NAME));
		});
		
	}
	
	@Test
	public void read_cached_image_statistics() {
		
		ImageId imageId = randomImageId();
		transaction(() -> {
			ElementRole  role  = repository.execute(findRoleByName(ELEMENT_ROLE));
			ElementGroup group = repository.addIfAbsent(findElementGroupByName(GROUP_TYPE, 
																  						 GROUP_NAME), 
																  () -> new ElementGroup(randomGroupId(), 
																		  				 GROUP_TYPE, 
																		  				 GROUP_NAME));
			
			Platform platform = repository.addIfAbsent(Platform.findByModel("image_it", "dummy"),
													   ()->new Platform(PlatformId.randomPlatformId(), "image_it", "dummy"));
			
			
			Element element = repository.addIfAbsent(findElementByName(ELEMENT_NAME), 
													 () -> new Element(group, 
															 		   role, 
															 		   randomElementId(), 
															 		   ELEMENT_NAME));

			Image image = new Image(imageId,
									"io.leitstand",
									ImageType.LXC,
									ImageName.valueOf("stats_active_image"),
									role,
									platform,
									new Version(1,0,0));
			repository.add(image);
			Element_Image ei = new Element_Image(element, image);
			ei.setImageInstallationState(CACHED);
			repository.add(ei);
		});
		
		transaction(()->{
			ImageStatistics stats = service.getImageStatistics(imageId);
			assertEquals(stats.getImage(),service.getImage(imageId));
			assertNull(stats.getActiveCount().get(GROUP_NAME));
			assertEquals(Integer.valueOf(1),stats.getCachedCount().get(GROUP_NAME));
		});
		
	}

}
