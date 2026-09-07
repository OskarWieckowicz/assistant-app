import { describe, expect, it } from "vitest";
import { formatToolPayload, applyChatEvent, parseChatEvent, type Message } from "./chatEvents";
import { readSseStream } from "./readSseStream";
const initial: Message = { role: "assistant", content: "", status: "running" };
describe("chat activity stream", () => {
  it("separates reasoning, repeated tool calls and answer across split SSE frames", async () => {
    const payload = [
      { type: "thinking_delta", text: "I will check Poland…" },
      { type: "tool_finished", callId: "1", name: "country", input: '{"countryName":"Poland"}', text: '{"capital":"Warsaw"}' },
      { type: "tool_failed", callId: "2", name: "country", input: '{"countryName":"Poland"}', text: "The tool failed." },
      { type: "answer_delta", text: "Warsaw" },
      { type: "answer_delta", text: " is the capital." },
      { type: "done" },
    ].map(event => `data:${JSON.stringify(event)}\n\n`).join("");
    const bytes = new TextEncoder().encode(payload);
    const stream = new ReadableStream<Uint8Array>({ start(controller) {
      for (const byte of bytes) controller.enqueue(new Uint8Array([byte]));
      controller.close();
    } });
    let message = initial;
    await readSseStream(stream, data => { message = applyChatEvent(message, parseChatEvent(data)); });
    expect(message.content).toBe("Warsaw is the capital.");
    expect(message.thinking).toBe("I will check Poland…");
    expect(message.tools?.map(tool => tool.status)).toEqual(["finished", "failed"]);
    expect(message.status).toBe("done");
    expect(message.tools?.[0].input).toBe('{"countryName":"Poland"}');
    expect(message.tools?.[0].output).toBe('{"capital":"Warsaw"}');
    expect(message.tools?.[1].error).toBe("The tool failed.");
    expect(message.tools?.[1].output).toBeUndefined();
  });
  it("preserves partial content and completed tools on interruption", () => {
    let message = applyChatEvent(initial, { type: "answer_delta", text: "Partial answer" });
    message = applyChatEvent(message, { type: "tool_finished", callId: "1", name: "country", input: "{}", text: "Warsaw" });
    message = applyChatEvent(message, { type: "error", text: "Interrupted" });
    expect(message.content).toBe("Partial answer");
    expect(message.tools?.[0].status).toBe("finished");
    expect(message.error).toBe("Interrupted");
  });
  it("reports no answer even when thinking was received", () => {
    const message = applyChatEvent(initial, { type: "thinking_delta", text: "Thinking…" });
    expect(applyChatEvent(message, { type: "done" }).error).toBeTruthy();
  });
  it("formats JSON and preserves plain text results", () => {
    expect(formatToolPayload('{"capital":"Warsaw"}')).toBe('{\n  "capital": "Warsaw"\n}');
    expect(formatToolPayload("Tool returned plain text")).toBe("Tool returned plain text");
  });
  it("rejects invalid event payloads", () => {
    for (const payload of ['null', '[]', '42', '{"type":"unknown"}',
      '{"type":"answer_delta","text":null}', '{"type":"tool_started"}',
      '{"type":"tool_finished","callId":"1","name":"country","text":{}}']) {
      expect(() => parseChatEvent(payload)).toThrow();
    }
  });
});
