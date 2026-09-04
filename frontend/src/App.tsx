import { useState } from "react";
import "./App.css";

type Message = {
  role: "user" | "assistant";
  content: string;
};

type ChatResponse = {
  answer: string;
};

const App = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [messageInput, setMessageInput] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const text = messageInput.trim();
    if (!text || pending) return;

    setMessages((current) => [...current, { role: "user", content: text }]);
    setMessageInput("");
    setPending(true);
    setError(null);

    fetch("/api/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message: text }),
    })
      .then((response) => {
        if (!response.ok) throw new Error("Request failed");
        return response.json();
      })
      .then((data: ChatResponse) => {
        setMessages((current) => [
          ...current,
          { role: "assistant", content: data.answer },
        ]);
      })
      .catch(() => setError("Couldn't send the message."))
      .finally(() => setPending(false));
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>AI Assistant</h1>
      </header>

      <div className="messages">
        {messages.length === 0 && (
          <p className="empty">Send a message to get started.</p>
        )}
        {messages.map((message, index) => (
          <div key={index} className={`bubble ${message.role}`}>
            <span className="role">
              {message.role === "user" ? "You" : "Assistant"}
            </span>
            <p>{message.content}</p>
          </div>
        ))}
        {pending && <p className="status">Thinking…</p>}
        {error && <p className="error">{error}</p>}
      </div>

      <form className="composer" onSubmit={handleSubmit}>
        <input
          type="text"
          value={messageInput}
          onChange={(e) => setMessageInput(e.target.value)}
          placeholder="Message…"
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
