import { useState, type FormEvent } from "react";
import "./App.css";
import { readSseStream } from "./api/readSseStream";

type Message = {
  role: "user" | "assistant";
  content: string;
};

const appendAssistantChunk = (
  current: Message[],
  chunk: string,
): Message[] => {
  const last = current.at(-1);

  if (last?.role !== "assistant") {
    return [...current, { role: "assistant", content: chunk }];
  }

  return [
    ...current.slice(0, -1),
    { ...last, content: last.content + chunk },
  ];
};

const App = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageInput, setMessageInput] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = messageInput.trim();
    if (!text || pending) return;

    setMessages((current) => [...current, { role: "user", content: text }]);
    setMessageInput("");
    setPending(true);
    setError(null);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "text/event-stream",
        },
        body: JSON.stringify({ message: text }),
      });
      if (!response.ok || !response.body) throw new Error("Request failed");

      const receivedData = await readSseStream(response.body, (chunk) => {
        setMessages((current) => appendAssistantChunk(current, chunk));
      });

      if (!receivedData) setError("Received an empty response.");
    } catch {
      setError("Couldn't send the message.");
    } finally {
      setPending(false);
    }
  };

  const waitingForFirstChunk = pending && messages.at(-1)?.role === "user";

  return (
    <div className="app">
      <header className="app-header">
        <h1>AI Assistant</h1>
      </header>

      <div
        className="messages"
        role="log"
        aria-live="polite"
        aria-busy={pending}
      >
        {messages.length === 0 && (
          <p className="empty">Send a message to get started.</p>
        )}
        {messages.map((message, index) => (
          <div key={index} className={`bubble ${message.role}`}>
            <span className="role">
              {message.role === "user" ? "You" : "Assistant"}
            </span>
            {message.content && <p>{message.content}</p>}
          </div>
        ))}
        {waitingForFirstChunk && (
          <p className="status" role="status">
            Thinking…
          </p>
        )}
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
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
