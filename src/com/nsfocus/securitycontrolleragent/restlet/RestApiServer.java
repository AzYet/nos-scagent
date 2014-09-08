package com.nsfocus.securitycontrolleragent.restlet;

import org.restlet.Component;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

public class RestApiServer extends ServerResource{
	public static void runServer() throws Exception {
		
		
		// new Server(Context.getCurrent(), Protocol.HTTP,
		// 8182,RestApiServer.class).start();
		Component component = new Component();
		component.getServers().add(Protocol.HTTP, 8182);
		component.getDefaultHost().attach(new RestApiApplication());
		component.start();
	}

	@Get
	public String toString() {
		return "Resource URI  : " + getReference() + '\n' + "Root URI      : "
		+ getRootRef() + '\n' + "Routed part   : "
		+ getReference().getBaseRef() + '\n' + "Remaining part: "
		+ getReference().getRemainingPart();	}
	
	 @Post
	    public Representation acceptItem(Representation entity) {  
			Representation result = null;  
	        // Parse the given representation and retrieve data
	        Form form = new Form(entity);  
	        String uid = form.getFirstValue("uid");  
	        String uname = form.getFirstValue("uname");  
	 
	        if(uid.equals("123")){ // Assume that user id 123 is existed
	        result = new StringRepresentation("User whose uid="+ uid +" is updated",  
	            MediaType.TEXT_PLAIN);
	        } 
	        else { // otherwise add user  
	        result = new StringRepresentation("User " + uname + " is added",  
	            MediaType.TEXT_PLAIN);
	        }  
	 
	        return result;  
	    }

}	
