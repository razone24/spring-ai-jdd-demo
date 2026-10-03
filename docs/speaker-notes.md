# Speaker Notes — index.html

<!-- Edit note text freely. Keep each '## Slide N' header line intact —
     apply maps blocks to <aside class="notes"> by order. -->

## Slide 1 — The Future of Software Architecture in the Age of AI

Every presentation has to be about AI these days, right? And fair enough — it's been the defining buzzword for the past three years, showing up everywhere from corporate decks to casual conversations with friends and colleagues. It moves so fast that even this presentation is struggling to keep up. A year ago, we were still asking whether AI was here to stay, tracing its evolution from the nineties to the present. But honestly, what happened in the last three years matters far more than everything that came before it. We all know the moment: late 2022, ChatGPT launched, and neither the world nor our industry has been the same since.

## Slide 2 — 2026: The Year of Agents

But it didn't all happen overnight. In 2023, AI entered our IDEs — copilots started autocompleting code, one snippet at a time. In 2024, teams began wiring RAG pipelines to real services, giving models actual knowledge of the world they were operating in. By 2025, orchestration patterns had matured and AI was gaining real autonomy. And now, in 2026, we stop experimenting. Agents are making decisions. Agents are shipping. Models got smarter. Systems got agentic.

## Slide 3 — Razvan Balasa

Hello everyone, my name is Razvan. I'm a Java Technical Lead at Playtika and a software engineer with eight years of experience — playing with pretty much every tool in the box, from writing features and crafting deployment pipelines, to shaping the infrastructure and architecture of the systems I work on.

## Slide 4 — Adrian Coman

I am Adrian. I have around 10 years experience across several industries: banking, telecom, gaming, security. Currently I'm part of GenAI team @Crowdstrike where I'm building solutions with and for AI. But enough about be me, let's dive right in.

## Slide 5 — What are AI agents?

Any ideas...? In simple words, An AI Agent is a system that can act on its own to achieve a goal. Unlike a regular chatbot that just answers questions, an AI Agent can: - Plan tasks - Make decisions - Take actions. Think of it like giving the AI a mission—and it figures out how to complete it, step by step, sometimes even adjusting along the way. For example: instead of sayin' what are the top rated steakhouses in Kraków? you would say to an agent: I'm at JDD 2026 ( he has to figure it out where it's taking place), and i would like to have a stake with a couple of friends. Look up top rated stakehoses around and book a reservation for today if they have available tables. And he will try to do just that.

## Slide 6 — From prompts to a harness

Starting from 2022 till now there were 3 main changes in how we work with AI. As title says we went from writing prompts to building harnesses. Let me walk you through how we got here. It started with prompts. The idea was simple, you ask better, get better. For a while, that was the whole game. Then we learned the words weren't enough. The focus moved to context: larger memory, history, documents. Now the motto was: Feed it well, and it answers well. But even a smart model with good context has one limit: it can only talk. It can't act, check its work, or fix its own mistakes. That's why we're here now, at Harness Engineering. The harness is everything around the model, and provides: tools, memory, loops, permissions, checks, retries. Think of a car. The model is the engine; the harness is the rest of the vehicle — wheels, brakes, steering. A great engine on the ground goes nowhere; you'll need the whole car for that. This is the key idea. An agent is not just a model. An agent is a model plus a harness. Takeaway here is that the best model alone won't win, frontier models are starting to look alike anyway. The real difference now is the harness around them. The next wave of AI won't be about which model you pick. It'll be about how well you build or choose the harness around it.

## Slide 7 — Software architectures with AI

Now back to software arhitecture. AI agents are getting smarter and more useful, and everyone expects they’ll soon be used in production if not already. But WHY: One big reason for this shift is the huge amount of data we deal with today. Traditional software can’t keep up, but AI can learn and adapt, making it a much better tool for the job. HOW: The key to making this work is by interacticting with an LLM from our software. LLMs can understand what you mean and help create clear, useful text outputs.

## Slide 8 — Retrieval‑Augmented Generation

Ok but, LLMs are not magical creatures, it won't solve any issues just by hooking them to your software. It needs help, and one way to help it is via RAGs ( which stangs for ...). You can think of it like any knowleadge sources which are outside of the llm, thay can be vectorised databases, apis, web searches etc. Not let's have an example: Imagine an smart AI companion inside a Mario game, not just one that's only hooked up to o standalone model. You are stuck in end game and you asks it a question: "How do I beat Bowser in the final castle?" Natural language, just like talking to a friend who just finished the game. The smart AI companian will look up into an external data source for Mario tips, (this is the RAG part) Next step is to build actual prompt. It takes the external infomation and combines them with your question. Best part now is that the the model will be less likely to halucinate anymore since it has factual informations from external sources. Lastly, the LLM will answer you back: "Dodge his fire breath, grab his tail when he charges, and throw him into the bombs behind him-three times to win."

## Slide 9 — RAG benefits

Let's now brifly look on the benefits of RAG: More relevant, factual outputs — Answers come from your data which will make it less likely to halucinate. Update knowledge without retraining — The model stays the same, you just need to update the external source which should be much cheaper. Lower cost with guardrails.

## Slide 10 — MCP — Model Context Protocol

Quick show of hands — anyone here know MCP? What is it, or what's it used for? MCP — the Model Context Protocol — is an open standard that lets an AI agent talk to external tools in one consistent way. Think of it like USB-C for AI: instead of a custom cable for every device, one connector fits them all. On the left side: Without MCP, you will have to write code for each new integration that you agent needs, which will become messy eventually. On the right, MCP simplifies the model. Your agent needs to have a MCP client and each tool ships needs to provide a MCP server. Adding a new tool goes from "build a custom integration" to "plug in another server", no need to change your application.

Now that you master the basics, I’ll let Razvan to present you one real world scenario.

## Slide 11 — Real-world scenario

Picture a scenario most of us run into more often than we'd like. That Teams notification pops up, and you're already bracing yourself. Someone in your feature's channel is asking how to configure the rewards distribution — a question you've answered a dozen times before. But it never comes at a good time. Either you're deep in the flow of building something new and the last thing you need is a context switch, or enough time has passed that giving a proper answer would actually require digging back in yourself. You're a developer and an AI enthusiast, so naturally your mind goes straight to automation. And conveniently, you just heard from us that AI agent creation is exactly where things are heading.

## Slide 12 — Naive solution

So you grab a pen and paper, or open a sketching app, and start designing the solution. You decide that a chatbot registered to that Teams channel will forward messages through a gateway straight into your Chatbot App. You've written plenty of wiki pages over the years, so you have enough data to answer most feature-related questions — you load that into memory to serve it to the model. Quick disclaimer — this is an oversimplified example. In reality, wiki data would also go through a gateway, and loading it into memory on every request isn't ideal. But bear with us. Once you realize you just need a solid prompt, you bundle the wiki data and the Teams question together into the context, send the request, and forward the LLM's response back to the chatbot.

## Slide 13 — Benefits & challenges

Now let’s see what we have achieved so far. We managed to process a lot of data at once, so there is no need for us to search for that wiki page that answers the asked question. However, adding all this data might exceed the maximum context window of any available LLM. Creating this simple solution will guarantee us a fast time to market, but for each asked question, the request will be forwarded to the LLM. Naturally, the costs will explode if two people start chatting in our Teams channel. To have a focused answer, we’ll use our prompt engineering skills. Unfortunately, with this amount of data (all the wikis that are not necessarily related to our question), we’ll encounter a lot of hallucinations and wrong answers.

## Slide 14 — RAG with semantic caching

We go back to the drawing board and remember that we heard from Adi about RAG systems. To obtain a better context for the prompt, we decide to implement semantic caching. We take all the data from the wiki pages and run it through a document vectorization application (there are many third-party apps capable of performing this task) and then store the data in a vector database. Remember that this is an overly simplified example. If we would like to have the database in sync with each update or new wiki page, we would need some kind of triggers to reprocess the data, but this is a discussion for another time. Having the data in a vector database will allow us to take the question asked in the Teams channel and perform a database search, retrieving data based on semantic similarity. After this step, we’ll use the returned data alongside the asked question as context for the LLM and return the response to the Teams chatbot.

## Slide 15 — Benefits & challenges

Let’s see how we improved the system. We managed to inhance the promts with focused and specific data, but in order to create the database vectors we have to rely on 3rd party solutions which might be expensive or inaccurate depending on the solution. We managed to retrieved only wiki data that is related to the asked question in a short time due to database capabilities, but we introduced a new component in our system which is an external storage. By using only the data that is related with the asked question, we rely less on the model’s knowledge about the subject reducing the hallucinations that it might have, but we complicated the architecture of our system a bit and the costs haven’t improved yet. So what do we do? We know that we are good engineers and we gained some confidence to go back to the drawing board once again.

## Slide 16 — RAG with semantic cache & chat history

Now we layer chat history on top of the semantic search from the previous iteration. The goal is still to cut LLM cost. When a question arrives, we first search a vector database of past question-and-answer pairs. If the similarity is above our threshold, we return the cached answer and skip the LLM entirely. Only on a miss do we fall back to the wiki RAG path: vector-search the wiki, build a rich prompt with the retrieved context, call the LLM, then vectorize and store the new Q+A pair in chat history before responding. The Chatbot App in this diagram orchestrates both the cache lookup and the retrieval path — we pay the LLM only when no past answer fits.

## Slide 17 — Benefits & challenges

Now let’s see how far we got with our third iteration. We achieved what we proposed in the first place: reducing the cost of the LLM requests. However, we have to carefully fine-tune the acceptable threshold of the previously asked questions to avoid returning partial responses. We also managed to improve the response time, as the fastest response could come from a database search. However, we might face another issue now: outdated responses. If the wiki data has changed in the meantime, previous responses might not be accurate anymore, so we would need some synchronization mechanisms to ensure the validity of the history data. Lastly, we introduced an additional layer of fault tolerance. In case the LLM model is unable to respond, we can rely solely on the history data stored in the vectorized chat history database. But as we have already seen, we increased the overall complexity of the solution even more, and I’ll stop with this third iteration because the fourth one will probably require two slides just for the drawing.

## Slide 18 — Agentic orchestration

With this fourth iteration, we shift from a proof-of-concept approach to something that actually resembles production architecture. We wire up all our designated systems to an orchestration agent using the MCP communication pattern Adi walked us through — and now the agent is the one deciding which tool to call and what to do with the results. Need to search chat history? Check the knowledge base via RAG? Fall back to web search if nothing else cuts it? The agent figures that out on its own. Around the model sits the harness from the beginning of the talk: conversation memory and an audit trail in a database, guardrails and budgets on every loop — and because every decision is now a tool call, we can finally observe it: tokens, cost, latency and cache hits, live in Grafana.

## Slide 19 — Benefits & challenges

We successfully addressed the main drawback of the previous iteration - partial responses and outdated data - by routing smarter and enriching context. The cost is response time: data has to travel through multiple components. The big modularity win is that we can add or extend a single tool without redeploying or impacting the rest of the system — but that demands an equally sophisticated infrastructure for continuous deployment and high availability. We also picked up additional fault tolerance via the web search tool: when other tools cannot answer, it kicks in. The trade-off is obvious from the diagram — the architecture is even more complex.

## Slide 20 — DEMO

Alright, enough theory. The best way to make all of this click is through actual code, so I'll hand it over to Adi — who will walk us through how it all works in practice, in the language we all know and love: Java. Everything runs locally: a Spring AI agent on a local LLM, four MCP servers, Postgres and Grafana. The run sheet with the questions is in docs/demo-script.md.

## Slide 21 — This presentation was made with AI

Now that we've seen a real-world AI agent in action, we can finally answer the question we started with. Yes — AI is here to stay. It's already becoming an integral part of how we build software, and that's only going to deepen as models get better at processing large amounts of data and acting on the results. And if you needed one last proof of that — this presentation was built with the help of AI.

## Slide 22 — Thank you

Thank you!
