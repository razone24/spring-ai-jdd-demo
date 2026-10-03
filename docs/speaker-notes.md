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

Picture a scenario anyone who has helped run a conference knows well. It's the week before JDD, and the questions start pouring into the conference app's chat — and during the event they never stop. "Which talks cover virtual threads?" "Is there anything on Valhalla, and which room is it in?" "Does it clash with the event-driven architecture talk?" The answers exist, but they're scattered across the agenda, the speakers' abstracts and the attendee wiki. The organisers are busy running a conference for hundreds of people, and every reply means digging through all of it again. You're a developer and an AI enthusiast, so naturally your mind goes straight to automation. And conveniently, you just heard from us that AI agent creation is exactly where things are heading.

## Slide 12 — Naive solution

So you grab a pen and paper, or open a sketching app, and start designing the solution. You decide that a chatbot in the conference app will forward attendee questions through a gateway straight into your Chatbot App. The conference team keeps everything in a wiki — the full agenda, every speaker's abstract and bio, the venue and practical info — so you have enough data to answer almost any attendee question. You load all of it into memory to serve it to the model. Quick disclaimer — this is an oversimplified example. In reality, the wiki data would also go through a gateway, and loading it into memory on every request isn't ideal. But bear with us. Once you realize you just need a solid prompt, you bundle the whole wiki and the attendee's question together into the context, send the request, and forward the LLM's response back to the app.

## Slide 13 — Benefits & challenges

Now let's see what we have achieved so far. We managed to process a lot of data at once, so there is no need for us to search the agenda for every talk that mentions virtual threads. However, two days of talks, abstracts and bios might exceed the maximum context window of any available LLM. Creating this simple solution guarantees us a fast time to market, but every question is forwarded to the LLM. Naturally, the costs will explode the moment a few hundred attendees open the app during the morning coffee break. To get a focused answer, we'll use our prompt engineering skills. Unfortunately, with this amount of data — most of it unrelated to virtual threads — we'll get hallucinations: a talk in the wrong room, at the wrong time, or a session that doesn't exist at all.

## Slide 14 — RAG with semantic caching

We go back to the drawing board and remember that we heard from Adi about RAG systems. To obtain a better context for the prompt, we decide to implement semantic search. We take all the wiki pages — agenda, abstracts, speaker bios — and run them through a document vectorization application (there are many third-party apps capable of performing this task), then store the data in a vector database. Remember that this is an overly simplified example. If we wanted the database in sync with every agenda change — a talk moved to another room, a speaker who cancelled — we would need some kind of triggers to reprocess the data, but this is a discussion for another time. Having the data in a vector database lets us take the attendee's question and search by semantic similarity: for "talks on virtual threads?" we retrieve the virtual threads session and the abstracts closest in meaning, not the whole agenda. We then use the returned data alongside the question as context for the LLM, and return the response to the conference app.

## Slide 15 — Benefits & challenges

Let's see how we improved the system. We managed to enhance the prompts with focused and specific data, but to create the vectors we have to rely on third-party solutions, which might be expensive or inaccurate depending on the solution. We now retrieve only the agenda and wiki chunks related to the question, and quickly, thanks to the database's capabilities — but we introduced a new component in our system: an external storage. Because we only send the data related to the question, we rely less on the model's own knowledge and it hallucinates less — no more invented talks. But we complicated the architecture a bit, and the costs haven't improved yet. So what do we do? We know that we are good engineers, and we gained some confidence to go back to the drawing board once again.

## Slide 16 — RAG with semantic cache & chat history

Now we layer chat history on top of the semantic search from the previous iteration. The goal is still to cut LLM cost — and at a conference, questions come in waves: right after the keynote, dozens of people ask about the virtual threads talk in slightly different words. When a question arrives, we first search a vector database of past question-and-answer pairs. If the similarity is above our threshold, we return the cached answer and skip the LLM entirely. Only on a miss do we fall back to the wiki RAG path: vector-search the wiki, build a rich prompt with the retrieved context, call the LLM, then vectorize and store the new Q+A pair in chat history before responding. The Chatbot App in this diagram orchestrates both the cache lookup and the retrieval path — we pay the LLM only when no past answer fits.

## Slide 17 — Benefits & challenges

Now let's see how far we got with our third iteration. We achieved what we proposed in the first place: reducing the cost of the LLM requests. However, we have to carefully fine-tune the similarity threshold — "talks on virtual threads" and "talks on virtual machines" are close in wording but not in meaning — or we return partial or wrong answers. We also improved the response time, as the fastest response now comes from a database search. However, we might face another issue now: outdated responses. If a talk moves to another room or a speaker cancels, the cached answers are wrong, and an attendee walks into the wrong room — so we would need a synchronization mechanism to keep the history valid. Lastly, we introduced an additional layer of fault tolerance: if the LLM is unable to respond, we can rely solely on the history stored in the chat history database. But as we have already seen, we increased the overall complexity of the solution even more, and I'll stop with this third iteration because the fourth one will probably require two slides just for the drawing.

## Slide 18 — Agentic orchestration

With this fourth iteration, we shift from a proof-of-concept approach to something that actually resembles production architecture. We wire up all our designated systems to an orchestration agent using the MCP communication pattern Adi walked us through — and now the agent is the one deciding which tool to call and what to do with the results. A question about the agenda? It asks the conference info server, with the right filters. A speaker's background? It checks the knowledge base via RAG. "What's new in the latest Spring AI release?" isn't in our data at all, so it falls back to web search. The agent figures that out on its own. Around the model sits the harness from the beginning of the talk: conversation memory and an audit trail in a database, guardrails and budgets on every loop — and because every decision is now a tool call, we can finally observe it: tokens, cost, latency and cache hits, live in Grafana.

## Slide 19 — Benefits & challenges

Compared with the previous iteration, partial and outdated answers become much rarer. On a cache miss the agent goes to the live tools and combines them — schedule, wiki, web — and the harness only serves a cached answer above a strict similarity threshold and while it is still fresh. The modularity win is real: every capability is an MCP server, so we can add or swap one — or even swap the LLM — without touching the rest. And we get fault tolerance and visibility: web search as a fallback, graceful degradation when a server is down, and because every decision is now a tool call, we can finally trace it.

The price? Every answer now takes several round trips to the LLM, and every round re-sends the tool definitions and results — so an uncached answer is slower and more expensive than before. The model is also non-deterministic: it can pick the wrong tool, guess a filter, or answer from memory. That is exactly why we wrap it in a harness — budgets, guardrails, a grounding check. And there are more moving parts to deploy, monitor and keep available: MCP servers, databases, observability. You'll see every one of these in the demo.

## Slide 20 — DEMO

Alright, enough theory. The best way to make all of this click is through actual code, so I'll hand it over to Adi — who will walk us through how it all works in practice, in the language we all know and love: Java. Everything runs locally: a Spring AI agent on a local LLM, four MCP servers, Postgres and Grafana. The run sheet with the questions is in docs/demo-script.md.

## Slide 21 — This presentation was made with AI

Now that we've seen a real-world AI agent in action, we can finally answer the question we started with. Yes — AI is here to stay. It's already becoming an integral part of how we build software, and that's only going to deepen as models get better at processing large amounts of data and acting on the results. And if you needed one last proof of that — this presentation was built with the help of AI.

## Slide 22 — Thank you

Thank you!
