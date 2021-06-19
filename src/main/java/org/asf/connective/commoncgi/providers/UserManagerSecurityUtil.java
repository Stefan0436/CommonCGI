package org.asf.connective.commoncgi.providers;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class UserManagerSecurityUtil extends SecurityUtil {

	public static void assign() {
		impl = new UserManagerSecurityUtil();
	}

	public String authenticate(HttpRequest request, HttpResponse response, String group) {
		AuthResult res = UserManagerModule.getAuthBackend().authenticate(group, request, response);
		if (!res.success())
			return null;
		else
			return res.getUsername();
	}

}
