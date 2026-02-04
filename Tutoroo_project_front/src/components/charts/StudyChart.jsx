import React from 'react';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Area,
  AreaChart
} from 'recharts';

const StudyChart = ({ data = [] }) => {
  // 데이터가 없을 때 처리
  if (!data || data.length === 0) {
    return (
      <div style={{ 
        width: '100%', 
        height: '100%', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        color: '#999',
        fontSize: '14px'
      }}>
        학습 데이터가 없습니다
      </div>
    );
  }

  // 커스텀 툴팁
  const CustomTooltip = ({ active, payload }) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div style={{
          backgroundColor: '#fff',
          border: '1px solid #ddd',
          borderRadius: '8px',
          padding: '10px 12px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
        }}>
          <p style={{ margin: 0, fontSize: '13px', fontWeight: '600', color: '#333' }}>
            {data.label}
          </p>
          <p style={{ margin: '4px 0 0 0', fontSize: '12px', color: '#666' }}>
            점수: <span style={{ color: '#ff8a3d', fontWeight: '700' }}>{data.score}점</span>
          </p>
          {data.completed && (
            <p style={{ margin: '2px 0 0 0', fontSize: '11px', color: '#52c41a' }}>
              ✓ 완료
            </p>
          )}
        </div>
      );
    }
    return null;
  };

  // 완료된 데이터 점 스타일
  const CustomDot = (props) => {
    const { cx, cy, payload } = props;
    
    if (payload.completed) {
      return (
        <svg x={cx - 6} y={cy - 6} width={12} height={12}>
          <circle cx={6} cy={6} r={5} fill="#ff8a3d" stroke="#fff" strokeWidth={2} />
          <circle cx={6} cy={6} r={2} fill="#fff" />
        </svg>
      );
    }
    
    return (
      <circle cx={cx} cy={cy} r={4} fill="#ff8a3d" stroke="#fff" strokeWidth={2} />
    );
  };

  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart 
        data={data}
        margin={{ top: 10, right: 10, left: 0, bottom: 0 }}
      >
        {/* 그라데이션 정의 */}
        <defs>
          <linearGradient id="colorScore" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#ff8a3d" stopOpacity={0.3}/>
            <stop offset="95%" stopColor="#ff8a3d" stopOpacity={0.05}/>
          </linearGradient>
        </defs>

        {/* 격자 */}
        <CartesianGrid strokeDasharray="3 3" stroke="#e0e0e0" />

        {/* X축 */}
        <XAxis 
          dataKey="label" 
          tick={{ fill: '#666', fontSize: 12 }}
          axisLine={{ stroke: '#d9d9d9' }}
        />

        {/* Y축 */}
        <YAxis 
          domain={[0, 100]}
          tick={{ fill: '#666', fontSize: 12 }}
          axisLine={{ stroke: '#d9d9d9' }}
          label={{ 
            value: '점수', 
            angle: -90, 
            position: 'insideLeft',
            style: { fontSize: 12, fill: '#666' }
          }}
        />

        {/* 툴팁 */}
        <Tooltip content={<CustomTooltip />} />

        {/* 영역 */}
        <Area 
          type="monotone" 
          dataKey="score" 
          stroke="#ff8a3d" 
          strokeWidth={3}
          fillOpacity={1} 
          fill="url(#colorScore)"
        />

        {/* 선 */}
        <Line 
          type="monotone" 
          dataKey="score" 
          stroke="#ff8a3d" 
          strokeWidth={3}
          dot={<CustomDot />}
          activeDot={{ r: 7, fill: '#ff8a3d', stroke: '#fff', strokeWidth: 2 }}
          animationDuration={1000}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default StudyChart;
