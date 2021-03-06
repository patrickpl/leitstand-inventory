import {Controller,Menu} from '/ui/js/ui.js';
import {Metadata} from '/ui/modules/inventory/inventory.js';
import {Images,Image} from './image.js';

let imagesController = function(){
	let images = new Metadata({"scope":"images"});
	return new Controller({
		resource:images,
		viewModel:async function(settings){ //TODO Refactor with view migration
			let location = this.location();
			let viewModel = settings;
			viewModel.filter = { "filter":location.param("filter"),
								 "image_type":location.param("image_type"),
								 "image_version":location.param("image_version"),
								 "image_state":location.param("image_state")};
			viewModel.image_types = [{"label":"All image types",
							   		  "value":""},
							   		 {"label":"ONL Images",
							   		  "value":"ONL"},
							   		 {"label":"LXC Images",
							   		  "value":"LXC"}];

	  		let images = new Images();
	  		viewModel.images = await images.load(viewModel.filter);
	  		viewModel.element_display_name = function(){
	  			return this["element_name"] ? this["element_name"] : "*";
	  		}
	  		return viewModel;
		},
		buttons:{
			"filter":function(){
				this.reload(this.getViewModel("filter"));
			}
		}
	})
};
	
let imageController = function(){
	let image = new Image();
		return new Controller({
			resource:image,
			viewModel: function(viewModel){
				viewModel.revoked = function(){
					return this.image_state == "REVOKED";
				};
				return viewModel;
			},
			buttons:{
				"apply-state":function(){
					let params = this.location().params();
					params["image_state"] = this.input("[name='image_state']").value();
					image.updateState(params);
				},
				"purge":function(){
					image.purgeCaches(this.location().params());
				}
			},
			onSuccess: function(){
				this.reload();
			}
		});
	};

let imageStatisticsController = function(){
	let imageStats = new Image({"scope":"statistics"});
	return new Controller({
			resource: imageStats,
			viewModel:function(stats){
				let pods = {};
				let totalActiveCount = 0;
				let totalCacheCount  = 0;
				let podCount = 0;
				if (stats.active_count){
					for(let podName in stats.active_count){
						let pod = pods[podName];
						if(!pod){
							pod = {"active_count":0,
								   "cached_count":0};
							pods[podName]= pod;
							podCount++;
						}
						pod.active_count = stats.active_count[podName];
						totalActiveCount += pod.active_count;
					}
				}
				if(stats.cached_count){
					for(let podName in stats.cached_count){
						let pod = pods[podName];
						if(!pod){
							pod = {"active_count":0,
								   "cached_count":0};
							pods[podName]= pod;
							podCount++;
						}
						pod.cached_count = stats.cached_count[podName];
						totalCacheCount += pod.cached_count;
					}
				}
				let images = [];
				for(let podName in pods){
					let pod = pods[podName];
					pod["group_name"] = podName;
					images.push(pod);
				}
				let viewModel = stats.image;
				viewModel["pod_count"]=podCount;
				viewModel["active_count"]=totalActiveCount;
				viewModel["cached_count"]=totalCacheCount;
				viewModel["total_count"]=(totalActiveCount+totalCacheCount);
				viewModel["images"]=images;
				return viewModel;
			}
	});
}
	
let imagesMenu = {
	"master" : imagesController(),
	"details"  : {
		"image.html" : imageController()
	}
}

export const menu = new Menu({"images.html" : imagesMenu,
							  "image-meta.html" : imageController(),
							  "image-pkgs.html" : imageController(),
							  "image-apps.html" : imageController(),
							  "image-state.html" : imageController(),
							  "image-stats.html" : imageStatisticsController()},
							  "/ui/views/image/images.html");