export type ChatEvent =
  | { type: "thinking_delta" | "answer_delta"; text: string }
  | { type: "tool_finished" | "tool_failed"; callId: string; name: string; input: string; text?: string }
  | { type: "done" }
  | { type: "error"; text: string };

export type ToolActivity = {
  callId: string;
  name: string;
  input?: string;
  output?: string;
  error?: string;
  status: "finished" | "failed";
};

export type Message = {
  role: "user" | "assistant";
  content: string;
  thinking?: string;
  tools?: ToolActivity[];
  status?: "running" | "done" | "error";
  error?: string;
};

export const parseChatEvent = (data: string): ChatEvent => {
  const event: unknown = JSON.parse(data);
  if (!event || typeof event !== "object") throw new Error("Invalid chat event");
  const { type, text, callId, name, input } = event as Record<string, unknown>;

  switch (type) {
    case "done":
      return { type };
    case "thinking_delta":
    case "answer_delta":
    case "error":
      if (typeof text === "string") return { type, text };
      break;
    case "tool_finished":
    case "tool_failed":
      if (typeof callId === "string" && typeof name === "string" && typeof input === "string"
          && (text == null || typeof text === "string")) {
        return { type, callId, name, input, text: text ?? undefined };
      }
      break;
  }
  throw new Error("Invalid chat event");
};

export const applyChatEvent = (message: Message, event: ChatEvent): Message => {
  if (message.status !== "running") return message;
  switch (event.type) {
    case "thinking_delta":
      return { ...message, thinking: (message.thinking ?? "") + event.text };
    case "answer_delta":
      return { ...message, content: message.content + event.text };
    case "tool_finished":
    case "tool_failed":
      return { ...message, tools: [...(message.tools ?? []), {
        callId: event.callId, name: event.name, input: event.input,
        status: event.type === "tool_finished" ? "finished" : "failed",
        output: event.type === "tool_finished" ? event.text : undefined,
        error: event.type === "tool_failed" ? event.text : undefined,
      }] };
    case "done":
    case "error": {
      let error: string | undefined;
      if (event.type === "error") error = event.text;
      else if (!message.content.trim()) error = "The model returned no answer.";

      return {
        ...message,
        status: event.type === "done" ? "done" : "error",
        error,
      };
    }
  }
};

export const formatToolPayload = (text: string): string => {
  try {
    return JSON.stringify(JSON.parse(text), null, 2);
  } catch {
    return text;
  }
};
