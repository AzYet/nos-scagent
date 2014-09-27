package com.nsfocus.scagent.restlet;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class RestApiApplication extends Application{
	
	
    @Override
    public Restlet createInboundRoot() {
        Router router = new Router(this.getContext());
        router.attach("/scagent/{op}", RestApiServer.class);
        router.attach("/scagent/policyaction/{id}", RestApiServer.class);
        return router;
    }
}
