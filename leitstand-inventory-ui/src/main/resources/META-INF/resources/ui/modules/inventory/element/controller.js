/*
 *  (c) RtBrick, Inc - All rights reserved, 2015 - 2017
 */
import {Json} from '/ui/js/client.js';
import {Controller,Menu} from '/ui/js/ui.js';
import {Select} from '/ui/js/ui-components.js';
import {units} from '/ui/js/widgets.js';
import {Metadata,Element,Pod,ElementPhysicalInterfaces,ElementPhysicalInterface,ElementLogicalInterfaces,ElementLogicalInterface,Connector,Platforms} from '/ui/modules/inventory/inventory.js';
import {Event,Events} from '/ui/modules/event/events.js';


class PlatformSelector extends Select {

	isSelected(option){
		let platform = this.viewModel.getProperty(this.binding);
		if(!platform){
			return option['default'];
		}
		return option == `[${platform.vendor_name}][${platform.model_name}]`;
	}

	select(option){
		let segments = /\[(.*)\]\[(.*)\]/.exec(option);
		let vendor = segments[1];
		let model  = segments[2];
		this.value = {"vendor_name":vendor,"model_name":model};
	}
	
}


customElements.define('element-platform',PlatformSelector);

//TODO: Implement Rack Component!


let elementRackController = function(){
	
	var Rack = function(rack){
		
		let units = [];
		let elements = {};
		
		for(let i = 0; i < rack.units; i++){
			units.push({
				"unit": ((100+i+1)+"").substring(1),
				"elements":[]
			});
		}			
		
		for(let i = 0; i < rack.elements.length; i++){
			let element = {
				"element":rack.elements[i],
				"selected":rack.elements[i].element_name == rack.element_name ? "selected" :  "",
				"height":rack.elements[i].height,
				"size":function(){
					if (this.element.half_rack){
						if(this.element.half_rack_pos == "LEFT"){
							return "half_rack left";
						}
						return "half_rack right";
					}
				} 		
			};
			units[rack.elements[i].unit-2+rack.elements[i].height].elements.push(element);
		}			
		
		this.units = function(){
			units.reverse();
			return units;
		}
	};
	
	let element = new Element({"scope":"rack"});
	return new Controller({
		resource:element,
		viewModel:function(settings){
			settings.rack = this.transient(new Rack(settings));
			return settings;
		},
		onNotFound : function(){
			this.navigate({"view":"new-element-location.html",
						   "?":this.location().params()});
		},
		buttons:{
			"save-rack":function(){
				let location = { "rack_name":this.input("rack_name").value(),
						  		 "rack_position":this.input("rack_position").value(),
						  		 "address":this.input("address").value()};
				let settings = this.updateViewModel({"location":location});
				element.saveSettings(this.location().params(),
				                     settings);
			}
		}
	});
};

let addElementLocationController = function(){
	let element = new Element({"scope":"settings"});
	return new Controller({
		resource:element,
		viewModel:async function(settings){
			let racks = new Pod({"scope":"racks"});
			settings.racks = await racks.load(this.location().params());
			return settings;
		},
		buttons:{
			"save-location":function(){
				let location = {"rack_name":this.input("rack_name").value(),
								  "unit":this.input("unit").value()};
				let rack = new Element({"scope":"rack"});
				this.attach(rack);
				rack.saveSettings(this.location().params(),
								  location);
			}
		},
		onSuccess : function(){
			this.navigate({"view":"element-rack.html",
						   "?": this.location().params()});
		}
	});
};

let elementMountPointController = function(){
	let element = new Element({"scope":"rack"});
	return new Controller({
		resource:element,
		init: function(settings){ 
			
			let racks = new Pod({"scope":"racks"});
			
			
			racks.onSuccess = this.newEventHandler(function(racks){
				
				let element = (function() {
					for(let i=0; i < settings.elements.length; i++){
					  if(settings.elements[i].element_id == settings.element_id){
						  return settings.elements[i];
					  }
				  }
				})();
				
				let half_rack_pos = [{"value":"LEFT",
									  "display_text":"Left side",
									  "selected":element.half_rack_pos == "LEFT" ? "selected" : ""},
									 {"value":"RIGHT",
									  "display_text":"Right side",
									  "selected":element.half_rack_pos == "RIGHT" ? "selected" : ""}];
				
				
				this.updateViewModel({"racks":racks.racks,
									  "unit": element.unit,
									  "half_rack":element.half_rack,
									  "positions":half_rack_pos,
									  "selected":function(){
										  return this.rack_name == settings.rack_name	? "selected" : "";}});
				this.render();
			});
			racks.load(this.location().params());
		},
		buttons:{
			"save-location":function(){
				let location = {"rack_name":this.input("rack_name").value(),
								  "unit":this.input("unit").value(),
								  "position":this.input("position").value()};
				let rack = new Element({"scope":"rack"});
				this.attach(rack);
				rack.saveSettings(this.location().params(),
								  location);
			},
			"remove-location":function(){
				let rack = new Element({"scope":"rack"});
				this.attach(rack);
				rack.remove(this.location().params());
			}
		},
		onSuccess:function(){
			this.navigate({"view":"element-rack.html",
						   "?":this.location().params()});
		}
	});
};

let elementLocationController = function(){
	let element = new Element({"scope":"settings"});
	return new Controller({
		resource:element,
		buttons:{
			"save-location":function(){
				let location = { "rack_name":this.input("rack_name").value(),
						  		 "rack_position":this.input("rack_position").value(),
						  		 "address":this.input("address").value()};
				let settings = this.updateViewModel({"location":location});
				element.saveSettings(this.location().params(),
				                     settings);
			}
		}
	});
};


let elementServiceController = function(){
	let element = new Element({"scope":"services"});
	return new Controller({
		resource:element,
		viewModel:function(model){
			//Expose top of stack to simplify UI template.
			model.service_name = this.transient(model.stack[0].service_name);
			model.display_name = this.transient(model.stack[0].display_name);
			model.operational_state = this.transient(model.stack[0].operational_state);
			return model;
		}
	});
};

// FIXME We need some means to pre-fetch resources 
let services = [];
(function(){
	let meta = new Json("/api/v1/services");
	meta.onUnauthenticated = function(){
		window.location.replace="/ui/login/login.html";
	};
	meta.onLoaded = function(response){
		response.forEach(service => services.push(service.name,service));
	};
	meta.load();
})();

let platforms = [];

/*
= (function(){
	let platforms = new Json("/api/v1/platforms");
	return platforms.loadSync();
})();
*/

let elementController = function(){
	let element = new Element({"scope":"settings"});
	return new Controller({
		resource:element,
		viewModel:async function(settings){
			
			let mgmt_interface_list = [];
			for(let mgmt_ifc_name in settings.mgmt_interfaces){
				mgmt_interface_list.push(settings.mgmt_interfaces[mgmt_ifc_name]);
			}
			
			let roles = new Metadata({'scope':'roles'});
			let platforms = new Platforms();
						
			let viewModel = settings;
			viewModel.element = settings;
			viewModel.roles = await roles.load();
			viewModel.roles = viewModel.roles.map(role => ({"value":role.role_name,"label":role.display_name}));
			viewModel.platforms = await platforms.load();
			viewModel.platforms = viewModel.platforms.map(platform => ({"value":`[${platform.vendor_name}][${platform.model_name}]`,"label":`${platform.vendor_name} ${platform.model_name}`}));
			viewModel.administrative_states = [{"value":"NEW",
												"label":"New"},
											   {"value":"ACTIVE",
												"label":"Active"},
											   {"value":"RETIRED",
												"label":"Retired"}];
			
			viewModel.operational_states = [{"value":"DOWN", 
											 "label": "Down"},		
										    {"value":"IMPAIRED", 
											 "label": "Impaired"}, 
										    {"value":"UP", 
											 "label": "Up"}, 
										    {"value":"DETACHED", 
											 "label": "Detached"}, 
										    {"value":"MAINTENANCE", 
											 "label": "Maintenance"}];
			
			viewModel.mgmt_interface_list = mgmt_interface_list;

			viewModel.inactive = function(){
				return settings.administrative_state != "ACTIVE";
			}
			return viewModel;
		},
		buttons:{
			"save-element":function(){
				let platform = this.input("platform").value();
				let segments = /\[(.*)\]\[(.*)\]/.exec(platform);
				let vendorName = segments[1];
				let modelName  = segments[2];
				
				//FIXME Select multivalue field
				
				
				let settings = this.updateViewModel({"element_name":this.input("element_name").value(),
													 "element_alias":this.input("element_alias").value() ? this.input("element_alias").value() : null,
													 "element_role":this.input("element_role").value(),
													 "description":this.input("description").value(),
													 "administrative_state":this.input("adm_state").value(),
													 "operational_state":this.input("op_state").value(),
													 "serial_number":this.input("serial_number").value(),
													 "platform":{
														"vendor_name":vendorName,
														"model_name":modelName
													 }
													});
				
				settings.mgmt_mac = this.input("mgmt_mac").value();
				
				element.saveSettings(this.location().params(),
				                     settings);
			},
			"remove-element":function(){
				let params = this.location().params();
				params["force"] = this.input("force").value();
				element.removeElement(params);
			},
			"add-mgmt":function(){
				this.navigate({"view" : "/ui/views/inventory/element/element-mgmt.html",
							   "?" : this.location().params()});
			}
		},
		onRemoved:function(){
			this.navigate({"view":"/ui/views/inventory/pod/pod-elements.html",
						    "?":{"group":this.location().param("group")}});
		},
		onSuccess:function(){
			this.reload();
		}
	});
};

let elementMgmtController = function(){

	let element = new Element({"scope":"settings"});
	
	return new Controller({
		resource:element,
		viewModel:function(settings){
			
			let mgmt_name = this.location().param("mgmt_name");
	
			let mgmt_ifc = settings.mgmt_interfaces[mgmt_name];
			if(!mgmt_ifc){
				mgmt_ifc = {};
			}
			//TODO Refactor to new UI component
			mgmt_ifc.mgmt_protocols = [{"label":"HTTP",
										"value":"http",
										"selected":(mgmt_ifc.mgmt_protocol == "http" ? "selected" : "")},
									   {"label":"HTTPS",
									    "value":"https",
										"selected":(mgmt_ifc.mgmt_protocol == "https" ? "selected" : "")},
									   {"label":"gNMI",
									    "value":"gnmi",
										"selected":(mgmt_ifc.mgmt_protocol == "gnmi" ? "selected" : "")},
									   {"label":"SSH",
										"value":"ssh",
										"selected":(mgmt_ifc.mgmt_protocol == "ssh" ? "selected" : "")}];
			mgmt_ifc.element_id = this.transient(settings.element_id);
			mgmt_ifc.element_name = this.transient(settings.element_name);
			mgmt_ifc.group_id = this.transient(settings.group_id);
			mgmt_ifc.group_name = this.transient(settings.group_name);
			return mgmt_ifc;
		},
		buttons:{
			"save-mgmt":function(){
				let settings = this.getViewModel();
				let mgmt_name = this.location().param("mgmt_name");
				//Remove existing mgmt interface
				delete settings.mgmt_interfaces[mgmt_name]
				// Add mgmt interface (by that, renaming is implicitly solved)
				let mgmt_ifc = {};
				mgmt_name = this.input("mgmt_name").value();
				mgmt_ifc["mgmt_name"] 	  = mgmt_name;
				mgmt_ifc["mgmt_protocol"] = this.input("mgmt_protocol").value();
				mgmt_ifc["mgmt_hostname"] = this.input("mgmt_hostname").value();
				if(this.input("mgmt_port").value()){
					mgmt_ifc["mgmt_port"] = this.input("mgmt_port").value();
				}
				mgmt_ifc["mgmt_path"] 	  = this.input("mgmt_path").value();
				settings.mgmt_interfaces[mgmt_name] = mgmt_ifc;
				element.saveSettings(this.location().params(),
									 settings);
			},
			"remove-mgmt":function(){
				let settings = this.getViewModel();
				let mgmt_name = this.location().param("mgmt_name");
				if(mgmt_name){
					delete settings.mgmt_interfaces[mgmt_name];
					element.saveSettings(this.location().params(),
										 settings);
				} else {
					this.navigate({"view":"/ui/views/inventory/element/element.html",
						   "?": {"group":this.location().param("group"),
							   	 "element":this.location().param("element")}});
				}
			}
		},
		onSuccess:function(){
			this.navigate({"view":"/ui/views/inventory/element/element.html",
						   "?": {"group":this.location().param("group"),
							   	 "element":this.location().param("element")}});
		}
	});
};

let elementImagesController = function(){
	let element = new Element({"scope":"images"});
	return new Controller({
		resource:element,
		viewModel:function(settings){
			settings.updateSummary = function(){
				let major = 0;
				let minor = 0;
				let patch = 0;
				if(this["available_updates"]){
					this["available_updates"].forEach(function(update){
						if(update["update_type"]=="MAJOR"){
							major++;
						}
						if(update["update_type"]=="MINOR"){
							minor++;
						}
						if(update["update_type"]=="PATCH"){
							patch++;
						}
					});					
				}

				return {"major":major,
						"minor":minor,
						"patch":patch, 
						"updates": (major+minor+patch) > 0 };
			};
			return settings;
		},
	});
};

let elementImageController = function(){
	let element = new Element({"scope":"images"});
	return new Controller({
		resource:element,
		viewModel:function(settings){
			settings.updatesAvailable = function(){
				return this["available_updates"] && this["available_updates"].length > 0;
			};
			
			settings.displayType = function(){
				if(this["type"] == "MAJOR") return "Major ";
				if(this["type"] == "MINOR") return "Minor ";
				if(this["type"] == "PATCH") return "Patch ";
				return "Pre-Release ";
			};
			
			settings.state = function(){
				return this["active"] ? "STARTED" : "CACHED";
			}
			return settings;
		},
	});
};

let elementServicesController = function(){
	let element = new Element({"scope":"services"});
	return new Controller({
		resource:element
	});
};

let local_metric_uri = function(uri){
	return uri;
	//return uri.replace(/http:\/\/[A-Za-z0-9\.\-]+(:\d+)?/,'/metrics');
}

let elementMetricsController = function(){
	let element = new Element({"scope":"metrics"});
	return new Controller({
		resource:element,
		viewModel:function(metrics){
			metrics.metric_date = this.transient(new Date());
			metrics.metric_display_name = function(){
									  if(this.visualization_config && this.visualization_config.title){
										  return this.visualization_config.title;
									  }
									  return this.metric_name
								  };
			metrics.element_scoped = function(){
									 return this.metric_scope === "ELEMENT";
								  };
			metrics.metrics_list = function (){
									 let list = [];
									 for(let metric in metrics["metrics"]){
									 	list.push(metrics["metrics"][metric]);
									 }
									 return list;
								  };
			return metrics;
		},
		postRender:function(){
			// Augment view with telemetry data.
			let metrics = this.getViewModel();
			let tsdb = new Connector();
			tsdb.onSuccess = this.newEventHandler(function(data){
				for(let name in data.metrics){
					let metric = data.metrics[name];
					if(!metrics.metrics[name]){
						continue;
					}
					let html = "<table>";
					
					let formattedSamples = [];
					
					metric.forEach(function(sample){
						let labels = function(){
							return mustache.render(metrics.metrics[name].visualization_config.legend_format,sample.labels);
						};
						formattedSamples.push({"labels":labels(),
											   "value":Units.format(sample.value,metrics.metrics[name]["metric_unit"])});
					});
					formattedSamples.forEach(function(sample){	
						html += "<tr><td class='text medium'>"+sample.labels+"</td><td class='text'>"+sample.value+"</td></tr>"
					});
					html+="</table>";
					this.element(name+".values").html(html);
				}
			});
			tsdb.onNotFound=function(){}; // Ignore when connector is not available.
			tsdb.load({"element":metrics.element_name});
		}
	})
};

let elementIfpMetricsController = function(){
	let element = new ElementPhysicalInterface({"scope":"metrics"});
	return new Controller({
		resource:element,
		viewModel:function(metrics){
			return this.updateViewModel({"metric_uri":function(){
											return local_metric_uri(this["chart-uri"]);
										 },
										 "metric_display_name":function(){
											 if(this.visualization_config && this.visualization_config.title){
											 return this.visualization_config.title;
												  }
												  return this.metric_name},
										 "group":this.location().param("group"),
										 "element":this.location().param("element"),
										 "ifp_name":this.location().param("ifp_name"),	
										 "metric_date":new Date(),
										 "metrics_list":function(){
			  						  			let list = [];
			  						  			for(p in this["metrics"]){
			  						  				list.push(this["metrics"][p]);
			  						  			}
			  						  			return list;
										 }});
		},
		postRender:function(){	
			let tsdb = new Connector({"scope":"ifp/{{&ifp_name}}"});
			tsdb.onSuccess = this.newEventHandler(function(data){
				for(let name in data.metrics){
					let metric = data.metrics[name];
					if(!metrics.metrics[name]){
						continue;
					}
					let html = "<table>";
					metric.forEach(function(sample){
						let del = "";
						let labels = function(){
							return mustache.render(metrics.metrics[name].visualization_config.legend_format,sample.labels);
						};
						
						html += "<tr><td class='text medium'>"+labels()+"</td><td class='text'>"+Units.format(sample.value,"Gbps")+"</td></tr>"
					});
					html+="</table>";
					this.element(name+".values").html(html);
				}
			});
			tsdb.onError = function(){alert("TSDB Not availble!")}; // Not a problem, if TDSB does not exist. TODO: Display error message.
			tsdb.load({"element":metrics.element_name,
					   "ifp_name":this.location().param("ifp_name")});
		}
	})
};

let elementIflMetricsController = function(){
	let element = new ElementLogicalInterface({"scope":"metrics?metric_scope=IFL"});
	return new Controller({
		resource:element,
		viewModel:function(metrics){
			return this.updateViewModel({"metric_uri":function(){
												return local_metric_uri(this["chart-uri"]);
										},
										"group":this.location().param("group"),
										"element":this.location().param("element"),
										"ifl_name":this.location().param("ifl_name"),
										"metric_date":new Date(),
										"metrics_list":function(){
												let list = [];
									 			for(p in this["metrics"]){
									 				list.push(this["metrics"][p]);
									 			}
									 			return list;
								 }});
		}
	})
};

let elementServiceMetricsController = function(){
	let element = new ElementLogicalInterface({"scope":"metrics?metric_scope=SERVICE"});
	return new Controller({
		resource:element,
		viewModel:function(metrics){
			return this.updateViewModel({"metric_uri":function(){
													  	return local_metric_uri(this["chart-uri"]);
											  		  },
										"metrics_list":function(){
									  			let list = [];
									  			for(p in this["metrics"]){
									  				list.push(this["metrics"][p]);
									  			}
									  			return list;
										}});				
		}
	})
};

let elementMetricController = function(){
	let element = new Element({"scope":"metrics/{{&metric_name}}"});
	return new Controller({
		resource:element,
		viewModel:function(settings){
			return this.updateViewModel({"metric_uri":function(){
														return local_metric_uri(settings["chart"]);
											   		  },
								         "check_observe":function(){
								        	 if(settings.metric.alert_config) {
								        		 if(settings.metric.alert_config.alert_policy == "ALL"){
								        			 return "checked readonly disabled";
								        		 } 
								        		 return settings.observe ? "checked" : "";
								        	 }
								        	 return "readonly disabled";
								         }});
		},			
		buttons: {
			"save-settings" : function(){
				let model = this.getViewModel();
				model["observe"]=this.input("observe").isChecked();
				element.saveSettings(this.location().params(),
									model);	
			}
		}
	});
};

let elementMetricsEditorController = function() {
	let element = new Element({"scope":"metrics",
										 "metric_scope":"ALL"});
	return new Controller({
		resource:element,
		viewModel: async function(settings){
			let metricsLoader = new metric.Metrics();
			let metrics = await metricsLoader.load();
			metrics.forEach(metric => metric.checked = settings.metrics[metric.metric_name] ? "checked" : "");
			settings.metrics = metrics;
			return metrics;
		},
		buttons:{
			"save":function(){
				element.saveSettings(this.location().params(),
									 this.input("metric").values());
				
			},
			"select-all":function(){
				this.elements("[name='metric']").forEach(function(metric){
					metric.check();
				});
			},
			"deselect-all":function(){
				this.elements("[name='metric']").forEach(function(metric){
					metric.check(false);
				});
			}
		},
		onSuccess:function(){
			this.navigate({"view":"element-metrics.html",
						   "?":this.location().params()});
		}
	});
}


let elementIfpMetricController = function(){
	let element = new Element({"scope":"physical_interfaces/{{&ifp_name}}/metrics/{{&metric_name}}"});
	return new Controller({
		resource:element,
		viewModel:function(settings){
			return this.updateViewModel({"metric_uri":function(){
											      return local_metric_uri(settings["chart"]);
											   },
							      "check_observe":function(){
							    	  	if(settings.metric.alert_config) {
							    	  		if(settings.metric.alert_config.alert_policy == "ALL"){
							    	  			return "checked readonly disabled";
							    	  		} 
							    	  		return settings.observe ? "checked" : "";
							    	  	}
							    	  	return "readonly disabled";
							      }});
		},			
		buttons: {
			"save-settings" : function(){
				let model = this.getViewModel();
				model["observe"]=this.input("observe").isChecked();
				element.saveSettings(this.location().params(),
									model);	
			}
		}
	})
};

let elementPodController = function(){
	let element = new Element({"scope":"settings"});
	return new Controller({
		resource:element,
		viewModel: function(settings){
			// Load all existing pods
			let podsLoader = new Pods({"filter":this.location().param("filter")});
			this.attach(podsLoader);
			let pods = podsLoader.load();
			pods.forEach(pod => pod.group_id == settings.group_id ? pod.checked = "checked" : pod.checked ="");
			settings.pods = pods;
			// Add filter statement
			settings.filter = filter;
			return settings;
		},
		buttons: {
			"filter-pods":function(){
				let params = this.location().params();
				params.filter = this.input("filter").value();
				this.reload(params);
			},
			"move-element":function(){
				this.updateViewModel({"group_id":this.input("group_id").value()});
				element.saveSettings(this.location().params(),this.getViewModel());
			}
		},
		onSuccess:function(){
			this.navigate({"view":"element.html",
						   "?":this.location().params()});
		}
	});
	
};

let elementIfpsController = function(){
	let ifps = new ElementPhysicalInterfaces();
	return new Controller({
		resource:ifps,
		postRender:function(){
			let ifps = this.getViewModel();
			let metrics = new Connector({"scope":"ifp"});
			metrics.onLoaded = function(response){
				let ifps = {};
				if(response && response.metrics && response.metrics.ifp_data_rate){
					response.metrics.ifp_data_rate.forEach(function(ifp){
						let metric = ifps[ifp.labels.ifp_name];
						if(!metric){
							metric = {};
							ifps[ifp.labels.ifp_name] = metric;
						}
						metric[ifp.labels.direction]=parseFloat(ifp.value);
					});
					for( let ifp in ifps ){
						document.getElementById(ifp).innerHTML=(Units.format(ifps[ifp]["in"],"Gbps")+" / "+Units.format(ifps[ifp]["out"],"Gbps"));
					}
				}
			};
			metrics.onNotFound = function(){alert("Cannot access interface metrics!")};
			metrics.load(this.location().params());
		}
	});
};

let elementIfpController = function(){
	let ifp = new ElementPhysicalInterface();
	return new Controller({
		resource:ifp
	});
};

let elementIflsController = function(){
	let ifls = new ElementLogicalInterfaces();
	return new Controller({
		resource:ifls
	});
};

let elementIflController = function(){
	let ifl = new ElementLogicalInterface();
	return new Controller({
		resource:ifl
	});
};

let elementModulesController = function(){
	let modules = new Element({"scope":"modules"});
	return new Controller({resource:modules});
};

let elementModuleController = function(){
	let module = new Element({"scope":"modules/{{module}}"});
	return new Controller({resource:module,
					 buttons:{
						 "save":function(){
							 let model = this.updateViewModel({"module.asset_id":this.input("asset_id").value()});
							 module.saveSettings(this.location().params(),
									 			 model.module);
						 }
					 },
					 onSuccess:function(){
						 this.navigate({"view":"element-modules.html",
							 		    "?":{"group":this.location().param("group"),
							 		         "element":this.location().param("element")}});
						 }
					 });
}



let elementImagesMenu = {
	"master" : elementImagesController(),
	"details" : {"element-image.html" : elementImageController() }
};


let modulesMenu = {
	"master" :  elementModulesController(),
	"details" : {"element-module.html" : elementModuleController()}
};

let elementMetricsMenu = {
	"master" : elementMetricsController(),
	"details"  : { "element-metric.html": elementMetricController(),
				   "element-metrics-editor.html":elementMetricsEditorController()}
};

let elementIfpMetricsMenu = {
	"master" : elementIfpMetricsController(),
	"details"  : { "element-ifp-metric.html" : elementIfpMetricController()}
};

let elementMenu = {
	"master" : elementController(),
	"details" : {"element-mgmt.html" : elementMgmtController(),
				 "element-pod.html" : elementPodController(),
				 "confirm-remove-element.html" : elementController()}
};

let elementRackMenu = {
	"master" : elementRackController(),
	"details" : {"new-element-location.html" : addElementLocationController(),
				 "element-location.html" : elementMountPointController(),
				 "confirm-remove-location.html": elementMountPointController()}
}
	
export const menu = new Menu({
		"element.html" : elementMenu,
		"element-ifls.html": elementIflsController(),
		"element-ifps.html":elementIfpsController(),
		"element-ifl.html": elementIflController(),
		"element-ifp.html":elementIfpController(),
		"element-metrics.html":elementMetricsMenu,
		"element-ifp-metrics.html":elementIfpMetricsMenu,
		"element-ifl-metrics.html":elementIflMetricsController(),
		"element-service_metrics.html":elementServiceMetricsController(),
		"element-location.html" : elementLocationController(),
		"element-rack.html":elementRackMenu,
		"element-modules.html" :modulesMenu,
		"element-images.html" : elementImagesMenu,
		"element-services.html":elementServicesController(),
		"element-service.html":elementServiceController()
 	});