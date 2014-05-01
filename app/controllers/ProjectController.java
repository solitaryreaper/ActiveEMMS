package controllers;

import static play.data.Form.form;
import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import views.html.project_setup;
import views.html.job_setup;

/**
 * Controller class to coordinate all actions related to entity matching project management.
 * @author excelsior
 *
 */
public class ProjectController extends Controller {

	public static Result index() {
       	return ok(project_setup.render());
    }

    public static Result saveProject()
    {
    	DynamicForm dynamicForm = form().bindFromRequest();
    	
    	String name = dynamicForm.get("project_name");
    	String description = dynamicForm.get("project_desc");

    	Logger.info("Saved project " + name + " ... ");
    	
    	return ok(job_setup.render(name));
    }	
}
