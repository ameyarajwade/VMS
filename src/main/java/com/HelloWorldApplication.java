package main.java.com;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import main.java.com.resource.HelloWorldResource;

public class HelloWorldApplication extends Application<HelloWorldConfiguration>{

	  public static void main(String[] args) throws Exception {
	        new HelloWorldApplication().run(args);
	    }

	    @Override
	    public String getName() {
	        return "hello-world";
	    }

	    @Override
	    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
	        // nothing to do yet
	    }
	
	@Override
	public void run(HelloWorldConfiguration configuration,
            Environment environment) throws Exception {
		 final HelloWorldResource resource = new HelloWorldResource(
			        configuration.getTemplate(),
			        configuration.getDefaultName()
			    );
			    environment.jersey().register(resource);
		
	}

}
