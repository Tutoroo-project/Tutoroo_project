/** @jsxImportSource @emotion/react */
import { css } from "@emotion/react";
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";

const wrap = css`
  width: 100%;
  height: 100%;
  min-width: 0;
  min-height: 0;
`;

const empty = css`
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
`;

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  const item = payload[0]?.payload;
  return (
    <div
      style={{
        background: "white",
        border: "1px solid #eee",
        borderRadius: 8,
        padding: "8px 10px",
        fontSize: 12,
      }}
    >
      <div style={{ fontWeight: 700, marginBottom: 4 }}>{label}</div>
      <div>점수: {item.score ?? 0}</div>
      <div>완료: {item.completed ? "O" : "X"}</div>
    </div>
  );
}

export default function StudyAchievementChart({ data }) {
  if (!data || data.length === 0) return <div css={empty}>표시할 데이터가 없어요</div>;

  const initialDimension = { width: 600, height: 200 };

  

  return (
    <div css={wrap}>
      <ResponsiveContainer width="100%" height="100%" initialDimension={initialDimension}>
        <BarChart data={data} barSize={18}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="label" />
          <YAxis domain={[0, 100]} allowDecimals={false} />
          <Tooltip content={<CustomTooltip />} />
          <Bar dataKey="score" fill="#FF8A3D" radius={[6, 6, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
