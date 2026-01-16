/** @jsxImportSource @emotion/react */
import * as s from "./styles";

function RankingList({ rankingList, isLoading }) {
  return (
    <div css={s.rankListArea}>
      {isLoading ? (
        <div css={s.loadingText}>로딩 중...</div>
      ) : rankingList.length > 0 ? (
        rankingList.map((user, index) => {
          const rank = user.dailyRank || index + 1;

          return (
            <div key={user.id || index} css={s.rankCard(rank)}>
              {/* 순위 아이콘/텍스트 */}
              <div css={s.rankBadge(rank)}>
                {rank <= 3 ? (
                  <>
                    <span className="medal-icon">
                      {rank === 1 && "🥇"}
                      {rank === 2 && "🥈"}
                      {rank === 3 && "🥉"}
                    </span>
                    {rank}위
                  </>
                ) : (
                  <>{rank}위</>
                )}
              </div>

              {/* 프로필 & 이름 */}
              <div css={s.userInfo}>
                {user.profileImage ? (
                  <img src={user.profileImage} css={s.userProfileImg} alt="profile" />
                ) : (
                  <div css={s.userIcon} />
                )}
                <span css={s.userName}>{user.name || user.username}</span>
              </div>

              {/* 포인트 */}
              <div css={s.pointText}>{user.totalPoint?.toLocaleString()} P</div>
            </div>
          );
        })
      ) : (
        <div css={s.rankNullText}>랭킹 데이터가 없습니다.</div>
      )}
    </div>
  );
}

export default RankingList;