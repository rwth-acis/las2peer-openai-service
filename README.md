<h1 align="center">las2peer-openai-service</h1>

This service acts as a wrapper for the [social bot manager service](https://github.com/rwth-acis/las2peer-social-bot-manager-service) to use OpenAI API functions. 

## Preparations

### Java


### Build Dependencies


### Important Repositories: 

- Social-Bot-Manager-Service: https://github.com/rwth-acis/las2peer-Social-Bot-Manager-Service
- OpenAI API: https://platform.openai.com/docs/api-reference

How to run with the Social-Bot-Manager Service
-------------------

Start a las2peer network as usual with the Social-Bot-Manager service at the default port 9011

Gradle build the las2peer-openai-service and add your IP Address to the bootstrap flag -b YOUR_IP_ADDRESS:9011 in /bin/start_network.sh

```bash
java -cp ... --port 9012 -b YOUR_IP_ADDRESS:9011 --service-directory service uploadStartupDirectory startService\(\'i5.las2peer.services.openAIService.OpenAIService@1.0.0\'\) startWebConnector interactive
```



The REST-API will be available via *http://localhost:8080/openai* and the las2peer node is available via port 9011.

## Bot Functions

| Path | Function name | Description | Parameters | Returns |
|-----|-----|-------------|---------|---------------|
| /personalize | personalize | Sends the current conversation path, including the most recent prompt and the bot's original response to it, to OpenAI's chat completion function to generate a more personalized response. | **model**: specify an openai model<br /> **openaikey**: insert your own openaiKey <br /> **messages**: Contains a list of system messages that primes chatGPT. Each message is formatted as a map, with "role" and "content" as the keys. (e.g. Role: "system", content: "You are an assistant that personalizes the generated example assistant message and returns it") <br /> **conversationPath**: Contains the entire conversation path between the user and the bot along with the most recent user prompt, with each message structured in the same way as specified above  | **openai** A flag that indicates the request was an openai request <br /> **text**: The personalized response from OpenAI <br /> **tokens**: total tokens used |
| /chat | chat | Handles standard messages that are not recognized by the NLU training model and forwards the message to OpenAI's chat completion function to not interrupt the chat flow. | **model**: specify an openai model<br /> **openaikey**: insert your own openaiKey <br /> **systemMessage**: define your own system Prompt (Default: "You are a helpul assistant that helps students with their questions.") <br /> **msg**: user message | **text**: Response from OpenAI <br /> **costs**: total costs and total tokens used |
