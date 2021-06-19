package org.asf.connective.commoncgi;

import java.util.ArrayList;
import java.util.HashMap;

import org.asf.connective.commoncgi.providers.SecurityUtil;
import org.asf.connective.commoncgi.providers.UserManagerSecurityUtil;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;

/**
 * 
 * CommonCGI Module - Internal Use Only
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class CgiModule extends CyanComponent {

	public static class ScriptConfig {
		public String virtualName;
		public String scriptPath;
		public ArrayList<String> allowedGroups = new ArrayList<String>();
		public ArrayList<String> scriptArgs = new ArrayList<String>();
		public ArrayList<String> contexts = new ArrayList<String>();
		public ArrayList<String> methods = new ArrayList<String>();
		public HashMap<String, String> variables = new HashMap<String, String>();
	}

	private static ArrayList<ScriptConfig> scripts = new ArrayList<ScriptConfig>();

	public static ScriptConfig[] getScripts() {
		return scripts.toArray(t -> new ScriptConfig[t]);
	}

	protected static void initComponent() {
		Memory.getInstance().getOrCreate("bootstrap.call").<Runnable>append(() -> {
			readConfig();
		});
		Memory.getInstance().getOrCreate("bootstrap.reload").<Runnable>append(() -> {
			readConfig();
		});
	}

	private static void readConfig() {
		try {
			Class.forName("org.asf.connective.usermanager.UserManagerModule");
			UserManagerSecurityUtil.assign();
		} catch (Exception e) {
			SecurityUtil.assign();
		}
		scripts.clear();
		ModuleBasedConfiguration<?> config = Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class);
		config.modules.forEach((module, conf) -> {
			if (module.startsWith("cgi:")) {
				String scriptPath = module.substring(4);
				ScriptConfig script = new ScriptConfig();
				if (!scriptPath.startsWith("/"))
					scriptPath = "/" + scriptPath;

				if (conf.containsKey("cgi:scriptfile")) {
					script.virtualName = scriptPath;
					script.scriptPath = conf.get("cgi:scriptfile");
					if (conf.containsKey("cgi:methods")) {
						for (String method : conf.get("cgi:methods").split(":")) {
							script.methods.add(method.toUpperCase());
						}
					} else {
						script.methods.add("GET");
					}
					for (String key : conf.keySet()) {
						if (key.startsWith("cgi:scriptarg")) {
							script.scriptArgs.add(conf.get(key));
						} else if (key.startsWith("cgi:method")) {
							script.methods.add(conf.get(key));
						} else if (key.startsWith("cgi:context")) {
							script.contexts.add(conf.get(key));
						} else if (key.startsWith("cgi:security")) {
							script.allowedGroups.add(conf.get(key));
						} else {
							script.variables.put(key, conf.get(key));
						}
					}
					scripts.add(script);
				}
			}
		});
	}

}
