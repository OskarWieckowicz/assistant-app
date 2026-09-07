import { afterEach, expect, it, vi } from "vitest";
import { streamChat } from "./streamChat";

function respond(events: object[]) {
  const body = events.map(event => `data:${JSON.stringify(event)}\n\n`).join("");
  vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(body)));
}

afterEach(() => vi.unstubAllGlobals());

it("delivers events in order and completes on done", async () => {
  const events = [{ type: "answer_delta", text: "Hello" }, { type: "done" }];
  respond(events);
  const receive = vi.fn();
  await streamChat("Hi", receive);
  expect(receive.mock.calls.map(([event]) => event)).toEqual(events);
});

it("rejects a truncated stream after delivering the partial answer", async () => {
  respond([{ type: "answer_delta", text: "Partial answer" }]);
  const receive = vi.fn();
  await expect(streamChat("Hi", receive)).rejects.toThrow("Stream ended before completion");
  expect(receive).toHaveBeenCalledWith({ type: "answer_delta", text: "Partial answer" });
});

it("preserves a backend error as a terminal event", async () => {
  respond([{ type: "error", text: "Could not complete the response." }]);
  const receive = vi.fn();
  await streamChat("Hi", receive);
  expect(receive).toHaveBeenCalledWith({ type: "error", text: "Could not complete the response." });
});
