package jsystem.extensions.report.difido;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.jfree.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import il.co.topq.difido.model.execution.MachineNode;
import il.co.topq.difido.model.remote.ExecutionDetails;
import il.co.topq.difido.model.test.TestDetails;
import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;

public class DifidoClient {

	private static final String BASE_URI_TEMPLATE = "http://%s:%d/api/";
	private final String baseUri;
	private final HttpClient client;

	public DifidoClient(String host, int port) {
		baseUri = String.format(BASE_URI_TEMPLATE, host, port);
		client = new HttpClient();
	}

	public int addExecution(ExecutionDetails details) throws Exception {
		final PostMethod method = new PostMethod(baseUri + "executions/");
		if (details != null) {
			addMailDetails(details);
			final String descriptionJson = new ObjectMapper().writeValueAsString(details);
			method.setRequestEntity(new StringRequestEntity(descriptionJson,"application/json","utf-8"));
		}
		final int responseCode = client.executeMethod(method);
		handleResponseCode(method, responseCode);
		return Integer.parseInt(method.getResponseBodyAsString());
	}

	public void endExecution(int executionId) throws Exception {
		final PutMethod method = new PutMethod(baseUri + "executions/" + executionId + "?active=false");
		method.setRequestHeader(new Header("Content-Type", "text/plain"));
		final int responseCode = client.executeMethod(method);
		handleResponseCode(method, responseCode);
	}

	public int addMachine(int executionId, MachineNode machine) throws Exception {
		PostMethod method = new PostMethod(baseUri + "executions/" + executionId + "/machines/");
		final ObjectMapper mapper = new ObjectMapper();
		final String json = mapper.writeValueAsString(machine);
		final RequestEntity entity = new StringRequestEntity(json,"application/json","utf-8");
		method.setRequestEntity(entity);
		int responseCode = client.executeMethod(method);
		handleResponseCode(method, responseCode);
		return Integer.parseInt(method.getResponseBodyAsString());
	}

	private void addMailDetails(ExecutionDetails details) {
		if (details == null) return;
		addJsystemPropertyAsExcecutionProperty(details, FrameworkOptions.MAIL_SEND_TO);
		String machineName = null;
		try {
			machineName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			Log.error("Failed to get local machine name!");
			e.printStackTrace();
		};
		addJsystemPropertyAsExcecutionProperty(details, FrameworkOptions.MAIL_FROM_ACCOUNT_NAME, machineName);
		addJsystemPropertyAsExcecutionProperty(details, FrameworkOptions.MAIL_SUBJECT);
	}

	private void addJsystemPropertyAsExcecutionProperty(ExecutionDetails details, FrameworkOptions frameworkOption) {
		addJsystemPropertyAsExcecutionProperty(details, frameworkOption, null);
	}
	private void addJsystemPropertyAsExcecutionProperty(ExecutionDetails details, FrameworkOptions frameworkOption, String defaultValue) {
		String key = frameworkOption.getString();
		JSystemProperties jsystemProperties = JSystemProperties.getInstance();
		String value = jsystemProperties.getPreference(frameworkOption);
		if (value == null)
			value = defaultValue;
		if (value == null) return;
		Log.info(String.format("Adding Jsystem Property as scenario property: %s:%s", key, value));
		addExecutionProperty(details, key, value);
	}

	private void addExecutionProperty(ExecutionDetails details, String key, String value) {
		Map<String, String> executionProperties = details.getExecutionProperties();
		if (executionProperties == null) {
			details.setExecutionProperties(new HashMap<String, String>());
		}
		executionProperties = details.getExecutionProperties();
		executionProperties.put(key, value);
		details.setExecutionProperties(executionProperties);
	}

	public void updateMachine(int executionId, int machineId, MachineNode machine) throws Exception {
		PutMethod method = new PutMethod(baseUri + "executions/" + executionId + "/machines/" + machineId);
		final ObjectMapper mapper = new ObjectMapper();
		final String json = mapper.writeValueAsString(machine);
		final RequestEntity entity = new StringRequestEntity(json,"application/json","utf-8");
		method.setRequestEntity(entity);
		int responseCode = client.executeMethod(method);
		handleResponseCode(method, responseCode);
	}

	public void addTestDetails(int executionId, TestDetails testDetails) throws Exception {
		PostMethod method = new PostMethod(baseUri + "executions/" + executionId + "/details");
		final ObjectMapper mapper = new ObjectMapper();
		final String json = mapper.writeValueAsString(testDetails);
		final RequestEntity entity = new StringRequestEntity(json,"application/json","utf-8");
		method.setRequestEntity(entity);
		final int responseCode = client.executeMethod(method);
		handleResponseCode(method, responseCode);
	}

	public void addFile(final int executionId, final String uid, final File file) throws Exception {
		PostMethod method = new PostMethod(baseUri + "executions/" + executionId + "/details/" + uid + "/file/");
		Part[] parts = new Part[] { new FilePart("file", file) };
		method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));
		final int responseCode = client.executeMethod(method);
		handleResponseCode(method, responseCode);
	}

	private void handleResponseCode(HttpMethod method, int responseCode) throws Exception {
		if (responseCode != 200 && responseCode != 204) {
			throw new Exception("Request was not successful. Response is: " + responseCode + ".\n Response body: "
					+ method.getResponseBodyAsString());
		}

	}

}
