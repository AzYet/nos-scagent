package com.nsfocus.securitycontrolleragent.restlet;

import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;

public class RestApiApplication extends Application{
	
	
    @Override
    public Restlet createInboundRoot() {
        Router router = new Router(this.getContext());
        router.attach("/hello", RestApiServer.class);
        
        return router;    
    }
}
