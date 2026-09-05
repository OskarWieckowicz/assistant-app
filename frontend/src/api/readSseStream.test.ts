import { describe, expect, it } from "vitest";
import { readSseStream } from "./readSseStream";

const streamFromChunks = (
  chunks: Uint8Array[],
): ReadableStream<Uint8Array> =>
  new ReadableStream({
    start(controller) {
      chunks.forEach((chunk) => controller.enqueue(chunk));
      controller.close();
    },
  });

const collectSseData = async (
  stream: ReadableStream<Uint8Array>,
): Promise<{ data: string[]; received: boolean }> => {
  const data: string[] = [];
  const received = await readSseStream(stream, (chunk) => data.push(chunk));

  return { data, received };
};

describe("readSseStream", () => {
  const encoder = new TextEncoder();

  it("preserves spaces at the beginning of streamed chunks", async () => {
    const stream = streamFromChunks([
      encoder.encode("data:Cześć!\n\ndata: Jak\n\ndata: się masz?\n\n"),
    ]);

    const result = await collectSseData(stream);

    expect(result.received).toBe(true);
    expect(result.data.join("")).toBe("Cześć! Jak się masz?");
  });

  it("handles CRLF and UTF-8 characters split across network chunks", async () => {
    const bytes = encoder.encode("data:zażółć\r\n\r\ndata: gęślą\r\n\r\n");
    const firstMultibyteCharacter = encoder.encode("data:za").length;
    const separatorMiddle = encoder.encode("data:zażółć\r").length;
    const stream = streamFromChunks([
      bytes.slice(0, firstMultibyteCharacter + 1),
      bytes.slice(firstMultibyteCharacter + 1, separatorMiddle),
      bytes.slice(separatorMiddle, separatorMiddle + 2),
      bytes.slice(separatorMiddle + 2),
    ]);

    const result = await collectSseData(stream);

    expect(result.data).toEqual(["zażółć", " gęślą"]);
  });

  it("reports an empty stream", async () => {
    const result = await collectSseData(streamFromChunks([]));

    expect(result).toEqual({ data: [], received: false });
  });
});
