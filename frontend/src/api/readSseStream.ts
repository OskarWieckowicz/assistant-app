const SSE_EVENT_SEPARATOR = /\r\n\r\n|\n\n|\r\r/;

const dataFromSseBlock = (block: string): string | null => {
  const dataLines = block
    .split(/\r\n|\n|\r/)
    .filter((line) => line === "data" || line.startsWith("data:"))
    .map((line) => line.slice(5));

  if (dataLines.length === 0) return null;

  const data = dataLines.join("\n");
  return data || null;
};

export const readSseStream = async (
  body: ReadableStream<Uint8Array>,
  onData: (data: string) => void,
): Promise<void> => {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  const emit = (block: string) => {
    const data = dataFromSseBlock(block);
    if (data === null) return;

    onData(data);
  };

  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const blocks = buffer.split(SSE_EVENT_SEPARATOR);
      buffer = blocks.pop() ?? "";
      blocks.forEach(emit);
    }

    buffer += decoder.decode();
    if (buffer.trim()) emit(buffer);
  } finally {
    reader.releaseLock();
  }
};
