package i5.las2peer.services.openAIService;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.nio.file.Paths;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.java_websocket.util.Base64;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.nimbusds.openid.connect.sdk.util.Resource;

import org.json.*;
import org.web3j.abi.datatypes.Int;

import i5.las2peer.connectors.webConnector.client.ClientResponse;
import i5.las2peer.connectors.webConnector.client.MiniClient;
import i5.las2peer.api.Context;
import i5.las2peer.api.ManualDeployment;
import i5.las2peer.api.logging.MonitoringEvent;
import i5.las2peer.api.security.UserAgent;
import i5.las2peer.logging.L2pLogger;
import i5.las2peer.restMapper.RESTService;
import i5.las2peer.restMapper.annotations.ServicePath;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.util.Json;
import kotlin.contracts.Returns;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import net.minidev.json.JSONValue;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;


// TODO Describe your own service
/**
 * las2peer-Template-Service
 * 
 * This is a template for a very basic las2peer service that uses the las2peer WebConnector for RESTful access to it.
 * 
 * Note: If you plan on using Swagger you should adapt the information below in the SwaggerDefinition annotation to suit
 * your project. If you do not intend to provide a Swagger documentation of your service API, the entire Api and
 * SwaggerDefinition annotation should be removed.
 * 
 */
// TODO Adjust the following configuration
@Api
@SwaggerDefinition(
		info = @Info(
				title = "las2peer OpenAI Service",
				version = "1.0.0",
				description = "A las2peer wrapper service for the social-bot-manager service to make request to OpenAI API functions.",
				termsOfService = "https://tech4comp.de/",
				contact = @Contact(
						name = "Samuel Kwong",
						email = "samuel.kwong@rwth-aachen.de"),
				license = @License(
						name = "CC0",
						url = "https://github.com/rwth-acis/las2peer-openai-service/blob/main/LICENSE")))
@ServicePath("/openai")
// TODO Your own service class
public class OpenAIService extends RESTService {
	
	EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
	Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);
	
	/*
	 * Template of a post function.
	 * 
	 * @return Returns the response generated from openAI
	*/
	@POST
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "A test response from OpenAI") })
	@ApiOperation(
			value = "test",
			notes = "Method that returns a response generated from openAI")
	public Response test(String body) {
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject jsonBody = null;
		JSONObject openaiBody = new JSONObject();
		JSONObject chatResponse = new JSONObject();
		
		try {
			jsonBody = (JSONObject) parser.parse(body);
			// Get the model 
			String model = jsonBody.getAsString("model");
			String prompt = "Who was the first president of the USA?";
				
			String url = "https://api.openai.com/v1/chat/completions";
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(url);
			
			String openai_api_key = jsonBody.getAsString("openaiKey");
			
			JSONArray messagesJsonArray = new JSONArray();
			HashMap<String, String> userMsgMap = new HashMap<String,String>();
			userMsgMap.put("role", "user");
			userMsgMap.put("content", prompt);
			JSONObject newJsonUserMsgMap = new JSONObject(userMsgMap);			
				
			messagesJsonArray.add(newJsonUserMsgMap);
			
			openaiBody.put("model", model);
			openaiBody.put("messages", messagesJsonArray);
			System.out.println(messagesJsonArray);
			
			// Count tokens
			List<ChatMessage> messages = new ArrayList<ChatMessage>();
		    for (int i = 0 ; i < messagesJsonArray.size(); i++) {
		        JSONObject jsonMsgMap = (JSONObject) messagesJsonArray.get(i);
		        HashMap<String, String> msgMap = toMap(jsonMsgMap);
		        String role = msgMap.get("role");
		        String content = msgMap.get("content");
		        String name = msgMap.get("name");
		        ChatMessage chatMsg = new ChatMessage(role, content, name);
		        messages.add(chatMsg);
		    }
			int tokens = countMessageTokens(registry, model, messages);
			System.out.println("TOKENS TO BE USED: " + tokens);
			
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(UriBuilder.fromUri(url).build())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openai_api_key)
                    .POST(HttpRequest.BodyPublishers.ofString(openaiBody.toJSONString()))
                    .build();

            // Send the request
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int responseCode = httpResponse.statusCode();
            JSONObject response = (JSONObject) parser.parse(httpResponse.body());

            if (responseCode == HttpURLConnection.HTTP_OK) {
            	
            	String textResponse = "";
    			JSONArray choices = (JSONArray) response.get("choices");
    			if (choices == null) {
    				textResponse = response.toString();
    			} else {
    				// System.out.println(choices);
    				JSONObject choicesObj = (JSONObject) choices.get(0);
    				JSONObject message = (JSONObject) choicesObj.get("message");
    				// System.out.println(message);
    				textResponse = message.getAsString("content");
    				//chatResponse.put("openai", "True");
    				// System.out.println(textResponse);
    			}
				chatResponse.put("tokens", tokens);
    			chatResponse.put("text", textResponse);
            } else {
                chatResponse.put("text", response.toString());
            }
        } catch (ParseException | IOException | InterruptedException e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An error has occurred.");
        }
		return Response.ok().entity(chatResponse).build();
	}

	/*
	 * Template of a post function.
	 * 
	 * @return Returns the response generated from openAI
	*/
	@POST
	@Path("/personalize")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Personalized response generated by OpenAI") })
	@ApiOperation(
			value = "personalize",
			notes = "Method that returns a response generated from openAI")
	public Response personalize(String body) {
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject jsonBody = null;
		JSONObject openaiBody = new JSONObject();
		JSONObject chatResponse = new JSONObject();
		
		try {
			jsonBody = (JSONObject) parser.parse(body);
			// Get the model 
			String model = jsonBody.getAsString("model");
			// Get the system messages json array from the body, specified in the bot model
			JSONArray messagesJsonArray = (JSONArray) jsonBody.get("messages");
			// Get the conversation history from the body
			JSONArray conversationPathJsonArray = (JSONArray) jsonBody.get("conversationPath");
				
			String url = "https://api.openai.com/v1/chat/completions";
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(url);

			String openai_api_key = jsonBody.getAsString("openaiKey");
			
			// TODO: Prepare openaiBody 
			//messagesJsonArray already formatted as [{"role":"system", "content":"You are a helpful assistant"}]
			//conversationPathJsonArray formatted as [{"role":"user", "content":"Hi"}, {"role":"assistant","content":"Hi, how are you doing?"}]
			// Append the conversationPathJsonArray to messagesJsonArray, replace the role of the last assistant message with "example_assistant"
			if (conversationPathJsonArray != null) {
				
				// Get the index of the last user message in the conversation
				// Convert the message to a example user message 
				// Then get all following messages, which we assume to be assistant messages
				// Convert the message(s) to an example assistant message
				// Remove the assistant messages from the conversation path array
				// Add the example responses to the messages array befor ethe last user message
				int lastUserMsgIdx = conversationPathJsonArray.size()-2;
				
				for (int i = 0, size = conversationPathJsonArray.size(); i < size; i++)
			    {
			      JSONObject jsonMsgMap = (JSONObject) conversationPathJsonArray.get(i);
			      if (jsonMsgMap.getAsString("role").equals("user")) {
			    	  lastUserMsgIdx = i;
			      }
			    }
				
				JSONObject jsonUserMsgMap = (JSONObject) conversationPathJsonArray.get(lastUserMsgIdx);
				HashMap<String, String> userMsgMap = toMap(jsonUserMsgMap);
				userMsgMap.put("role", "system");
				userMsgMap.put("name", "example_user");
				JSONObject newJsonUserMsgMap = new JSONObject(userMsgMap);
				
				JSONArray botMessagesJsonArray = new JSONArray();
				JSONArray exampleBotMessagesJsonArray = new JSONArray();
				for (int i = lastUserMsgIdx + 1, size = conversationPathJsonArray.size(); i < size; i++)
			    {
					JSONObject jsonBotMsgMap = (JSONObject) conversationPathJsonArray.get(i);
					botMessagesJsonArray.add(jsonBotMsgMap);
					HashMap<String, String> botMsgMap = toMap(jsonBotMsgMap);
					botMsgMap.put("role", "system");
					botMsgMap.put("name", "example_assistant");
					JSONObject newJsonBotMsgMap = new JSONObject(botMsgMap);
					exampleBotMessagesJsonArray.add(newJsonBotMsgMap);
			    }
				
				//Remove the non example bot's response from the conversation path array
				for (int i = 0, size = botMessagesJsonArray.size(); i < size; i++)
			    {
					JSONObject jsonBotMsgMap = (JSONObject) botMessagesJsonArray.get(i);
					conversationPathJsonArray.remove(jsonBotMsgMap);
			    }
				
				//Add the example messages before the user prompt
				conversationPathJsonArray.add(lastUserMsgIdx, newJsonUserMsgMap);
				for (int i = 0, size = exampleBotMessagesJsonArray.size(); i < size; i++)
			    {
			      JSONObject jsonMsgMap = (JSONObject) exampleBotMessagesJsonArray.get(i);
			      conversationPathJsonArray.add(lastUserMsgIdx + 1 + i, jsonMsgMap);
			    }
				
				messagesJsonArray.addAll(conversationPathJsonArray);
			}
			
			openaiBody.put("model", model);
			openaiBody.put("messages", messagesJsonArray);
			System.out.println(messagesJsonArray);
			
			// Count tokens
			List<ChatMessage> messages = new ArrayList<ChatMessage>();
		    for (int i = 0 ; i < messagesJsonArray.size(); i++) {
		        JSONObject jsonMsgMap = (JSONObject) messagesJsonArray.get(i);
		        HashMap<String, String> msgMap = toMap(jsonMsgMap);
		        String role = msgMap.get("role");
		        String content = msgMap.get("content");
		        String name = msgMap.get("name");
		        ChatMessage chatMsg = new ChatMessage(role, content, name);
		        messages.add(chatMsg);
		    }
			int tokens = countMessageTokens(registry, model, messages);
			System.out.println("TOKENS TO BE USED: " + tokens);
			
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(UriBuilder.fromUri(url).build())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openai_api_key)
                    .POST(HttpRequest.BodyPublishers.ofString(openaiBody.toJSONString()))
                    .build();

            // Send the request
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int responseCode = httpResponse.statusCode();
            JSONObject response = (JSONObject) parser.parse(httpResponse.body());

            if (responseCode == HttpURLConnection.HTTP_OK) {
            	
            	String textResponse = "";
    			JSONArray choices = (JSONArray) response.get("choices");
    			if (choices == null) {
    				textResponse = response.toString();
    			} else {
    				// System.out.println(choices);
    				JSONObject choicesObj = (JSONObject) choices.get(0);
    				JSONObject message = (JSONObject) choicesObj.get("message");
    				// System.out.println(message);
    				textResponse = message.getAsString("content");
    				chatResponse.put("openai", "True");
    				// System.out.println(textResponse);
    			}
				chatResponse.put("tokens", tokens);
    			chatResponse.put("text", textResponse);
            } else {
                chatResponse.put("text", response.toString());
            }
        } catch (ParseException | IOException | InterruptedException e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An error has occurred.");
        }
		return Response.ok().entity(chatResponse).build();
	}
	

	@POST
	@Path("/chat")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(
			value = { @ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Handling default messages from the Bot Model") })
	@ApiOperation(
			value = "chat",
			notes = "Returns a response by OpenAI and classifies the intent")
	public Response chat(String body) {
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject jsonBody = null;
		JSONObject openaiBody = new JSONObject();
		JSONObject intentBody = new JSONObject();
		JSONObject chatResponse = new JSONObject();
		JSONObject costs = new JSONObject();
		JSONObject costsIntent = new JSONObject();

		try {
			jsonBody = (JSONObject) parser.parse(body);
			String model = jsonBody.getAsString("model");
			String openaiKey = jsonBody.getAsString("openaiKey");
			String systemMessage = jsonBody.getAsString("systemMessage");
			String userMessage = jsonBody.getAsString("msg");
			String user_email = jsonBody.getAsString("user");
			JSONArray messagesJsonArray = new JSONArray();
			JSONObject system = new JSONObject();

			//for intent classification
			String classifyIntent = jsonBody.getAsString("classifyIntent");
			String in_service_context = jsonBody.getAsString("in-service-context");
			JSONObject remarks = new JSONObject(costsIntent);
			remarks.put("user", user_email);
			remarks.put("in-service-context", in_service_context);
			String caseID = jsonBody.getAsString("caseID");
			String resource = jsonBody.getAsString("Resource");
			String time = jsonBody.getAsString("TIME_OF_EVENT");
			JSONArray intentMessageJsonArray = new JSONArray();
			JSONObject intent = new JSONObject();

			JSONObject user = new JSONObject();
			user.put("role", "user");
			user.put("content", userMessage);

			//classify the intent of the user message using GPT and store it into SQL database
			if (classifyIntent.contains("true")) {
				String intentMessage = "You are a intent classifier that classifies the intent of the user message. The intent should contain a verb and a noun. Here is an example: User message: 'I want to book a flight to Berlin.', Answer: 'bookFlight'.";
				intent.put("role", "system");
				intent.put("content", intentMessage);
				intentMessageJsonArray.add(intent);
				intentMessageJsonArray.add(user);
				intentBody.put("messages",intentMessageJsonArray);
				intentBody.put("model", model);

				String url = "https://api.openai.com/v1/chat/completions";
				MiniClient client = new MiniClient();
				client.setConnectorEndpoint(url);
				
				HttpClient httpClientIntent = HttpClient.newHttpClient();
				HttpRequest httpRequestIntent = HttpRequest.newBuilder()
						.uri(UriBuilder.fromUri(url).build())
						.header("Content-Type", "application/json")
						.header("Authorization", "Bearer " + openaiKey)
						.POST(HttpRequest.BodyPublishers.ofString(intentBody.toJSONString()))
						.build();
						
				// Send the request
				HttpResponse<String> httpResponse = httpClientIntent.send(httpRequestIntent, HttpResponse.BodyHandlers.ofString());
				int responseCodeIntent = httpResponse.statusCode();
				JSONObject responseIntent = (JSONObject) parser.parse(httpResponse.body());
				
				if (responseCodeIntent == HttpURLConnection.HTTP_OK) {
					String textResponseIntent = "";
					JSONArray choices = (JSONArray) responseIntent.get("choices");

					if (choices == null) {
						textResponseIntent = responseIntent.toString();
					} else {
						System.out.println(choices);
						JSONObject choicesObj = (JSONObject) choices.get(0);
						JSONObject message = (JSONObject) choicesObj.get("message");
						System.out.println(message);
						textResponseIntent = message.getAsString("content");
						System.out.println(textResponseIntent);
					}

					chatResponse.put("Intent", textResponseIntent);
					costsIntent = costCalculation(responseIntent);
					chatResponse.put("costsIntent", costsIntent);

					//Save data to SQL database
					// PreparedStatement stmt = null;
					// Connection conn = null;
					// try {
					// 	conn = datasource.getConnection();

					// 	stmt = conn.prepareStatement("INSERT INTO MESSAGE (`Event`, `REMARKS`, `CASE_ID`, `ACTIVITY_NAME`, `RESOURCE`, `RESOURCE_TYPE`, `TIME_OF_EVENT`) VALUES (?, ?, ?)");
					// 	stmt.setString(1, "SERVICE_CUSTOM_MESSAGE_1");
					// 	stmt.setString(2, remarks.toJSONString());
					// 	stmt.setString(3, caseID);
					// 	stmt.setString(4, textResponseIntent);
					// 	stmt.setString(5, in_service_context);
					// 	stmt.setString(6, resource);
					// 	stmt.setString(7, "bot");
					// 	stmt.setString(8, time);
					// 	stmt.executeUpdate();
					// } catch (SQLException e) {
					// 	e.printStackTrace();
					// } finally {
					// 	try {
					// 		if (stmt != null)
					// 			stmt.close();
					// 	} catch (Exception e) {
					// 		e.printStackTrace();
					// 	}
					// 	;
					// 	try {
					// 		if (conn != null) 
					// 			conn.close();
					// 	} catch (Exception e) {
					// 		e.printStackTrace();
					// 	}
					// }
				} else {
					chatResponse.put("intent", responseIntent.toString());
				}
			} else {
				chatResponse.put("intent", "No intent classification");
			}

			//setup message array for the openai request to answer the user message
			if (systemMessage != null) {
				system.put("role", "system");
				system.put("content", systemMessage); 
				messagesJsonArray.add(system);
				messagesJsonArray.add(user);
				openaiBody.put("messages",messagesJsonArray);
			} else {
				system.put("role", "system");
				system.put("content", "You are a helpul assistant that helps students with their questions.");
				messagesJsonArray.add(system);
				messagesJsonArray.add(user);
				openaiBody.put("messages",messagesJsonArray);
			}

			openaiBody.put("model", model);
			
			String url = "https://api.openai.com/v1/chat/completions";
			MiniClient client = new MiniClient();
			client.setConnectorEndpoint(url);
			
			HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(UriBuilder.fromUri(url).build())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openaiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(openaiBody.toJSONString()))
                    .build();

            // Send the request
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int responseCode = httpResponse.statusCode();
            JSONObject response = (JSONObject) parser.parse(httpResponse.body());
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				String textResponse = "";
				JSONArray choices = (JSONArray) response.get("choices");
				if (choices == null) {
					textResponse = response.toString();
				} else {
					System.out.println(choices);
					JSONObject choicesObj = (JSONObject) choices.get(0);
					JSONObject message = (JSONObject) choicesObj.get("message");
					System.out.println(message);
					textResponse = message.getAsString("content");
					System.out.println(textResponse);
				}
				chatResponse.put("text", textResponse);
				costs = costCalculation(response);
				chatResponse.put("costs", costs);
			} else {
				chatResponse.put("text", response.toString());
			}
	
		} catch (Throwable e) {
			e.printStackTrace();
			chatResponse.appendField("text", "An error has occurred.");
		}

		return Response.ok().entity(chatResponse).build();
	}

	@POST
	@Path("/biwibot")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiResponses(
		value = { 
				@ApiResponse(
					code = HttpURLConnection.HTTP_OK,
					message = "Connected.")})
	@ApiOperation(
			value = "Get the chat response from biwibot",
			notes = "Returns the chat response from biwibot")
	public Response biwibot(@FormDataParam("msg") String msg, @FormDataParam("channel") String channel) {
		System.out.println("Msg:" + msg);
		System.out.println("Channel:" + channel);
		Boolean contextOn = false;
		JSONObject chatResponse = new JSONObject();
		JSONObject newEvent = new JSONObject();
		String question = null;
		
		if(!msg.equals("!exit")){
			try {
				question = msg;
				chatResponse.put("channel", channel);
				newEvent.put("question", question);
				newEvent.put("channel", channel);
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
				HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
				int responseCode = response.statusCode();

				if (responseCode == HttpURLConnection.HTTP_OK) {
					System.out.println("Response from service: " + response.body());
					
					// Update chatResponse with the result from the POST request
					chatResponse.appendField("AIResponse", response.body());
					chatResponse.appendField("closeContext", contextOn);
				} else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
					// Handle unsuccessful response
					chatResponse.appendField("AIResponse", "An error has occurred.");
				}
				//System.out.println(chatResponse);
			} catch ( IOException | InterruptedException e) {
				e.printStackTrace();
				chatResponse.appendField("AIResponse", "An error has occurred.");
			} catch (Throwable e) {
				e.printStackTrace();
				chatResponse.appendField("AIResponse", "An unknown error has occurred.");
			}

		} else if (msg.equals("!exit")){
			chatResponse.appendField("AIResponse", "Exit ausgeführt");
		} else {
			chatResponse.appendField("AIResponse", "Ich habe leider keine Nachricht bekommen.");
		}
		
		return Response.ok().entity(chatResponse.toString()).build();
	}

	public static HashMap<String, String> toMap(JSONObject jsonobj) {
        HashMap<String, String> map = new HashMap<String, String>();
        for (String key : jsonobj.keySet()) {
            String value = jsonobj.getAsString(key);
            map.put(key, value);
        }   return map;
    }
	
	private int countMessageTokens(
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

	private JSONObject costCalculation(JSONObject response){
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
