/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.commons.model.StringUtil.isEmptyString;
import static io.leitstand.inventory.service.ImageState.CANDIDATE;
import static io.leitstand.inventory.service.ImageState.REVOKED;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;

import io.leitstand.commons.model.Query;
import io.leitstand.commons.model.Update;
import io.leitstand.commons.model.VersionableEntity;
import io.leitstand.inventory.jpa.ImageNameConverter;
import io.leitstand.inventory.jpa.ImageStateConverter;
import io.leitstand.inventory.service.ElementPlatformInfo;
import io.leitstand.inventory.service.ElementRoleName;
import io.leitstand.inventory.service.ImageId;
import io.leitstand.inventory.service.ImageName;
import io.leitstand.inventory.service.ImageState;
import io.leitstand.inventory.service.ImageType;
import io.leitstand.inventory.service.Version;
//TODO Config images are platform agnostic. We need to support three kind of images:
// Images for a certain element role on a certain platform
// Images for a certain element, role and platform
@Entity
@Table(schema="inventory", name="image")
@NamedQuery(name="Image.findByImageId", 
		    query="SELECT d FROM Image d WHERE d.uuid=:id")
@NamedQuery(name="Image.markElementImageSuperseded",
			query="UPDATE Image d "+ 
			      "SET d.imageState=io.leitstand.inventory.service.ImageState.SUPERSEDED "+
			      "WHERE d.platform=:platform "+
			      "AND d.role=:role "+
			      "AND d.element=:element "+
			      "AND d.imageType=:type "+
			      "AND d.imageName=:name "+
			      "AND d.imageState<>io.leitstand.inventory.service.ImageState.REVOKED "+
			      "AND ("+
			      "(d.major=:major AND d.minor=:minor AND  d.patch=:patch AND d.prerelease IS NOT NULL AND d.prerelease < :prerelease) OR "+
			      "(d.major=:major AND d.minor=:minor AND  d.patch<:patch) OR "+
			      "(d.major=:major AND d.minor<:minor) OR "+
			      "(d.major<:major))")

@NamedQuery(name="Image.restoreElementImageCandidates",
		    query="UPDATE Image d "+ 
		    		  "SET d.imageState=io.leitstand.inventory.service.ImageState.CANDIDATE "+
		    		  "WHERE d.platform=:platform "+
		    		  "AND d.role=:role "+
		    		  "AND d.element=:element "+
		    		  "AND d.imageType=:type "+
		    		  "AND d.imageName=:name "+
		    		  "AND d.imageState<>io.leitstand.inventory.service.ImageState.REVOKED "+
		    		  "AND ("+
		    		  "(d.major=:major AND d.minor=:minor AND d.patch=:patch AND d.prerelease IS NOT NULL and d.prerelease > :prerelease) OR "+
		    		  "(d.major=:major AND d.minor=:minor AND  d.patch>:patch) OR "+
		    		  "(d.major=:major AND d.minor>:minor) OR "+
				      "(d.major>:major))")
@NamedQuery(name="Image.markRoleImageSuperseded",
			query="UPDATE Image d "+ 
				  "SET d.imageState=io.leitstand.inventory.service.ImageState.SUPERSEDED "+
				  "WHERE d.platform=:platform "+
				  "AND d.role=:role "+
				  "AND d.element IS NULL "+
				  "AND d.imageType=:type "+
				  "AND d.imageName=:name "+
				  "AND d.imageState<>io.leitstand.inventory.service.ImageState.REVOKED "+
				  "AND ("+
			      "(d.major=:major AND d.minor=:minor AND  d.patch=:patch AND d.prerelease IS NOT NULL AND d.prerelease is NOT NULL AND d.prerelease < :prerelease) OR "+
				  "(d.major=:major AND d.minor=:minor AND  d.patch<:patch) OR "+
				  "(d.major=:major AND d.minor<:minor) OR "+
				  "(d.major<:major))")
@NamedQuery(name="Image.restoreRoleImageCandidates",
				query="UPDATE Image d "+ 
					  "SET d.imageState=io.leitstand.inventory.service.ImageState.CANDIDATE "+
					  "WHERE d.platform=:platform "+
					  "AND d.role=:role "+
					  "AND d.element IS NULL "+
					  "AND d.imageType=:type "+
					  "AND d.imageName=:name "+
					  "AND d.imageState<>io.leitstand.inventory.service.ImageState.REVOKED "+
					  "AND ("+
		    		  "(d.major=:major AND d.minor=:minor AND d.patch=:patch AND d.prerelease is NOT NULL AND d.prerelease > :prerelease) OR "+
					  "(d.major=:major AND d.minor=:minor AND  d.patch>:patch) OR "+
					  "(d.major=:major AND d.minor>:minor) OR "+
					  "(d.major>:major))")
@NamedQuery(name="Image.findElementRoleImage",
		    query="SELECT d FROM Image d "+
				  "WHERE d.platform=:platform "+
		    	  "AND d.role=:role "+
				  "AND d.imageType=:type "+
		    	  "AND d.imageName=:name "+
		    	  "AND d.major=:major "+
				  "AND d.minor=:minor "+
		    	  "AND d.patch=:patch "+
				  "AND d.prerelease=:prerelease "+
				  "AND d.element IS NULL")
@NamedQuery(name="Image.findDefaultImage",
			query="SELECT d FROM Image d "+
				  "WHERE d.platform.vendor=:vendor "+
				  "AND d.platform.model=:model "+
				  "AND d.role.name=:role "+
				  "AND d.imageType=:type "+
				  "AND d.imageName=:name "+
				  "AND d.imageState=io.leitstand.inventory.service.ImageState.RELEASE "+
				  "AND d.element is null")
@NamedQuery(name="Image.findDefaultImages",
			query="SELECT d FROM Image d "+
				  "WHERE d.platform=:platform "+
				  "AND d.role=:role "+
				  "AND d.imageState=io.leitstand.inventory.service.ImageState.RELEASE "+
				  "AND d.element is null")
@NamedQuery(name="Image.findAvailableUpdates",
			    query="SELECT d FROM Image d "+
			    	  "WHERE d.platform=:platform "+
			    	  "AND d.imageState <> io.leitstand.inventory.service.ImageState.REVOKED " +
			    	  "AND d.role.name=:role "+
			    	  "AND d.imageType=:type "+
			    	  "AND d.imageName=:name "+
			    	  "AND (d.element IS NULL OR d.element=:element) "+
			   		  "AND ((d.major > :major) "+
			   		  "OR ( d.major = :major AND d.minor > :minor) "+
			   		  "OR (d.major=:major AND d.minor=:minor AND d.patch > :patch) "+
			   		  "OR (d.major=:major AND d.minor=:minor AND d.patch = :patch AND d.prerelease IS NOT NULL AND d.prerelease > :prerelease)) "+
				  "ORDER BY d.major DESC, d.minor DESC, d.patch DESC")
@NamedQuery(name="Image.findByElementAndImageTypeAndVersion", 
			query="SELECT d FROM Image d "+
				  "WHERE d.role=:role "+
				  "AND d.platform=:platform "+
				  "AND d.imageType=:type "+
				  "AND d.imageName=:name "+
				  "AND (d.element is NULL OR d.element=:element) "+
				  "AND d.major=:major "+
				  "AND d.minor=:minor "+
				  "AND d.patch=:patch "+
				  "AND d.prerelease=:prerelease")
@NamedQuery(name="Image.countReferences",
			query="SELECT count(ei) FROM Element_Image ei WHERE ei.image=:image")
public class Image extends VersionableEntity{
	
	private static final long serialVersionUID = 1L;
	private static final String RELEASE = "~RELEASE";
	
	static final String prerelease(Version version) {
		return isEmptyString(version.getPreRelease()) ? RELEASE : version.getPreRelease();
	}

	static final String prerelease(String version) {
		return RELEASE.equals(version) ? null : version;
	}
	
	public static Update markAllSuperseded(Image image) {
		
		if(image.getElement() != null) {
			return em -> em.createNamedQuery("Image.markElementImageSuperseded",Image.class)
						   .setParameter("platform", image.getPlatform())
						   .setParameter("role", image.getElementRole())
						   .setParameter("element", image.getElement())
						   .setParameter("type", image.getImageType())
						   .setParameter("name", image.getImageName())
						   .setParameter("major",image.getImageVersion().getMajorLevel())
						   .setParameter("minor",image.getImageVersion().getMinorLevel())
						   .setParameter("patch",image.getImageVersion().getPatchLevel())
						   .setParameter("prerelease", prerelease(image.getImageVersion()))
						   .executeUpdate();
		}
		return em -> em.createNamedQuery("Image.markRoleImageSuperseded",Image.class)
				   	   .setParameter("platform", image.getPlatform())
				   	   .setParameter("role", image.getElementRole())
				   	   .setParameter("type", image.getImageType())
				   	   .setParameter("name", image.getImageName())
				   	   .setParameter("major",image.getImageVersion().getMajorLevel())
				   	   .setParameter("minor",image.getImageVersion().getMinorLevel())
				   	   .setParameter("patch",image.getImageVersion().getPatchLevel())
					   .setParameter("prerelease", prerelease(image.getImageVersion()))
				   	   .executeUpdate();
			
	}

	public static Update restoreCandidates(Image image) {
		if(image.getElement() != null) {
			return em -> em.createNamedQuery("Image.restoreElementImageCandidates",Image.class)
						   .setParameter("platform", image.getPlatform())
						   .setParameter("role", image.getElementRole())
						   .setParameter("element", image.getElement())
						   .setParameter("type", image.getImageType())
						   .setParameter("name", image.getImageName())
						   .setParameter("major",image.getImageVersion().getMajorLevel())
						   .setParameter("minor",image.getImageVersion().getMinorLevel())
						   .setParameter("patch",image.getImageVersion().getPatchLevel())
						   .setParameter("prerelease", prerelease(image.getImageVersion()))
						   .executeUpdate();
		}
		return em -> em.createNamedQuery("Image.restoreRoleImageCandidates",Image.class)
					   .setParameter("platform", image.getPlatform())
					   .setParameter("role", image.getElementRole())
					   .setParameter("type", image.getImageType())
					   .setParameter("name", image.getImageName())
					   .setParameter("major",image.getImageVersion().getMajorLevel())
					   .setParameter("minor",image.getImageVersion().getMinorLevel())
					   .setParameter("patch",image.getImageVersion().getPatchLevel())
					   .setParameter("prerelease", prerelease(image.getImageVersion()))
					   .executeUpdate();
	}
	
	public static Query<Image> findElementRoleImage(Platform platform,
										     		ElementRole elementRole, 
										     		ImageType imageType,
										     		ImageName imageName,
										     		Version version) {
		return em -> em.createNamedQuery("Image.findElementRoleImage",Image.class)
					   .setParameter("platform", platform)
					   .setParameter("role", elementRole)
					   .setParameter("type", imageType)
					   .setParameter("name", imageName)
					   .setParameter("major", version.getMajorLevel())
					   .setParameter("minor", version.getMinorLevel())
					   .setParameter("patch", version.getPatchLevel())
					   .setParameter("prerelease", prerelease(version))
					   .getSingleResult();
	}
	
	public static Query<Image> findDefaultImage(ElementRoleName elementRole,
												ElementPlatformInfo platform,
												ImageType imageType,
												ImageName imageName){
		return em -> em.createNamedQuery("Image.findDefaultImage",Image.class)
					   .setParameter("vendor",platform.getVendorName())
					   .setParameter("model",platform.getModelName())
					   .setParameter("role",elementRole)
					   .setParameter("type",imageType)
					   .setParameter("name", imageName)
					   .getSingleResult();
		
	}
	
	public static Query<List<Image>> findDefaultImages(ElementRole elementRole,
													   Platform platform){
		return em -> em.createNamedQuery("Image.findDefaultImages",Image.class)
					   .setParameter("platform",platform)
					   .setParameter("role",elementRole)
					   .getResultList();

}
	
	
	public static Query<List<Image>> findUpdates(Platform platform,
												 ImageType imageType, 
												 ImageName imageName,
	                                             ElementRole role, 
	                                             Version version, 
	                                             Element element){
		return em -> em.createNamedQuery("Image.findAvailableUpdates",
										 Image.class)
					   .setParameter("platform",platform)
				       .setParameter("role", role.getRoleName())
				       .setParameter("major",version.getMajorLevel())
				       .setParameter("minor",version.getMinorLevel())
				       .setParameter("patch",version.getPatchLevel())
					   .setParameter("prerelease", prerelease(version))
				       .setParameter("type",imageType)
				       .setParameter("name",imageName)
				       .setParameter("element", element)
				       .getResultList();
	}

	public static Query<Image> findByImageId(ImageId id){
		return em -> em.createNamedQuery("Image.findByImageId",
										 Image.class)
					   .setParameter("id", id.toString())
					   .getSingleResult();
	}
	
	public static Query<Image> findByElementAndImageTypeAndVersion(Element element,
																   ImageType imageType, 
																   ImageName imageName,
	                                                               Version version) {
		
		return em -> em.createNamedQuery("Image.findByElementAndImageTypeAndVersion", 
										 Image.class)
					   .setParameter("platform",element.getPlatform())
					   .setParameter("role", element.getElementRole())
					   .setParameter("element", element)
					   .setParameter("type",imageType)
					   .setParameter("name",imageName)
					   .setParameter("major",version.getMajorLevel())
					   .setParameter("minor", version.getMinorLevel())
					   .setParameter("patch", version.getPatchLevel())
					   .setParameter("prerelease", prerelease(version))
					   .getSingleResult();
	}
	
	public static Query<Long> countImageReferences(Image image){
		return em -> em.createNamedQuery("Image.countReferences",Long.class)
					   .setParameter("image",image)
					   .getSingleResult();
	}
	
	
	private String org;
	

	@Enumerated(STRING)
	@Column(name="type")
	private ImageType imageType;
	
	@Convert(converter=ImageNameConverter.class)
	@Column(name="name")
	private ImageName imageName;
	
	@Convert(converter=ImageStateConverter.class)
	@Column(name="state")
	private ImageState imageState;
	
	private String category;
	
	@ManyToOne
	@JoinColumn(name="elementrole_id")
	private ElementRole role;
	
	@ManyToOne
	@JoinColumn(name="Element_id")
	private Element element;
	
	@Column(nullable=false)
	private int major;
	
	@Column(nullable=false)
	private int minor;
	
	@Column(nullable=false)
	private int patch;

	private String prerelease;
	
	private String ext;
	
	private String description;
	
	@ElementCollection
	@CollectionTable(schema="inventory",
					 name="image_checksum",
					 joinColumns=@JoinColumn(name="image_id"))
	private List<Checksum> checksums = emptyList();
	
	@ManyToOne(cascade=PERSIST)
	@JoinColumn(name="PLATFORM_ID")
	private Platform platform;
	
	@ManyToMany
	@JoinTable(
			schema="INVENTORY",
			name="IMAGE_APPLICATION",
			joinColumns=@JoinColumn(name="IMAGE_ID", referencedColumnName="ID"),
			inverseJoinColumns=@JoinColumn(name="APPLICATION_ID",referencedColumnName="ID")
			)
	private List<Application> applications;
	

	@Temporal(TIMESTAMP)
	private Date tsbuild;
	
	private String buildId;
	
	@ManyToMany
	@JoinTable(
			schema="INVENTORY",
			name="IMAGE_PACKAGEVERSION",
			joinColumns=@JoinColumn(name="IMAGE_ID",referencedColumnName="ID")
			)	
	private List<PackageVersion> packages;
	
	protected Image(){
		// JPA
	}
	
	public Image(ImageId id){
		super(id.toString());
	}
	
	// Stub record!
	protected Image(ImageId id, 
					String org,
					ImageType imageType,
					ImageName imageName,
					ElementRole role, 
					Platform platform,
					Version version) {
		super(id.toString());
		this.org = org;
		this.imageType = imageType;
		this.imageName = imageName;
		this.imageState = CANDIDATE;
		this.role = role;
		this.platform = platform;
		this.major = version.getMajorLevel();
		this.minor = version.getMinorLevel();
		this.patch = version.getPatchLevel();
		this.prerelease = prerelease(version);
		this.packages = emptyList();
		this.applications = emptyList();
	}


	public Version getImageVersion(){
		return new Version(major,minor,patch,prerelease(prerelease));
	}
	
	public ImageId getImageId() {
		return new ImageId(getUuid());
	}

	public List<Checksum> getChecksums() {
		return unmodifiableList(checksums);
	}
	
	public Date getBuildDate(){
		if(tsbuild == null) {
			return null;
		}
		return new Date(tsbuild.getTime());
	}
	
	public List<PackageVersion> getPackages(){
		return unmodifiableList(packages);
	}
	
	public ImageType getImageType() {
		return imageType;
	}
	
	public void setImageType(ImageType imageType) {
		this.imageType = imageType;
	}
	
	
	public ElementRole getElementRole() {
		return role;
	}

	public void setElementRole(ElementRole role) {
		this.role = role;
	}
	
	
	public String getImageExtension() {
		return ext;
	}
	
	public void setExtension(String ext) {
		this.ext = ext;
	}
	
	public void setImageVersion(Version version){
		this.major = version.getMajorLevel();
		this.minor = version.getMinorLevel();
		this.patch = version.getPatchLevel();
		this.prerelease = prerelease(version);
	}
	
	public void setBuildDate(Date buildDate){
		if(buildDate != null) {
			this.tsbuild = new Date(buildDate.getTime());
		} else {
			this.tsbuild = null;
		}
	}
	
	public void setBuildId(String buildId) {
		this.buildId = buildId;
	}
	
	public void setPackages(List<PackageVersion> packages){
		this.packages = packages;
	}

	public void setChecksums(List<Checksum> checksums) {
		this.checksums = new ArrayList<>(checksums);
	}

	public void setOrganization(String org) {
		this.org = org;
	}

	public String getOrganization() {
		return org;
	}
	
	public void setElement(Element element) {
		this.element = element;
	}
	
	public Element getElement() {
		return element;
	}

	public void setApplications(List<Application> applications) {
		this.applications = applications;
	}

	public List<Application> getApplications() {
		return applications;
	}

	public String getVendorName() {
		return platform.getVendor();
	}
	
	public String getBuildId() {
		return buildId;
	}
	
	public String getModelName() {
		return platform.getModel();
	}

	public Platform getPlatform() {
		return platform;
	}

	public ImageState getImageState() {
		return imageState;
	}
	
	public void setImageState(ImageState imageState) {
		this.imageState = imageState;
	}

	public boolean isRevoked() {
		return getImageState() == REVOKED;
	}
	
	public String getCategory() {
		return category;
	}
	
	public void setCategory(String category) {
		this.category = category;
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public void setImageName(ImageName imageName) {
		this.imageName = imageName;
	}
	
	public ImageName getImageName() {
		return imageName;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

	public String getQualifiedName() {
		return format("%s-%s-%s-%s-%s-%s-%s-%s.%s",
					  getOrganization(),
					  getCategory(),
					  getElementRoleName(),
					  getImageName(),
					  getVendorName(),
					  getModelName(),
					  getImageType(),
					  getImageVersion(),
					  getImageExtension());	
	}

	public ElementRoleName getElementRoleName() {
		return getElementRole().getRoleName();
	}

}
