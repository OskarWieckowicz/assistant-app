import { parseChatEvent, type ChatEvent } from "./chatEvents";
import { readSseStream } from "./readSseStream";

export async function streamChat(message: string, onEvent: (event: ChatEvent) => void): Promise<void> {
  const response = await fetch("/api/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "text/event-stream" },
    body: JSON.stringify({ message }),
  });
  if (!response.ok || !response.body) throw new Error("Request failed");

  let completed = false;
  await readSseStream(response.body, data => {
    const event = parseChatEvent(data);
    if (completed) throw new Error("Event after completion");
    onEvent(event);
    completed = event.type === "done" || event.type === "error";
  });
  if (!completed) throw new Error("Stream ended before completion");
}
