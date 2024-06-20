package services.openAIService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import org.springframework.stereotype.Service;

// import org.apache.commons.dbcp2.BasicDataSource;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAIService {

    EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
	Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

    HashMap<String, Boolean> isActive = new HashMap<String, Boolean>();
    public JSONObject response = new JSONObject();
    public Boolean responseBiwi = false;

    public void biwibotAsync(@FormDataParam("msg") String msg, @FormDataParam("channel") String orgaChannel, @FormDataParam("sbfmUrl") String sbfmUrl, @FormDataParam("material") @DefaultValue ("default") String material){
		System.out.println("Msg:" + msg);
		System.out.println("Channel:" + orgaChannel);
		Boolean contextOn = false;
		JSONObject chatResponse = new JSONObject();
		JSONObject newEvent = new JSONObject();
		JSONObject error = new JSONObject();
		String channel = orgaChannel;
		try {
			new Thread(new Runnable() {
				public void run() {
					try {
						System.out.println("Thread started.");
						String question = msg;
						response.put("channel", channel);
						response.put("AIenhanced", true);
						chatResponse.put("channel", channel);
						error.put("channel", channel);
						newEvent.put("question", question);
						newEvent.put("channel", channel);
						if (!material.equals("default")) {
							newEvent.put("material", material);
						} else {
							newEvent.put("material", "None");
						}
						System.out.print(newEvent);
						// Make the POST request to localhost:5000/chat
						String url = "https://biwibot.tech4comp.dbis.rwth-aachen.de/generate_response";
						HttpClient httpClient = HttpClient.newHttpClient();
						HttpRequest httpRequest = HttpRequest.newBuilder()
								.uri(UriBuilder.fromUri(url).build())
								.header("Content-Type", "application/json")
								.POST(HttpRequest.BodyPublishers.ofString(newEvent.toJSONString()))
								.build();

						// Send the request
						HttpResponse<String> serviceResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
						int responseCode = serviceResponse.statusCode();

						if (responseCode == HttpURLConnection.HTTP_OK) {
							responseBiwi = true;
							System.out.println("Response from service: " + serviceResponse.body());
							response.appendField("closeContext", contextOn);
							response.appendField("AIResponse", serviceResponse.body());
							chatResponse.appendField("closeContext", contextOn);
							chatResponse.appendField("AIResponse", serviceResponse.body());
							System.out.println(chatResponse);
							// RESTcallBack(sbfmUrl, chatResponse);
						} else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
							responseBiwi = true;
							// Handle unsuccessful response
							response.appendField("AIResponse", "Biwibot error has occured.");
							error.appendField("error", "Biwibot error has occured.");
							RESTcallBack(sbfmUrl, error);
						}
						//System.out.println(chatResponse);
						isActive.put(channel, false);
					} catch ( IOException | InterruptedException e) {
						responseBiwi = true;
						e.printStackTrace();
						error.appendField("error", "An error has occurred.");
						isActive.put(channel, false);
						RESTcallBack(sbfmUrl, error);
					} catch (Throwable e) {
						responseBiwi = true;
						e.printStackTrace();
						error.appendField("error", "An unknown error has occurred.");
						isActive.put(channel, false);
						RESTcallBack(sbfmUrl, error);
					}
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
			isActive.put(channel, false);
			// chatResponse.appendField("text","An error has occured (Exception).");
			return;
		} catch (Throwable e) {
			e.printStackTrace();
			isActive.put(channel,false);
			// chatResponse.appendField("text", "An unknown error has occured.");
			return;
		}

		return;
	}

	public void callBack(String callbackUrl, String channel, JSONObject body, String email){
		try {
			String token = "TestBot:TestBot";    
			System.out.println("Starting callback to botmanager with url: " + callbackUrl+ "/"+ "sendMessageToRocketChatCallback/" + token + "/" + email + "/" + channel);
			Client textClient = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
			String mp = null;
			System.out.println(body);
			mp = body.toJSONString();
			WebTarget target = textClient
					.target(callbackUrl + "/" + "sendMessageToRocketChatCallback" + "/" + token + "/" + email + "/" + channel);
			Response response = target.request()
					.post(javax.ws.rs.client.Entity.entity(mp, MediaType.APPLICATION_JSON));
					String test = response.readEntity(String.class);
			System.out.println("Finished callback to botmanager with response: " + test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void RESTcallBack(String callbackUrl, JSONObject body){
		try {
			System.out.println("Starting callback to botmanager with url: " + callbackUrl + "/AsyncMessage");
			Client textClient = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
			String mp = null;
			System.out.println(body);
			mp = body.toJSONString();
			WebTarget target = textClient
					.target(callbackUrl
					+ "/AsyncMessage");
			Response response = target.request()
					.post(javax.ws.rs.client.Entity.entity(mp, MediaType.APPLICATION_JSON));
					String test = response.readEntity(String.class);
			System.out.println("Finished callback to botmanager with response: " + test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public HashMap<String, String> toMap(JSONObject jsonobj) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String key : jsonobj.keySet()) {
            String value = jsonobj.getAsString(key);
            map.put(key, value);
        }   return map;
    }
	
	public int countMessageTokens(
	        EncodingRegistry registry,
	        String model,
	        List<ChatMessage> messages // consists of role, content and an optional name
	) {
	    Encoding encoding = registry.getEncodingForModel(model).orElseThrow();
	    int tokensPerMessage;
	    int tokensPerName;
	    if (model.startsWith("gpt-4")) {
	        tokensPerMessage = 3;
	        tokensPerName = 1;
	    } else if (model.startsWith("gpt-3.5-turbo")) {
	        tokensPerMessage = 4; // every message follows <|start|>{role/name}\n{content}<|end|>\n
	        tokensPerName = -1; // if there's a name, the role is omitted
	    } else {
	        throw new IllegalArgumentException("Unsupported model: " + model);
	    }

	    int sum = 0;
	    for (final var message : messages) {
	        sum += tokensPerMessage;
	        sum += encoding.countTokens(message.getContent());
	        sum += encoding.countTokens(message.getRole());
	        if (message.hasName()) {
	            sum += encoding.countTokens(message.getName());
	            sum += tokensPerName;
	        }
	    }

	    sum += 3; // every reply is primed with <|start|>assistant<|message|>

	    return sum;
	}

	public JSONObject costCalculation(JSONObject response){
		JSONObject costs = new JSONObject();
		double cost = 0;
		JSONObject usage = (JSONObject) response.get("usage");
		System.out.println(usage);
		int promptTokens = Integer.parseInt(usage.getAsString("prompt_tokens"));
		int completionTokens = Integer.parseInt(usage.getAsString("completion_tokens"));
		int totalTokens = Integer.parseInt(usage.getAsString("total_tokens"));
		String model = response.getAsString("model");

		if (model.startsWith("gpt-3.5-turbo")) {
			double inputCosts = promptTokens * 0.0015;
			double outputCosts = completionTokens * 0.002;
			cost = inputCosts + outputCosts;
		} else if (model.startsWith("gpt-4")) {
			double inputCosts = promptTokens * 0.03;
			double outputCosts = completionTokens * 0.06;
			cost = inputCosts + outputCosts;
		} else if (model.startsWith("gpt-3.5-turbo-16k")) {
			double inputCosts = promptTokens * 0.003;
			double outputCosts = completionTokens * 0.004;
			cost =	inputCosts + outputCosts;
		} else if (model.startsWith("gpt-4-32k")) {
			double inputCosts = promptTokens * 0.06;
			double outputCosts = completionTokens * 0.12;
			cost = inputCosts + outputCosts;
		}

		costs.appendField("model", model);
		costs.appendField("prompt_tokens", promptTokens);
		costs.appendField("completion_tokens", completionTokens);
		costs.appendField("total_tokens", totalTokens);
		costs.appendField("total_cost", cost);
		
		return costs;
	}

}
