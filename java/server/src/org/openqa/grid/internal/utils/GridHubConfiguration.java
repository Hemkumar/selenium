package org.openqa.grid.internal.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.grid.common.CommandLineOptionHelper;
import org.openqa.grid.common.JSONConfigurationUtils;
import org.openqa.grid.common.exception.GridConfigurationException;
import org.openqa.grid.internal.listeners.Prioritizer;
import org.yaml.snakeyaml.Yaml;

public class GridHubConfiguration {

	private static final Logger log = Logger.getLogger(GridHubConfiguration.class.getName());

	/**
	 * The hub needs to know its hostname in order to write the proper Location
	 * header for the request being forwarded. Usually this can be guessed
	 * correctly, but in case it cannot it can be passed via this config param.
	 */
	private String host = null;

	/**
	 * port for the hub.
	 */
	private int port;

	/**
	 * how often in ms each proxy will detect that a session has timed out. All
	 * new proxy registering will have that value if they don't specifically
	 * mention the parameter.
	 */
	private int cleanupCycle;

	/**
	 * how long can a session be idle before being considered timed out. Working
	 * together with cleanup cycle. Worst case scenario, a session can be idle
	 * for timout + cleanup cycle before the timeout is detected
	 */
	private int timeout;

	/**
	 * how long a new session request can stay in the queue without being
	 * assigned before being rejected. -1 = forever.
	 */
	private int newSessionWaitTimeout;

	/**
	 * list of extra serlvets this hub will display. Allows to present custom
	 * view of the hub for monitoring and management purpose
	 */
	private List<String> servlets = new ArrayList<String>();

	/**
	 * name <-> browser mapping from grid1
	 */
	private Map<String, String> grid1Mapping = new HashMap<String, String>();

	/**
	 * to specify the order in which the new session request will be handled.
	 */
	private Prioritizer prioritizer = null;

	/**
	 * to specify how new request and nodes will be matched.
	 */
	private CapabilityMatcher matcher;

	/**
	 * true by default.If true, the hub will throw exception as soon as a
	 * request not supported by the grid is received. If set to false, the
	 * request will be queued, hoping that a node will be registered at some
	 * point, supporting that capability.
	 */
	private boolean throwOnCapabilityNotPresent = true;

	private Map<String, Object> allParams = new HashMap<String, Object>();

	/**
	 * original command line param, useful for debugging
	 */
	private String[] args;
	private String grid1Yml = null;
	private String grid2JSON = null;

	public GridHubConfiguration(){
		loadDefault();
	}
	/**
	 * builds a grid configuration from the parameters passed command line.
	 * 
	 * @param args
	 * @return
	 */
	public static GridHubConfiguration build(String[] args) {
		GridHubConfiguration res = new GridHubConfiguration();
		CommandLineOptionHelper helper = new CommandLineOptionHelper(args);
		res.args = args;

		// is there a grid1 config file to load ?
		if (helper.isParamPresent("-grid1Yml")) {
			String value = helper.getParamValue("-grid1Yml");
			res.grid1Yml = value;
			res.loadFromGridYml(value);
		}

		// is there a grid2 config file ?
		if (helper.isParamPresent("-hubConfig")) {
			String value = helper.getParamValue("-hubConfig");
			res.grid2JSON = value;
			res.loadFromJSON(value);
		}

		// are there some command line param to overwrite the config file ?
		res.loadFromCommandLine(args);

		return res;

	}

	public String getGrid1Yml() {
		return grid1Yml;
	}

	public String getGrid2JSON() {
		return grid2JSON;
	}

	public void loadDefault() {
		loadFromJSON("defaults/DefaultHub.json");
	}

	public void loadFromCommandLine(String[] args) {
		CommandLineOptionHelper helper = new CommandLineOptionHelper(args);
		// handle the core config.
		if (helper.isParamPresent("-host")) {
			host = helper.getParamValue("-host");
		}
		if (helper.isParamPresent("-port")) {
			port = Integer.parseInt(helper.getParamValue("-port"));
		}
		if (helper.isParamPresent("-cleanUpCycle")) {
			cleanupCycle = Integer.parseInt(helper.getParamValue("-cleanUpCycle"));
		}
		if (helper.isParamPresent("-timeout")) {
			timeout = Integer.parseInt(helper.getParamValue("-timeout"));
		}
		if (helper.isParamPresent("-newSessionWaitTimeout")) {
			newSessionWaitTimeout = Integer.parseInt(helper.getParamValue("-newSessionWaitTimeout"));
		}
		if (helper.isParamPresent("-throwOnCapabilityNotPresent")) {
			throwOnCapabilityNotPresent = Boolean.parseBoolean(helper.getParamValue("-throwOnCapabilityNotPresent"));
		}
		if (helper.isParamPresent("-prioritizer")) {
			setPrioritizer(helper.getParamValue("-prioritizer"));
		}
		if (helper.isParamPresent("-capabilityMatcher")) {
			setCapabilityMatcher(helper.getParamValue("-capabilityMatcher"));
		}
		if (helper.isParamPresent("-servlets")) {
			servlets = helper.getParamValues("-servlets");
		}

		// storing them all.
		List<String> params = helper.getKeys();
		for (String param : params) {
			String value = helper.getParamValue(param);
			if (value.split(",").length != 1) {
				List<String> values = Arrays.asList(value.split(","));
				allParams.put(param, value);
			} else {
				allParams.put(param, value);
			}

		}

	}

	/**
	 * 
	 * @param resource
	 *            /grid_configuration.yml for instance
	 * @return
	 */
	public void loadFromGridYml(String resource) {
		InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);

		if (in == null) {
			try {
				in = new FileInputStream(resource);
			} catch (FileNotFoundException e) {
				// ignore
			}
		}
		if (in == null) {
			throw new InvalidParameterException(resource + " is not a valid resource.");
		}

		Yaml yaml = new Yaml();
		Map<String, Object> config = (Map<String, Object>) yaml.load(in);
		Map<String, Object> hub = (Map<String, Object>) config.get("hub");
		List<Map<String, String>> environments = (List<Map<String, String>>) hub.get("environments");

		
		// Now pull out each of the grid config values.
		Integer p = (Integer) hub.get("port");
		if (p != null) {
			this.port = p;
		}
		
		// Store a copy of the environment names => browser strings
		for (Map<String, String> environment : environments) {
			getGrid1Mapping().put(environment.get("name"), environment.get("browser"));
		}

		// Now pull out each of the grid config values.
		Integer poll = (Integer) hub.get("remoteControlPollingIntervalInSeconds");
		if (poll != null) {
			allParams.put("nodePolling", poll.intValue() * 1000);
			cleanupCycle = poll.intValue() * 1000;
		}

		Integer timeout = (Integer) hub.get("sessionMaxIdleTimeInSeconds");
		if (timeout != null) {
			timeout = timeout.intValue() * 1000;
		}

		Integer port = (Integer) hub.get("port");
		if (port != null) {
			port = port.intValue();
		}

		Integer newSessionWait = (Integer) hub.get("newSessionMaxWaitTimeInSeconds");
		if (newSessionWait != null) {
			newSessionWaitTimeout = newSessionWait.intValue() * 1000;
		}

	}

	/**
	 * 
	 * @param resource
	 *            default/DefaultHub.json for instance
	 * @return
	 */
	public void loadFromJSON(String resource) {

		try {
			JSONObject o = JSONConfigurationUtils.loadJSON(resource);

			// handling the core config.
			if (o.has("host") && !o.isNull("host")) {
				host = o.getString("host");
			}
			if (o.has("port") && !o.isNull("port")) {
				port = o.getInt("port");
			}
			if (o.has("cleanUpCycle") && !o.isNull("cleanUpCycle")) {
				cleanupCycle = o.getInt("cleanUpCycle");
			}
			if (o.has("timeout") && !o.isNull("timeout")) {
				timeout = o.getInt("timeout");
			}
			if (o.has("newSessionWaitTimeout") && !o.isNull("newSessionWaitTimeout")) {
				newSessionWaitTimeout = o.getInt("newSessionWaitTimeout");
			}
			if (o.has("servlets") && !o.isNull("servlets")) {
				JSONArray jsservlets = o.getJSONArray("servlets");
				for (int i = 0; i < jsservlets.length(); i++) {
					servlets.add(jsservlets.getString(i));
				}
			}
			if (o.has("prioritizer") && !o.isNull("prioritizer")) {
				String prioritizerClass = o.getString("prioritizer");
				setPrioritizer(prioritizerClass);
			}
			if (o.has("capabilityMatcher") && !o.isNull("capabilityMatcher")) {
				String capabilityMatcherClass = o.getString("capabilityMatcher");
				setCapabilityMatcher(capabilityMatcherClass);
			}
			if (o.has("throwOnCapabilityNotPresent") && !o.isNull("throwOnCapabilityNotPresent")) {
				throwOnCapabilityNotPresent = o.getBoolean("throwOnCapabilityNotPresent");
			}

			// store them all.
			for (Iterator iterator = o.keys(); iterator.hasNext();) {
				String key = (String) iterator.next();
				Object value = o.get(key);
				if (value instanceof JSONArray) {
					JSONArray a = (JSONArray) value;
					List<String> as = new ArrayList<String>();
					for (int i = 0; i < a.length(); i++) {
						as.add(a.getString(i));
					}
					allParams.put(key, as);
				} else {
					allParams.put(key, o.get(key));
				}
			}

		} catch (Throwable e) {
			throw new GridConfigurationException("Error with the JSON of the config : " + e.getMessage(), e);
		}
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public int getCleanupCycle() {
		return cleanupCycle;
	}

	public int getTimeout() {
		return timeout;
	}

	public int getNewSessionWaitTimeout() {
		return newSessionWaitTimeout;
	}

	public List<String> getServlets() {
		return servlets;
	}

	public Map<String, String> getGrid1Mapping() {
		return grid1Mapping;
	}

	public Prioritizer getPrioritizer() {
		return prioritizer;
	}

	public CapabilityMatcher getCapabilityMatcher() {
		return matcher;
	}

	public boolean isThrowOnCapabilityNotPresent() {
		return throwOnCapabilityNotPresent;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setCleanupCycle(int cleanupCycle) {
		this.cleanupCycle = cleanupCycle;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setNewSessionWaitTimeout(int newSessionWaitTimeout) {
		this.newSessionWaitTimeout = newSessionWaitTimeout;
	}

	public void setServlets(List<String> servlets) {
		this.servlets = servlets;
	}

	public void setPrioritizer(String prioritizerClass) {
		try {
			Class<Prioritizer> p = (Class<Prioritizer>) Class.forName(prioritizerClass);
			Class<?>[] argsClass = new Class[] {};
			Constructor<?> c = p.getConstructor(argsClass);
			Object[] args = new Object[] {};
			Prioritizer prio = (Prioritizer) c.newInstance(args);
			prioritizer = prio;
		} catch (Throwable e) {
			throw new GridConfigurationException("Error creating the prioritize from class " + prioritizerClass + " : " + e.getMessage(), e);
		}
	}

	public void setPrioritizer(Prioritizer prioritizer) {
		this.prioritizer = prioritizer;
	}

	public void setCapabilityMatcher(String matcherClass) {
		try {
			Class<CapabilityMatcher> p = (Class<CapabilityMatcher>) Class.forName(matcherClass);
			Class<?>[] argsClass = new Class[] {};
			Constructor<?> c = p.getConstructor(argsClass);
			Object[] args = new Object[] {};
			CapabilityMatcher cm = (CapabilityMatcher) c.newInstance(args);
			matcher = cm;
		} catch (Throwable e) {
			throw new GridConfigurationException("Error creating the capability matcher from class " + matcherClass + " : " + e.getMessage(), e);
		}
	}

	public void setCapabilityMatcher(CapabilityMatcher matcher) {
		this.matcher = matcher;
	}

	public void setThrowOnCapabilityNotPresent(boolean throwOnCapabilityNotPresent) {
		this.throwOnCapabilityNotPresent = throwOnCapabilityNotPresent;
	}

	public String[] getArgs() {
		return args;
	}

	public String prettyPrint() {
		StringBuilder b = new StringBuilder();
		b.append("host: ").append(host).append("\n");
		b.append("port: ").append(port).append("\n");
		b.append("cleanupCycle: ").append(cleanupCycle).append("\n");
		b.append("timeout: ").append(timeout).append("\n");

		b.append("newSessionWaitTimeout: ").append(newSessionWaitTimeout).append("\n");
		b.append("grid1Mapping: ").append(grid1Mapping).append("\n");
		b.append("throwOnCapabilityNotPresent: ").append(throwOnCapabilityNotPresent).append("\n");

		b.append("capabilityMatcher: ").append(matcher == null ? "null" : matcher.getClass().getCanonicalName()).append("\n");
		b.append("prioritizer: ").append(prioritizer == null ? "null" : prioritizer.getClass().getCanonicalName()).append("\n");
		b.append("servlets: ");
		for (String s : servlets) {
			b.append(s.getClass().getCanonicalName() + ",");
		}
		b.append("\n\n");
		b.append("all params :\n");
		List<String> keys = new ArrayList<String>();
		keys.addAll(allParams.keySet());
		Collections.sort(keys);
		for (String s : keys) {
			b.append(s.replaceFirst("-", "") + ":" + allParams.get(s) + "\n");
		}
		return b.toString();
	}

	public Map<String, Object> getAllParams() {
		return allParams;
	}

}
