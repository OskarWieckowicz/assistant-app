import { formatToolPayload, type ToolActivity } from "../api/chatEvents";

const statusLabels = {
  finished: "Completed",
  failed: "Failed",
};

function ToolResult({ tool }: { tool: ToolActivity }) {
  switch (tool.status) {
    case "failed":
      return <p className="error">{tool.error ?? "The tool could not complete the request."}</p>;
    case "finished":
      if (tool.output === undefined) {
        return <p className="status">The result is unavailable.</p>;
      }
      return <pre>{tool.output === "" ? "(Empty result)" : formatToolPayload(tool.output)}</pre>;
  }
}

export function ToolCall({ tool }: { tool: ToolActivity }) {
  return (
    <li data-status={tool.status}>
      <details className="tool-call">
        <summary>
          <span className="tool-name">{tool.name}</span>
          <span className="tool-status">{statusLabels[tool.status]}</span>
        </summary>
        <div className="tool-content">
          <h3>Arguments</h3>
          {tool.input === undefined
            ? <p className="status">Arguments are unavailable.</p>
            : <pre>{formatToolPayload(tool.input)}</pre>}
          <h3>{tool.status === "failed" ? "Error" : "Result"}</h3>
          <ToolResult tool={tool} />
        </div>
      </details>
    </li>
  );
}
