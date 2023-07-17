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



The REST-API will be available via *http://localhost:8080/openapi* and the las2peer node is available via port 9011.

## Bot Functions

| Path | Function name | Description | Parameters | Returns |
|-----|-----|-------------|---------|---------------|
| /personalize | personalize | Sends the current conversation path, including the most recent prompt and the bot's original response to it, to OpenAI's chat completion function to generate a more personalized response. |  | |
