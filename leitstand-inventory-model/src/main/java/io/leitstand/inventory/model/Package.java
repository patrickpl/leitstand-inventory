/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.inventory.service.ReasonCode.IVT0511E_PACKAGE_VERSION_EXISTS;
import static java.util.Collections.unmodifiableList;
import static javax.persistence.CascadeType.ALL;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.LockModeType;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import io.leitstand.commons.ConflictException;
import io.leitstand.commons.model.Query;
import io.leitstand.commons.model.VersionableEntity;
import io.leitstand.inventory.service.Version;

@Entity
@Table(schema="inventory", name="package")
@NamedQuery(name="Package.findByName", 
			query="SELECT p FROM Package p WHERE p.org=:org AND p.name=:name AND p.ext=:ext")
public class Package extends VersionableEntity{

	private static final long serialVersionUID = 1L;

	public static Query<Package> findByName(String org, String name, String ext, LockModeType locking) {
		return em -> em.createNamedQuery("Package.findByName",Package.class)
					   .setParameter("org",org)
					   .setParameter("name",name)
					   .setParameter("ext", ext)
					   .setLockMode(locking)
					   .getSingleResult();
		
	}
	
	@Column(nullable=false, length=64)
	private String org; // e.g. net.rtbrick.bgp , e.g. net.rtbrick.isis

	@Column(nullable=false, length=64)
	private String name;
	
	@Column(nullable=false, length=16)
	private String ext;
	
	@OneToMany(cascade=ALL, orphanRemoval=true, mappedBy="pkg")
	private List<PackageVersion> versions;
	
	protected Package(){
		//JPA
	}
	
	public Package(String org, String name, String ext){
		this.org = org;
		this.name = name;
		this.ext = ext;
		this.versions = new LinkedList<>();
	}
	
	public String getOrganization() {
		return org;
	}
	
	public String getPackageName() {
		return name;
	}
	
	PackageVersion newVersion(Version rev){
		if(findVersion(rev) != null){
			throw new ConflictException(IVT0511E_PACKAGE_VERSION_EXISTS,getOrganization(),getPackageName(),rev);
		}
		PackageVersion revision = new PackageVersion(this,rev);
		versions.add(revision);
		return revision;
	}
	
	void removeVersion(Version rev){
		PackageVersion version = findVersion(rev);
		if(version != null){
			versions.remove(version);
		}
	}
	
	private PackageVersion findVersion(Version rev){
		for(PackageVersion revision : getVersions()){
			if(revision.getPackageVersion().equals(rev)){
				return revision;
			}
		}
		return null;
	}
	
	List<PackageVersion> getVersions(){
		return unmodifiableList(versions);
	}

	public String getPackageExtension() {
		return ext;
	}

}
