package controllers;

import static play.data.Form.form;
import models.Constants;
import models.Project;
import models.service.CacheService;
import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.project_setup;
import views.html.job_setup;
import play.cache.Cache;

/**
 * Controller class to coordinate all actions related to entity matching project management.
 * @author excelsior
 *
 */
public class ProjectController extends Controller {

	public static Result index() {
		Logger.info("Setting up project ..");
		CacheService.clearCache();
       	return ok(project_setup.render());
    }

    public static Result saveProject()
    {
    	DynamicForm dynamicForm = form().bindFromRequest();
    	
    	String name = dynamicForm.get("project_name");
    	String description = dynamicForm.get("project_desc");
    	
    	Logger.info("Saving project " + name + " .. ");
    	Project project = new Project(name, description);
    	project.save();

    	Cache.set(Constants.CACHE_PROJECT, project);
    	Logger.info("Saved project " + project.toString() + " ... ");
    	
    	return ok(job_setup.render(name));
    }	
}
