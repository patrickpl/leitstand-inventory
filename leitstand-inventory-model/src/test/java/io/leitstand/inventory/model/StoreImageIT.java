/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.inventory.model.ElementRole.findRoleByName;
import static io.leitstand.inventory.model.ImageInfoMother.BAR_200;
import static io.leitstand.inventory.model.ImageInfoMother.FOO_100;
import static io.leitstand.inventory.model.ImageInfoMother.FOO_101;
import static io.leitstand.inventory.model.ImageInfoMother.newLeafImage;
import static io.leitstand.inventory.service.Plane.DATA;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Provider;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.leitstand.commons.db.DatabaseService;
import io.leitstand.commons.messages.Messages;
import io.leitstand.commons.model.Repository;
import io.leitstand.commons.tx.SubtransactionService;
import io.leitstand.inventory.service.ElementRoleName;
import io.leitstand.inventory.service.ImageInfo;
import io.leitstand.inventory.service.ImageService;
import io.leitstand.inventory.service.PackageVersionInfo;
import io.leitstand.inventory.service.Version;

public class StoreImageIT extends InventoryIT{

	private ImageService service;
	private PackageVersionService packages;
	private ImageInfo image;
	
	@Before
	public void initTestEnvironment(){
		Repository repository = new Repository(getEntityManager());
		this.packages = new PackageVersionService(repository);
		Provider<SubtransactionService> provider = mock(Provider.class);
		SubtransactionService transactions = new InventorySubtransactionService(repository, provider);
		when(provider.get()).thenReturn(transactions);
		
		service = new DefaultImageService(transactions,
										  packages, 
										  repository,
										  mock(DatabaseService.class),
										  mock(Messages.class),
										  mock(Event.class));
		addElementRole(repository, new ElementRoleName("LEAF"));
		addElementRole(repository, new ElementRoleName("SPINE"));
	}

	private void addElementRole(Repository repository, ElementRoleName name) {
		ElementRole type = repository.execute(findRoleByName(name));
		if(type == null) {
			type = new ElementRole(name,DATA);
			repository.add(type);
		}
	}
	
	@Test
	public void create_new_container_image_and_new_package(){
		image = ImageInfoMother.newLeafImage(new Version(1,0,0),FOO_101,BAR_200);
		service.storeImage(image);
	}
	
	@Test
	public void create_new_container_image_referring_to_existing_packages(){
		packages.storePackageVersion(FOO_101);
		packages.storePackageVersion(BAR_200);
		transaction(()->{
			image = newLeafImage(new Version(1,0,0),FOO_101,BAR_200);
			service.storeImage(image);
		});
	}
	
	@Test
	public void create_new_container_image_adding_new_package_revision_to_existing_package(){
		packages.storePackageVersion(FOO_100);
		transaction(()->{
			image = ImageInfoMother.newLeafImage(new Version(1,0,0),FOO_101,BAR_200);
			service.storeImage(image);
		});
	}
	
	@After
	public void verify_created_container_image(){
		transaction(()->{
			ImageInfo restored = service.getImage(image.getImageId());
			Assert.assertNotNull(restored);
			assertEquals(image.getImageId(),restored.getImageId());

			List<PackageVersionInfo> packages = image.getPackages();
			assertEquals("net.rtbrick",packages.get(0).getOrganization());
			assertEquals("foo",packages.get(0).getPackageName());
			assertEquals(new Version(1,0,1),packages.get(0).getPackageVersion());

			assertEquals("net.rtbrick",packages.get(1).getOrganization());
			assertEquals("bar",packages.get(1).getPackageName());
			assertEquals(new Version(2,0,0),packages.get(1).getPackageVersion());
		});
	
	}
	
}
