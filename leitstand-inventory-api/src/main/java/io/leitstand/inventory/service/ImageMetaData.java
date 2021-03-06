/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.service;

import static java.util.Collections.unmodifiableList;

import java.util.LinkedList;
import java.util.List;

import javax.json.bind.annotation.JsonbProperty;

import io.leitstand.commons.model.ValueObject;


public class ImageMetaData extends ValueObject{


	public static Builder newImageMetaData() {
		return new Builder();
	}
	
	public static class Builder {
		
		private ImageMetaData metadata = new ImageMetaData();
		
		public Builder withPlatforms(List<ElementPlatformInfo> platforms ) {
			metadata.platforms = unmodifiableList(new LinkedList<>(platforms));
			return this;
		}
		
		public Builder withImageTypes(List<ImageType> types) {
			metadata.types = unmodifiableList(new LinkedList<>(types));
			return this;
		}
		
		public Builder withElementRoles(List<ElementRoleInfo> roles) {
			metadata.roles = unmodifiableList(new LinkedList<>(roles));
			return this;
		}
		
		public ImageMetaData build() {
			try {
				return metadata;
			} finally {
				this.metadata = null;
			}
		}
	}
	
	
	private List<ElementPlatformInfo> platforms;
	@JsonbProperty("image_types")
	private List<ImageType> types;
	@JsonbProperty("element_roles")
	private List<ElementRoleInfo> roles;
	
	
	public List<ElementRoleInfo> getRoles() {
		return unmodifiableList(roles);
	}
	
	public List<ElementPlatformInfo> getPlatforms() {
		return unmodifiableList(platforms);
	}
	
	public List<ImageType> getTypes() {
		return unmodifiableList(types);
	}
	
}
