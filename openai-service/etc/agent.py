from langchain.callbacks import get_openai_callback
from langchain.chat_models import ChatOpenAI
from langchain.agents import AgentExecutor, ConversationalChatAgent
from langchain.agents.tools import Tool

def defaultAgent(OPENAI_API_KEY, prompt):
    llm = ChatOpenAI(temperature=0, model="gpt-3.5-turbo", openai_api_key=OPENAI_API_KEY)
    agent = ConversationalChatAgent.from_llm_and_tools(llm=llm, system_message=prompt, verbose=True, max_iterations=3)
    
    return agent 

def chat(authToken, prompt ,input):
    OPENAI_API_KEY = authToken
    with get_openai_callback() as cb:
        try: 
            agent = defaultAgent(OPENAI_API_KEY, prompt)
            answer = agent.run(input)
            return str(answer)
        except Exception as err:
            return 'Exception occurred: ' + str(err)