/** @jsxImportSource @emotion/react */
import * as s from "./styles";

function MyRankingCard({ myRanking }) {
  return (
    <aside css={s.myStatusArea}>
      <div css={s.statusCard}>
        <h3 css={s.cardTitle}>포인트 현황</h3>

        {myRanking ? (
          <div css={s.cardContent}>
            <span css={s.cardLabel}>누적 포인트 / 랭킹</span>
            <div css={s.bigPoint}>
              {myRanking.totalPoint?.toLocaleString() || 0} P
            </div>
            <div css={s.userInfo}>
              {myRanking.profileImage ? (
                <img src={myRanking.profileImage} css={s.userProfileImg} alt="my profile" />
              ) : (
                <div css={s.userIcon} />
              )}
              <span css={s.userName}>{myRanking.name}</span>
            </div>
          </div>
        ) : (
          <div css={s.isUnauthenticated}>
            로그인 정보가<br />없습니다.
          </div>
        )}
      </div>
    </aside>
  );
}

export default MyRankingCard;