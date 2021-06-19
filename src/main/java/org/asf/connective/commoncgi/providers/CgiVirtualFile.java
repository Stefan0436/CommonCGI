package org.asf.connective.commoncgi.providers;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import org.asf.connective.commoncgi.CgiModule;
import org.asf.connective.commoncgi.CgiScript;
import org.asf.connective.commoncgi.CgiScript.CgiContext;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.IContextProviderExtension;
import org.asf.rats.http.providers.IContextRootProviderExtension;
import org.asf.rats.http.providers.IServerProviderExtension;
import org.asf.rats.http.providers.IVirtualFileProvider;

public class CgiVirtualFile extends CyanComponent implements IVirtualFileProvider, IServerProviderExtension,
		IContextProviderExtension, IContextRootProviderExtension {

	private ProviderContext ctx;
	private String contextRoot;
	private ConnectiveHTTPServer server;

	@Override
	public IVirtualFileProvider newInstance() {
		return new CgiVirtualFile();
	}

	@Override
	public boolean supportsUpload() {
		return true;
	}

	@Override
	public boolean match(String path, HttpRequest request) {
		for (CgiModule.ScriptConfig conf : CgiModule.getScripts()) {
			if (conf.methods.contains(request.method.toUpperCase())
					&& (conf.virtualName.equals(path) || path.startsWith(conf.virtualName + "/"))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void process(String path, String uploadMediaType, HttpRequest request, HttpResponse response, Socket client,
			String method) {
		for (CgiModule.ScriptConfig conf : CgiModule.getScripts()) {
			if (conf.methods.contains(request.method.toUpperCase())
					&& (conf.virtualName.equals(path) || path.startsWith(conf.virtualName + "/"))) {
				String ctxPath = ctx.getSourceDirectory();
				if (!new File(ctxPath).isAbsolute()) {
					try {
						if (!new File(ctxPath).getCanonicalFile().toPath().getRoot()
								.equals(new File(".").getCanonicalFile().toPath().getRoot())) {
							try {
								ctxPath = new File(ctxPath).getCanonicalPath();
							} catch (IOException e) {
								ctxPath = new File(ctxPath).getAbsolutePath();
							}
						} else {
							ctxPath = new File(".").toPath().relativize(new File(ctxPath).toPath()).toString();
						}
					} catch (IOException e) {
						try {
							ctxPath = new File(ctxPath).getCanonicalPath();
						} catch (IOException e2) {
							ctxPath = new File(ctxPath).getAbsolutePath();
						}
					}
				}
				if (conf.contexts.size() != 0) {
					boolean found = false;
					for (String ctx : conf.contexts) {
						if (ctx.equals(ctxPath)) {
							found = true;
							break;
						}
					}
					if (!found) {
						response.status = 404;
						response.message = "File not found";
						response.setContent("text/html", (String) null);
					}
				}
				CgiScript script = CgiScript.create(server, conf.scriptPath,
						conf.scriptArgs.toArray(t -> new String[t]));
				if (conf.allowedGroups.size() != 0) {
					boolean auth = false;
					for (String group : conf.allowedGroups) {
						String user = SecurityUtil.getInstance().authenticate(request, response, group);
						if (user != null) {
							auth = true;
							script.setVariable("REMOTE_USER", user);
							break;
						}
					}
					if (!auth)
						return;
				}
				script.addContentProvider(request);
				if (conf.variables.containsKey("cgi:version")) {
					script.setCGIVersion(conf.variables.get("cgi:version"));
					conf.variables.remove("cgi:version");
				}
				script.setDefaultVariables(conf.variables.getOrDefault("cgi:server", "ASF Connective"), request,
						client);
				if (conf.variables.containsKey("cgi:server"))
					conf.variables.remove("cgi:version");
				for (String key : conf.variables.keySet()) {
					String value = conf.variables.get(key);

					value = value.replace("${server.name}", server.getName());
					value = value.replace("${server.version}", server.getVersion());
					value = value.replace("${server.protocol}", server.getPreferredProtocol());
					value = value.replace("${server.port}", server.getPort() + "");
					value = value.replace("${server.hostname}", server.getIp().getHostName());

					value = value.replace("${context.virtual.root}", contextRoot);
					value = value.replace("${context.root}", ctx.getSourceDirectory());

					value = value.replace("${request.method}", request.method);
					value = value.replace("${request.version}", request.version);
					value = value.replace("${request.path}", request.path);

					for (String header : request.headers.keySet()) {
						value = value.replace("${request.headers." + header.toLowerCase() + "}",
								request.headers.get(header));
					}

					script.setVariable(key, value);
				}
				try {
					CgiContext sc = script.run();
					sc.applyFullCGI(response);
				} catch (IOException e) {
					response.status = 503;
					response.message = "Internal server error";
					response.setContent("text/html", (String) null);
					error("CGI failed to run.", e);
				}
				return;
			}
		}
	}

	@Override
	public void provide(ProviderContext context) {
		ctx = context;
	}

	@Override
	public void provideVirtualRoot(String virtualRoot) {
		contextRoot = virtualRoot;
	}

	@Override
	public void provide(ConnectiveHTTPServer server) {
		this.server = server;
	}

}
