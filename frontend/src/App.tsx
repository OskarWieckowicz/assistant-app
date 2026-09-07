import { useLayoutEffect, useRef, useState, type FormEvent } from "react";
import "./App.css";
import { streamChat } from "./api/streamChat";
import { MessageBubble } from "./components/MessageBubble";

import { applyChatEvent, type ChatEvent, type Message } from "./api/chatEvents";

const App = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageInput, setMessageInput] = useState("");
  const [pending, setPending] = useState(false);
  const messagesRef = useRef<HTMLDivElement>(null);
  const followOutput = useRef(true);

  useLayoutEffect(() => {
    const container = messagesRef.current;
    if (container && followOutput.current) {
      container.scrollTop = container.scrollHeight;
    }
  }, [messages]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = messageInput.trim();
    if (!text || pending) return;

    followOutput.current = true;
    const assistantIndex = messages.length + 1;
    setMessages((current) => [...current,
      { role: "user", content: text },
      { role: "assistant", content: "", thinking: "", tools: [], status: "running" },
    ]);
    const receive = (event: ChatEvent) => setMessages(current => current.map((message, index) =>
      index === assistantIndex ? applyChatEvent(message, event) : message));
    setMessageInput("");
    setPending(true);

    try {
      await streamChat(text, receive);
    } catch {
      receive({ type: "error", text: "The connection was interrupted. Please try again." });
    } finally {
      setPending(false);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>AI Assistant</h1>
      </header>

      <div
        ref={messagesRef}
        className="messages"
        onScroll={(event) => {
          const container = event.currentTarget;
          followOutput.current = container.scrollHeight - container.scrollTop - container.clientHeight < 48;
        }}
        role="log"
        aria-live="polite"
        aria-busy={pending}
      >
        {messages.length === 0 && (
          <p className="empty">Send a message to get started.</p>
        )}
        {messages.map((message, index) => (
          <MessageBubble key={index} message={message} />
        ))}
      </div>

      <form className="composer" onSubmit={handleSubmit}>
        <input
          type="text"
          value={messageInput}
          onChange={(e) => setMessageInput(e.target.value)}
          placeholder="Message…"
          aria-label="Message"
          disabled={pending}
        />
        <button type="submit" disabled={pending || !messageInput.trim()}>
          Send
        </button>
      </form>
    </div>
  );
};

export default App;
