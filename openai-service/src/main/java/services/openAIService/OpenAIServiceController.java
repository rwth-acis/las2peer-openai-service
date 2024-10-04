package services.openAIService;

import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.UriBuilder;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

@Tag(name = "OpenAIService", description = "A service to make request to OpenAI API functions and connect to the Biwibot service.")
@RestController
@RequestMapping("/")
public class OpenAIServiceController {

	@Autowired
	OpenAIService openAIservice;

	@GetMapping("/swagger.json")
	public ResponseEntity<JSONObject> getSwagger() {
		JSONObject swaggerJson = openAIservice.getSwagger();
		return ResponseEntity.ok(swaggerJson);
	}
	
	@Operation(tags = {"test"}, summary = "Returns success if it works.")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200", description = "Success"), 
		@ApiResponse(responseCode = "500", description = "Fail.")})
	@GetMapping("/test123")
	public String test(@RequestParam(value = "id", defaultValue = "0") int id) {
		System.out.println("OK." + id);
		String s = "OK";
		return s;
	}

	/*
	 * Template of a post function.
	 * 
	 * @return Returns the response generated from openAI
	*/
	@Operation(tags = {"test"}, description = "Method that returns a response generated from openAI")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "A test response from OpenAI",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping("/test")
	public ResponseEntity<JSONObject> test(@RequestBody JSONObject body) {
		JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
		JSONObject openaiBody = new JSONObject();
		JSONObject chatResponse = new JSONObject();
		String model = body.getAsString("model");
		try {
			String prompt = "Who was the first president of the USA?";
				
			String url = "https://api.openai.com/v1/chat/completions";
			
			String openai_api_key = body.getAsString("openaikey");
			
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
		        HashMap<String, String> msgMap = openAIservice.toMap(jsonMsgMap);
		        String role = msgMap.get("role");
		        String content = msgMap.get("content");
		        String name = msgMap.get("name");
		        ChatMessage chatMsg = new ChatMessage(role, content, name);
		        messages.add(chatMsg);
		    }
			int tokens = openAIservice.countMessageTokens(openAIservice.registry, model, messages);
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
    				JSONObject choicesObj = (JSONObject) choices.get(0);
    				JSONObject message = (JSONObject) choicesObj.get("message");
    				textResponse = message.getAsString("content");
    				chatResponse.put("openai", "True");
    			}
				chatResponse.put("tokens", tokens);
    			chatResponse.put("text", textResponse);
            } else {
                chatResponse.put("text", response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An error has occurred.");
        }
		return ResponseEntity.ok(chatResponse);
	}

	/*
	 * Template of a post function.
	 * 
	 * @return Returns the response generated from openAI
	*/
	@Operation(tags = {"personalize"}, description = "Method that returns a response generated from openAI")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "Personalized response generated by OpenAI",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping("/personalize")
	public ResponseEntity<JSONObject> personalize(@RequestBody JSONObject body) {
		JSONParser parser = new JSONParser();
		JSONObject openaiBody = new JSONObject();
		JSONObject chatResponse = new JSONObject();
		
		try {
			// Get the model 
			String model = body.getAsString("model");
			// Get the system messages json array from the body, specified in the bot model
			JSONArray messagesJsonArray = (JSONArray) body.get("messages");
			// Get the conversation history from the body
			JSONArray conversationPathJsonArray = (JSONArray) body.get("conversationPath");
			String url = "https://api.openai.com/v1/chat/completions";

			String openai_api_key = body.getAsString("openaiKey");
			
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
				HashMap<String, String> userMsgMap = openAIservice.toMap(jsonUserMsgMap);
				userMsgMap.put("role", "system");
				userMsgMap.put("name", "example_user");
				JSONObject newJsonUserMsgMap = new JSONObject(userMsgMap);
				
				JSONArray botMessagesJsonArray = new JSONArray();
				JSONArray exampleBotMessagesJsonArray = new JSONArray();
				for (int i = lastUserMsgIdx + 1, size = conversationPathJsonArray.size(); i < size; i++)
			    {
					JSONObject jsonBotMsgMap = (JSONObject) conversationPathJsonArray.get(i);
					botMessagesJsonArray.add(jsonBotMsgMap);
					HashMap<String, String> botMsgMap = openAIservice.toMap(jsonBotMsgMap);
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
			
			// Count tokens
			List<ChatMessage> messages = new ArrayList<ChatMessage>();
		    for (int i = 0 ; i < messagesJsonArray.size(); i++) {
		        JSONObject jsonMsgMap = (JSONObject) messagesJsonArray.get(i);
		        HashMap<String, String> msgMap = openAIservice.toMap(jsonMsgMap);
		        String role = msgMap.get("role");
		        String content = msgMap.get("content");
		        String name = msgMap.get("name");
		        ChatMessage chatMsg = new ChatMessage(role, content, name);
		        messages.add(chatMsg);
		    }
			int tokens = openAIservice.countMessageTokens(openAIservice.registry, model, messages);
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
        } catch (Exception e) {
            e.printStackTrace();
            chatResponse.appendField("text", "An error has occurred.");
        }
		return ResponseEntity.ok(chatResponse);
	}

	@Operation(tags = {"chat"}, description = "Returns a response by OpenAI and classifies the intent")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "Handling default messages from the Bot Model",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Response failed.") 
	})
	@PostMapping("/chat")
	public ResponseEntity<JSONObject> chat(@RequestBody JSONObject body) {
		JSONParser parser = new JSONParser();
		JSONObject openaiBody = new JSONObject();
		JSONObject intentBody = new JSONObject();
		JSONObject chatResponse = new JSONObject();
		JSONObject costs = new JSONObject();
		JSONObject costsIntent = new JSONObject();

		try {
			String model = body.getAsString("model");
			String openaiKey = body.getAsString("openaiKey");
			String systemMessage = body.getAsString("systemMessage");
			String userMessage = body.getAsString("msg");
			String user_email = body.getAsString("user");
			JSONArray messagesJsonArray = new JSONArray();
			JSONObject system = new JSONObject();

			//for intent classification
			String classifyIntent = body.getAsString("classifyIntent");
			// String in_service_context = body.getAsString("in-service-context");
			JSONObject remarks = new JSONObject(costsIntent);
			remarks.put("user", user_email);

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
					costsIntent = openAIservice.costCalculation(responseIntent);
					chatResponse.put("costsIntent", costsIntent);


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
			// MiniClient client = new MiniClient();
			// client.setConnectorEndpoint(url);
			
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
				costs = openAIservice.costCalculation(response);
				chatResponse.put("costs", costs);
			} else {
				chatResponse.put("text", response.toString());
			}
	
		} catch (Throwable e) {
			e.printStackTrace();
			chatResponse.appendField("text", "An error has occurred.");
		}

		return ResponseEntity.ok(chatResponse);
	}

	private static HashMap<String,String> selectedMaterial = new HashMap<String,String> ();

	@Operation(tags = {"biwibotMaterials"}, summary = "Returns all available materials to select from.")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200", description = "Success", content = {@Content(mediaType = "application/json")}), 
		@ApiResponse(responseCode = "500", description = "Fail.")})
	@GetMapping("/biwibotMaterials")
	public ResponseEntity<JSONObject> biwibotMaterials(@RequestParam(value = "channel", defaultValue = "0") int channel){
		
		JSONArray jsonArray = new JSONArray();
		JSONArray interactiveElements = new JSONArray();
		JSONObject lecture = new JSONObject();
		lecture.put("couseid", channel);
		lecture.put("material", "Lecture Material");
		jsonArray.add(lecture);
		lecture.put("intent", "material lecture material");
		lecture.put("label", "Vorlesung");
		lecture.put("description", "Vorlesung");
		lecture.put("isFile", false);
		interactiveElements.add(lecture);

		JSONObject seminar_all = new JSONObject();
		seminar_all.put("couseid", channel);
		seminar_all.put("material", "All Seminar Material");
		jsonArray.add(seminar_all);
		seminar_all.put("intent", "material all seminar material");
		seminar_all.put("label", "Seminar");
		seminar_all.put("description", "Seminar");
		seminar_all.put("isFile", false);
		interactiveElements.add(seminar_all);

		JSONObject all = new JSONObject();
		all.put("couseid", channel);
		all.put("material", "All Material");
		jsonArray.add(all);
		all.put("intent", "material all material");
		all.put("label", "Alle Materialien");
		all.put("description", "Alle Materialien");
		all.put("isFile", false);
		interactiveElements.add(all);

		JSONObject organizational = new JSONObject();
		organizational.put("couseid", channel);
		organizational.put("material", "Organisational Material");
		jsonArray.add(organizational);
		organizational.put("intent", "material organisational material");
		organizational.put("label", "Organisatorisches");
		organizational.put("description", "Organisatorisches");
		organizational.put("isFile", false);
		interactiveElements.add(organizational);

		JSONObject response = new JSONObject();
		response.put("data", jsonArray);
		response.put("interactiveElements", interactiveElements);
		response.put("closeContext", true);
		return ResponseEntity.ok(response);
	}

	@Operation(tags = {"setBiwibotMaterials"}, description = "Set the material for user.")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "Sets the selected materials and respond with set.",content = {@Content(mediaType = "application/json")} ),
		@ApiResponse(responseCode = "500", description = "Setting materials failed.") 
	})
	@PostMapping(value = "/setBiwibotMaterials", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<JSONObject> setBiwibotMaterials(@RequestBody Object body, @RequestParam(value = "channel", defaultValue = "") String channel) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject request = new JSONObject();

		if (body instanceof String) {
			request = (JSONObject) parser.parse((String) body);
		} else if (body instanceof JSONObject) {
			request = (JSONObject) body;
		}
		String material = request.getAsString("material");
		selectedMaterial.put(channel, material);
		System.out.println("Selected Materials are: " + selectedMaterial.toString());
		JSONObject response = new JSONObject();
		response.put("message", "Material " + material + "set.");
		response.put("material", material);
		response.put("closeContext", true);
		return ResponseEntity.ok(response);
	}


	@Operation(tags = {"biwibot"}, description = "Returns the chat response from biwibot.")
	@ApiResponses({ 
		@ApiResponse(responseCode = "200" , description = "Get the chat response from biwibot."),
		@ApiResponse(responseCode = "500", description = "Getting response failed.") 
	})
	@PostMapping(value = "/biwibot", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<JSONObject> biwibot(HttpServletRequest req, @RequestBody Object body) throws IOException, InterruptedException, ParseException {
		JSONParser parser = new JSONParser();
		String contentType = req.getContentType();
		JSONObject request = new JSONObject();

		if (contentType != null && contentType.equals(MediaType.TEXT_PLAIN_VALUE)) {
			request = (JSONObject) parser.parse((String) body);
		} else if (contentType != null && contentType.contains("application/json")) {
			request = (JSONObject) body;
		}
		System.out.println(request);

		String msg = request.getAsString("msg");
		String channel = request.getAsString("channel");
		String material = request.getAsString("material");
		String sbfmUrl = request.getAsString("sbfmUrl");
		System.out.println("Msg:" + request.getAsString("msg"));
		System.out.println("Channel:" + request.getAsString("channel"));
		System.out.println("Material:" + request.getAsString("material"));
		Boolean contextOn = false;
		Boolean contextOff = !contextOn;
		JSONObject chatResponse = new JSONObject();
		JSONObject newEvent = new JSONObject();
		String question = null;
		String orgaChannel = channel;
		JSONObject exit = new JSONObject();
		exit.appendField("channel", channel);
		material = selectedMaterial.get(channel);
		if (selectedMaterial.containsKey(channel)) {
			newEvent.put("material", material);
		} else {
			newEvent.put("material", "None");
		}
		if (!sbfmUrl.equals("default")) {
			if (openAIservice.isActive.containsKey(orgaChannel)) {
				if(openAIservice.isActive.getOrDefault(orgaChannel, false) && !msg.startsWith("!")) {
					openAIservice.response.put("AIResponse", "Einen Moment bitte, ich verarbeite noch deine erste Nachricht.");
					openAIservice.response.put("closeContext", false);
					return ResponseEntity.ok(openAIservice.response);
				} else if (msg.startsWith("!")) {
					exit.appendField("message", "!exit");
					openAIservice.RESTcallBack(sbfmUrl, exit);
					openAIservice.response.appendField("AIResponse", "Nutze bitte das X im Eingabefeld, um zum Hauptmenü zu gelangen.");
					openAIservice.response.appendField("closeContext", true);
					return ResponseEntity.ok(openAIservice.response);
				}
			}
			
			if (msg.contains("!welcome")) {
				exit.appendField("message", "!exit");
				openAIservice.RESTcallBack(sbfmUrl, exit);
				openAIservice.response.appendField("AIResponse", "Nutze bitte das X im Eingabefeld, um zum Hauptmenü zu gelangen.");
				openAIservice.response.appendField("closeContext", true);
				return ResponseEntity.ok(openAIservice.response);
			}

			if (!msg.startsWith("!")){
				openAIservice.isActive.put(channel, true);
				//call biwibot
				openAIservice.biwibotAsync(msg, orgaChannel, sbfmUrl, material);

				ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

				if (!openAIservice.responseBiwi) {
					openAIservice.response.appendField("AIResponse", "Bitte warte einen Moment ich denke darüber nach.");
					openAIservice.response.appendField("channel", channel);
					openAIservice.response.appendField("closeContext", false);

					scheduler.scheduleAtFixedRate(() -> {
						openAIservice.RESTcallBack(sbfmUrl, openAIservice.response);
						if (openAIservice.responseBiwi) {
							openAIservice.response.clear();
							openAIservice.responseBiwi=false;
							scheduler.shutdown();
						}
					}, 0, 20, TimeUnit.SECONDS);
				}

				return ResponseEntity.ok(openAIservice.response);
			} else {
				exit.appendField("message", "!exit");
				exit.appendField("closeContext", true);
				openAIservice.RESTcallBack(sbfmUrl, exit);
				openAIservice.response.appendField("AIResponse", "Exit wird ausgeführt.");
				openAIservice.response.appendField("closeContext", true);
				return ResponseEntity.ok(openAIservice.response);
			}

		} else {

			if (msg.contains("!welcome")) {
				chatResponse.appendField("AIResponse", "Nutze bitte das X im Eingabefeld, um zum Hauptmenü zu gelangen.");
				chatResponse.appendField("closeContext", contextOff);
				
				return ResponseEntity.ok(chatResponse);
			}

			if(!msg.equals("!exit")){
				try {
					question = msg;
					chatResponse.put("channel", channel);
					chatResponse.put("AIenhanced", true);
					newEvent.put("question", question);
					newEvent.put("channel", channel);
					System.out.println("Start calling biwibot...");
					// Make the POST request to localhost:5000/chat
					String url = "https://biwibot.tech4comp.dbis.rwth-aachen.de/generate_response";
					HttpClient httpClient = HttpClient.newHttpClient();
					HttpRequest httpRequest = HttpRequest.newBuilder()
							.uri(UriBuilder.fromUri(url).build())
							.header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofString(newEvent.toJSONString()))
							.build();
					// Send the request
					HttpResponse<String> respond = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
					int responseCode = respond.statusCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						System.out.println("Response from service: " + respond.body());
						
						// Update chatResponse with the result from the POST request
						chatResponse.appendField("AIResponse", respond.body());
						chatResponse.appendField("closeContext", contextOn);
					} else if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
						// Handle unsuccessful response
						chatResponse.appendField("AIResponse", "An error has occurred.");
						chatResponse.appendField("AIResponse", "Biwibot error has occured.");
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
				// JSONObject input = new JSONObject();
				// input.put("message", "!exit");
				// String url = "https://las2peer.tech4comp.dbis.rwth-aachen.der/SBFManager/RESTfulChat/Feedbot/" + channel.split("-")[0] + "/" + channel.split("-")[1];
				// 	HttpClient httpClient = HttpClient.newHttpClient();
				// 	HttpRequest httpRequest = HttpRequest.newBuilder()
				// 			.uri(UriBuilder.fromUri(url).build())
				// 			.header("Content-Type", "application/json")
				// 			.POST(HttpRequest.BodyPublishers.ofString(input.toJSONString()))
				// 			.build();
				// 	// Send the request
				// 	HttpResponse<String> respond = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
				// 	int responseCode = respond.statusCode();
				// System.out.println("Response from SBF:" + responseCode);
				chatResponse.put("message", "Exit AI Tutor, benutze bitte noch einmal das X im Eingabefeld um zum Hauptmenü zu gelangen.");
				chatResponse.put("closeContext", contextOff);
			} else {
				chatResponse.appendField("AIResponse", "Ich habe leider keine Nachricht erhalten.");
			}
		}

		return ResponseEntity.ok(chatResponse);
	}


}
