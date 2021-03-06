/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.service;

import java.util.List;

import javax.persistence.EntityNotFoundException;

/**
 * A stateless and transaction service to manage the images on an element.
 */
public interface ElementImagesService {

	/**
	 * Returns the available information for the specified image on the specified element.
	 * Returns <code>null</code> if the image is not present on the element.
	 * @param elementId - the element ID
	 * @param imageId - the image ID
	 * @return the image information for the specified image on the specified element. 
	 * @throws EntityNotFoundException if the specified element or the specified image does not exist.
	 */
	ElementInstalledImage getElementInstalledImage(ElementId elementId, ImageId imageId);

	/**
	 * Returns the available information for the specified image on the specified element.
	 * Returns <code>null</code> if the image is not present on the element.
	 * @param name - the element name
	 * @param imageId - the image ID
	 * @return the image information for the specified image on the specified element. 
	 * @throws EntityNotFoundException if the specified element or the specified image does not exist.
	 */
	ElementInstalledImage getElementInstalledImage(ElementName name, ImageId imageId);
	
	/**
	 * Returns informations of all images available on the element.
	 * Returns an empty list if no image information is available.
	 * @param id - the element ID
	 * @return the image information for the specified image on the specified element. 
	 * @throws EntityNotFoundException if the specified element does not exist.
	 */
	ElementInstalledImages getElementInstalledImages(ElementId id);

	/**
	 * Returns informations of all images available on the element.
	 * Returns an empty list if no image information is available.
	 * @param name - the element name
	 * @return the image information for the specified image on the specified element. 
	 * @throws EntityNotFoundException if the specified element does not exist.
	 */
	ElementInstalledImages getElementInstalledImages(ElementName name);

	/**
	 * Updates the information of installed images on the element. 
	 * This includes both, the currently active images as well as cached images 
	 * that could be activated instantly.
	 * @param id - the element id
	 * @param images - all images installed on the element.
	 */
	void storeInstalledImages(ElementId id, List<ElementInstalledImageReference> images);

	/**
	 * Updates the information of installed images on the element. 
	 * This includes both, the currently active images as well as cached images 
	 * that could be activated instantly.
	 * @param id - the element name
	 * @param images - all images installed on the element.
	 */
	void storeInstalledImages(ElementName name, List<ElementInstalledImageReference> images);

	/**
	 * Adds the specified images to the list of <em>cached</em> images on the element. 
	 * Cached images are inactive, but could be activated instantly.
	 * @param id - the element id
	 * @param images - add cached images
	 */
	void addCachedImages(ElementId id, List<ElementInstalledImageReference> images);

	/**
	 * Adds the specified images to the list of <em>cached</em> images on the element. 
	 * Cached images are inactive, but could be activated instantly.
	 * @param name - the element name
	 * @param images - all images installed on the element.
	 */
	void addCachedImages(ElementName name, List<ElementInstalledImageReference> images);
	
	/**
	 * Removes the specified images from the list of <em>cached</em> images.
	 * Cached images are inactive, but could be activated instantly.
	 * @param id - the element id
	 * @param images - all images installed on the element.
	 */
	void removeCachedImages(ElementId id, List<ElementInstalledImageReference> images);
	
	
	/**
	 * Removes the specified images from the list of <em>cached</em> images.
	 * Cached images are inactive, but could be activated instantly.
	 * @param id - the element name
	 * @param images - all images installed on the element.
	 */
	void removeCachedImages(ElementName name, List<ElementInstalledImageReference> images);
	
}