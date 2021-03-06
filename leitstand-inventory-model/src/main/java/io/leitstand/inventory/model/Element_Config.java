/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.model;

import static io.leitstand.inventory.service.ConfigurationState.ACTIVE;
import static io.leitstand.inventory.service.ConfigurationState.CANDIDATE;
import static io.leitstand.inventory.service.ElementConfigId.randomConfigId;
import static javax.persistence.EnumType.STRING;
import static javax.persistence.FetchType.LAZY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.ws.rs.core.MediaType;

import io.leitstand.commons.model.Query;
import io.leitstand.commons.model.Update;
import io.leitstand.inventory.jpa.ElementConfigIdConverter;
import io.leitstand.inventory.jpa.ElementConfigNameConverter;
import io.leitstand.inventory.service.ConfigurationState;
import io.leitstand.inventory.service.ElementConfigId;
import io.leitstand.inventory.service.ElementConfigName;
import io.leitstand.security.auth.UserId;
import io.leitstand.security.auth.jpa.UserIdConverter;

@Entity
@Table(schema="inventory", name="element_config")
@NamedQuery(name="Element_Config.removeConfigRevisions",
			query="DELETE FROM Element_Config c WHERE c.element=:element AND c.name=:configName AND c.state != io.leitstand.inventory.service.ConfigurationState.ACTIVE")	
@NamedQuery(name="Element_Config.removeAll",
			query="DELETE FROM Element_Config c WHERE c.element=:element")	
@NamedQuery(name="Element_Config.findLatestConfig",
			query="SELECT c FROM Element_Config c WHERE c.element=:element AND c.name=:configName ORDER BY c.tsmodified DESC")
@NamedQuery(name="Element_Config.findActiveConfig",
			query="SELECT c FROM Element_Config c WHERE c.element=:element AND c.name=:configName AND c.state=io.leitstand.inventory.service.ConfigurationState.ACTIVE")
public class Element_Config implements Serializable {

	private static final long serialVersionUID = 1L;

	public static Query<Element_Config> findElementConfig(ElementConfigId configId){
		return em -> em.find(Element_Config.class, configId);
	}

	public static Query<Element_Config> findLatestConfig(Element element, 
														 ElementConfigName name){
		return em -> em.createNamedQuery("Element_Config.findLatestConfig",Element_Config.class)
					   .setParameter("element", element)
					   .setParameter("configName", name)
					   .setMaxResults(1)
					   .getSingleResult();
	}
	
	public static Query<Element_Config> findActiveConfig(Element element, 
				 										 ElementConfigName name){
		return em -> em.createNamedQuery("Element_Config.findActiveConfig",Element_Config.class)
					   .setParameter("element", element)
					   .setParameter("configName", name)
					   .getSingleResult();
	}
	
	public static Update removeAllConfigurations(Element element) {
		return em -> em.createNamedQuery("Element_Config.removeAll",int.class)
					   .setParameter("element",element)
					   .executeUpdate();
	}
	
	public static Update removeConfigRevisions(Element element, 
											   ElementConfigName configName) {
		return em -> em.createNamedQuery("Element_Config.removeConfigRevisions", int.class)
					   .setParameter("element", element)
					   .setParameter("configName",configName)
					   .executeUpdate();
	}

	@Id
	@Convert(converter=ElementConfigIdConverter.class)
	@Column(name="uuid")
	private ElementConfigId configId;
	
	@ManyToOne
	@JoinColumn(name="element_id")
	private Element element;
	
	@Convert(converter=ElementConfigNameConverter.class)
	private ElementConfigName name;
	
	@Temporal(TIMESTAMP)
	private Date tsmodified;

	private String contentHash;
	private String contentType;
	@Enumerated(STRING)
	private ConfigurationState state;
	@Basic(fetch=LAZY)
	private String config;
	private String comment;
	@Convert(converter=UserIdConverter.class)
	private UserId creator;
	
	protected Element_Config(){
		// JPA
	}
	
	public Element_Config(Element element, 
						  ElementConfigName name, 
						  ConfigurationState configState,
						  MediaType contentType,
						  String contentHash,
						  String config,
						  UserId creator){
		this(element,
			 name,
			 configState,
			 contentType.toString(),
			 contentHash,
			 config,
			 creator);
	}
	
	public Element_Config(Element element, 
			  			  ElementConfigName name, 
						  ConfigurationState configState,
						  String contentType,
			  			  String contentHash,
			  			  String config,
			  			  UserId creator){
		this.configId = randomConfigId();
		this.element = element;
		this.name = name;
		this.contentType = contentType;
		this.contentHash = contentHash;
		this.state = configState;
		this.config = config;
		this.creator = creator;
		this.tsmodified = new Date();
	}
	
	public ElementConfigName getName() {
		return name;
	}
	
	public Element getElement() {
		return element;
	}
	
	public String getConfig() {
		return config;
	}
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setContentType(MediaType contentType) {
		setContentType(contentType.toString());
	}

	public void setConfigState(ConfigurationState state) {
		this.state = state;
	}
	
	public ConfigurationState getConfigState() {
		return state;
	}
	
	public String getContentHash() {
		return contentHash;
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getComment() {
		return comment;
	}
	
	public void setConfig(String config) {
		this.config = config;
		this.tsmodified = new Date();
	}
	
	public Date getDateModified() {
		return new Date(tsmodified.getTime());
	}

	public boolean isJsonConfig() {
		return "application/json".equalsIgnoreCase(getContentType());
	}
	
	public boolean isSameContentHash(String contentHash){
		return Objects.equals(this.contentHash, contentHash);
	}

	public boolean isCandidateConfig() {
		return getConfigState() == CANDIDATE;
	}
	
	public boolean isActiveConfig() {
		return getConfigState() == ACTIVE;
	}
	
	public ElementConfigId getConfigId() {
		return configId;
	}

	public void setContentHash(String contentHash) {
		this.contentHash = contentHash;
	}
	
	public UserId getCreator() {
		return creator;
	}
}