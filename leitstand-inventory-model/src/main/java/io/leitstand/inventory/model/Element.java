/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.inventory.service.AdministrativeState.ACTIVE;
import static io.leitstand.inventory.service.AdministrativeState.NEW;
import static io.leitstand.inventory.service.OperationalState.OPERATIONAL;
import static io.leitstand.inventory.service.OperationalState.UP;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static javax.persistence.CascadeType.PERSIST;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import io.leitstand.commons.model.Query;
import io.leitstand.commons.model.VersionableEntity;
import io.leitstand.inventory.jpa.AdministrativeStateConverter;
import io.leitstand.inventory.jpa.ElementAliasConverter;
import io.leitstand.inventory.jpa.ElementNameConverter;
import io.leitstand.inventory.jpa.MACAddressConverter;
import io.leitstand.inventory.jpa.OperationalStateConverter;
import io.leitstand.inventory.service.AdministrativeState;
import io.leitstand.inventory.service.ElementAlias;
import io.leitstand.inventory.service.ElementGroupId;
import io.leitstand.inventory.service.ElementGroupName;
import io.leitstand.inventory.service.ElementGroupType;
import io.leitstand.inventory.service.ElementId;
import io.leitstand.inventory.service.ElementManagementInterface;
import io.leitstand.inventory.service.ElementName;
import io.leitstand.inventory.service.ElementRoleName;
import io.leitstand.inventory.service.MACAddress;
import io.leitstand.inventory.service.OperationalState;
import io.leitstand.inventory.service.Plane;

@Entity
@Table(schema="inventory", name="element")
@NamedQuery(name="Element.findByElementId", 
			query="SELECT e FROM Element e WHERE e.uuid=:id")
@NamedQuery(name="Element.findByElementName", 
			query="SELECT e FROM Element e WHERE e.elementName=:name OR e.elementAlias=:alias")
@NamedQuery(name="Element.findByElementGroupAndElementRoleAndPlatform",
			query="SELECT e FROM Element e WHERE e.group=:group AND e.role=:role AND e.platform=:platform")
@NamedQuery(name="Element.findByElementGroupAndElementRole",
			query="SELECT e FROM Element e WHERE e.group=:group AND e.role=:role")
@NamedQuery(name="Element.findByElementNamePattern", 
			query="SELECT e FROM Element e WHERE CONCAT('',e.elementName) REGEXP :name  OR  CONCAT('',e.elementAlias) REGEXP :name  ORDER by e.group.name ASC, e.elementName ASC")
@NamedQuery(name="Element.findByElementGroupAndPlane",
			query="SELECT e FROM Element e WHERE e.group=:group AND e.role.plane=:plane")

public class Element extends VersionableEntity {
	private static final long serialVersionUID = 1L;
	
	public static Query<Element> findElementById(ElementId id) {
	    return em -> em.createNamedQuery("Element.findByElementId",Element.class)
					   .setParameter("id",id.toString())
					   .getSingleResult();
	}
	
	public static Query<Element> findElementByName(ElementName name) {
	    return em -> em.createNamedQuery("Element.findByElementName",Element.class)
					   .setParameter("name",name)
					   .setParameter("alias", ElementAlias.valueOf(name))
					   .getSingleResult();
	}

	public static Query<List<Element>> findElementsByName(String pattern, 
														  int offset, 
														  int limit){
		return em -> em.createNamedQuery("Element.findByElementNamePattern",Element.class)
					   .setParameter("name", pattern)
					   .setFirstResult(offset)
					   .setMaxResults(limit)
					   .getResultList();
	}
	
	public static Query<List<Element>> findElementsByGroupAndElementRoleAndPlatform(ElementGroup group, 
																					ElementRole role, 
																					Platform platform) {
		return em -> em.createNamedQuery("Element.findByElementGroupAndElementRoleAndPlatform",Element.class)
					   .setParameter("group", group)
					   .setParameter("role", role)
					   .setParameter("platform", platform)
					   .getResultList();
	}
	
	public static Query<List<Element>> findByGroupAndRole(ElementGroup group, 
														  ElementRole role) {
		return em -> em.createNamedQuery("Element.findByElementGroupAndElementRole",Element.class)
					   .setParameter("group", group)
					   .setParameter("role", role)
					   .getResultList();
	}
	
	
	public static Query<List<Element>> findByGroupAndPlane(ElementGroup group, 
														   Plane plane){
		return em -> em.createNamedQuery("Element.findByElementGroupAndPlane",Element.class)
					   .setParameter("group", group)
					   .setParameter("plane", plane)
					   .getResultList();
	}
	
	
	@Column(name="adm_state")
	@Convert(converter=AdministrativeStateConverter.class)
	private AdministrativeState admState;

	@Column(length=1024)
	private String description;
	
	@Column(name="name", length=64, unique=true)
	@Convert(converter=ElementNameConverter.class)
	private ElementName elementName;

	@Column(name="alias", length=64, unique=true)
	@Convert(converter=ElementAliasConverter.class)
	private ElementAlias elementAlias;
	
	
	@ElementCollection
	@CollectionTable(schema="inventory",
					 name="element_management_interface", 
					 joinColumns=@JoinColumn(name="element_id"))	
	@MapKey(name="name")
	private Map<String,ElementManagementInterface> managementInterfaces;
	
	
	@Column(name="op_state")
	@Convert(converter=OperationalStateConverter.class)
	private OperationalState opState;
	
	@ManyToOne(cascade=PERSIST)
	@JoinColumn(name="platform_id")
	private Platform platform;
	
	@ManyToOne(cascade=PERSIST)
	@JoinColumn(name="elementgroup_id")
	private ElementGroup group;
	
	@ManyToOne(cascade=PERSIST)
	@JoinColumn(name="elementrole_id")
	private ElementRole role;
	
	@Column(name="serial")
	private String serialNumber;
	
	@Column(name="mgmt_mac")
	@Convert(converter=MACAddressConverter.class)
	private MACAddress mgmtMacAddress;

	
	@ElementCollection
	@CollectionTable(schema="inventory", 
					 name="element_tag", 
					 joinColumns=@JoinColumn(name="element_id"))
	@Column(name="tag")
	private Set<String> tags;
	
	protected Element(){
		// JPA
	}

	public Element(ElementGroup group, 
	               ElementRole role, 
	               ElementId elementId, 
	               ElementName elementName){
		super(elementId.toString());
		this.tags = new TreeSet<String>();
		this.group = group;
		this.role = role;
		this.elementName = elementName;
		this.opState = OperationalState.DOWN;
		this.admState = AdministrativeState.NEW;
		this.tags = new HashSet<>();
		this.managementInterfaces = new HashMap<>();
		group.add(this);
	}

	
	protected void addManagementInterface(ElementManagementInterface managementInterface) {
		managementInterfaces.put(managementInterface.getName(), 
								 managementInterface);
	}
	
	protected void addTag(String tag){
		tags.add(tag);
	}
		
	public AdministrativeState getAdministrativeState() {
		return admState;
	}
	
	public String getDescription() {
		return description;
	}

	public ElementId getElementId() {
		return new ElementId(getUuid());
	}
	
	public ElementName getElementName() {
		return elementName;
	}
	
	public ElementRole getElementRole() {
		return role;
	}

	public ElementRoleName getElementRoleName() {
		return getElementRole().getRoleName();
	}
	
	public ElementManagementInterface getManagementInterface(String name) {
		return managementInterfaces.get(name);
	}

	public Map<String,ElementManagementInterface> getManagementInterfaces(){
		return unmodifiableMap(managementInterfaces);
	}

	public String getModelName() {
		if(platform == null) {
			return null;
		}
		return platform.getModel();
	}

	public OperationalState getOperationalState() {
		return opState;
	}

	public Platform getPlatform() {
		return platform;
	}

	public ElementGroup getGroup() {
		return group;
	}
	

	public String getSerialNumber() {
		return serialNumber;
	}
	
	public void setManagementInterfaceMacAddress(MACAddress mgmtMacAddress) {
		this.mgmtMacAddress = mgmtMacAddress;
	}
	
	public MACAddress getManagementInterfaceMacAddress() {
		return mgmtMacAddress;
	}

	public Set<String> getTags() {
		return unmodifiableSet(tags);
	}
	
	public String getVendorName() {
		if(platform == null) {
			return null;
		}
		return platform.getVendor();
	}

	public boolean hasElementId(ElementId elementId) {
		return Objects.equals(getElementId(), elementId);
	}

	public boolean hasTag(String tag){
		return tags.contains(tag);
	}
	
	public boolean isActive() {
		return getAdministrativeState().is(ACTIVE);
	}
	
	public boolean isElementOfPlane(Plane plane){
		return getElementRole().getPlane() == plane;
	}

	public boolean isOperational() {
		return getOperationalState().is(OPERATIONAL) || getOperationalState().is(UP);
	}
	
	protected void removeManagementInterface(ElementManagementInterface managementInterface) {
		managementInterfaces.remove(managementInterface.getName()); 
	}
	
	protected void removeManagementInterface(String name) {
		managementInterfaces.remove(name); 
	}
	
	
	protected void removeTag(String tag){
		tags.remove(tag);
	}
	
	protected void setAdministrativeState(AdministrativeState admState){
		this.admState = admState;
	}

	protected void setDescription(String description) {
		this.description = description;
	}
	
	protected void setElementManagementInterfaces(Collection<ElementManagementInterface> interfaces) {
		this.managementInterfaces = interfaces.stream().collect(toMap(ElementManagementInterface::getName,identity()));
	}
	
	protected ElementName setElementName(ElementName name) {
		ElementName old = this.elementName;
		this.elementName = name;
		return old;
	}
	
	protected OperationalState setOperationalState(OperationalState opState){
		OperationalState old = this.opState;
		this.opState = opState;
		if((opState.is(OPERATIONAL) || opState.is(UP)) && admState.is(NEW)){
			admState = ACTIVE;
		}
		return old;
	}

	protected void setPlatform(Platform platform) {
		this.platform = platform;
	}

	protected void setGroup(ElementGroup group) {
		this.group.remove(this);
		this.group = group;
		this.group.add(this);
	}
	
	protected void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}
	
	protected void setTags(Set<String> tags){
		this.tags.clear();
		if(tags!=null) {
			this.tags.addAll(tags);
		}
	}

	public ElementGroupId getGroupId() {
		return getGroup().getGroupId();
	}

	public ElementGroupName getGroupName() {
		return getGroup().getGroupName();
	}
	
	public ElementGroupType getGroupType() {
		return getGroup().getGroupType();
	}

	public void setElementRole(ElementRole role) {
		this.role = role;
	}

	public void setElementAlias(ElementAlias elementAlias) {
		this.elementAlias = elementAlias;
	}
	
	public ElementAlias getElementAlias() {
		return elementAlias;
	}


	
}
