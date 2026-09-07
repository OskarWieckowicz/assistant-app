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
): Promise<string[]> => {
  const data: string[] = [];
  await readSseStream(stream, (chunk) => data.push(chunk));

  return data;
};

describe("readSseStream", () => {
  const encoder = new TextEncoder();

  it("preserves spaces at the beginning of streamed chunks", async () => {
    const stream = streamFromChunks([
      encoder.encode("data:Hello!\n\ndata: How\n\ndata: are you?\n\n"),
    ]);

    const result = await collectSseData(stream);

    expect(result.join("")).toBe("Hello! How are you?");
  });

  it("handles CRLF and UTF-8 characters split across network chunks", async () => {
    const bytes = encoder.encode("data:Hello 🌍\r\n\r\ndata: world\r\n\r\n");
    const firstMultibyteCharacter = encoder.encode("data:Hello ").length;
    const separatorMiddle = encoder.encode("data:Hello 🌍\r").length;
    const stream = streamFromChunks([
      bytes.slice(0, firstMultibyteCharacter + 1),
      bytes.slice(firstMultibyteCharacter + 1, separatorMiddle),
      bytes.slice(separatorMiddle, separatorMiddle + 2),
      bytes.slice(separatorMiddle + 2),
    ]);

    const result = await collectSseData(stream);

    expect(result).toEqual(["Hello 🌍", " world"]);
  });

  it("reports an empty stream", async () => {
    const result = await collectSseData(streamFromChunks([]));

    expect(result).toEqual([]);
  });
});
