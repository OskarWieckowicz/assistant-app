import type { Message } from "../api/chatEvents";
import Markdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { ToolCall } from "./ToolCall";

export function MessageBubble({ message }: { message: Message }) {
  const tools = message.tools ?? [];
  const running = message.status === "running";

  return (
    <div className={`bubble ${message.role}`}>
      <span className="role">{message.role === "user" ? "You" : "Assistant"}</span>
      {message.role === "assistant" && (
        <details
          className="activity"
          onToggle={(event) => {
            if (!event.currentTarget.open) return;
            const bubble = event.currentTarget.closest(".bubble");
            const container = event.currentTarget.closest(".messages");
            if (!(bubble instanceof HTMLElement) || !(container instanceof HTMLElement)) return;
            const overflow = bubble.getBoundingClientRect().bottom - container.getBoundingClientRect().bottom;
            if (overflow > 0) {
              container.scrollTop += overflow;
            }
          }}
        >
          <summary>
            {running ? "Working…" : "Activity"}
            {tools.length > 0 && ` · tools: ${tools.length}`}
          </summary>
          {message.thinking && <p className="thinking">{message.thinking}</p>}
          {tools.length > 0 && (
            <ul className="tool-list">
              {tools.map(tool => <ToolCall key={tool.callId} tool={tool} />)}
            </ul>
          )}
          {!message.thinking && tools.length === 0 && (
            <p className="status">
              {running ? "Waiting for model activity…" : "No additional activity."}
            </p>
          )}
        </details>
      )}
      {message.content && (message.role === "assistant"
        ? <div className="markdown"><Markdown remarkPlugins={[remarkGfm]}>{message.content}</Markdown></div>
        : <p>{message.content}</p>)}
      {message.error && <p className="error" role="alert">{message.error}</p>}
    </div>
  );
}
