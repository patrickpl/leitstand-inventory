/*
 * (c) RtBrick, Inc. - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.service;

import static io.leitstand.commons.model.BuilderUtil.assertNotInvalidated;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.json.bind.annotation.JsonbProperty;

import io.leitstand.commons.model.ValueObject;

/**
 * Contains the properties of a physical interface.
 */
public class ElementPhysicalInterfaceData extends ValueObject {

	/**
	 * Returns a builder to create an immutable <code>ElementPhysicalInterfaceData</code> instance.
	 * @return a builder to create an immutable <code>ElementPhysicalInterfaceData</code> instance.
	 */
	public static Builder newPhysicalInterfaceData(){
		return new Builder();
	}
	
	/**
	 * The builder to create an immutable <code>ElementPhysicalInterfaceData</code> instance.
	 */
	public static class Builder {
		
		private ElementPhysicalInterfaceData data = new ElementPhysicalInterfaceData();
		
		/**
		 * Sets the physical interface name.
		 * @param name - the interface name
		 * @return a reference to this builder to continue object creation
		 */
		public Builder withIfpName(InterfaceName name){
			assertNotInvalidated(getClass(), data);
			data.name = name;
			return this;
		}
		
		public Builder withIfpAlias(String ifpAlias) {
			assertNotInvalidated(getClass(), data);
			data.ifpAlias = ifpAlias;
			return this;
		}

		public Builder withIfpClass(String ifpClass) {
			assertNotInvalidated(getClass(), data);
			data.ifpClass = ifpClass;
			return this;
		}
		
		/**
		 * Sets the bandwidth of the physical interface name.
		 * @param bandwidth - the interface bandwidth
		 * @return a reference to this builder to continue object creation
		 */
		public Builder withBandwidth(Bandwidth bandwidth) {
			assertNotInvalidated(getClass(), data);
			data.bandwidth = bandwidth;
			return this;
		}
		
		/**
		 * Sets the configured MTU size
		 * @param mtuSize - the MTU size
		 * @return a reference to this builder to continue object creation
		 */
		public Builder withMtuSize(int mtuSize) {
			assertNotInvalidated(getClass(), data);
			data.mtuSize = mtuSize;
			return this;
		}
		
		/**
		 * Sets the MAC address of the physical interface
		 * @param macAddress - the MAC address
		 * @return a reference to this builder to continue object creation
		 */
		public Builder withMacAddress(MACAddress macAddress){
			assertNotInvalidated(getClass(), data);
			data.macAddress = macAddress;
			return this;
		}
		
		/**
		 * Sets the operational state of the physical interface
		 * @param opState - the operational state
		 * @return a reference to this builder to continue object creation
		 */
		public Builder withOperationalState(OperationalState opState){
			assertNotInvalidated(getClass(), data);
			data.operationalState = opState;
			return this;
		}

		/**
		 * Sets the administrative state of the physical interface
		 * @param admState - the administrative state
		 * @return a reference to this builder to continue object creation
		 */
		public Builder withAdministrativeState(AdministrativeState admState){
			assertNotInvalidated(getClass(), data);
			data.administrativeState = admState;
			return this;
		}

		/**
		 * Sets the names of all logical interfaces defined on top of this physical interface.
		 * @param ifcs - the logical interface names
		 * @return a reference to this builder to continue with object creation
		 */
		public Builder withLogicalInterfaces(InterfaceName... ifcs){
			return withLogicalInterfaces(asList(ifcs));
		}

		/**
		 * Sets the names of all logical interfaces defined on top of this physical interface.
		 * @param ifcs - the logical interface names
		 * @return a reference to this builder to continue with object creation
		 */
		public Builder withLogicalInterfaces(Collection<InterfaceName> ifcs){
			assertNotInvalidated(getClass(), data);
			data.logicalInterfaces = unmodifiableSet(new TreeSet<>(ifcs));
			return this;
		}
		
		/**
		 * Sets the neighbor interface of this physical interface.
		 * @param neighbor - the neighbor interface
		 * @return a reference to this builder to continue with object creation
		 */
		public Builder withNeighbor(ElementPhysicalInterfaceNeighbor.Builder neighbor) {
			return withNeighbor(neighbor.build());
		}
		
		/**
		 * Sets the neighbor interface of this physical interface.
		 * @param neighbor - the neighbor interface
		 * @return a reference to this builder to continue with object creation
		 */
		public Builder withNeighbor(ElementPhysicalInterfaceNeighbor neighbor) {
			assertNotInvalidated(getClass(), data);
			data.neighbor = neighbor;
			return this;
		}

		/**
		 * Returns an immutable <code>ElementPhysicalInterfaceData</code> instance and invalidates this builder.
		 * All further interactions with this builder raises an exception.
		 * @return an immutable <code>ElementPhysicalInterfaceData</code> instance.
		 */
		public ElementPhysicalInterfaceData build(){
			try{
				assertNotInvalidated(getClass(), data);
				return data;
			} finally {
				this.data = null;
			}
		}
	}
	
	
	@JsonbProperty("ifp_name")
	private InterfaceName name;
	
	@JsonbProperty("bandwidth")
	private Bandwidth bandwidth;
	
	@JsonbProperty("mac_address")
	private MACAddress macAddress;
	@JsonbProperty("operational_state")
	private OperationalState operationalState;
	@JsonbProperty("administrative_state")
	private AdministrativeState administrativeState;
	
	@JsonbProperty("mtu_size")
	private int mtuSize;
	
	@JsonbProperty("ifl_names")
	private Set<InterfaceName> logicalInterfaces;
	
	private String ifpAlias;
	private String ifpClass;
	
	private ElementPhysicalInterfaceNeighbor neighbor;
	
	/**
	 * Returns the neighbor interface of this physical interface or <code>null</code> if no neighbor information is available.
	 * @return the neighbor interface 
	 */
	public ElementPhysicalInterfaceNeighbor getNeighbor() {
		return neighbor;
	}
	
	/**
	 * Returns the physical interface name.
	 * @return the physical interface name.
	 */
	public InterfaceName getName(){
		return name;
	}
	
	/**
	 * Retruns the MAC address of the physical interface.
	 * @return the MAC address
	 */
	public MACAddress getMacAddress(){
		return macAddress;
	}
	
	/**
	 * Returns the operational state of the physical interface.
	 * @return the operational state of the physical interface.
	 */
	public OperationalState getOperationalState() {
		return operationalState;
	}
	
	/**
	 * Returns the administrative state of the physical interface.
	 * @return the administrative state of the physical interface.
	 */
	public AdministrativeState getAdministrativeState() {
		return administrativeState;
	}
	
	/**
	 * Returns the names of all logical interfaces defined on this physical interface.
	 * Returns an empty set, if no logical interfaces exist on this physical interface.
	 * @return the logical interface names.
	 */
	public Set<InterfaceName> getLogicalInterfaces(){
		return unmodifiableSet(logicalInterfaces);
	}
	
	/**
	 * Returns the configured MTU size.
	 * @return the MTU size
	 */
	public int getMtuSize() {
		return mtuSize;
	}
	
	/**
	 * Returns the bandwidth of the physical interface.
	 * @return the bandwidth
	 */
	public Bandwidth getBandwidth() {
		return bandwidth;
	}
	
	public String getIfpAlias() {
		return ifpAlias;
	}
	
	public String getIfpClass() {
		return ifpClass;
	}
}
