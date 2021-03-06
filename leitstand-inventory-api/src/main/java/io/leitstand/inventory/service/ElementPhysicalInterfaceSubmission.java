/*
 * (c) RtBrick, Inc. - All rights reserved, 2015 - 2019
 */
package io.leitstand.inventory.service;

import static io.leitstand.commons.model.BuilderUtil.assertNotInvalidated;

import javax.json.bind.annotation.JsonbProperty;

import io.leitstand.commons.model.ValueObject;

/**
 * A submission to store a new physical interface of a certain element in the resource inventory.
 */

public class ElementPhysicalInterfaceSubmission extends ValueObject {
	
	public static Builder newPhysicalInterfaceSubmission() {
		return new Builder();
	}
	
	public static class Builder {
		
		private ElementPhysicalInterfaceSubmission submission = new ElementPhysicalInterfaceSubmission();
		
		public Builder withIfpName(InterfaceName ifpName) {
			assertNotInvalidated(getClass(), submission);
			submission.ifpName = ifpName;
			return this;
		}
		
		public Builder withIfpAlias(String alias) {
			assertNotInvalidated(getClass(), submission);
			submission.ifpAlias = alias;
			return this;
		}
		
		public Builder withIfpClass(String ifpClass) {
			assertNotInvalidated(getClass(), submission);
			submission.ifpClass = ifpClass;
			return this;
		}
		
		public Builder withBandwidth(Bandwidth bandwidth) {
			assertNotInvalidated(getClass(), submission);
			submission.bandwidth = bandwidth;
			return this;
		}
		
		public Builder withMtuSize(int mtuSize) {
			assertNotInvalidated(getClass(), submission);
			submission.mtuSize = mtuSize;
			return this;
		}
		
		public Builder withMacAddress(MACAddress macAddress) {
			assertNotInvalidated(getClass(), submission);
			submission.macAddress = macAddress;
			return this;
		}
		
		public Builder withOperationalState(OperationalState operationalState) {
			assertNotInvalidated(getClass(), submission);
			submission.operationalState = operationalState;
			return this;
		}
		
		public Builder withAdministrativeState(AdministrativeState administrativeState) {
			assertNotInvalidated(getClass(), submission);
			submission.administrativeState = administrativeState;
			return this;
		}
		
		public Builder withIfcName(InterfaceName ifcName) {
			assertNotInvalidated(getClass(), submission);
			submission.ifcName = ifcName;
			return this;
		}
		
		public Builder withNeighbor(ElementPhysicalInterfaceNeighbor.Builder neighbor) {
			return withNeighbor(neighbor.build());
		}
		
		public Builder withNeighbor(ElementPhysicalInterfaceNeighbor neighbor) {
			assertNotInvalidated(getClass(), submission);
			submission.neighbor = neighbor;
			return this;
		}
		
		public ElementPhysicalInterfaceSubmission build() {
			try {
				assertNotInvalidated(getClass(), submission);
				return submission;
			} finally {
				this.submission = null;
			}
		}
		
	}
	

	@JsonbProperty("ifp_name")
	private InterfaceName ifpName;
	
	private String ifpAlias;
	
	private String ifpClass;

	@JsonbProperty("bandwidth")
	private Bandwidth bandwidth;

	@JsonbProperty("mtu_size")
	private int mtuSize;
	
	@JsonbProperty("mac_address")
	private MACAddress macAddress;
	
	@JsonbProperty("operational_state")
	private OperationalState operationalState;
	
	@JsonbProperty("administrative_state")
	private AdministrativeState administrativeState;
	
	@JsonbProperty("ifc_name")
	private InterfaceName ifcName;
	
	private ElementPhysicalInterfaceNeighbor neighbor;
	
	/**
	 * Returns the physical interface name.
	 * @return the physical interface name.
	 */
	public InterfaceName getIfpName(){
		return ifpName;
	}
	
	/**
	 * Returns the MAC address of the physical interface
	 * @return the physical interface MAC address
	 */
	public MACAddress getMacAddress(){
		return macAddress;
	}
	
	/**
	 * Returns the operational state of the physical interface.
	 * @return the operational state
	 */
	public OperationalState getOperationalState() {
		return operationalState;
	}
	
	/**
	 * Returns the container interface name.
	 * <p>
	 * Multiple physical interfaces can be bundled to a single interface, such that 
	 * logical interfaces can leverage multiple physical interfaces. 
	 * Consequently, the container interface contains either one or multiple physical interfaces which
	 * means that different physical interfaces can refer to the same container interface.
	 * </p>
	 * @return the container interface name.
	 */
	public InterfaceName getIfcName() {
		if(ifcName == null && ifpName != null) {
			return new InterfaceName(ifpName+"/0");
		}
		return ifcName;
	}
	
	/**
	 * Returns the bandwidth of the physical interface.
	 * @return the bandwidth
	 */
	public Bandwidth getBandwidth() {
		return bandwidth;
	}
	
	/**
	 * Returns the adminstrative state of the physical interface.
	 * @return the administrative state
	 */
	public AdministrativeState getAdministrativeState() {
		return administrativeState;
	}
	
	/**
	 * Returns the configure MTU size.
	 * @return the MTU size.
	 */
	public int getMtuSize() {
		return mtuSize;
	}

	public String getIfpAlias() {
		return ifpAlias;
	}
	
	public String getIfpClass() {
		return ifpClass;
	}
	
	public ElementPhysicalInterfaceNeighbor getNeighbor() {
		return neighbor;
	}
	
}
