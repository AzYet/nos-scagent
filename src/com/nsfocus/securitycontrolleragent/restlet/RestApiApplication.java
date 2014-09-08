package com.nsfocus.securitycontrolleragent.restlet;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class RestApiApplication extends Application{
	
	
    @Override
    public Restlet createInboundRoot() {
        Router router = new Router(this.getContext());
        router.attach("/hello", RestApiServer.class);
        
        return router;    
    }
}
