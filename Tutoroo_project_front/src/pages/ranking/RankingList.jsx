/** @jsxImportSource @emotion/react */
import * as s from "./styles";

function RankingList({ rankingList, isLoading }) {
  return (
    <div css={s.rankListArea}>
      {isLoading ? (
        <div css={s.loadingText}>로딩 중...</div>
      ) : rankingList && rankingList.length > 0 ? (
        rankingList.map((user, index) => {
          // [수정 포인트] DTO 필드: rank
          const rank = user.rank; 

          return (
            <div key={index} css={s.rankCard(rank)}>
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

              <div css={s.userInfo}>
                {user.profileImage ? (
                  <img src={user.profileImage} css={s.userProfileImg} alt="profile" />
                ) : (
                  <div css={s.userIcon} />
                )}
                {/* [수정 포인트] DTO 필드: maskedName */}
                <span css={s.userName}>{user.maskedName}</span>
              </div>

              {/* [수정 포인트] DTO 필드: totalPoint */}
              <div css={s.pointText}>{user.totalPoint?.toLocaleString()} P</div>
            </div>
          );
        })
      ) : (
        <div css={s.loadingText}>랭킹 데이터가 없습니다.</div>
      )}
    </div>
  );
}

export default RankingList;