/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.commons.db.DatabaseService.prepare;
import static io.leitstand.inventory.model.Package.findByName;
import static io.leitstand.inventory.model.PackageVersion.findPackageVersion;
import static io.leitstand.inventory.service.PackageInfo.newPackageInfo;
import static io.leitstand.inventory.service.PackageVersionId.newPackageVersionId;
import static io.leitstand.inventory.service.PackageVersionInfo.newPackageVersionInfo;
import static io.leitstand.inventory.service.QualifiedPackageName.newQualifiedPackageName;
import static io.leitstand.inventory.service.ReasonCode.IVT0500E_PACKAGE_NOT_FOUND;
import static io.leitstand.inventory.service.ReasonCode.IVT0510E_PACKAGE_VERSION_NOT_FOUND;
import static java.util.stream.Collectors.toMap;
import static javax.persistence.LockModeType.OPTIMISTIC;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import io.leitstand.commons.EntityNotFoundException;
import io.leitstand.commons.db.DatabaseService;
import io.leitstand.commons.model.Repository;
import io.leitstand.commons.model.Service;
import io.leitstand.inventory.service.PackageInfo;
import io.leitstand.inventory.service.PackageService;
import io.leitstand.inventory.service.PackageVersionId;
import io.leitstand.inventory.service.PackageVersionInfo;
import io.leitstand.inventory.service.QualifiedPackageName;
import io.leitstand.inventory.service.Version;
@Service 
public class DefaultPackageService implements PackageService {

	static PackageVersionInfo packageVersionInfo(PackageVersion revision) {
		return newPackageVersionInfo()
			   .withOrganization(revision.getOrganization())
			   .withPackageName(revision.getPackageName())
			   .withPackageExtension(revision.getPackageExtension())
			   .withPackageVersion(revision.getPackageVersion())
			   .withBuildId(revision.getBuildId())
			   .withBuildDate(revision.getBuildDate())
			   .withChecksums(revision.getChecksums()
					   				  .stream()
					   				  .collect(toMap(c -> c.getAlgorithm().name(),
					   						 	     Checksum::getValue)))
			   .build();
	}
	
	@Inject
	@Inventory
	private Repository repository;
	
	@Inject
	@Inventory
	private DatabaseService database;
	
	@Inject
	private PackageVersionService service;
	
	public DefaultPackageService(){
		// Tool constructor
	}
	
	public DefaultPackageService(	Repository repository,
									DatabaseService database,
									PackageVersionService service) {
		this.repository = repository;
		this.database = database;
		this.service  = service;
	}

	@Override
	public List<QualifiedPackageName> getPackages() {
		return database.executeQuery(prepare("SELECT org, name FROM INVENTORY.PACKAGE"), 
						 			 rs -> newQualifiedPackageName()
						 			 	   .withOrganization(rs.getString(1))
						 			 	   .withName(rs.getString(2))
						 			 	   .build());
	}

	@Override
	public PackageInfo getPackage(String org, String name, String ext) {
		Package pkg = repository.execute(findByName(org, 
										            name,
										            ext,
										            OPTIMISTIC));
		if(pkg == null){
			throw new EntityNotFoundException(IVT0500E_PACKAGE_NOT_FOUND,org+"-"+name+"."+ext); 
		}
		
		List<PackageVersionId> versions = new LinkedList<>();
		for(PackageVersion version : pkg.getVersions()){
			versions.add(newPackageVersionId()
						 .withOrganization(version.getOrganization())
						 .withPackageName(version.getPackageName())
						 .withPackageVersion(version.getPackageVersion())
						 .build());
		}
		
		return newPackageInfo()
			   .withOrganization(pkg.getOrganization())
			   .withName(pkg.getPackageName())
			   .withVersions(versions)
			   .build();
		
	}

	@Override
	public PackageVersionInfo getPackageVersion(String org, 
												String name, 
												Version version) {
		PackageVersion pkg = repository.execute(findPackageVersion(org, 
																   name, 
																   version));
		if(pkg == null){
			throw new EntityNotFoundException(IVT0510E_PACKAGE_VERSION_NOT_FOUND, org+"."+name+"-"+version);
		}
		return packageVersionInfo(pkg);
	}

	@Override
	public void storePackageVersion(PackageVersionInfo info) {
		service.storePackageVersion(info);
	}

	void removeRevision(String org, String name, Version rev) {
		service.removePackageVersion(org, name, rev);
	}

}
