/** @jsxImportSource @emotion/react */
import * as s from "./styles";

function MyRankingCard({ myRanking }) {
  return (
    <aside css={s.myStatusArea}>
      <div css={s.statusCard}>
        <h3 css={s.cardTitle}>내 랭킹 현황</h3>

        {myRanking ? (
          <div css={s.cardContent}>
            <span css={s.cardLabel}>현재 순위 / 보유 포인트</span>
            <div css={s.bigPoint}>
              {/* 랭킹: DashboardDTO.rank */}
              <span style={{ fontSize: '0.8em', marginRight: '8px' }}>
                {myRanking.rank ? `${myRanking.rank}위` : '-'}
              </span>
              {myRanking.currentPoint?.toLocaleString() || 0} P
            </div>
            
            <div css={s.userInfo}>
              
              <div css={s.userIcon} /> 
              <span css={s.userName}>{myRanking.name}</span>
            </div>
          </div>
        ) : (
          <div css={s.isUnauthenticated}>
            로그인이 필요한<br />서비스입니다.
          </div>
        )}
      </div>
    </aside>
  );
}

export default MyRankingCard;