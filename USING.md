# Using CommonCGI in your modules
This guide explains how to use this library in your modules.

# Installation
The following guide will help install the dependency.

## Adding the maven repository
If you are using the template project, you should already have the repository and can skip this step.
Otherwise, add the following to your build.gradle

```groovy
// File: build.gradle
// ...
repositories {
	// ...
	maven { name = "AerialWorks"; url = "https://aerialworks.ddns.net/maven" }
	// ...
}
```

## Adding the CommonCGI library to the project
Now that you have our maven repository, you can add CommonCGI as following:

```groovy
// File: build.gradle
// ...
dependencies {
	// ...
	implementation group: 'org.asf.connective.commoncgi', name: 'CommonCGI', version: '1.0.0.A2'
	// ...
}
```

## Downloading jar for testing
Next you will need to download the jar from our maven server and add it in the libraries folder of the project.

1. Navigate to [our maven server](https://aerialworks.ddns.net/maven) (yes, it is hosted through ConnectiveHTTP)
2. Open org/asf/connective/commoncgi
3. Open CommonCGI
4. Select the latest version
5. Download the first jar (not sources or javadoc)
6. Drop it in libraries
7. Re-run `(./)gradlew(.bat) createEclipse` (`./gradlew` on linux, `gradlew.bat` on windows)

## Creating a dependency provider
The following system has not been documented, but it is real.
On startup, the standalone server searches for `IModuleMavenDependencyProvider` implementations for dependency resolution.<br />
<br />
It's best you use it for dependency download, but you will need to modify the flatdir repository:

```groovy
// File: build.gradle
// ...
repositories {
	// ...
	
	// The following must be changed:
	flatDir {
		dirs 'libraries'
	}
	
	// It should be:
	flatDir {
		dirs 'libraries', 'server'
	}
	
	// ...
}
```

This way, we can use standalone types.
Now, lets add the server dependency:

```groovy
// File: build.gradle
// ...
dependencies {
	// ...
	implementation name: "ConnectiveStandalone"
	// ...
}
```

After that, run: `(./)gradlew(.bat) eclipse`

## Creating the provider
Now we can finally create the provider:

```java
// File: <Module>CGIDependecyProvider.java
// ...

// package ... ;

import org.asf.connective.standalone.IModuleMavenDependencyProvider;

public class <Module>CGIDependencyProvider implements IModuleMavenDependencyProvider {

	@Override
	public String group() {
		return "org.asf.connective.commoncgi";
	}

	@Override
	public String name() {
		return "CommonCGI";
	}

	@Override
	public String version() {
		return "1.0.0.A2"; // Use the latest version
	}

}

```

After that, you're good to go, this class is automatically detected by the standalone.<br/>
Just remember to avoid direct referencing, it can cause issues on other software.<br/>
<b>TIP:</b> it's best to state that this project uses CommonCGI, not all servers support the `IModuleMavenDependencyProvider` interface.

# Using the project
After you have refreshed your project in eclipse, you can use the CgiScript class to start CGI scripts.
The following examples will help get you started.

## Examples of CGI scripts
Take a look at [the example scripts](CGI_EXAMPLE_SCRIPTS.md) we have prepared for this.

# Using the scripts
Now we will go into the code needed to run it.

## Basic example
The following snippet is a general-purpose example for using CGI scripts:

```java
// Server and request information
String serverName = "Some server name"; // best to use a (shared) config
String path = ... ; // The alias-processed path provided by the file module
Socket client = ... ; // The client making the request
ProviderContext context = ... ; // The provider context used by the file module
HttpRequest request = ... ; // The HTTP request

// CGI information
String executable = "<yourscriptfile>";
String[] arguments = new String[] {}; // Arguments needed to run it



// Create the container and add the defaults
CgiScript script = CgiScript.create(server, executable, arguments);
script.setDefaultVariables(serverName, request, client);
script.setFileVariable(context, path);

// Run the script and retrieve the context
CgiContext ctx = script.run();



// Depending on your method of processing, select one of the following to apply the script:
// Apply the full context (outside of extension processors)
ctx.applyFullCGI(response);

// Apply headers and create FileContext (extension processors only)
return FileContext.create(input, "text/html", ctx.getOutput()); // replace text/html with the right type
```

## File extension example (no upload support)

<b>Tip:</b> It's best to combine this example with an upload handler.<br/>
Note: this example needs the following interfaces to work:

```java
... implements IFileExtensionProvider, IContextProviderExtension, IPathProviderExtension, IClientSocketProvider, IServerProviderExtension ...
```

And the following fields are needed too:

```java
ProviderContext ctx;
ConnectiveHTTPServer server;
String serverName; // use a configuration for this

String path; // provider by the IPathProviderExtension interface
Socket client; // provided by the IClientSocketProvider interface.

String executable; // your script
String[] arguments; // script arguments
```

Processing code:

```java
//...

@Override
public FileContext rewrite(HttpResponse input, HttpRequest request) {
	try {
		// Create a script instance.
		CgiScript script = CgiScript.create(server, executable, arguments);
		script.setDefaultVariables(serverName, request, client);

		// Set context information variables
		script.setFileVariable(ctx, path);
		
		// Run the script
		CgiContext ctx = script.run();
		ctx.applyToResponse(input);
		return FileContext.create(input, "text/html", ctx.getOutput());
	} catch (IOException e) {
		input.status = 503;
		input.message = "Internal server error";

		return FileContext.create(input, "text/html",
				new ByteArrayInputStream(server.genError(input, request).getBytes()));
	}
}

//...
```

## FileUploadHandler example (upload only)
Note: this example needs the following interface to work:

```java
... extends FileUploadHandler implements IContextProviderExtension ...
```

And the following fields are needed too:

```java
ProviderContext ctx;
String serverName; // use a configuration for this

String executable; // your script
String[] arguments; // script arguments
```

Processing code:

```java
//...

@Override
public boolean process(String contentType, Socket client, String method) {
	// TIP: if you don't want your script to receive specific methods
	// return false if the method string equals it. (use case in-sensitive methods)

	try {
		
		// Create a script instance.
		CgiScript script = CgiScript.create(getServer(), executable, arguments);
		script.setDefaultVariables(serverName, getRequest(), client);
		script.setFileVariable(ctx, getFolderPath());
		
		// Add the content body provider
		script.addContentProvider(getRequest());
		
		// Apply the script
		script.run().applyFullCGI(getResponse());
		
	} catch (IOException e) {
		setResponseCode(503);
		setResponseMessage("Internal server error");
		setBody("text/html", getServer().genError(getResponse(), getRequest()));
	}
	
	return true;
}

//...
```