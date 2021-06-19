package org.asf.connective.commoncgi.providers;

import java.io.IOException;
import java.util.Base64;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.IAuthenticationProvider;
import org.asf.rats.Memory;

public class SecurityUtil extends CyanComponent {
	protected static SecurityUtil impl = new SecurityUtil();

	public static SecurityUtil getInstance() {
		return impl;
	}

	public String authenticate(HttpRequest request, HttpResponse response, String group) {
		if (!request.headers.containsKey("Authorization")) {
			String header = request.headers.get("Authorization");
			String type = header.substring(0, header.indexOf(" "));
			String cred = header.substring(header.indexOf(" ") + 1);

			if (type.equals("Basic")) {
				cred = new String(Base64.getDecoder().decode(cred));
				String username = cred.substring(0, cred.indexOf(":"));
				String password = cred.substring(cred.indexOf(":") + 1);

				try {
					if (Memory.getInstance().get("connective.standard.authprovider")
							.getValue(IAuthenticationProvider.class)
							.authenticate(group, username, password.toCharArray())) {
						password = null;
						return username;
					} else {
						response.status = 403;
						response.message = "Access denied";
						response.setContent("text/html", (String) null);
						password = null;
						return null;
					}
				} catch (IOException e) {
					response.status = 503;
					response.message = "Internal server error";
					response.setContent("text/html", (String) null);
					error("Failed to process user, group: " + group + ", username: " + username, e);
					password = null;
					return null;
				}
			} else {
				response.status = 403;
				response.message = "Access denied";
				response.setContent("text/html", (String) null);
				return null;
			}
		} else {
			response.setHeader("WWW-Authenticate", "Basic realm=" + group);

			response.status = 401;
			response.message = "Authorization required";
			response.setContent("text/html", "");
			return null;
		}
	}

	public static void assign() {
		impl = new SecurityUtil();
	}

}
