package com.nsfocus.securitycontrolleragent.restlet;

import java.io.IOException;

import org.restlet.Component;
import org.restlet.data.MediaType;
import org.restlet.data.Protocol;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
			try {
				String text = entity.getText();
				Gson gson = new Gson();
				JsonParser parser = new JsonParser();
				JsonObject jsonObject = parser.parse(text).getAsJsonObject();
				String id = jsonObject.get("id").getAsString();
				System.out.println("id: " + id);
				JsonArray ifs = jsonObject.get("ifs").getAsJsonArray();
				for (int i = 0; i < ifs.size(); i++) {
					JsonObject ifObj = gson.fromJson(ifs.get(i), JsonObject.class);
					JsonElement jsonElement = ifObj.get("abc");
					String connectTo = ifObj.get("connect_to").getAsString();
					String mac = ifObj.get("mac").getAsString();
					System.out.println("connect_to:" + connectTo + "\t mac: " + mac);
				}
				result = new StringRepresentation(text, MediaType.APPLICATION_JSON);
			} catch (IOException e) {
				// TODO Auto-generated catch block
		 		result = null;
				e.printStackTrace();
			}
	 
	        return result;  
	    }

}	
